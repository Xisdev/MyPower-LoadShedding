#include <WiFi.h>
#include <esp_now.h>
#include <esp_wifi.h>

// Definisi Slave ID (Ubah menjadi 3 untuk ESP3)
#define SLAVE_ID 2
#define RELAY_PIN 26 // Asumsi pin relay slave

// Variabel Daya Slave (Nantinya diganti dengan pembacaan sensor sebenarnya)
float my_power = 120.0; 

// Struktur Data ESP-NOW untuk Mengirim Daya ke Master
typedef struct struct_message {
    int id;
    float power;
} struct_message;
struct_message myData;

// Struktur Data ESP-NOW untuk Menerima Perintah Relay dari Master
typedef struct command_message {
    bool relay_state;
} command_message;

// MAC Address dari Master Node (HARUS DISESUAIKAN DENGAN MAC ASLI MASTER)
uint8_t masterAddress[] = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};

// Variabel untuk Fitur Channel Scanner (Auto-Recovery)
int fail_count = 0;
bool scanning = false;
int current_channel = 1;
unsigned long lastScanTime = 0;

// Callback ketika data selesai dikirim via ESP-NOW
void OnDataSent(const uint8_t *mac_addr, esp_now_send_status_t status) {
    if (status == ESP_NOW_SEND_SUCCESS) {
        fail_count = 0;
        scanning = false; // Pengiriman sukses, menandakan channel sudah sinkron
    } else {
        fail_count++;
        Serial.println("Gagal mengirim data ke Master.");
        if (fail_count >= 3) {
            scanning = true; // Masuk ke mode scanning setelah 3 kali gagal
            Serial.println("Masuk ke mode Channel Scanner (Auto-Recovery)...");
        }
    }
}

// Callback ketika menerima instruksi relay dari Master
void OnDataRecv(const uint8_t * mac, const uint8_t *incomingData, int len) {
    if (len == sizeof(command_message)) {
        command_message cmd;
        memcpy(&cmd, incomingData, sizeof(cmd));
        if (cmd.relay_state) {
            digitalWrite(RELAY_PIN, LOW); // ON (Aktif LOW)
            Serial.println("Perintah Master: Relay ON");
        } else {
            digitalWrite(RELAY_PIN, HIGH); // OFF
            Serial.println("Perintah Master: Relay OFF");
        }
    }
}

void setup() {
    Serial.begin(115200);
    
    pinMode(RELAY_PIN, OUTPUT);
    digitalWrite(RELAY_PIN, LOW); // Default ON
    
    // ESP-NOW mewajibkan WiFi station diaktifkan, namun tidak terhubung ke AP
    WiFi.mode(WIFI_STA);
    WiFi.disconnect();
    
    if (esp_now_init() != ESP_OK) {
        Serial.println("Error inisialisasi ESP-NOW");
        return;
    }
    
    esp_now_register_send_cb(OnDataSent);
    esp_now_register_recv_cb(OnDataRecv);
    
    // Set Peer Info Master
    esp_now_peer_info_t peerInfo;
    memset(&peerInfo, 0, sizeof(peerInfo));
    memcpy(peerInfo.peer_addr, masterAddress, 6);
    peerInfo.channel = current_channel;  
    peerInfo.encrypt = false;
    
    if (esp_now_add_peer(&peerInfo) != ESP_OK){
        Serial.println("Gagal menambahkan Master sebagai peer");
    }
}

void loop() {
    static unsigned long lastSendTime = 0;
    
    // Algoritma Channel Scanner (Auto-Recovery)
    if (scanning) {
        // Melakukan lompatan channel (Hopping) setiap 200 milidetik
        if (millis() - lastScanTime > 200) { 
            current_channel++;
            if (current_channel > 13) {
                current_channel = 1;
            }
            
            // Ubah channel WiFi ESP32
            esp_wifi_set_promiscuous(true);
            esp_wifi_set_channel(current_channel, WIFI_SECOND_CHAN_NONE);
            esp_wifi_set_promiscuous(false);
            
            // Update channel pada konfigurasi Peer ESP-NOW
            esp_now_peer_info_t peerInfo;
            memset(&peerInfo, 0, sizeof(peerInfo));
            memcpy(peerInfo.peer_addr, masterAddress, 6);
            peerInfo.channel = current_channel;
            esp_now_mod_peer(&peerInfo);
            
            Serial.print("Mencari Master di Channel: ");
            Serial.println(current_channel);
            lastScanTime = millis();
            
            // Kirim ping secara berulang untuk mencoba menemukan Master
            myData.id = SLAVE_ID;
            myData.power = my_power;
            esp_now_send(masterAddress, (uint8_t *) &myData, sizeof(myData));
        }
    } else {
        // Mode Normal: Mengirim data ke Master setiap 1 detik
        if (millis() - lastSendTime > 1000) {
            myData.id = SLAVE_ID;
            myData.power = my_power; // Pada implementasi nyata, baca dari sensor lokal
            
            esp_now_send(masterAddress, (uint8_t *) &myData, sizeof(myData));
            lastSendTime = millis();
        }
    }
}
