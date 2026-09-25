package com.implementasi.myqwknn.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.implementasi.myqwknn.R;
import com.implementasi.myqwknn.database.FingerprintDatabase;

import java.util.List;

public class RekapActivity extends AppCompatActivity {

    private FingerprintDatabase db;
    private LinearLayout llContent;

    // Kombinasi (eta, L) terbaik dari rekap yang sedang ditampilkan.
    // Diisi di tampilkanSemua(), dipakai oleh tombol "Terapkan".
    private Double bestEtaFound = null;
    private Integer bestLFound = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout programatik — tidak perlu file XML baru
        ScrollView scroll = new ScrollView(this);
        llContent = new LinearLayout(this);
        llContent.setOrientation(LinearLayout.VERTICAL);
        llContent.setPadding(32, 32, 32, 32);
        scroll.addView(llContent);
        setContentView(scroll);

        setTitle("Rekap Hasil Analisis");
        db = new FingerprintDatabase(this);

        // Tombol hapus semua data analisis
        Button btnHapus = new Button(this);
        btnHapus.setText("Hapus Semua Data Analisis");
        btnHapus.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Hapus Semua?")
                    .setMessage("Semua data analisis tersimpan akan dihapus permanen. Lanjutkan?")
                    .setPositiveButton("Hapus", (d, w) -> {
                        db.clearAllAnalysisResults();
                        llContent.removeAllViews();
                        llContent.addView(btnHapus);
                        tampilkanSemua();
                        Toast.makeText(this, "Semua data analisis dihapus.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });
        llContent.addView(btnHapus);

        tampilkanSemua();
    }

    private void tampilkanSemua() {

        // ── 1. REKAP PARAMETER ──────────────────────────────────
        addHeader("REKAP CARI PARAMETER");
        addSubHeader("Rata-rata error & persentil ke-75 per kombinasi (η, L)");

        List<String[]> rekapParam = db.getRekapParameter();
        if (rekapParam.isEmpty()) {
            addText("Belum ada data 'Cari Parameter' tersimpan.");
        } else {
            // Header tabel
            addTableRow("η", "L", "n TP", "Mean Q", "P75 Q", "Mean W", "P75 W", true);
            for (String[] row : rekapParam) {
                addTableRow(row[0], row[1], row[2],
                        row[3] + "m", row[4] + "m",
                        row[5] + "m", row[6] + "m", false);
            }

            // Cari kombinasi terbaik (mean Q terkecil)
            String[] best = rekapParam.get(0);
            double bestMean = parseD(best[3]);
            for (String[] row : rekapParam) {
                double mean = parseD(row[3]);
                if (mean < bestMean) { bestMean = mean; best = row; }
            }
            addText("\n→ Kombinasi terbaik Q-WKNN: η=" + best[0] + ", L=" + best[1] +
                    " (mean error=" + best[3] + "m, P75=" + best[4] + "m)");

            // Simpan untuk tombol "Terapkan" di bawah
            bestEtaFound = parseD(best[0]);
            bestLFound = (int) parseD(best[1]);

            final double etaUntukDiterapkan = bestEtaFound;
            final int lUntukDiterapkan = bestLFound;
            final String etaLabel = best[0];
            final String lLabel = best[1];
            Button btnTerapkan = new Button(this);
            btnTerapkan.setText("Terapkan η=" + etaLabel + ", L=" + lLabel + " sebagai parameter Positioning");
            btnTerapkan.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Terapkan parameter ini?")
                        .setMessage("Halaman Positioning (dipakai user/client) akan memakai " +
                                "η=" + etaLabel + " dan L=" + lLabel +
                                " untuk seluruh proses positioning berikutnya, " +
                                "menggantikan parameter yang sedang aktif sekarang.")
                        .setPositiveButton("Terapkan", (d, w) -> {
                            SharedPreferences prefs = getSharedPreferences(
                                    PositioningActivity.PREFS_NAME, MODE_PRIVATE);
                            prefs.edit()
                                    .putFloat(PositioningActivity.KEY_BEST_ETA, (float) etaUntukDiterapkan)
                                    .putInt(PositioningActivity.KEY_BEST_L, lUntukDiterapkan)
                                    .apply();
                            Toast.makeText(this,
                                    "Parameter diterapkan: η=" + etaUntukDiterapkan + ", L=" + lUntukDiterapkan,
                                    Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            });
            llContent.addView(btnTerapkan);
        }

        addSpacer();

        // ── 2. REKAP FORMAL ─────────────────────────────────────
        addHeader("REKAP PENGUJIAN FORMAL");
        addSubHeader("Perbandingan final Q-WKNN vs WKNN");

        String[] rekapFormal = db.getRekapFormal();
        if (rekapFormal == null) {
            addText("Belum ada data 'Pengujian Formal' tersimpan.");
        } else {
            addText("Parameter dipakai: η=" + rekapFormal[5] + ", L=" + rekapFormal[6]);
            addText("Jumlah TP diuji  : " + rekapFormal[0]);
            addSpacer();

            double meanQ = parseD(rekapFormal[1]);
            double meanW = parseD(rekapFormal[3]);
            double p75Q  = parseD(rekapFormal[2]);
            double p75W  = parseD(rekapFormal[4]);

            // Persentase penurunan error (sesuai Table 2 artikel Zhou)
            double pctMean = meanW > 0 ? (meanW - meanQ) / meanW * 100.0 : 0;
            double pctP75  = p75W  > 0 ? (p75W  - p75Q)  / p75W  * 100.0 : 0;

            addTableRow("Metrik", "Q-WKNN", "WKNN", "Penurunan (%)", "", "", "", true);
            addTableRow("Mean Error",
                    rekapFormal[1] + "m",
                    rekapFormal[3] + "m",
                    String.format(java.util.Locale.US, "%.2f%%", pctMean),
                    "", "", "", false);
            addTableRow("Persentil-75",
                    rekapFormal[2] + "m",
                    rekapFormal[4] + "m",
                    String.format(java.util.Locale.US, "%.2f%%", pctP75),
                    "", "", "", false);

            String kesimpulan = meanQ < meanW
                    ? "Q-WKNN LEBIH BAIK dari WKNN\n  Mean error turun " +
                    String.format(java.util.Locale.US, "%.3f", meanW - meanQ) +
                    "m (" + String.format(java.util.Locale.US, "%.2f%%", pctMean) + ")"
                    : "WKNN lebih baik dari Q-WKNN\n  Mean error selisih " +
                    String.format(java.util.Locale.US, "%.3f", meanQ - meanW) + "m";
            addText("\n→ " + kesimpulan);
        }

        addSpacer();

        // ── 3. DISTRIBUSI K ─────────────────────────────────────
        addHeader("DISTRIBUSI K ADAPTIVE");

        for (String jenisLabel : new String[]{"parameter", "pengujian"}) {
            String label = jenisLabel.equals("parameter") ? "Cari Parameter" : "Pengujian Formal";
            addSubHeader("Jenis: " + label);
            List<String[]> distK = db.getDistribusiK(jenisLabel);
            if (distK.isEmpty()) {
                addText("Belum ada data.");
            } else {
                String lastAlgo = "";
                for (String[] row : distK) {
                    if (!row[0].equals(lastAlgo)) {
                        addText(row[0] + ":");
                        lastAlgo = row[0];
                    }
                    addText("   K=" + row[1] + " → " + row[2] + " kali");
                }
            }
        }
    }

    // ── HELPER UI ───────────────────────────────────────────────
    private void addHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText("\n" + text);
        tv.setTextSize(16);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(0xFF1565C0);
        llContent.addView(tv);
    }

    private void addSubHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTypeface(null, android.graphics.Typeface.ITALIC);
        tv.setPadding(0, 4, 0, 8);
        llContent.addView(tv);
    }

    private void addText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        tv.setPadding(0, 2, 0, 2);
        llContent.addView(tv);
    }

    private void addTableRow(String c1, String c2, String c3,
                             String c4, String c5, String c6, String c7,
                             boolean isHeader) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        if (isHeader) row.setBackgroundColor(0xFFE3F2FD);

        String[] cols = {c1, c2, c3, c4, c5, c6, c7};
        int[] weights = {2, 1, 1, 2, 2, 2, 2};
        for (int i = 0; i < cols.length; i++) {
            if (cols[i].isEmpty()) continue;
            TextView tv = new TextView(this);
            tv.setText(cols[i]);
            tv.setTextSize(11);
            tv.setPadding(4, 6, 4, 6);
            if (isHeader) tv.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, weights[i]);
            tv.setLayoutParams(lp);
            row.addView(tv);
        }
        llContent.addView(row);
    }

    private void addSpacer() {
        android.view.View v = new android.view.View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2));
        v.setBackgroundColor(0xFFBBBBBB);
        v.setPadding(0, 8, 0, 8);
        llContent.addView(v);
    }

    // Helper: parse double yang aman untuk locale Indonesia (koma = desimal)
    private double parseD(String s) {
        if (s == null || s.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) db.close();
    }
}