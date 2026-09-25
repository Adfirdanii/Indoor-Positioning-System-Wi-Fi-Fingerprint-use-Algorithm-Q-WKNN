package com.implementasi.myqwknn;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.implementasi.myqwknn.database.FingerprintDatabase;
import com.implementasi.myqwknn.model.AccessPoint;
import com.implementasi.myqwknn.model.PositionResult;
import com.implementasi.myqwknn.algorithm.PositioningEngine;
import com.implementasi.myqwknn.scanner.WifiScanner;
import com.implementasi.myqwknn.ui.FloorMapView;
import com.implementasi.myqwknn.ui.PositioningActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Daftar ruangan yang bisa dipilih sebagai tujuan (dialog pilih tujuan)
    private static final String[] ROOMS = {
            "Ruang Kelas AH 1.1", "Ruang Kelas AH 1.2", "Ruang Kelas AH 1.3",
            "Ruang Dosen AH 1.4", "Ruang Kelas AH 1.5",
            "Lab Wireless Sensor Network AH 1.6", "Staff Only AH 1.7",
            "Lab Praktik TUK AH 1.8", "Lab Sinyal Digital AH 1.9", "Lab Fiber Optik AH 1.10",
            "Ruang Kelas AH 1.12", "Ruang Kelas AH 1.13", "Ruang Kelas AH 1.14",
            "Pintu Masuk", "Lobi Tengah", "Lobi Kiri", "Lobi Kanan"
    };

    // Koordinat tengah tiap ruangan (meter) — untuk navigasi.
    // Diukur ULANG langsung dari posisi dinding di gambar denah_ah.png
    // (bukan asumsi 7 ruangan lebar rata), karena lebar ruangan asli tidak
    // seragam -- ruangan dekat tangga (AH1.5-1.10) makin ke kanan makin
    // sempit dari yang diasumsikan sebelumnya.
    private static final double[][] ROOM_COORDS = {
            {3.62,  13.875},  // Ruang Kelas AH 1.1
            {10.57, 13.875},  // Ruang Kelas AH 1.2
            {17.19, 13.875},  // Ruang Kelas AH 1.3
            {23.23, 13.875},  // Ruang Dosen AH 1.4
            {29.30, 13.875},  // Ruang Kelas AH 1.5
            {35.89, 13.875},  // Lab Wireless Sensor Network AH 1.6
            {42.48, 13.875},  // Staff Only AH 1.7
            {42.48, 3.75},    // Lab Praktik TUK AH 1.8 (tutup, tetap ada koordinat)
            {35.89, 3.75},    // Lab Sinyal Digital AH 1.9
            {29.30, 3.75},    // Lab Fiber Optik AH 1.10
            {17.19, 3.75},    // Ruang Kelas AH 1.12
            {10.57, 3.75},    // Ruang Kelas AH 1.13
            {3.62,  3.75},    // Ruang Kelas AH 1.14
            {23.23, 3.75},    // Pintu Masuk
            {24.56, 9.12},    // Lobi Tengah
            {3.00,  9.12},    // Lobi Kiri
            {44.00, 9.12},    // Lobi Kanan (di dalam blok ruangan utama, bukan kotak kecil terpisah dekat tangga)
    };

    // Bounding box tiap ruangan {x_min, x_max, y_min, y_max} dalam meter
    // -- juga diukur ulang dari dinding asli (lihat catatan di atas)
    private static final double[][] ROOM_BOUNDS = {
            {0.01,  7.23,  9.5,  18.25},  // Ruang Kelas AH 1.1
            {7.23,  13.91, 9.5,  18.25},  // Ruang Kelas AH 1.2
            {13.91, 20.47, 9.5,  18.25},  // Ruang Kelas AH 1.3
            {20.47, 25.98, 9.5,  18.25},  // Ruang Dosen AH 1.4
            {25.98, 32.61, 9.5,  18.25},  // Ruang Kelas AH 1.5
            {32.61, 39.17, 9.5,  18.25},  // Lab Wireless Sensor Network AH 1.6
            {39.17, 45.79, 9.5,  18.25},  // Staff Only AH 1.7
            {39.17, 45.79, 0,    7.5},    // Lab Praktik TUK AH 1.8
            {32.61, 39.17, 0,    7.5},    // Lab Sinyal Digital AH 1.9
            {25.98, 32.61, 0,    7.5},    // Lab Fiber Optik AH 1.10
            {13.91, 20.47, 0,    7.5},    // Ruang Kelas AH 1.12
            {7.23,  13.91, 0,    7.5},    // Ruang Kelas AH 1.13
            {0.01,  7.23,  0,    7.5},    // Ruang Kelas AH 1.14
            {20.47, 25.98, 0,    7.5},    // Pintu Masuk (area gap, dekat dinding luar)
            {21,    25,    2,    6},      // Lobi Tengah (bounds tidak dipakai utk deteksi posisi)
            {0,     7.5,   7.5,  9.5},    // Lobi Kiri  (bounds tidak dipakai utk deteksi posisi)
            {41.62, 49.12, 7.5,  9.5},    // Lobi Kanan (bounds tidak dipakai utk deteksi posisi)
    };

    /**
     * Cari nama ruangan dari koordinat estimasi (x,y).
     * Lebih akurat dari majority voting Q-WKNN.
     *
     * CATATAN: khusus untuk DETEKSI posisi saat scan, kita hanya cek ruangan
     * AH1.1-1.14 dan Pintu Masuk (index 0-13). Lobi Kiri/Tengah/Kanan sengaja
     * TIDAK dicek di sini -- kalau posisi user ada di area lorong manapun
     * (bukan di dalam salah satu ruangan/pintu masuk), cukup dianggap satu
     * zona umum "Selasar" (lihat fallback di bawah). Pembagian Lobi
     * Kiri/Tengah/Kanan itu HANYA dipakai di dialog pilih tujuan, bukan
     * untuk label posisi saat ini.
     */
    private static final int DETECT_ROOM_COUNT = 14; // AH1.1-1.14 (13) + Pintu Masuk (1)

    private String getRoomFromCoords(double x, double y) {
        for (int i = 0; i < DETECT_ROOM_COUNT; i++) {
            double xMin = ROOM_BOUNDS[i][0];
            double xMax = ROOM_BOUNDS[i][1];
            double yMin = ROOM_BOUNDS[i][2];
            double yMax = ROOM_BOUNDS[i][3];
            if (x >= xMin && x <= xMax && y >= yMin && y <= yMax) {
                return ROOMS[i];
            }
        }
        return "Selasar"; // fallback: area lorong mana pun, satu label umum
    }
    private FloorMapView mapView;
    private TextView tvStatus, tvNavigation;
    private AutoCompleteTextView etTujuan;
    private Button btnLocate, btnClearTujuan;

    private FingerprintDatabase db;
    private WifiScanner wifiScanner;
    private PositioningEngine engine;

    private double currentX = -1, currentY = -1;
    private String currentRoom = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db          = new FingerprintDatabase(this);
        wifiScanner = new WifiScanner(this);

        // ── Pakai parameter hasil tuning terbaik (kalau sudah pernah
        // ditekan tombol "Terapkan" di RekapActivity). Kalau belum pernah,
        // fallback ke default lama (eta=7.0, L=999) supaya app tetap jalan.
        SharedPreferences prefs = getSharedPreferences(
                PositioningActivity.PREFS_NAME, MODE_PRIVATE);
        double etaDipakai = prefs.getFloat(PositioningActivity.KEY_BEST_ETA, 7.0f);
        int lDipakai = prefs.getInt(PositioningActivity.KEY_BEST_L, 999);

        engine = new PositioningEngine(db);
        engine.setEta(etaDipakai);
        engine.setTopL(lDipakai);

        mapView      = findViewById(R.id.floor_map_view);
        tvStatus     = findViewById(R.id.tv_status);
        tvNavigation = findViewById(R.id.tv_navigation);
        etTujuan     = findViewById(R.id.et_tujuan);
        btnLocate    = findViewById(R.id.btn_locate);
        btnClearTujuan = findViewById(R.id.btn_clear_tujuan);

        // Setup pilihan tujuan ruangan: TAP untuk buka daftar, bukan ngetik.
        // (Sebelumnya pakai AutoCompleteTextView yang search tiap ketikan --
        // itu sumber bug: teks belum selesai diketik ikut dicocokkan dan
        // salah nyasar ke ruangan lain.)
        etTujuan.setFocusable(false);
        etTujuan.setClickable(true);
        etTujuan.setKeyListener(null);
        etTujuan.setOnClickListener(v -> showRoomPickerDialog());

        btnClearTujuan.setOnClickListener(v -> {
            etTujuan.setText("");
            tvNavigation.setText("");
            tvNavigation.setVisibility(View.GONE);
            btnClearTujuan.setVisibility(View.GONE);
            mapView.setDestination(-1, -1, "");
        });

        btnLocate.setOnClickListener(v -> runPositioning());
    }

    private void runPositioning() {
        if (!db.isFingerprintReady()) {
            Toast.makeText(this, "Database fingerprint belum siap.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        tvStatus.setText("🔍 Mencari posisi...");
        btnLocate.setEnabled(false);
        btnLocate.setText("Scanning...");

        wifiScanner.startScan(new WifiScanner.ScanCallback() {
            @Override
            public void onScanComplete(List<AccessPoint> results) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (results.isEmpty()) {
                        tvStatus.setText("⚠ Tidak ada WiFi terdeteksi.");
                        resetButton();
                        return;
                    }

                    new Thread(() -> {
                        PositionResult[] both = engine.runBoth(results);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (both != null && both[0] != null) {
                                PositionResult q = both[0];
                                currentX    = q.getX();
                                currentY    = q.getY();
                                // Nama ruangan dari koordinat, bukan majority voting
                                currentRoom = getRoomFromCoords(currentX, currentY);

                                // DEBUG
                                Toast.makeText(MainActivity.this,
                                        "x=" + String.format("%.2f", currentX) +
                                                " y=" + String.format("%.2f", currentY) +
                                                " room=" + currentRoom,
                                        Toast.LENGTH_LONG).show();

                                // Update denah
                                mapView.setUserPosition(currentX, currentY, currentRoom);

                                // Update status dengan koordinat
                                tvStatus.setText("📍 " + currentRoom +
                                        " (" + String.format("%.2f", currentX) +
                                        ", " + String.format("%.2f", currentY) + ")");

                                // Update navigasi kalau ada tujuan
                                String tujuan = etTujuan.getText().toString().trim();
                                if (!tujuan.isEmpty()) updateNavigation(tujuan);
                            } else {
                                tvStatus.setText("⚠ Posisi tidak dapat ditentukan.");
                            }
                            resetButton();
                        });
                    }).start();
                });
            }

            @Override
            public void onScanFailed(String reason) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    tvStatus.setText("⚠ Scan gagal: " + reason);
                    resetButton();
                });
            }
        });
    }

    private void resetButton() {
        btnLocate.setEnabled(true);
        btnLocate.setText("📡  Locate Me");
    }

    private void updateNavigation(String tujuan) {
        if (currentX < 0) {
            tvNavigation.setText("Tekan Locate Me dulu untuk mengetahui posisimu.");
            return;
        }

        // Cari koordinat tujuan
        double[] dest = findRoomCoords(tujuan);
        if (dest == null) {
            tvNavigation.setText("");
            mapView.setDestination(-1, -1, "");
            return;
        }

        double destX = dest[0];
        double destY = dest[1];

        // Hitung jarak
        double jarak = Math.sqrt(Math.pow(destX - currentX, 2) +
                Math.pow(destY - currentY, 2));

        // Tentukan arah horizontal dan vertikal
        double dx = destX - currentX;
        double dy = destY - currentY;

        String arahH = "";
        String arahV = "";

        if (Math.abs(dx) > 0.5) {
            arahH = dx > 0 ?
                    String.format("%.1f m ke kanan", Math.abs(dx)) :
                    String.format("%.1f m ke kiri", Math.abs(dx));
        }
        if (Math.abs(dy) > 0.5) {
            arahV = dy > 0 ?
                    String.format("%.1f m ke atas", Math.abs(dy)) :
                    String.format("%.1f m ke bawah", Math.abs(dy));
        }

        // Susun keterangan
        StringBuilder nav = new StringBuilder();
        nav.append("🎯 Menuju ").append(tujuan).append("\n");
        nav.append("📏 Jarak: ").append(String.format("%.1f", jarak)).append(" m\n");
        if (!arahH.isEmpty()) nav.append("➡ ").append(arahH);
        if (!arahH.isEmpty() && !arahV.isEmpty()) nav.append(", ");
        if (!arahV.isEmpty()) nav.append(arahV);

        if (arahH.isEmpty() && arahV.isEmpty()) {
            nav.append("✅ Kamu sudah di dekat tujuan!");
        }

        tvNavigation.setText(nav.toString());

        // Tampilkan tujuan di denah
        mapView.setDestination(destX, destY, tujuan);
    }

    private void showRoomPickerDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Pilih ruangan tujuan")
                .setItems(ROOMS, (dialog, which) -> {
                    String tujuan = ROOMS[which];
                    etTujuan.setText(tujuan);
                    btnClearTujuan.setVisibility(View.VISIBLE);
                    tvNavigation.setVisibility(View.VISIBLE);
                    updateNavigation(tujuan);
                })
                .show();
    }

    private double[] findRoomCoords(String tujuan) {
        String query = tujuan.trim();
        for (int i = 0; i < ROOMS.length; i++) {
            // Pencocokan PERSIS (bukan substring), supaya "AH 1.1" tidak
            // ketiban duluan oleh "AH 1.10", "AH 1.12", "AH 1.13", "AH 1.14"
            // (yang semuanya berawalan sama persis dengan "AH 1.1").
            if (ROOMS[i].equalsIgnoreCase(query)) {
                return ROOM_COORDS[i];
            }
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wifiScanner != null) wifiScanner.release();
    }
}