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

#define V_PIN 33
#define I_PIN 32
#define RELAY_PIN 26 // Aktif LOW

LiquidCrystal_I2C lcd(0x27, 20, 4);
EnergyMonitor emon1;
BlynkTimer timer;

// Faktor Kalibrasi Master Node
float calibrationVoltage = 0.585;
float calibrationCurrent = 0.022;

// Filter Master Node
float noiseCurrent = 0.15;
float noiseVoltage = 5.0;

// Variabel Global
float power_p1 = 0;
float power_p2 = 0; // Data dari ESP2
float power_p3 = 0; // Data dari ESP3
float total_power = 0;
float voltage = 0;
float current = 0;
bool isAutoMode = true; // V10 (0 = Auto, 1 = Manual)

typedef struct struct_message {
    int id; // 2 untuk ESP2, 3 untuk ESP3
    float voltage;
    float current;
    float power;
} struct_message;
struct_message incomingReadings;

typedef struct command_message {
    bool relay_state; // true = ON, false = OFF
} command_message;

// MAC Address Slave Nodes (Sesuaikan dengan perangkat Anda)
uint8_t slave2Address[] = {0xAA, 0xBB, 0xCC, 0xDD, 0xEE, 0x22}; // MAC Address ESP32 Slave 2
uint8_t slave3Address[] = {0xAA, 0xBB, 0xCC, 0xDD, 0xEE, 0x33}; // MAC Address ESP32 Slave 3
bool slave2_relay_state = true;
bool slave3_relay_state = true;

float voltage_p2 = 0;
float current_p2 = 0;
float voltage_p3 = 0;
float current_p3 = 0;

void OnDataRecv(const uint8_t * mac, const uint8_t *incomingData, int len) {
    if (len == sizeof(struct_message)) {
        memcpy(&incomingReadings, incomingData, sizeof(incomingReadings));
        if (incomingReadings.id == 2) {
            voltage_p2 = incomingReadings.voltage;
            current_p2 = incomingReadings.current;
            power_p2 = incomingReadings.power;
        } else if (incomingReadings.id == 3) {
            voltage_p3 = incomingReadings.voltage;
            current_p3 = incomingReadings.current;
            power_p3 = incomingReadings.power;
        }
    }
}

void readSensors() {
    emon1.calcVI(20, 2000); 
    voltage = emon1.Vrms;
    current = emon1.Irms;
    
    // Kalibrasi
    voltage = voltage * calibrationVoltage;
    voltage = round(voltage);
    current = current * calibrationCurrent;
    
    // Filter Noise
    if (voltage < noiseVoltage) {
        voltage = 0;
    }
    if (current < noiseCurrent) {
        current = 0;
    }
    
    power_p1 = voltage * current;
}

void loadShedding() {
    total_power = power_p1 + power_p2 + power_p3;
    
    if (power_p1 > 440) {
        if (digitalRead(RELAY_PIN) == LOW) { 
            digitalWrite(RELAY_PIN, HIGH); 
            Blynk.virtualWrite(V7, 0); 
        }
    } else if (isAutoMode) {
        if (digitalRead(RELAY_PIN) == HIGH) {
            digitalWrite(RELAY_PIN, LOW);
            Blynk.virtualWrite(V7, 1);
        }
    }
    
    if (isAutoMode) {
        bool target_s2 = true;
        bool target_s3 = true;
        
        if (total_power <= 350) {
            target_s2 = true;  
            target_s3 = true;  
        } else if (total_power > 350 && total_power <= 400) {
            target_s2 = true;  
            target_s3 = false; 
        } else if (total_power > 400) {
            target_s2 = false; 
            target_s3 = false; 
        }
        
        if (target_s2 != slave2_relay_state) {
            slave2_relay_state = target_s2;
            command_message cmd = {slave2_relay_state};
            esp_now_send(slave2Address, (uint8_t *) &cmd, sizeof(cmd));
            Blynk.virtualWrite(V8, target_s2 ? 1 : 0); 
        }
        
        if (target_s3 != slave3_relay_state) {
            slave3_relay_state = target_s3;
            command_message cmd = {slave3_relay_state};
            esp_now_send(slave3Address, (uint8_t *) &cmd, sizeof(cmd));
            Blynk.virtualWrite(V9, target_s3 ? 1 : 0); 
        }
    }
}

BLYNK_CONNECTED() {
    Blynk.syncVirtual(V7, V8, V9, V10);
}

BLYNK_WRITE(V10) {
    isAutoMode = (param.asInt() == 0); 
}

BLYNK_WRITE(V7) {
    if (!isAutoMode) {
        bool target = (param.asInt() == 1);
        digitalWrite(RELAY_PIN, target ? LOW : HIGH); 
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

void updateLCD() {
    lcd.clear();
    
    lcd.setCursor(0, 0);
    lcd.print("P1:");
    lcd.print(power_p1, 1);
    lcd.print("W");
    
    lcd.setCursor(0, 1);
    lcd.print("P2:");
    lcd.print(power_p2, 1);
    lcd.print("W");
    
    lcd.setCursor(0, 2);
    lcd.print("P3:");
    lcd.print(power_p3, 1);
    lcd.print("W");
    
    lcd.setCursor(0, 3);
    lcd.print("TOTAL:");
    lcd.print(total_power, 1);
    lcd.print("W");
}

void printSerial() {
    Serial.println("====================================");
    Serial.println("ESP32 PRIORITAS 1");
    Serial.println("------------ P1 ------------");
    Serial.print("Voltage P1 : "); Serial.print(voltage); Serial.println(" V");
    Serial.print("Current P1 : "); Serial.print(current); Serial.println(" A");
    Serial.print("Power P1   : "); Serial.print(power_p1); Serial.println(" W");
    Serial.println();
    
    Serial.println("------------ P2 ------------");
    Serial.print("Voltage P2 : "); Serial.print(voltage_p2); Serial.println(" V");
    Serial.print("Current P2 : "); Serial.print(current_p2); Serial.println(" A");
    Serial.print("Power P2   : "); Serial.print(power_p2); Serial.println(" W");
    Serial.println();
    
    Serial.println("------------ P3 ------------");
    Serial.print("Voltage P3 : "); Serial.print(voltage_p3); Serial.println(" V");
    Serial.print("Current P3 : "); Serial.print(current_p3); Serial.println(" A");
    Serial.print("Power P3   : "); Serial.print(power_p3); Serial.println(" W");
    Serial.println();
    
    Serial.println("--------- TOTAL DAYA ---------");
    Serial.print("TOTAL POWER : "); Serial.print(total_power); Serial.println(" W");
    Serial.println("====================================");
}

float last_voltage = -999;
float last_current = -999;
float last_voltage_p2 = -999;
float last_current_p2 = -999;
float last_voltage_p3 = -999;
float last_current_p3 = -999;
float last_power_p1 = -999;
float last_power_p2 = -999;
float last_power_p3 = -999;
float last_total = -999;

void sendBlynkFast() {
    // MULTIPLEXING DATA KE V0 UNTUK ANDROID APP (1 DETIK DELAY)
    String payload = String(voltage) + "," + String(current) + "," + 
                     String(voltage_p2) + "," + String(current_p2) + "," + 
                     String(voltage_p3) + "," + String(current_p3) + "," + 
                     String(power_p1) + "," + String(power_p2) + "," + 
                     String(power_p3) + "," + String(total_power) + "," + 
                     String(digitalRead(RELAY_PIN) == LOW ? 1 : 0) + "," + 
                     String(slave2_relay_state ? 1 : 0) + "," + 
                     String(slave3_relay_state ? 1 : 0) + "," + 
                     String(isAutoMode ? 0 : 1);
    Blynk.virtualWrite(V0, payload);

    // BLYNK IOT WORKAROUND: Paksa tarik data (sync) dari server HTTP
    // Karena update via HTTP REST API di Android kadang tidak memicu BLYNK_WRITE otomatis
    Blynk.syncVirtual(V7, V8, V9, V10);
}

void sendBlynkSlow() {
    // DEADBAND FILTER UNTUK BLYNK WEB CONSOLE (4 DETIK DELAY AGAR TIDAK RATE LIMIT)
    if (fabs(voltage - last_voltage) >= 1.0) { Blynk.virtualWrite(V1, voltage); last_voltage = voltage; }
    if (fabs(current - last_current) >= 0.05) { Blynk.virtualWrite(V2, current); last_current = current; }
    
    if (fabs(voltage_p2 - last_voltage_p2) >= 1.0) { Blynk.virtualWrite(V11, voltage_p2); last_voltage_p2 = voltage_p2; }
    if (fabs(current_p2 - last_current_p2) >= 0.05) { Blynk.virtualWrite(V12, current_p2); last_current_p2 = current_p2; }
    
    if (fabs(voltage_p3 - last_voltage_p3) >= 1.0) { Blynk.virtualWrite(V13, voltage_p3); last_voltage_p3 = voltage_p3; }
    if (fabs(current_p3 - last_current_p3) >= 0.05) { Blynk.virtualWrite(V14, current_p3); last_current_p3 = current_p3; }
    
    if (fabs(power_p1 - last_power_p1) >= 1.0) { Blynk.virtualWrite(V3, power_p1); last_power_p1 = power_p1; }
    if (fabs(power_p2 - last_power_p2) >= 1.0) { Blynk.virtualWrite(V4, power_p2); last_power_p2 = power_p2; }
    if (fabs(power_p3 - last_power_p3) >= 1.0) { Blynk.virtualWrite(V5, power_p3); last_power_p3 = power_p3; }
    if (fabs(total_power - last_total) >= 1.0) { Blynk.virtualWrite(V6, total_power); last_total = total_power; }
}

void setup() {
    Serial.begin(115200);
    
    pinMode(RELAY_PIN, OUTPUT);
    digitalWrite(RELAY_PIN, LOW); // Default ON
    
    lcd.init();
    lcd.backlight();
    
    emon1.voltage(V_PIN, 220.0, 1.7);
    emon1.current(I_PIN, 111.1);
    
    WiFi.mode(WIFI_STA);
    WiFi.begin(ssid, pass);
    
    lcd.setCursor(0, 0);
    lcd.print("Connecting WiFi...");
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println("\nWiFi Connected.");
    
    Blynk.config(auth, "blynk.cloud", 80);
    Blynk.connect();
    
    if (esp_now_init() != ESP_OK) {
        Serial.println("Error inisialisasi ESP-NOW");
        return;
    }
    esp_now_register_recv_cb(OnDataRecv);
    
    int32_t channel = WiFi.channel();
    
    esp_now_peer_info_t peerInfo;
    memset(&peerInfo, 0, sizeof(peerInfo));
    peerInfo.channel = channel;  
    peerInfo.encrypt = false;
    
    memcpy(peerInfo.peer_addr, slave2Address, 6);
    if (esp_now_add_peer(&peerInfo) != ESP_OK) Serial.println("Gagal add ESP2");
    
    memcpy(peerInfo.peer_addr, slave3Address, 6);
    if (esp_now_add_peer(&peerInfo) != ESP_OK) Serial.println("Gagal add ESP3");
    
    Serial.print("Master ESP-NOW Berjalan di Channel: ");
    Serial.println(channel);

    timer.setInterval(1000L, readSensors);  
    timer.setInterval(1200L, loadShedding); 
    timer.setInterval(2000L, updateLCD);    // Update LCD tiap 2 detik
    timer.setInterval(2000L, printSerial);  // Tampilkan di Serial Monitor
    timer.setInterval(1000L, sendBlynkFast); // V0 tiap 1 detik
    timer.setInterval(4000L, sendBlynkSlow); // V1-V14 tiap 4 detik (mencegah API rate limit)
}

void loop() {
    Blynk.run();
    timer.run();
}
