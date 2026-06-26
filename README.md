# MyPower - Smart Load Shedding System

MyPower is an advanced, hybrid IoT Load Shedding and Energy Monitoring system. It leverages multiple ESP32 microcontrollers communicating via ESP-NOW for local high-speed control, integrated with a Native Android Application and Blynk IoT for global monitoring and manual override.

## System Architecture

The hardware architecture consists of three distributed ESP32 nodes:
1. **Master Node (ESP1)**:
   - Reads main power parameters (Voltage, Current, Power) using the `EmonLib` library.
   - Hosts the primary Load Shedding algorithm.
   - Controls Priority 1 load (Relay P1).
   - Bridges communication between the local ESP-NOW network and the global Blynk Cloud via WiFi.
   - Displays real-time data on a 20x4 I2C LCD.

2. **Slave Nodes (ESP2 & ESP3)**:
   - Operate on the ESP-NOW protocol (No WiFi AP connection required).
   - Transmit their local power consumption data to the Master Node.
   - Control Priority 2 (Relay P2) and Priority 3 (Relay P3) loads based on high-speed commands from the Master.
   - Equipped with an **Auto-Recovery Channel Scanner** to ensure connection resilience even if the Master's WiFi channel hops.

## Features

- **Automated Load Shedding**: Intelligently drops lower-priority loads (P3, then P2, then P1) when total power consumption exceeds safety thresholds (e.g., > 400W).
- **Hybrid Communication**: Uses ultra-fast `ESP-NOW` for node-to-node communication and standard `WiFi` (REST API) for Cloud/App communication.
- **Native Android App**: A dedicated, beautifully designed Jetpack Compose Android app that interfaces with the Blynk Cloud API.
- **Dynamic Configuration**: The Android app features an in-app setup screen to dynamically change Blynk Auth Tokens and Template IDs without recompiling the app.
- **Manual Override Mode**: Switch between "Auto" (Load Shedding) and "Manual" mode directly from the Android App to manually control individual relays.

## Repository Structure

- `/master_node`: C++ (Arduino IDE) firmware for the ESP32 Master Node.
- `/slave_node`: C++ (Arduino IDE) firmware for the ESP32 Slave Nodes.
- `/aplikasi`: Native Android App source code (Kotlin/Jetpack Compose).

## Hardware Setup (Pinout)

**Master Node (ESP32):**
- **Voltage Sensor**: GPIO 33
- **Current Sensor**: GPIO 32
- **Relay P1**: GPIO 26 (Active LOW)
- **LCD I2C**: Default SDA/SCL

**Slave Nodes (ESP32):**
- **Relay (P2/P3)**: GPIO 26 (Active LOW)

## Blynk Datastream Setup

To connect the hardware and the Android App, configure your Blynk Web Console Datastreams as follows:

| Virtual Pin | Name | Data Type | Min | Max |
| :--- | :--- | :--- | :--- | :--- |
| **V1** | Voltage | Double | 0 | 300 |
| **V2** | Current | Double | 0 | 30 |
| **V3** | Power P1 | Double | 0 | 2000 |
| **V4** | Power P2 | Double | 0 | 2000 |
| **V5** | Power P3 | Double | 0 | 2000 |
| **V6** | Total Power | Double | 0 | 5000 |
| **V7** | Relay P1 | Integer | 0 | 1 |
| **V8** | Relay P2 | Integer | 0 | 1 |
| **V9** | Relay P3 | Integer | 0 | 1 |
| **V10** | Mode (Auto/Manual)| Integer | 0 | 1 |

## Getting Started

### 1. Firmware Flashing (ESP32)
1. Open `master_node/master_node.ino` and replace `YOUR_TEMPLATE_ID`, `YOUR_TEMPLATE_NAME`, `YOUR_AUTH_TOKEN`, `YOUR_WIFI_SSID`, and `YOUR_WIFI_PASSWORD` with your actual network and Blynk credentials.
2. In both `master_node.ino` and `slave_node.ino`, update the MAC Address arrays (`masterAddress`, `slave2Address`, `slave3Address`) to match the actual hardware MAC addresses of your ESP32 boards.
3. Flash the firmware to the respective ESP32 boards.

### 2. Android App Compilation
1. Open the `/aplikasi` folder in **Android Studio**.
2. Sync the Gradle files.
3. Build and Run the application on your Android smartphone or emulator.
4. On the first launch, open the side Navigation Drawer, go to **Konfigurasi Blynk**, and input your Blynk Auth Token.

## License
MIT License
