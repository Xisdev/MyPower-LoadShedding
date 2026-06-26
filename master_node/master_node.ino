#define BLYNK_TEMPLATE_ID "YOUR_TEMPLATE_ID"
#define BLYNK_TEMPLATE_NAME "YOUR_TEMPLATE_NAME"
#define BLYNK_AUTH_TOKEN "YOUR_AUTH_TOKEN"

#include <WiFi.h>
#include <esp_now.h>
#include <esp_wifi.h>
#include <BlynkSimpleEsp32.h>
#include <EmonLib.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

char auth[] = BLYNK_AUTH_TOKEN;
char ssid[] = "YOUR_WIFI_SSID";
char pass[] = "YOUR_WIFI_PASSWORD";

// Definisi Pin (Sesuai spesifikasi mutlak)
#define V_PIN 33
#define I_PIN 32
#define RELAY_PIN 26 // Aktif LOW

LiquidCrystal_I2C lcd(0x27, 20, 4);
EnergyMonitor emon1;

// Inisialisasi BlynkTimer untuk proses Non-Blocking
BlynkTimer timer;

// Variabel Global
float power_p1 = 0;
float power_p2 = 0; // Data dari ESP2
float power_p3 = 0; // Data dari ESP3
float total_power = 0;
float voltage = 0;
float current = 0;
bool isAutoMode = true; // V10 (0 = Auto, 1 = Manual)

// Struktur Data ESP-NOW untuk Menerima Daya dari Slave
typedef struct struct_message {
    int id; // 2 untuk ESP2, 3 untuk ESP3
    float power;
} struct_message;
struct_message incomingReadings;

// Struktur Data ESP-NOW untuk Mengirim Perintah ke Slave
typedef struct command_message {
    bool relay_state; // true = ON, false = OFF
} command_message;

// MAC Address dari Slave Nodes (HARUS DISESUAIKAN DENGAN MAC ASLI SLAVE)
uint8_t slave2Address[] = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};
uint8_t slave3Address[] = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};

bool slave2_relay_state = true; // State terakhir
bool slave3_relay_state = true; // State terakhir

// Callback ketika menerima data dari Slave
void OnDataRecv(const uint8_t * mac, const uint8_t *incomingData, int len) {
    if (len == sizeof(struct_message)) {
        memcpy(&incomingReadings, incomingData, sizeof(incomingReadings));
        if (incomingReadings.id == 2) {
            power_p2 = incomingReadings.power;
        } else if (incomingReadings.id == 3) {
            power_p3 = incomingReadings.power;
        }
    }
}

// Timer Task 1: Baca Sensor Lokal
void readSensors() {
    // Parameter kalibrasi tegangan dan arus harus disesuaikan dengan hardware Anda
    emon1.calcVI(20, 2000); 
    voltage = emon1.Vrms;
    current = emon1.Irms;
    power_p1 = emon1.realPower; // atau (voltage * current) jika ingin apparent power
}

// Timer Task 2: Logika Load Shedding
void loadShedding() {
    total_power = power_p1 + power_p2 + power_p3;
    
    // Logika Proteksi Overload Master (Priority 1)
    if (power_p1 > 440) {
        if (digitalRead(RELAY_PIN) == LOW) { // Jika sedang ON
            digitalWrite(RELAY_PIN, HIGH); // Matikan Relay P1 (Aktif LOW)
            Blynk.virtualWrite(V7, 0); // Sinkronisasi UI App
        }
    } else if (isAutoMode) {
        if (digitalRead(RELAY_PIN) == HIGH) {
            digitalWrite(RELAY_PIN, LOW); // Hidupkan Relay P1
            Blynk.virtualWrite(V7, 1);
        }
    }
    
    if (isAutoMode) {
        bool target_s2 = true;
        bool target_s3 = true;
        
        // Logika Load Shedding Total Sistem (Sesuai dengan Tabel Analisa Data Pengujian)
        if (total_power <= 350) {
            target_s2 = true;  // ESP2 ON
            target_s3 = true;  // ESP3 ON
        } else if (total_power > 350 && total_power <= 400) {
            target_s2 = true;  // ESP2 ON
            target_s3 = false; // ESP3 OFF
        } else if (total_power > 400) {
            target_s2 = false; // ESP2 OFF
            target_s3 = false; // ESP3 OFF
        }
        
        // Kirim perintah via ESP-NOW ke Slave 2 jika ada perubahan
        if (target_s2 != slave2_relay_state) {
            slave2_relay_state = target_s2;
            command_message cmd = {slave2_relay_state};
            esp_now_send(slave2Address, (uint8_t *) &cmd, sizeof(cmd));
            Blynk.virtualWrite(V8, target_s2 ? 1 : 0); // Update status UI di App
        }
        
        // Kirim perintah via ESP-NOW ke Slave 3 jika ada perubahan
        if (target_s3 != slave3_relay_state) {
            slave3_relay_state = target_s3;
            command_message cmd = {slave3_relay_state};
            esp_now_send(slave3Address, (uint8_t *) &cmd, sizeof(cmd));
            Blynk.virtualWrite(V9, target_s3 ? 1 : 0); // Update status UI di App
        }
    }
}

// BLYNK_WRITE Handlers untuk menerima perintah Manual dari Aplikasi Android
BLYNK_CONNECTED() {
    Blynk.syncVirtual(V7, V8, V9, V10);
}

BLYNK_WRITE(V10) {
    isAutoMode = (param.asInt() == 0); // 0 = Auto, 1 = Manual
}

BLYNK_WRITE(V7) {
    if (!isAutoMode) {
        bool target = (param.asInt() == 1);
        digitalWrite(RELAY_PIN, target ? LOW : HIGH); // Aktif LOW
    }
}

BLYNK_WRITE(V8) {
    if (!isAutoMode) {
        bool target = (param.asInt() == 1);
        if (target != slave2_relay_state) {
            slave2_relay_state = target;
            command_message cmd = {slave2_relay_state};
            esp_now_send(slave2Address, (uint8_t *) &cmd, sizeof(cmd));
        }
    }
}

BLYNK_WRITE(V9) {
    if (!isAutoMode) {
        bool target = (param.asInt() == 1);
        if (target != slave3_relay_state) {
            slave3_relay_state = target;
            command_message cmd = {slave3_relay_state};
            esp_now_send(slave3Address, (uint8_t *) &cmd, sizeof(cmd));
        }
    }
}

// Timer Task 3: Update LCD
void updateLCD() {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("V:"); lcd.print(voltage, 1); lcd.print(" I:"); lcd.print(current, 2);
    lcd.setCursor(0, 1);
    lcd.print("P1:"); lcd.print(power_p1, 1); lcd.print("W");
    lcd.setCursor(0, 2);
    lcd.print("P2:"); lcd.print(power_p2, 1); lcd.print(" P3:"); lcd.print(power_p3, 1);
    lcd.setCursor(0, 3);
    lcd.print("Total: "); lcd.print(total_power, 1); lcd.print("W");
}

// Variabel Global untuk Filter Blynk (Menghemat Kuota Message)
float last_voltage = -1;
float last_current = -1;
float last_power_p1 = -1;
float last_power_p2 = -1;
float last_power_p3 = -1;
float last_total = -1;

// Timer Task 4: Kirim Data ke Blynk (Optimasi)
void sendBlynk() {
    // Hanya kirim pesan jika ada perubahan nilai signifikan (Deadband filter)
    if (fabs(voltage - last_voltage) >= 1.0) { Blynk.virtualWrite(V1, voltage); last_voltage = voltage; }
    if (fabs(current - last_current) >= 0.05) { Blynk.virtualWrite(V2, current); last_current = current; }
    if (fabs(power_p1 - last_power_p1) >= 2.0) { Blynk.virtualWrite(V3, power_p1); last_power_p1 = power_p1; }
    if (fabs(power_p2 - last_power_p2) >= 2.0) { Blynk.virtualWrite(V4, power_p2); last_power_p2 = power_p2; }
    if (fabs(power_p3 - last_power_p3) >= 2.0) { Blynk.virtualWrite(V5, power_p3); last_power_p3 = power_p3; }
    if (fabs(total_power - last_total) >= 2.0) { Blynk.virtualWrite(V6, total_power); last_total = total_power; }
}

void setup() {
    Serial.begin(115200);
    
    // Inisialisasi Relay P1
    pinMode(RELAY_PIN, OUTPUT);
    digitalWrite(RELAY_PIN, LOW); // Default ON
    
    // Inisialisasi LCD
    lcd.init();
    lcd.backlight();
    
    // Inisialisasi Kalibrasi EmonLib
    emon1.voltage(V_PIN, 234.26, 1.7);
    emon1.current(I_PIN, 111.1);
    
    // Mode WiFi Station untuk Konek Router & Blynk
    WiFi.mode(WIFI_STA);
    WiFi.begin(ssid, pass);
    
    lcd.setCursor(0, 0);
    lcd.print("Connecting WiFi...");
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\nWiFi Connected.");
    
    // Koneksi ke Blynk
    Blynk.config(auth);
    Blynk.connect();
    
    // Inisialisasi ESP-NOW
    if (esp_now_init() != ESP_OK) {
        Serial.println("Error inisialisasi ESP-NOW");
        return;
    }
    esp_now_register_recv_cb(OnDataRecv);
    
    // FITUR KHUSUS: Sinkronisasi WiFi Channel Router ke ESP-NOW
    int32_t channel = WiFi.channel();
    
    // Set Peer Info
    esp_now_peer_info_t peerInfo;
    memset(&peerInfo, 0, sizeof(peerInfo));
    peerInfo.channel = channel;  
    peerInfo.encrypt = false;
    
    // Tambah ESP2 sebagai Peer
    memcpy(peerInfo.peer_addr, slave2Address, 6);
    if (esp_now_add_peer(&peerInfo) != ESP_OK) Serial.println("Gagal add ESP2");
    
    // Tambah ESP3 sebagai Peer
    memcpy(peerInfo.peer_addr, slave3Address, 6);
    if (esp_now_add_peer(&peerInfo) != ESP_OK) Serial.println("Gagal add ESP3");
    
    Serial.print("Master ESP-NOW Berjalan di Channel: ");
    Serial.println(channel);

    // Registrasi Jadwal Multi-Tasking Non-Blocking
    timer.setInterval(1000L, readSensors);  // Baca sensor tiap 1 detik
    timer.setInterval(1200L, loadShedding); // Eksekusi load shedding tiap 1.2 detik
    timer.setInterval(2000L, updateLCD);    // Update LCD tiap 2 detik
    timer.setInterval(3000L, sendBlynk);    // Kirim ke server Blynk tiap 3 detik
}

void loop() {
    // Sangat bersih, tanpa delay sama sekali
    Blynk.run();
    timer.run();
}
