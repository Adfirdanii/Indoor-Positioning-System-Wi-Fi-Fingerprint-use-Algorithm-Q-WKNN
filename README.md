# Indoor Positioning System — Wi-Fi Fingerprinting dengan Algoritma Q-WKNN

Aplikasi Android untuk sistem penentuan posisi dalam ruangan (indoor positioning) menggunakan metode Wi-Fi fingerprinting dengan algoritma **Q-WKNN (Q-based Weighted K-Nearest Neighbor)**. Dikembangkan sebagai proyek skripsi.

## 📋 Deskripsi

Sistem ini memanfaatkan kekuatan sinyal Wi-Fi (RSSI) dari beberapa access point untuk memperkirakan posisi pengguna di dalam gedung, tanpa memerlukan GPS (yang tidak akurat di dalam ruangan). Metode Q-WKNN merupakan pengembangan dari algoritma WKNN standar dengan pembobotan berbasis kualitas kecocokan sinyal, mengacu pada pendekatan RSS Transform-Based WKNN.

## ✨ Fitur

- **Kalibrasi (Fingerprinting)** — perekaman data RSSI di titik-titik referensi dalam gedung
- **Positioning real-time** — estimasi posisi pengguna berdasarkan pemindaian Wi-Fi terkini
- **Visualisasi denah** — peta lantai interaktif menampilkan posisi hasil estimasi
- **Analisis akurasi** — perbandingan dan evaluasi performa algoritma terhadap dataset uji
- **Rekap data** — riwayat hasil positioning

## 🛠️ Teknologi

- **Bahasa:** Java
- **Platform:** Android (min SDK 24, target SDK 35)
- **Build tool:** Gradle (Kotlin DSL)
- **Algoritma:** Q-WKNN, WKNN (untuk perbandingan)

## 📁 Struktur Proyek

```
app/src/main/java/com/implementasi/myqwknn/
├── algorithm/          # Implementasi algoritma positioning
│   ├── QWKNNAlgorithm.java
│   ├── WKNNAlgorithm.java
│   ├── DataPreprocessor.java
│   └── PositioningEngine.java
├── database/            # Pengelolaan data fingerprint
│   └── FingerprintDatabase.java
├── model/               # Model data
│   ├── AccessPoint.java
│   ├── ReferencePoint.java
│   └── PositionResult.java
├── scanner/             # Pemindaian sinyal Wi-Fi
│   └── WifiScanner.java
├── ui/                  # Activity & tampilan
│   ├── CalibrationActivity.java
│   ├── PositioningActivity.java
│   ├── AnalysisActivity.java
│   ├── FloorMapView.java
│   └── ...
└── MainActivity.java

app/src/main/assets/     # Dataset training & testing (RSSI + koordinat)
```

## 🚀 Cara Menjalankan

1. Clone repository ini
   ```bash
   git clone https://github.com/Adfirdanii/Indoor-Positioning-System-Wi-Fi-Fingerprint-use-Algorithm-Q-WKNN.git
   ```
2. Buka project menggunakan **Android Studio**
3. Tunggu proses Gradle sync selesai
4. Sambungkan perangkat Android (atau gunakan emulator) yang mendukung akses Wi-Fi scanning
5. Klik **Run** ▶️

## 📊 Dataset

Dataset RSSI (`*rss.csv`) dan koordinat referensi (`*crd.csv`) digunakan untuk proses training dan testing algoritma, disimpan di `app/src/main/assets/`.

## 📄 Lisensi

Proyek ini dibuat untuk keperluan tugas akhir/skripsi.
