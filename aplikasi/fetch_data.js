/**
 * Script Fetch API Sederhana (JavaScript/Node.js/Browser)
 * Menarik data dari Blynk REST API dengan arsitektur Pull Method.
 * Nantinya ini dapat Anda integrasikan ke framework aplikasi kustom Anda (misalnya React Native/Flutter via WebView, dsb).
 */

const BLYNK_AUTH_TOKEN = '-GGAneWID3u4RPK8iy4w3mAFkAwbvh1o';

// URL Endpoint Blynk Cloud
// Ganti blynk.cloud jika server lokal/server region Anda berbeda (misal sgp1.blynk.cloud)
const BASE_URL = 'https://blynk.cloud/external/api/get';

async function fetchSensorData() {
    try {
        // Construct endpoint URLs untuk Virtual Pin tertentu
        const urlV1 = `${BASE_URL}?token=${BLYNK_AUTH_TOKEN}&v1`; // Tegangan
        const urlV2 = `${BASE_URL}?token=${BLYNK_AUTH_TOKEN}&v2`; // Arus
        const urlV6 = `${BASE_URL}?token=${BLYNK_AUTH_TOKEN}&v6`; // Total Daya

        // Lakukan eksekusi HTTP GET Request secara asynchronous
        const [resV1, resV2, resV6] = await Promise.all([
            fetch(urlV1),
            fetch(urlV2),
            fetch(urlV6)
        ]);

        // Karena Blynk API return berupa plain text nilai, kita gunakan .text()
        const voltage = await resV1.text();
        const current = await resV2.text();
        const totalPower = await resV6.text();

        // Print atau render output
        console.log('--- Data Real-Time Smart Load Shedding ---');
        console.log(`Tegangan   : ${voltage} Volt`);
        console.log(`Arus       : ${current} Ampere`);
        console.log(`Total Daya : ${totalPower} Watt`);
        console.log('------------------------------------------');

        // Return object JSON untuk digunakan di UI aplikasi kustom
        return {
            voltage: parseFloat(voltage),
            current: parseFloat(current),
            totalPower: parseFloat(totalPower)
        };

    } catch (error) {
        console.error('Terjadi kegagalan saat mengambil data dari REST API Blynk:', error.message);
    }
}

// Opsional: Lakukan polling / pull data setiap 3 detik
setInterval(() => {
    fetchSensorData();
}, 3000);

// Panggil sekali untuk saat pertama kali berjalan
fetchSensorData();
