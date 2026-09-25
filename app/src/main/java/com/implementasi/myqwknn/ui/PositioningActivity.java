package com.implementasi.myqwknn.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.implementasi.myqwknn.R;
import com.implementasi.myqwknn.algorithm.PositioningEngine;
import com.implementasi.myqwknn.database.FingerprintDatabase;
import com.implementasi.myqwknn.model.AccessPoint;
import com.implementasi.myqwknn.model.PositionResult;
import com.implementasi.myqwknn.scanner.WifiScanner;

import java.util.List;

public class PositioningActivity extends AppCompatActivity {

    // Nama SharedPreferences tempat hasil tuning terbaik (dari RekapActivity) disimpan.
    // Harus SAMA dengan yang dipakai di RekapActivity.
    public static final String PREFS_NAME = "qwknn_config";
    public static final String KEY_BEST_ETA = "best_eta";
    public static final String KEY_BEST_L = "best_L";

    private TextView tvQwknn, tvWknn, tvStatus, tvParamInfo;
    private Button btnLocate;

    private WifiScanner wifiScanner;
    private FingerprintDatabase db;
    private PositioningEngine engine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_positioning);

        db          = new FingerprintDatabase(this);
        wifiScanner = new WifiScanner(this);

        tvQwknn     = findViewById(R.id.tv_result_qwknn);
        tvWknn      = findViewById(R.id.tv_result_wknn);
        tvStatus    = findViewById(R.id.tv_status);
        tvParamInfo = findViewById(R.id.tv_param_info);
        btnLocate   = findViewById(R.id.btn_locate);

        // ── Ambil parameter hasil tuning terbaik (kalau ada) ──────────
        // Jika belum pernah ada sweep/rekap yang "diterapkan" di RekapActivity,
        // fallback ke default lama (eta=7.0, L=999) supaya app tetap jalan.
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean adaHasilTuning = prefs.contains(KEY_BEST_ETA);
        double etaDipakai = prefs.getFloat(KEY_BEST_ETA, 7.0f);
        int lDipakai = prefs.getInt(KEY_BEST_L, 999);

        engine = new PositioningEngine(db);
        engine.setEta(etaDipakai);
        engine.setTopL(lDipakai);

        tvParamInfo.setText(String.format(java.util.Locale.US,
                "Parameter: η=%.2f, L=%d %s",
                etaDipakai, lDipakai,
                adaHasilTuning ? "(hasil tuning)" : "(default, belum ada tuning diterapkan)"));

        btnLocate.setOnClickListener(v -> runPositioning());
    }

    private void runPositioning() {
        if (!db.isFingerprintReady()) {
            Toast.makeText(this, "Fingerprint belum siap. Lakukan kalibrasi dulu.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        tvStatus.setText("Scanning...");
        btnLocate.setEnabled(false);

        wifiScanner.startScan(new WifiScanner.ScanCallback() {
            @Override
            public void onScanComplete(List<AccessPoint> results) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (results.isEmpty()) {
                        tvStatus.setText("Tidak ada AP terdeteksi.");
                        btnLocate.setEnabled(true);
                        return;
                    }

                    new Thread(() -> {
                        PositionResult[] both = engine.runBoth(results);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            displayResults(both, results.size());
                            btnLocate.setEnabled(true);
                        });
                    }).start();
                });
            }

            @Override
            public void onScanFailed(String reason) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    tvStatus.setText("Scan gagal: " + reason);
                    btnLocate.setEnabled(true);
                });
            }
        });
    }

    private void displayResults(PositionResult[] both, int apCount) {
        tvStatus.setText("Scan selesai. AP terdeteksi: " + apCount);

        if (both == null) {
            tvQwknn.setText("Q-WKNN: gagal");
            tvWknn.setText("WKNN: gagal");
            return;
        }

        PositionResult q = both[0];
        PositionResult w = both[1];

        if (q != null) {
            tvQwknn.setText(
                    "Q-WKNN\n" +
                            "Ruangan : " + q.getRoomName() + "\n" +
                            "X       : " + String.format("%.3f", q.getX()) + " m\n" +
                            "Y       : " + String.format("%.3f", q.getY()) + " m\n" +
                            "K adaptif: " + q.getKAdaptive() + "\n" +
                            "Waktu   : " + q.getComputationTimeMs() + " ms"
            );
        } else {
            tvQwknn.setText("Q-WKNN: hasil tidak tersedia");
        }

        if (w != null) {
            tvWknn.setText(
                    "WKNN\n" +
                            "Ruangan : " + w.getRoomName() + "\n" +
                            "X       : " + String.format("%.3f", w.getX()) + " m\n" +
                            "Y       : " + String.format("%.3f", w.getY()) + " m\n" +
                            "K tetap : " + w.getKAdaptive() + "\n" +
                            "Waktu   : " + w.getComputationTimeMs() + " ms"
            );
        } else {
            tvWknn.setText("WKNN: hasil tidak tersedia");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wifiScanner != null) wifiScanner.release();
    }
}