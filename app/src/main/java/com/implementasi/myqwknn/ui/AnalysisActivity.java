package com.implementasi.myqwknn.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.implementasi.myqwknn.R;
import com.implementasi.myqwknn.algorithm.PositioningEngine;
import com.implementasi.myqwknn.database.FingerprintDatabase;
import com.implementasi.myqwknn.model.AccessPoint;
import com.implementasi.myqwknn.model.PositionResult;
import com.implementasi.myqwknn.model.ReferencePoint;
import com.implementasi.myqwknn.scanner.WifiScanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisActivity extends AppCompatActivity {

    // ── Koordinat ground truth 20 TP (hardcode dari denah, tidak bisa salah ketik) ──
    private static final String[] TP_NAMES = {
            "TP1","TP2","TP3","TP4","TP5","TP6","TP7","TP8","TP9","TP10",
            "TP11","TP12","TP13","TP14","TP15","TP16","TP17","TP18","TP19"
    };
    private static final double[][] TP_COORDS = {
            {6.38,  17.25},  // TP1  — AH 1.1
            {13.07, 17.25},  // TP2  — AH 1.2
            {21.32, 17.25},  // TP3  — AH 1.3
            {25.21, 10.45},  // TP4  — AH 1.4
            {33.56, 17.25},  // TP5  — AH 1.5
            {41.74, 17.25},  // TP6  — AH 1.6
            {1.0,   1.0},    // TP7  — AH 1.14  (dulu TP8)
            {8.38,  1.0},    // TP8  — AH 1.13  (dulu TP9)
            {15.76, 1.0},    // TP9  — AH 1.12  (dulu TP10)
            {28.25, 7.8},    // TP10 — selasar   (dulu TP11)
            {34.36, 1.0},    // TP11 — AH 1.9   (dulu TP12)
            {4.0,   8.0},    // TP12 — selasar bawah (dulu TP13)
            {10.0,  9.8},    // TP13 — selasar atas  (dulu TP14)
            {16.0,  8.0},    // TP14 — selasar bawah (dulu TP15)
            {22.0,  9.8},    // TP15 — selasar atas  (dulu TP16)
            {24.56, 5.0},    // TP16 — lobi tengah   (dulu TP17)
            {34.0,  8.0},    // TP17 — selasar bawah (dulu TP18)
            {40.0,  8.0},    // TP18 — selasar bawah (dulu TP19)
            {46.0,  9.8},    // TP19 — selasar atas  (dulu TP20)
    };

    // Jumlah scan untuk pengujian formal (sesuai Zhou Park = 10)
    private static final int SCAN_PENGUJIAN = 10;
    // Jumlah scan untuk sweep parameter (disamakan dgn pengujian formal)
    private static final int SCAN_SWEEP = 10;

    // ── UI ──
    private Spinner spinnerTp;
    private TextView tvTrueCoord, tvTpScanStatus;
    private Button btnScanSweep, btnScanPengujian, btnHapusTpRaw, btnHapusFormal, btnSweepSemua;
    private EditText etEta, etL;
    private Button btnRekap;
    private TextView tvResultSweep, tvResultFormal;
    private LinearLayout llResultList;

    private FingerprintDatabase db;
    private WifiScanner wifiScanner;

    private double lastEta, lastTrueX, lastTrueY;
    private int lastL;
    private String selectedTpName = "TP1";

    // Akumulator hasil pengujian formal (dipakai untuk ringkasan mean error
    // 10x scan, bukan cuma menampilkan scan terakhir)
    private final List<Double> formalErrQList = new ArrayList<>();
    private final List<Double> formalErrWList = new ArrayList<>();
    private final List<Integer> formalKQList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        db          = new FingerprintDatabase(this);
        wifiScanner = new WifiScanner(this);

        spinnerTp        = findViewById(R.id.spinner_tp);
        tvTrueCoord      = findViewById(R.id.tv_true_coord);
        tvTpScanStatus   = findViewById(R.id.tv_tp_scan_status);
        btnScanSweep     = findViewById(R.id.btn_scan_sweep);
        btnScanPengujian = findViewById(R.id.btn_scan_pengujian);
        btnHapusTpRaw    = findViewById(R.id.btn_hapus_tp_raw);
        btnHapusFormal   = findViewById(R.id.btn_hapus_formal_tp);
        btnSweepSemua    = findViewById(R.id.btn_sweep_semua);
        etEta            = findViewById(R.id.et_eta);
        etL              = findViewById(R.id.et_l);
        btnRekap         = findViewById(R.id.btn_rekap);
        tvResultSweep    = findViewById(R.id.tv_result_sweep);
        tvResultFormal   = findViewById(R.id.tv_result_formal);
        llResultList     = findViewById(R.id.ll_result_list);

        setupSpinner();

        btnScanSweep.setOnClickListener(v -> scanTpRaw("sweep"));
        btnScanPengujian.setOnClickListener(v -> scanPengujianFormal());
        btnHapusTpRaw.setOnClickListener(v -> hapusTpRaw());
        btnHapusFormal.setOnClickListener(v -> hapusPengujianFormalTp());
        btnSweepSemua.setOnClickListener(v -> sweepSemuaKombinasi());
        btnRekap.setOnClickListener(v ->
                startActivity(new Intent(this, RekapActivity.class)));

        refreshResultList();
    }

    // ════════════════════════════════════════════════════════════
    // SPINNER
    // ════════════════════════════════════════════════════════════
    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, TP_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTp.setAdapter(adapter);

        spinnerTp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedTpName = TP_NAMES[pos];
                lastTrueX = TP_COORDS[pos][0];
                lastTrueY = TP_COORDS[pos][1];
                tvTrueCoord.setText(String.format(
                        "Koordinat sebenarnya: (%.2f, %.2f) m", lastTrueX, lastTrueY));
                refreshTpScanStatus();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void refreshTpScanStatus() {
        int n = db.getTpRawScanCount(selectedTpName);
        if (n == 0) {
            tvTpScanStatus.setText("Belum ada raw scan tersimpan.");
            btnHapusTpRaw.setVisibility(View.GONE);
        } else {
            tvTpScanStatus.setText("✓ " + n + " sesi scan tersimpan untuk sweep.");
            btnHapusTpRaw.setVisibility(View.VISIBLE);
        }
    }

    // ════════════════════════════════════════════════════════════
    // SCAN SWEEP — 1 klik = otomatis SCAN_SWEEP (10x) scan berturut,
    // sama seperti pola pengujian formal. Semua tersimpan sbg
    // sesi scan berbeda (scan_id 1..10) di tabel tp_raw_scan.
    // ════════════════════════════════════════════════════════════
    private void scanTpRaw(String mode) {
        if (!wifiScanner.isWifiEnabled()) {
            Toast.makeText(this, "WiFi tidak aktif", Toast.LENGTH_SHORT).show();
            return;
        }
        btnScanSweep.setEnabled(false);
        tvTpScanStatus.setText("Sweep " + selectedTpName + " — scan 1/" + SCAN_SWEEP + "...");
        jalankanSweepKe(1);
    }

    // Deteksi scan yang identik dengan scan sebelumnya (indikasi Android
    // scan-throttling mengembalikan cache lama, bukan scan baru).
    private String lastSweepSignature = null;
    private int sweepThrottleRetry = 0;
    private String lastFormalSignature = null;
    private int formalThrottleRetry = 0;
    private static final int MAX_THROTTLE_RETRY = 6;

    private String buatSignature(List<AccessPoint> results) {
        List<String> parts = new ArrayList<>();
        for (AccessPoint ap : results) {
            parts.add(ap.getBssid() + ":" + ap.getRss());
        }
        Collections.sort(parts);
        return String.join(",", parts);
    }

    private void jalankanSweepKe(int scanKe) {
        if (scanKe > SCAN_SWEEP) {
            lastSweepSignature = null;
            sweepThrottleRetry = 0;
            new Handler(Looper.getMainLooper()).post(() -> {
                btnScanSweep.setEnabled(true);
                refreshTpScanStatus();
                Toast.makeText(this,
                        selectedTpName + " sweep selesai (" + SCAN_SWEEP + "x scan)",
                        Toast.LENGTH_SHORT).show();
            });
            return;
        }

        final int scanKeF = scanKe;
        tvTpScanStatus.post(() -> tvTpScanStatus.setText(
                "Sweep " + selectedTpName + " — scan " + scanKeF + "/" + SCAN_SWEEP + "..."));

        wifiScanner.startScan(new WifiScanner.ScanCallback() {
            @Override
            public void onScanComplete(List<AccessPoint> results) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!results.isEmpty()) {
                        String sig = buatSignature(results);

                        // 🔍 Cek apakah hasil scan ini identik dengan scan sebelumnya
                        // (indikasi kena throttle Android, bukan scan baru beneran)
                        if (sig.equals(lastSweepSignature) && sweepThrottleRetry < MAX_THROTTLE_RETRY) {
                            sweepThrottleRetry++;
                            int delaySec = 8 * sweepThrottleRetry; // 8,16,24,32,40,48s
                            tvTpScanStatus.setText("Sweep " + selectedTpName +
                                    " — scan " + scanKeF + " kena throttle, tunggu " +
                                    delaySec + "s lalu coba lagi (" + sweepThrottleRetry +
                                    "/" + MAX_THROTTLE_RETRY + ")...");
                            new Handler(Looper.getMainLooper()).postDelayed(
                                    () -> jalankanSweepKe(scanKeF), delaySec * 1000L);
                            return;
                        }

                        lastSweepSignature = sig;
                        sweepThrottleRetry = 0;

                        int scanId = db.getTpRawScanCount(selectedTpName) + 1;
                        List<String> bssids = new ArrayList<>();
                        List<Double> rssList = new ArrayList<>();
                        for (AccessPoint ap : results) {
                            bssids.add(ap.getBssid());
                            rssList.add(ap.getRss());
                        }
                        db.insertTpRawScan(selectedTpName, scanId, bssids, rssList);
                    }
                    // Jeda kecil sebelum scan berikutnya, membantu menghindari throttle
                    new Handler(Looper.getMainLooper()).postDelayed(
                            () -> jalankanSweepKe(scanKeF + 1), 1500);
                });
            }
            @Override
            public void onScanFailed(String reason) {
                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> jalankanSweepKe(scanKeF + 1), 1500);
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    // SWEEP SEMUA KOMBINASI — 2 tahap sesuai urutan Zhou:
    // Tahap 1: cari eta terbaik (L = semua AP, belum ada AP selection)
    // Tahap 2: cari L terbaik (eta dikunci ke hasil Tahap 1)
    // Keduanya pakai Q-WKNN dengan adaptive K (sistem yang benar-benar
    // dipakai), bukan KNN K=3 tetap seperti Zhou — simplifikasi yang
    // sudah didiskusikan & dijustifikasi di metodologi.
    // ════════════════════════════════════════════════════════════
    private void sweepSemuaKombinasi() {
        double[] etaList = {2, 3, 4, 5, 6, 7};
        int[] lList = {10, 20, 30, 40, 50, 60, 70, 80};

        List<String> tpNames = db.getAllTpNamesWithRaw();
        if (tpNames.isEmpty()) {
            Toast.makeText(this, "Belum ada raw scan sweep tersimpan.", Toast.LENGTH_LONG).show();
            return;
        }

        tvResultSweep.setText("Menjalankan batch sweep (Tahap 1: eta)...\n0%");

        new Thread(() -> {
            List<ReferencePoint> fingerprints = db.getAllFingerprints();
            List<String> allBssids = getAllBssidsFromFingerprints(fingerprints);
            int lAll = allBssids.size();

            Map<String, Map<String, Double>> tpRssMap = new HashMap<>();
            Map<String, double[]> tpTrueCoord = new HashMap<>();
            for (String tpName : tpNames) {
                Map<String, Double> rss = db.getTpRawScanMapAveraged(tpName);
                if (rss.isEmpty()) continue;
                tpRssMap.put(tpName, rss);
                for (int i = 0; i < TP_NAMES.length; i++) {
                    if (TP_NAMES[i].equals(tpName)) {
                        tpTrueCoord.put(tpName, TP_COORDS[i]);
                        break;
                    }
                }
            }

            // ═══ TAHAP 1: sweep eta, L = ALL AP ═══
            double bestEta = etaList[0];
            double bestMeanErr = Double.MAX_VALUE;
            int totalTahap1 = tpRssMap.size() * etaList.length;
            int selesai = 0;

            for (double eta : etaList) {
                List<Double> errList = new ArrayList<>();
                for (Map.Entry<String, Map<String, Double>> entry : tpRssMap.entrySet()) {
                    String tpName = entry.getKey();
                    Map<String, Double> testRssMap = entry.getValue();
                    double[] trueXY = tpTrueCoord.get(tpName);
                    if (trueXY == null) continue;

                    PositioningEngine engine = new PositioningEngine(db);
                    engine.setEta(eta);
                    engine.setTopL(lAll);

                    PositionResult qRes = engine.runQWKNNDirect(testRssMap, fingerprints, allBssids);
                    if (qRes != null) {
                        double err = Math.sqrt(Math.pow(qRes.getX() - trueXY[0], 2) +
                                Math.pow(qRes.getY() - trueXY[1], 2));
                        errList.add(err);

                        int scanKeDb = db.getScanCountForTp(tpName) + 1;
                        db.insertAnalysisResult(
                                tpName, scanKeDb, eta, lAll, trueXY[0], trueXY[1],
                                qRes.getRoomName(), qRes.getX(), qRes.getY(), err,
                                "-", 0, 0, 999, "parameter_eta",
                                qRes.getKAdaptive(), 0
                        );
                    }

                    selesai++;
                    int progress = selesai * 50 / totalTahap1;
                    int finalSelesai = selesai;
                    new Handler(Looper.getMainLooper()).post(() ->
                            tvResultSweep.setText("Tahap 1 (eta): " + progress + "% (" +
                                    finalSelesai + "/" + totalTahap1 + ")"));
                }

                double meanErr = 0;
                for (double e : errList) meanErr += e;
                if (!errList.isEmpty()) meanErr /= errList.size();

                if (meanErr < bestMeanErr) {
                    bestMeanErr = meanErr;
                    bestEta = eta;
                }
            }

            final double etaTerbaik = bestEta;
            new Handler(Looper.getMainLooper()).post(() ->
                    tvResultSweep.setText("Tahap 1 selesai. eta terbaik = " + etaTerbaik +
                            "\nLanjut Tahap 2 (L)...\n0%"));

            // ═══ TAHAP 2: sweep L, eta = eta terbaik dari Tahap 1 ═══
            int totalTahap2 = tpRssMap.size() * lList.length;
            selesai = 0;

            for (int lVal : lList) {
                for (Map.Entry<String, Map<String, Double>> entry : tpRssMap.entrySet()) {
                    String tpName = entry.getKey();
                    Map<String, Double> testRssMap = entry.getValue();
                    double[] trueXY = tpTrueCoord.get(tpName);
                    if (trueXY == null) continue;

                    PositioningEngine engine = new PositioningEngine(db);
                    engine.setEta(etaTerbaik);
                    engine.setTopL(lVal);

                    List<String> selectedBssids = db.getSelectedBssids(lVal);
                    if (selectedBssids.isEmpty()) selectedBssids = allBssids;

                    PositionResult qRes = engine.runQWKNNDirect(testRssMap, fingerprints, selectedBssids);
                    PositionResult wRes = engine.runWKNNDirect(testRssMap, fingerprints, selectedBssids);

                    double errQ = (qRes != null) ? Math.sqrt(Math.pow(qRes.getX() - trueXY[0], 2) +
                            Math.pow(qRes.getY() - trueXY[1], 2)) : 999;
                    double errW = (wRes != null) ? Math.sqrt(Math.pow(wRes.getX() - trueXY[0], 2) +
                            Math.pow(wRes.getY() - trueXY[1], 2)) : 999;

                    int scanKeDb = db.getScanCountForTp(tpName) + 1;
                    db.insertAnalysisResult(
                            tpName, scanKeDb, etaTerbaik, lVal, trueXY[0], trueXY[1],
                            qRes != null ? qRes.getRoomName() : "-",
                            qRes != null ? qRes.getX() : 0,
                            qRes != null ? qRes.getY() : 0, errQ,
                            wRes != null ? wRes.getRoomName() : "-",
                            wRes != null ? wRes.getX() : 0,
                            wRes != null ? wRes.getY() : 0, errW,
                            "parameter",
                            qRes != null ? qRes.getKAdaptive() : 0,
                            wRes != null ? wRes.getKAdaptive() : 0
                    );

                    selesai++;
                    int progress = 50 + (selesai * 50 / totalTahap2);
                    int finalSelesai = selesai;
                    new Handler(Looper.getMainLooper()).post(() ->
                            tvResultSweep.setText("Tahap 2 (L): " + progress + "% (" +
                                    finalSelesai + "/" + totalTahap2 + ")"));
                }
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                tvResultSweep.setText("Batch sweep selesai!\n" +
                        "eta terbaik (Tahap 1): " + etaTerbaik + "\n" +
                        "Lihat REKAP untuk L terbaik (Tahap 2) dan detail lainnya.");
                refreshResultList();
            });
        }).start();
    }

    // ════════════════════════════════════════════════════════════
    // SCAN 10x — untuk pengujian formal
    // Alasan 10x: mengukur akurasi algoritma secara statistik.
    // RSS fluktuatif — 1 scan bisa kebetulan bagus/buruk. Dengan
    // 10x scan, fluktuasi sesaat teraveragekan sehingga mean error
    // representatif kondisi sebenarnya. Sesuai prosedur Zhou Park.
    // Tiap scan = 1 pengujian terpisah (tidak dirata-rata).
    // ════════════════════════════════════════════════════════════
    private void scanPengujianFormal() {
        String etaStr = etEta.getText().toString().trim();
        String lStr   = etL.getText().toString().trim();
        if (etaStr.isEmpty() || lStr.isEmpty()) {
            Toast.makeText(this, "Isi η dan L optimal dulu", Toast.LENGTH_SHORT).show();
            return;
        }

        lastEta = Double.parseDouble(etaStr);
        lastL   = Integer.parseInt(lStr);

        new AlertDialog.Builder(this)
                .setTitle("Pengujian Formal " + selectedTpName)
                .setMessage("Akan dilakukan " + SCAN_PENGUJIAN + "x scan berturut-turut.\n\n" +
                        "η=" + lastEta + "  L=" + lastL + "\n" +
                        "Koordinat: (" + lastTrueX + ", " + lastTrueY + ")\n\n" +
                        "Pastikan kamu berdiri di titik " + selectedTpName +
                        " dan tidak berpindah selama scan berlangsung.")
                .setPositiveButton("Mulai", (d, w) -> jalankanPengujianFormal())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void jalankanPengujianFormal() {
        btnScanPengujian.setEnabled(false);
        btnScanSweep.setEnabled(false);
        formalErrQList.clear();
        formalErrWList.clear();
        formalKQList.clear();
        lastFormalSignature = null;
        formalThrottleRetry = 0;
        tvResultFormal.setText("Pengujian formal " + selectedTpName +
                " — scan 1/" + SCAN_PENGUJIAN + "...");

        // Rekursif: scan satu per satu sebanyak SCAN_PENGUJIAN kali
        jalankanScanKe(1);
    }

    private void jalankanScanKe(int scanKe) {
        if (scanKe > SCAN_PENGUJIAN) {
            lastFormalSignature = null;
            formalThrottleRetry = 0;
            // Selesai semua scan — tampilkan RINGKASAN, bukan cuma pesan "tersimpan"
            new Handler(Looper.getMainLooper()).post(() -> {
                btnScanPengujian.setEnabled(true);
                btnScanSweep.setEnabled(true);

                int n = formalErrQList.size();
                if (n == 0) {
                    tvResultFormal.setText("Pengujian formal " + selectedTpName +
                            " selesai, tapi tidak ada scan yang berhasil (AP kosong terus).");
                } else {
                    double meanQ = 0, meanW = 0;
                    for (double e : formalErrQList) meanQ += e;
                    for (double e : formalErrWList) meanW += e;
                    meanQ /= n; meanW /= n;
                    double avgK = 0;
                    for (int k : formalKQList) avgK += k;
                    avgK /= n;

                    tvResultFormal.setText(
                            "Pengujian formal " + selectedTpName + " selesai (" + n + "/" + SCAN_PENGUJIAN + " scan valid)\n" +
                                    "η=" + lastEta + "  L=" + lastL + "\n\n" +
                                    "Mean Error Q-WKNN : " + String.format("%.3f", meanQ) + " m\n" +
                                    "Mean Error WKNN   : " + String.format("%.3f", meanW) + " m\n" +
                                    "K adaptif rata-rata: " + String.format("%.1f", avgK) + "\n\n" +
                                    "Detail per-scan ada di menu Lihat Rekap.");
                }
                refreshResultList();
                Toast.makeText(this,
                        selectedTpName + " pengujian formal selesai!",
                        Toast.LENGTH_SHORT).show();
            });
            return;
        }

        int scanKeF = scanKe;
        tvResultFormal.post(() -> tvResultFormal.setText(
                "Pengujian formal " + selectedTpName +
                        " — scan " + scanKeF + "/" + SCAN_PENGUJIAN + "..."));

        wifiScanner.startScan(new WifiScanner.ScanCallback() {
            @Override
            public void onScanComplete(List<AccessPoint> results) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!results.isEmpty()) {
                        String sig = buatSignature(results);

                        // 🔍 Cek apakah hasil scan ini identik dengan scan sebelumnya
                        // (indikasi kena throttle Android, bukan scan baru beneran)
                        if (sig.equals(lastFormalSignature) && formalThrottleRetry < MAX_THROTTLE_RETRY) {
                            formalThrottleRetry++;
                            int delaySec = 8 * formalThrottleRetry; // 8,16,24,32,40,48s
                            tvResultFormal.setText("Pengujian formal " + selectedTpName +
                                    " — scan " + scanKeF + " kena throttle, tunggu " +
                                    delaySec + "s lalu coba lagi (" + formalThrottleRetry +
                                    "/" + MAX_THROTTLE_RETRY + ")...");
                            new Handler(Looper.getMainLooper()).postDelayed(
                                    () -> jalankanScanKe(scanKeF), delaySec * 1000L);
                            return;
                        }
                        lastFormalSignature = sig;
                        formalThrottleRetry = 0;

                        // Buat RSS map dari scan ini
                        Map<String, Double> testRssMap = new HashMap<>();
                        for (AccessPoint ap : results) {
                            testRssMap.put(ap.getBssid(), ap.getRss());
                        }

                        // Hitung Q-WKNN dan WKNN langsung dari scan live ini
                        new Thread(() -> {
                            PositioningEngine engine = new PositioningEngine(db);
                            engine.setEta(lastEta);
                            engine.setTopL(lastL);

                            List<ReferencePoint> fingerprints = db.getAllFingerprints();
                            List<String> selectedBssids = db.getSelectedBssids(lastL);
                            if (selectedBssids.isEmpty()) {
                                selectedBssids = getAllBssidsFromFingerprints(fingerprints);
                            }

                            PositionResult qRes = engine.runQWKNNDirect(
                                    testRssMap, fingerprints, selectedBssids);
                            PositionResult wRes = engine.runWKNNDirect(
                                    testRssMap, fingerprints, selectedBssids);

                            double errQ = (qRes != null) ?
                                    Math.sqrt(Math.pow(qRes.getX()-lastTrueX,2) +
                                            Math.pow(qRes.getY()-lastTrueY,2)) : 999;
                            double errW = (wRes != null) ?
                                    Math.sqrt(Math.pow(wRes.getX()-lastTrueX,2) +
                                            Math.pow(wRes.getY()-lastTrueY,2)) : 999;

                            int scanKeDb = db.getScanCountForTp(selectedTpName) + 1;
                            db.insertAnalysisResult(
                                    selectedTpName, scanKeDb, lastEta, lastL,
                                    lastTrueX, lastTrueY,
                                    qRes != null ? qRes.getRoomName() : "-",
                                    qRes != null ? qRes.getX() : 0,
                                    qRes != null ? qRes.getY() : 0,
                                    errQ,
                                    wRes != null ? wRes.getRoomName() : "-",
                                    wRes != null ? wRes.getX() : 0,
                                    wRes != null ? wRes.getY() : 0,
                                    errW,
                                    "pengujian",
                                    qRes != null ? qRes.getKAdaptive() : 0,
                                    wRes != null ? wRes.getKAdaptive() : 0
                            );

                            formalErrQList.add(errQ);
                            formalErrWList.add(errW);
                            formalKQList.add(qRes != null ? qRes.getKAdaptive() : 0);

                            // Lanjut ke scan berikutnya (jeda kecil, hindari throttle)
                            new Handler(Looper.getMainLooper()).postDelayed(() ->
                                    jalankanScanKe(scanKeF + 1), 1500);
                        }).start();
                    } else {
                        // Scan kosong, skip dan lanjut
                        new Handler(Looper.getMainLooper()).postDelayed(
                                () -> jalankanScanKe(scanKeF + 1), 1500);
                    }
                });
            }

            @Override
            public void onScanFailed(String reason) {
                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> jalankanScanKe(scanKeF + 1), 1500);
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    // HAPUS RAW
    // ════════════════════════════════════════════════════════════
    private void hapusTpRaw() {
        new AlertDialog.Builder(this)
                .setTitle("Hapus raw scan " + selectedTpName + "?")
                .setMessage("Raw scan sweep untuk " + selectedTpName + " akan dihapus.")
                .setPositiveButton("Hapus", (d, w) -> {
                    db.deleteTpRawScan(selectedTpName);
                    refreshTpScanStatus();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void hapusPengujianFormalTp() {
        new AlertDialog.Builder(this)
                .setTitle("Hapus data Pengujian Formal " + selectedTpName + "?")
                .setMessage("Hanya data jenis 'Pengujian Formal' milik " + selectedTpName +
                        " yang akan dihapus. Data sweep parameter (η/L) dan TP lain TIDAK terpengaruh.")
                .setPositiveButton("Hapus", (d, w) -> {
                    db.deleteAnalysisResultByTpAndJenis(selectedTpName, "pengujian");
                    tvResultFormal.setText("Data Pengujian Formal " + selectedTpName + " sudah dihapus. Silakan scan ulang.");
                    refreshResultList();
                    Toast.makeText(this, "Data Pengujian Formal " + selectedTpName + " dihapus.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void refreshResultList() {
        llResultList.removeAllViews();
        List<String[]> results = db.getAllAnalysisResults();
        if (results.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Belum ada hasil tersimpan.");
            empty.setPadding(8, 8, 8, 8);
            llResultList.addView(empty);
            return;
        }
        for (String[] row : results) {
            int id        = Integer.parseInt(row[0]);
            String tpName = row[1];
            String scanKe = row[2];
            String eta    = row[3];
            String l      = row[4];
            String trueX  = row[5];
            String trueY  = row[6];
            String roomQ  = row[7];
            String errQ   = row[10];
            String roomW  = row[11];
            String errW   = row[14];
            String jenis  = row[15];
            String kQ     = (row[16] != null) ? row[16] : "-";
            String kW     = (row[17] != null) ? row[17] : "-";

            TextView tv = new TextView(this);
            tv.setPadding(8, 16, 8, 16);
            tv.setTextSize(12);
            tv.setText(
                    "[" + jenis.toUpperCase() + "] " + tpName +
                            " scan ke-" + scanKe + " | η=" + eta + " L=" + l + "\n" +
                            "  Q-WKNN: " + roomQ + " K=" + kQ +
                            " error=" + String.format("%.3f", Double.parseDouble(errQ)) + " m\n" +
                            "  WKNN  : " + roomW + " K=" + kW +
                            " error=" + String.format("%.3f", Double.parseDouble(errW)) + " m"
            );

            android.view.View divider = new android.view.View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(0xFFDDDDDD);

            tv.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Hapus hasil ini?")
                        .setPositiveButton("Hapus", (dialog, which) -> {
                            db.deleteAnalysisResult(id);
                            refreshResultList();
                        })
                        .setNegativeButton("Batal", null)
                        .show();
                return true;
            });

            llResultList.addView(tv);
            llResultList.addView(divider);
        }
    }

    private List<String> getAllBssidsFromFingerprints(List<ReferencePoint> fingerprints) {
        List<String> bssids = new ArrayList<>();
        for (ReferencePoint rp : fingerprints) {
            for (String bssid : rp.getRssMap().keySet()) {
                if (!bssids.contains(bssid)) bssids.add(bssid);
            }
        }
        return bssids;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wifiScanner != null) wifiScanner.release();
    }
}