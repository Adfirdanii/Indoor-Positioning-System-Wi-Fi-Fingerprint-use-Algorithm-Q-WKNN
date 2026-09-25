package com.implementasi.myqwknn.algorithm;

import android.util.Log;

import com.implementasi.myqwknn.model.PositionResult;
import com.implementasi.myqwknn.model.ReferencePoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * PERBAIKAN sesuai Rong Zhou, "An RSS Transform-Based WKNN for Indoor
 * Positioning", Sensors 2021, 21, 5685, Section 3.6 & 3.7.
 *
 * Perubahan dari versi asli:
 *  1. Sj = SUM murni (bukan rata-rata per matchedCount) -- Persamaan (16).
 *  2. RSS yang hilang (tidak terdeteksi) diganti nilai default RSS_MISSING
 *     (mis. -100 dBm), bukan di-skip. Ini wajib karena formula artikel
 *     menjumlahkan L AP secara utuh (i=1..L), dan AP dengan sinyal hilang
 *     justru harus menghasilkan d(i,j) besar (penalti), bukan dihilangkan
 *     dari pembagi rata-rata.
 *  3. E(G) dihitung HANYA dari i=2..f, dibagi (f-1) -- Persamaan (14).
 *     Versi lama menyertakan G1=0 ke pembilang & membagi dengan f,
 *     sehingga E(G) selalu setengah dari nilai yang seharusnya dan membuat
 *     adaptive K nyaris selalu runtuh ke K=1.
 */
public class QWKNNAlgorithm {

    // RSS default untuk AP yang tidak terdeteksi (harus SAMA dengan nilai U
    // yang dipakai di DataPreprocessor.runApSelection, agar konsisten).
    public static final double RSS_MISSING = -100.0;

    private double eta;   // eta - path loss exponent
    private double Q;     // Q = 10^(1/(10*eta))

    public QWKNNAlgorithm() {
        this.eta = 7.0;
        this.Q = Math.pow(10, 1.0 / (10.0 * eta));
    }

    public QWKNNAlgorithm(double eta) {
        this.eta = eta;
        this.Q = Math.pow(10, 1.0 / (10.0 * eta));
    }

    public void setEta(double eta) {
        this.eta = eta;
        this.Q = Math.pow(10, 1.0 / (10.0 * eta));
    }

    public double getEta() { return eta; }
    public double getQ()   { return Q; }

    public PositionResult calculate(Map<String, Double> testRssMap,
                                    List<ReferencePoint> fingerprints,
                                    List<String> selectedBssids) {

        long startTime = System.currentTimeMillis();

        if (fingerprints == null || fingerprints.isEmpty()) return null;
        if (selectedBssids == null || selectedBssids.isEmpty()) return null;

        // ── Step 1: Hitung Q-Manhattan distance (Sj) tiap RP ──────────
        // Sj = SUM_{i=1..L} |Q^RSS(i,j) - Q^RSS(i,TP)|   (Persamaan 15-16)
        // AP yang tidak terdeteksi (baik di RP maupun TP) diganti RSS_MISSING,
        // BUKAN di-skip, supaya seluruh RP dibandingkan dengan basis L AP yang sama.
        List<double[]> distList = new ArrayList<>();
        // distList[i] = {Sj, x, y, index_rp}

        for (int j = 0; j < fingerprints.size(); j++) {
            ReferencePoint rp = fingerprints.get(j);
            double Sj = 0;

            for (String bssid : selectedBssids) {
                Double rssRpObj = rp.getRssMap().get(bssid);
                Double rssTpObj = testRssMap.get(bssid);

                double rssRp = (rssRpObj == null) ? RSS_MISSING : rssRpObj;
                double rssTp = (rssTpObj == null) ? RSS_MISSING : rssTpObj;

                double qRssRp = Math.pow(Q, rssRp);
                double qRssTp = Math.pow(Q, rssTp);
                Sj += Math.abs(qRssRp - qRssTp);
            }

            distList.add(new double[]{Sj, rp.getX(), rp.getY(), j});
        }

        if (distList.isEmpty()) return null;

        // Sort ascending berdasarkan Sj
        Collections.sort(distList, (a, b) -> Double.compare(a[0], b[0]));

        // 🔍 DEBUG: lihat 5 Sj terkecil (RP paling mirip sinyal ke TP ini)
        StringBuilder logSj = new StringBuilder("Sj 5 terkecil: ");
        for (int i = 0; i < Math.min(5, distList.size()); i++) {
            logSj.append(String.format(java.util.Locale.US, "%.2f ", distList.get(i)[0]));
        }
        Log.d("QWKNN_DEBUG", logSj.toString());

        // ── Step 2: Adaptive K (Persamaan 13-14) ──────────────────────
        double Smin = distList.get(0)[0];
        double KTh  = 2 * Smin;

        // Filter pertama: ambil semua Sj <= KTh
        List<double[]> sPrime = new ArrayList<>();
        for (double[] d : distList) {
            if (d[0] <= KTh) sPrime.add(d);
        }
        // Jaga-jaga: jika Smin == 0, KTh juga 0, dan sPrime minimal berisi S1 sendiri.
        if (sPrime.isEmpty()) sPrime.add(distList.get(0));

        int f = sPrime.size();
        double S1 = sPrime.get(0)[0];

        // 🔍 DEBUG: lihat hasil filter tahap 1 (KTh)
        Log.d("QWKNN_DEBUG", String.format(java.util.Locale.US,
                "Smin=%.2f | KTh=%.2f | RP lolos filter 1 (f)=%d", Smin, KTh, f));

        List<double[]> kFinal;
        if (f == 1) {
            // Hanya ada 1 RP dalam threshold pertama -> K = 1, tidak perlu filter kedua.
            kFinal = sPrime;
        } else {
            // Filter kedua: E(G) = (sum_{i=2..f} Gi) / (f-1)   (Persamaan 14)
            // PENTING: index dimulai dari i=2 (elemen kedua), G1 = 0 TIDAK ikut
            // dijumlahkan maupun ikut membagi.
            double sumG = 0;
            List<Double> gList = new ArrayList<>(); // gList[0] sesuai sPrime[0], dst (untuk pengecekan akhir)
            gList.add(0.0); // G1 = |S1 - S1| = 0, disimpan untuk indeks tapi tidak dihitung ke E(G)
            for (int i = 1; i < f; i++) {
                double gi = Math.abs(sPrime.get(i)[0] - S1);
                gList.add(gi);
                sumG += gi;
            }
            double Eg = sumG / (f - 1);

            // 🔍 DEBUG: lihat filter tahap 2 (E(G))
            Log.d("QWKNN_DEBUG", String.format(java.util.Locale.US,
                    "E(G)=%.2f (dari %d gap)", Eg, f - 1));

            kFinal = new ArrayList<>();
            kFinal.add(sPrime.get(0)); // S1/G1 selalu masuk (G1=0 <= E(G) selalu benar)
            for (int i = 1; i < f; i++) {
                if (gList.get(i) <= Eg) {
                    kFinal.add(sPrime.get(i));
                }
            }
        }

        if (kFinal.isEmpty()) kFinal.add(distList.get(0));
        int K = kFinal.size();

        // 🔍 DEBUG: K adaptif final yang benar-benar dipakai
        Log.d("QWKNN_DEBUG", "K ADAPTIF FINAL = " + K);

        // ── Step 3: WKNN weighted position (Persamaan 17-18) ──────────
        double sumW  = 0;
        double sumWX = 0;
        double sumWY = 0;

        // Hitung total bobot dulu untuk normalisasi persentase
        for (double[] d : kFinal) {
            double Sj = d[0];
            double wj = (Sj == 0) ? 1e6 : 1.0 / Sj;
            sumW  += wj;
            sumWX += wj * d[1];
            sumWY += wj * d[2];
        }

        double estX = sumWX / sumW;
        double estY = sumWY / sumW;

        // ── Detail tetangga untuk fitur debug ──────────────────
        StringBuilder detail = new StringBuilder();
        detail.append("K = ").append(K).append(" tetangga terpilih\n\n");
        for (int i = 0; i < kFinal.size(); i++) {
            double[] d = kFinal.get(i);
            double Sj = d[0];
            double wj = (Sj == 0) ? 1e6 : 1.0 / Sj;
            double pct = wj / sumW * 100.0;
            int rpIdx = (int) d[3];
            String rpRoom = fingerprints.get(rpIdx).getRoomName();
            double rpX = fingerprints.get(rpIdx).getX();
            double rpY = fingerprints.get(rpIdx).getY();
            detail.append(String.format(java.util.Locale.US,
                    "[%d] %s (%.2f, %.2f)\n    Sj=%.6f | w=%.4f | bobot=%.1f%%\n\n",
                    i + 1, rpRoom, rpX, rpY, Sj, wj, pct));
        }
        detail.append(String.format(java.util.Locale.US,
                "→ Estimasi: (%.3f, %.3f)", estX, estY));

        int nearestIdx = (int) kFinal.get(0)[3];
        String roomName = fingerprints.get(nearestIdx).getRoomName();

        long elapsed = System.currentTimeMillis() - startTime;

        // 🔍 DEBUG: hasil akhir + waktu eksekusi (buat bab pembahasan/sidang)
        Log.d("QWKNN_DEBUG", String.format(java.util.Locale.US,
                "Estimasi=(%.2f, %.2f) | Ruangan=%s | Waktu eksekusi=%d ms",
                estX, estY, roomName, elapsed));

        PositionResult result = new PositionResult(estX, estY, roomName, K, elapsed, "Q-WKNN");
        result.setNeighborDetails(detail.toString());
        return result;
    }
}