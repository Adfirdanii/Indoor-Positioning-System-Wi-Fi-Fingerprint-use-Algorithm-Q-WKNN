# Implementasi Algoritma Q-WKNN untuk Indoor Positioning System Menggunakan Wi-Fi Fingerprint Berbasis Android

Skripsi — Program Studi Jaringan Telekomunikasi Digital, Jurusan Teknik Elektro, **Politeknik Negeri Malang** (2026)

**Disusun oleh:** Ade Achmad Firdani (NIM. 1941160113)
**Dosen Pembimbing:** Ahmad Wilda Yulianto, S.T., M.T. & Ir. Hudiono, M.T.

## 📋 Deskripsi

Global Positioning System (GPS) kehilangan akurasi di dalam ruangan karena sinyal terhalang bangunan. Penelitian ini mengimplementasikan **algoritma Q-WKNN (Q-Weighted K-Nearest Neighbor)** pada aplikasi Android untuk estimasi posisi indoor berbasis **Wi-Fi fingerprinting**, dengan **algoritma WKNN konvensional** sebagai pembanding performa.

Lokasi penelitian: **Lantai 1 Gedung AH, Politeknik Negeri Malang**, dengan 42 titik Reference Point (RP), 19 titik Test Point (TP), dan total 153 Access Point (AP) unik yang terdeteksi.

## 🎯 Tujuan Penelitian

1. Merancang dan membangun sistem Indoor Positioning System berbasis Android yang dapat menentukan posisi pengguna serta menampilkan arah dan jarak menuju ruangan tujuan di Lantai 1 Gedung AH
2. Menerapkan algoritma Q-WKNN, termasuk menentukan nilai optimal parameter **η (eta)** dan **L**, untuk mengestimasi posisi pengguna berdasarkan fingerprint Wi-Fi
3. Mengetahui perbandingan akurasi estimasi posisi antara algoritma Q-WKNN dengan algoritma WKNN konvensional

## 📊 Hasil Penelitian

- Parameter optimal hasil pengujian sweep dua tahap: **η = 4** dan **L = 70** (dari 153 AP terdeteksi)
- Pengujian formal pada 19 TP (190 data uji):
  - **Mean error jarak** — WKNN: 3,72 m | Q-WKNN: 4,02 m (WKNN sedikit lebih unggul dari sisi akurasi jarak)
  - **Ketepatan penentuan ruangan** — Q-WKNN: 61,6% | WKNN: 48,9% (Q-WKNN lebih unggul, khususnya di area lobi/selasar)
- Kesimpulan: tidak ada algoritma yang unggul secara mutlak — performa bergantung pada karakteristik lingkungan dan metrik evaluasi yang digunakan (akurasi jarak vs. ketepatan ruangan)

## ✨ Fitur Aplikasi

- **Kalibrasi (Fingerprinting)** — perekaman data RSSI di titik-titik Reference Point
- **Positioning real-time** — estimasi posisi pengguna berdasarkan pemindaian Wi-Fi terkini menggunakan Q-WKNN & WKNN
- **Visualisasi denah** — peta lantai interaktif menampilkan posisi hasil estimasi
- **Perhitungan jarak & arah navigasi** — menampilkan estimasi jarak dan arah (azimuth) menuju ruangan tujuan (mengikuti struktur koridor)
- **Analisis akurasi** — perbandingan performa algoritma terhadap dataset uji
- **Validasi dengan dataset publik** — pengujian implementasi algoritma menggunakan dataset Zenodo (UJIIndoorLoc, DOI: 10.5281/zenodo.1066041) sebelum diuji pada data mandiri
- **Rekap data** — riwayat hasil positioning

## 🛠️ Teknologi & Alat/Bahan

| Kategori | Alat/Bahan | Keterangan |
|---|---|---|
| Hardware | Smartphone Android | Wi-Fi scanning & menjalankan aplikasi |
| Hardware | Access Point Wi-Fi (IEEE 802.11) | Sumber sinyal RSS di Gedung AH |
| Hardware | Meteran | Pengukuran koordinat RP/TP |
| Software | Android Studio | IDE pengembangan aplikasi |
| Software | Java | Bahasa pemrograman aplikasi & algoritma |
| Software | Microsoft Excel | Pengolahan & analisis data hasil pengujian |
| Dataset | Zenodo (UJIIndoorLoc) | Validasi kebenaran implementasi algoritma |

- **Platform:** Android (min SDK 24, target SDK 35)
- **Build tool:** Gradle (Kotlin DSL)

## 📐 Metode Perhitungan

- **Manhattan Distance** berbasis transformasi Q — untuk penentuan ruangan (Q-WKNN)
- **Euclidean Distance** — untuk pengukuran akurasi posisi (mean error & P75 error)

## 📁 Struktur Proyek

```
app/src/main/java/com/implementasi/myqwknn/
├── algorithm/          # Implementasi algoritma positioning
│   ├── QWKNNAlgorithm.java
│   ├── WKNNAlgorithm.java
│   ├── DataPreprocessor.java   # AP Selection, preprocessing data
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
│   ├── CalibrationActivity.java   # Kalibrasi / fingerprinting
│   ├── PositioningActivity.java   # Estimasi posisi real-time
│   ├── AnalysisActivity.java      # Analisis akurasi
│   ├── FloorMapView.java          # Visualisasi denah
│   ├── ZenodoActivity.java        # Validasi dataset publik
│   └── ...
└── MainActivity.java

app/src/main/assets/     # Dataset training (RP) & testing (TP): RSSI + koordinat
```

## 🚀 Cara Menjalankan

1. Clone repository ini
   ```bash
   git clone https://github.com/Adfirdanii/Indoor-Positioning-System-Wi-Fi-Fingerprint-use-Algorithm-Q-WKNN.git
   ```
2. Buka project menggunakan **Android Studio**
3. Tunggu proses Gradle sync selesai
4. Sambungkan perangkat Android (atau emulator yang mendukung Wi-Fi scanning)
5. Klik **Run** ▶️

## 💡 Saran Pengembangan Selanjutnya

1. Menambah uji parameter di berbagai kondisi lantai dan kelengkapan data fingerprint per ruangan
2. Memperbanyak pengambilan data fingerprint agar sebaran RP lebih merata dan mean error lebih kecil
3. Mengganti navigasi berbasis aturan tetap (mengikuti struktur koridor) dengan algoritma pencarian jalur terpendek (Dijkstra/A*)
4. Menambahkan fitur augmented reality atau posisi koordinat real-time untuk navigasi yang lebih jelas

## 📚 Referensi Utama

1. R. Zhou, Y. Yang, and P. Chen, "An RSS Transform—Based WKNN for Indoor Positioning," *Sensors*, vol. 21, no. 17, p. 5685, 2021.
2. A. P. H. Yulianto, M. N. Zakaria, and A. W. Yulianto, "Indoor Positioning and Navigating System Application Using Wi-Fi with Fingerprinting Method and Weighted K-Nearest Neighbor Algorithm," *Jurnal Jaringan Telekomunikasi*, vol. 12, no. 3, 2022.
3. S. Liu, R. de Lacerda, and J. Fiorina, "Performance Analysis of Adaptive K for Weighted K-Nearest Neighbor Based Indoor Positioning," in *Proc. 2022 IEEE 95th VTC2022-Spring*, Helsinki, Finland, Jun. 2022.

## 📄 Lisensi

Proyek ini dibuat untuk keperluan Tugas Akhir/Skripsi, Politeknik Negeri Malang, 2026.
