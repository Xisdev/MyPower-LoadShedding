#include <WiFi.h>
#include <esp_now.h>
#include <esp_wifi.h>
#include <EmonLib.h>

#define SLAVE_ID 2
#define RELAY_PIN 26
#define PIN_VOLTAGE 33
#define PIN_CURRENT 32

EnergyMonitor emon;

// Faktor Kalibrasi Slave 2
float calibrationVoltage = 0.550;
float calibrationCurrent = 0.022;

// Filter Slave 2
float noiseCurrent = 0.10;
float noiseVoltage = 5.0;

typedef struct struct_message {
    int id;
    float voltage;
    float current;
    float power;
} struct_message;
struct_message myData;

typedef struct command_message {
    bool relay_state;
} command_message;

// MAC Address Master Node
uint8_t masterAddress[] = {0xAA, 0xBB, 0xCC, 0xDD, 0xEE, 0x11};

int fail_count = 0;
bool scanning = false;
int current_channel = 1;
unsigned long lastScanTime = 0;

void OnDataSent(const uint8_t *mac_addr, esp_now_send_status_t status) {
    if (status == ESP_NOW_SEND_SUCCESS) {
        fail_count = 0;
        scanning = false;
    } else {
        fail_count++;
        if (fail_count >= 3) {
            scanning = true;
        }
    }
}

void OnDataRecv(const uint8_t * mac, const uint8_t *incomingData, int len) {
    if (len == sizeof(command_message)) {
        command_message cmd;
        memcpy(&cmd, incomingData, sizeof(cmd));
        if (cmd.relay_state) {
            digitalWrite(RELAY_PIN, LOW); // ON (Aktif LOW)
        } else {
            digitalWrite(RELAY_PIN, HIGH); // OFF
        }
    }
}

void setup() {
    Serial.begin(115200);
    
    pinMode(RELAY_PIN, OUTPUT);
    digitalWrite(RELAY_PIN, LOW); // Default ON
    
    emon.current(PIN_CURRENT, 111.1);
    emon.voltage(PIN_VOLTAGE, 220.0, 1.7);
    
    WiFi.mode(WIFI_STA);
    WiFi.disconnect();
    
    if (esp_now_init() != ESP_OK) {
        Serial.println("Error inisialisasi ESP-NOW");
        return;
    }
    
    esp_now_register_send_cb(OnDataSent);
    esp_now_register_recv_cb(OnDataRecv);
    
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
    
    if (scanning) {
        if (millis() - lastScanTime > 200) { 
            current_channel++;
            if (current_channel > 13) current_channel = 1;
            
            esp_wifi_set_promiscuous(true);
            esp_wifi_set_channel(current_channel, WIFI_SECOND_CHAN_NONE);
            esp_wifi_set_promiscuous(false);
            
            esp_now_peer_info_t peerInfo;
            memset(&peerInfo, 0, sizeof(peerInfo));
            memcpy(peerInfo.peer_addr, masterAddress, 6);
            peerInfo.channel = current_channel;
            esp_now_mod_peer(&peerInfo);
            
            lastScanTime = millis();
            
            myData.id = SLAVE_ID;
            // Send ping
            esp_now_send(masterAddress, (uint8_t *) &myData, sizeof(myData));
        }
    } else {
        if (millis() - lastSendTime > 1000) {
            emon.calcVI(20, 2000);
            
            float voltage = emon.Vrms;
            float current = emon.Irms;
            
            voltage = voltage * calibrationVoltage;
            voltage = round(voltage);
            current = current * calibrationCurrent;
            
            if (voltage < noiseVoltage) voltage = 0;
            if (current < noiseCurrent) current = 0;
            
            float power = voltage * current;
            
            myData.id = SLAVE_ID;
            myData.voltage = voltage;
            myData.current = current;
            myData.power = power; 
            
            esp_now_send(masterAddress, (uint8_t *) &myData, sizeof(myData));
            lastSendTime = millis();
        }
    }
}
