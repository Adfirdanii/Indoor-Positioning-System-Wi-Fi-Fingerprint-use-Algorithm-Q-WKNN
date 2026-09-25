package com.implementasi.myqwknn.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.implementasi.myqwknn.R;
import com.implementasi.myqwknn.algorithm.QWKNNAlgorithm;
import com.implementasi.myqwknn.algorithm.WKNNAlgorithm;
import com.implementasi.myqwknn.model.PositionResult;
import com.implementasi.myqwknn.model.ReferencePoint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZenodoActivity extends AppCompatActivity {

    private static final int    NUM_AP  = 620;
    private static final int    TOP_L   = 90;
    private static final double ETA     = 7.0;
    private static final double U       = 100.0;
    private static final int    FLOOR   = 3;

    private TextView tvStatus, tvResult;
    private Button   btnRun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zenodo);
        tvStatus = findViewById(R.id.tv_status);
        tvResult = findViewById(R.id.tv_result);
        btnRun   = findViewById(R.id.btn_run_zenodo);
        btnRun.setOnClickListener(v -> runVerification());
    }

    private void runVerification() {
        btnRun.setEnabled(false);
        tvResult.setText("");
        tvStatus.setText("Memuat data mentah Zenodo bulan 01...");

        new Thread(() -> {
            try {
                // 1. Load trn01 — 288 baris raw lantai 3
                log("Load trn01rss.csv + trn01crd.csv...");
                List<double[]> trnRss = loadRss("trn01rss.csv");
                List<double[]> trnCrd = loadCrd("trn01crd.csv");

                // Filter lantai 3
                List<double[]> fpRss = new ArrayList<>();
                List<double[]> fpCrd = new ArrayList<>();
                for (int i = 0; i < trnCrd.size(); i++) {
                    if ((int) trnCrd.get(i)[2] == FLOOR) {
                        fpRss.add(trnRss.get(i));
                        fpCrd.add(trnCrd.get(i));
                    }
                }
                log("Fingerprint: " + fpRss.size() + " baris (raw, lantai 3)");

                // 2. AP Selection dari 288 baris raw
                log("AP Selection top-" + TOP_L + " dari " + NUM_AP + " AP...");
                List<Integer> topAps = apSelection(fpRss, TOP_L);
                log("AP terpilih: " + topAps.size());

                // 3. Build ReferencePoint list dari 288 baris
                // Setiap baris = 1 ReferencePoint (raw, tidak di-average)
                List<ReferencePoint> fingerprints = new ArrayList<>();
                for (int i = 0; i < fpRss.size(); i++) {
                    double x = fpCrd.get(i)[0];
                    double y = fpCrd.get(i)[1];
                    ReferencePoint rp = new ReferencePoint(i, "Z_" + i, x, y);
                    for (int ap : topAps) {
                        double rss = fpRss.get(i)[ap];
                        // Nilai 100 dipertahankan apa adanya
                        rp.addRss("AP_" + ap, rss);
                    }
                    fingerprints.add(rp);
                }

                // 4. Load semua tst01-tst05
                log("Load tst01-tst05...");
                List<double[]> tstRssAll = new ArrayList<>();
                List<double[]> tstCrdAll = new ArrayList<>();
                for (int t = 1; t <= 5; t++) {
                    List<double[]> tr = loadRss(String.format("tst%02drss.csv", t));
                    List<double[]> tc = loadCrd(String.format("tst%02dcrd.csv", t));
                    for (int i = 0; i < tc.size(); i++) {
                        if ((int) tc.get(i)[2] == FLOOR) {
                            tstRssAll.add(tr.get(i));
                            tstCrdAll.add(tc.get(i));
                        }
                    }
                }
                log("Test points: " + tstRssAll.size() + " TP");

                // 5. Buat selected bssids list
                List<String> selectedBssids = new ArrayList<>();
                for (int ap : topAps) selectedBssids.add("AP_" + ap);

                // 6. Jalankan Q-WKNN + WKNN
                log("Menjalankan Q-WKNN dan WKNN...");
                QWKNNAlgorithm qwknn = new QWKNNAlgorithm(ETA);
                WKNNAlgorithm  wknn  = new WKNNAlgorithm(3);

                List<Double> errsQ = new ArrayList<>();
                List<Double> errsW = new ArrayList<>();

                for (int i = 0; i < tstRssAll.size(); i++) {
                    // Buat test RSS map
                    Map<String, Double> testMap = new HashMap<>();
                    for (int ap : topAps) {
                        // Nilai 100 dipertahankan
                        testMap.put("AP_" + ap, tstRssAll.get(i)[ap]);
                    }

                    double[] gt = tstCrdAll.get(i);

                    PositionResult qRes = qwknn.calculate(
                            testMap, fingerprints, selectedBssids);
                    PositionResult wRes = wknn.calculate(
                            testMap, fingerprints, selectedBssids);

                    if (qRes != null && wRes != null) {
                        errsQ.add(dist(qRes.getX(), qRes.getY(), gt[0], gt[1]));
                        errsW.add(dist(wRes.getX(), wRes.getY(), gt[0], gt[1]));
                    }
                }

                double meanQ = mean(errsQ);
                double meanW = mean(errsW);
                double p75Q  = percentile75(errsQ);
                double p75W  = percentile75(errsW);

                String result =
                                "===== Hasil  =====\n\n" +
                                "Dataset  : UJI Library v2.2\n" +
                                "DOI      : 10.5281/zenodo.1066041\n" +
                                "Bulan    : 01 (dari 20 bulan valid)\n" +
                                "Train    : " + fpRss.size() + " baris raw (trn01, lantai 3)\n" +
                                "Test     : " + errsQ.size() + " TP (tst01-tst05, lantai 3)\n" +
                                "AP pilih : " + topAps.size() + " dari " + NUM_AP + "\n" +
                                "η = " + ETA + ", L = " + TOP_L + "\n\n" +
                                "Metrik        Q-WKNN      WKNN\n" +
                                "Mean error  : " + String.format("%.3f", meanQ) + " m   " +
                                String.format("%.3f", meanW) + " m\n" +
                                "P-75 error  : " + String.format("%.3f", p75Q)  + " m   " +
                                String.format("%.3f", p75W)  + " m\n\n" +
                                "Q-WKNN < WKNN (mean) : " + (meanQ < meanW ? "YA ✓" : "TIDAK ✗") + "\n" +
                                "Q-WKNN < WKNN (p75)  : " + (p75Q  < p75W  ? "YA ✓" : "TIDAK ✗") + "\n\n" +
                                "Referensi Zhou (2021) rata-rata 20 bulan:\n" +
                                "Mean error  : 1.858 m   2.331 m\n" +
                                "P-75 error  : 2.524 m   3.075 m\n\n" +
                                        "Hasil ini adalah hasil bulan 1\n" +
                                        "Hasil 20 bulan bisa dilakukan setiap data per bulan\n" +
                                        "kemudian di rata-rata untuk mendekati hasil artikel Zhou." ;

                showResult(result);

            } catch (Exception e) {
                showResult("Error: " + e.getMessage());
            }
        }).start();
    }

    // ── Helper ───────────────────────────────────────────────

    private List<double[]> loadRss(String filename) throws Exception {
        List<double[]> list = new ArrayList<>();
        BufferedReader br = new BufferedReader(
                new InputStreamReader(getAssets().open(filename)));
        String line;
        while ((line = br.readLine()) != null) {
            String[] p = line.split(",");
            double[] row = new double[p.length];
            for (int i = 0; i < p.length; i++)
                row[i] = Double.parseDouble(p[i].trim());
            list.add(row);
        }
        br.close();
        return list;
    }

    private List<double[]> loadCrd(String filename) throws Exception {
        List<double[]> list = new ArrayList<>();
        BufferedReader br = new BufferedReader(
                new InputStreamReader(getAssets().open(filename)));
        String line;
        while ((line = br.readLine()) != null) {
            String[] p = line.split(",");
            list.add(new double[]{
                    Double.parseDouble(p[0].trim()),
                    Double.parseDouble(p[1].trim()),
                    Double.parseDouble(p[2].trim())
            });
        }
        br.close();
        return list;
    }

    private List<Integer> apSelection(List<double[]> fpRss, int topL) {
        Map<Integer, Double> scores = new HashMap<>();
        int n = fpRss.size();
        for (int ap = 0; ap < NUM_AP; ap++) {
            double maxRss = -999; int cnt = 0;
            for (double[] row : fpRss) {
                if (row[ap] != U) {
                    cnt++;
                    if (row[ap] > maxRss) maxRss = row[ap];
                }
            }
            if (cnt == 0) continue;
            double mAp = (maxRss + U) / U;
            double pAp = (double) n / (n - cnt + 0.001);
            scores.put(ap, mAp * pAp);
        }
        List<Integer> sorted = new ArrayList<>(scores.keySet());
        sorted.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));
        return sorted.subList(0, Math.min(topL, sorted.size()));
    }

    private double dist(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
    }

    private double mean(List<Double> l) {
        double s=0; for (double v:l) s+=v; return s/l.size();
    }

    private double percentile75(List<Double> l) {
        List<Double> s = new ArrayList<>(l);
        Collections.sort(s);
        return s.get((int)(0.75*(s.size()-1)));
    }

    private void log(String msg) {
        new Handler(Looper.getMainLooper()).post(() -> tvStatus.setText(msg));
    }

    private void showResult(String result) {
        new Handler(Looper.getMainLooper()).post(() -> {
            tvStatus.setText("Selesai.");
            tvResult.setText(result);
            btnRun.setEnabled(true);
        });
    }
}