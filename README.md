# MyPower - Sistem Load Shedding Cerdas

MyPower adalah sistem IoT hibrida tingkat lanjut untuk Pemantauan Energi (Energy Monitoring) dan Pelepasan Beban Listrik (Load Shedding). Sistem ini memanfaatkan beberapa mikrokontroler ESP32 yang saling berkomunikasi menggunakan protokol ESP-NOW untuk kendali lokal berkecepatan tinggi, dan terintegrasi dengan Aplikasi Android Native serta Blynk IoT untuk pemantauan global dan kontrol manual.

## Arsitektur Sistem

Arsitektur perangkat keras ini terdiri dari tiga node ESP32 yang terdistribusi:
1. **Master Node (ESP1)**:
   - Membaca parameter daya utama (Tegangan, Arus, Daya) menggunakan pustaka `EmonLib`.
   - Menjalankan algoritma cerdas *Load Shedding* sebagai pengendali utama.
   - Mengendalikan beban Prioritas 1 (Relay P1).
   - Menjadi jembatan komunikasi antara jaringan lokal ESP-NOW dengan global Blynk Cloud via WiFi.
   - Menampilkan data secara *real-time* melalui layar LCD I2C 20x4.

2. **Slave Nodes (ESP2 & ESP3)**:
   - Beroperasi sepenuhnya pada protokol ESP-NOW (Tidak memerlukan koneksi WiFi Router/AP).
   - Mengirimkan data konsumsi daya masing-masing beban lokalnya ke Master Node.
   - Mengendalikan beban Prioritas 2 (Relay P2) dan Prioritas 3 (Relay P3) berdasarkan komando super cepat dari Master.
   - Dilengkapi dengan fitur **Auto-Recovery Channel Scanner** untuk memastikan koneksi antar node tidak pernah terputus meskipun saluran (*channel*) WiFi Master berubah-ubah.

## Fitur Utama

- **Automated Load Shedding**: Secara cerdas memutuskan beban prioritas rendah (P3, lalu P2, lalu P1) apabila total konsumsi listrik melampaui batas keamanan yang ditentukan (misalnya > 400W).
- **Hybrid Communication**: Menggunakan `ESP-NOW` yang ultra cepat untuk komunikasi *node-to-node*, dipadukan dengan WiFi standar (REST API) untuk koneksi ke Aplikasi dan *Cloud*.
- **Aplikasi Android Native**: Aplikasi khusus yang dibangun menggunakan teknologi mutakhir *Jetpack Compose*, dijamin *smooth* dan terhubung langsung ke Blynk Cloud API.
- **Konfigurasi Dinamis**: Aplikasi Android memiliki halaman pengaturan di dalamnya yang memungkinkan Anda mengubah *Blynk Auth Token* dan *Template ID* kapan saja tanpa perlu melakukan kompilasi / *build* ulang *source code*-nya.
- **Mode Manual (Override)**: Anda dapat beralih antara mode "Auto" (Load Shedding otomatis) dan "Manual" langsung dari Aplikasi Android untuk memegang kendali penuh atas setiap relay.

## Struktur Repositori

- `/master_node`: *Firmware* C++ (Arduino IDE) untuk Master Node ESP32.
- `/slave_node`: *Firmware* C++ (Arduino IDE) untuk Slave Node ESP32.
- `/aplikasi`: *Source code* murni Aplikasi Android Native (Kotlin/Jetpack Compose).

## Konfigurasi Hardware (Pinout)

**Master Node (ESP32):**
- **Sensor Tegangan (ZMPT)**: GPIO 33
- **Sensor Arus (SCT)**: GPIO 32
- **Relay P1**: GPIO 26 (Aktif LOW)
- **LCD I2C**: Default SDA/SCL

**Slave Nodes (ESP32):**
- **Relay (P2/P3)**: GPIO 26 (Aktif LOW)

## Pengaturan Datastream Blynk

### 2. Setup Blynk IoT
1. Buat Template baru di Blynk Console dengan tipe ESP32 & WiFi.
2. Buat Datastream berikut di tab Datastreams:
   - **V0**: Tipe `String` - Nama: "Multiplex Data" *(Khusus untuk Android UI Cepat)*
   - **V1**: Tipe `Double` - Nama: "Tegangan Master"
   - **V2**: Tipe `Double` - Nama: "Arus Master"
   - **V3**: Tipe `Double` - Nama: "Daya P1"
   - **V4**: Tipe `Double` - Nama: "Daya P2"
   - **V5**: Tipe `Double` - Nama: "Daya P3"
   - **V6**: Tipe `Double` - Nama: "Total Daya"
   - **V7**: Tipe `Integer` - Nama: "Relay P1" (Min: 0, Max: 1)
   - **V8**: Tipe `Integer` - Nama: "Relay P2" (Min: 0, Max: 1)
   - **V9**: Tipe `Integer` - Nama: "Relay P3" (Min: 0, Max: 1)
   - **V10**: Tipe `Integer` - Nama: "Mode" (0: Auto, 1: Manual)
   - **V11**: Tipe `Double` - Nama: "Tegangan P2"
   - **V12**: Tipe `Double` - Nama: "Arus P2"
   - **V13**: Tipe `Double` - Nama: "Tegangan P3"
   - **V14**: Tipe `Double` - Nama: "Arus P3"
3. Copy **Template ID**, **Template Name**, dan **Auth Token** ke dalam kode `master_node.ino`.

## Panduan Memulai Instalasi

### 1. Flashing Firmware (ESP32)
1. Buka `master_node/master_node.ino` dan ganti teks `YOUR_TEMPLATE_ID`, `YOUR_TEMPLATE_NAME`, `YOUR_AUTH_TOKEN`, `YOUR_WIFI_SSID`, dan `YOUR_WIFI_PASSWORD` dengan data jaringan/Blynk asli Anda.
2. Di dalam file `master_node.ino` maupun `slave_node.ino`, ganti deretan *MAC Address* (`masterAddress`, `slave2Address`, `slave3Address`) agar sesuai dengan *MAC Address* perangkat ESP32 fisik Anda.
3. Unggah (*flash*) masing-masing *firmware* tersebut ke ESP32 yang sesuai.

### 2. Kompilasi Aplikasi Android
1. Buka folder `/aplikasi` menggunakan perangkat lunak **Android Studio**.
2. Tunggu proses sinkronisasi Gradle selesai.
3. Tekan *Build and Run* untuk memasang aplikasi di gawai Android Anda.
4. Pada saat aplikasi pertama kali dibuka, usap dari kiri layar untuk membuka laci navigasi (Drawer), pilih **Konfigurasi Blynk**, dan masukkan Auth Token Blynk Anda untuk mulai mengendalikan sistem!

## Lisensi
MIT License
