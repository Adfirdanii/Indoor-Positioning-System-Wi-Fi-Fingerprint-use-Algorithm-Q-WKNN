package com.implementasi.myqwknn.algorithm;

import com.implementasi.myqwknn.model.PositionResult;
import com.implementasi.myqwknn.model.ReferencePoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * PERBAIKAN WKNN baseline sesuai metodologi Rong Zhou (Sensors 2021, 21, 5685).
 *
 * Kenapa diubah dari Euclidean ke Manhattan:
 * Artikel menyatakan eksplisit (Section 3.4, par. terakhir): "Research
 * indicates that for a Wi-Fi fingerprint system, Euclidean distance for NN,
 * and Manhattan distance for KNN or WKNN gave the least mean distance
 * error... Accordingly, we chose Manhattan distance as our fingerprint
 * similarity measurement." WKNN pembanding (baseline "noQ") DAN Q-WKNN
 * keduanya memakai Manhattan distance; satu-satunya pembeda adalah
 * transformasi basis-Q pada RSS sebelum dikurangkan (lihat Figure 6 & 7,
 * label "haveQ" vs "noQ" -- bukan "Manhattan" vs "Euclidean").
 *
 * Jika WKNN pembanding memakai Euclidean (seperti versi lama), maka
 * perbandingan Q-WKNN vs WKNN TIDAK mengisolasi efek base Q saja -- ada
 * dua variabel berubah sekaligus (metrik jarak DAN transformasi Q),
 * sehingga klaim "Q-WKNN lebih baik dari WKNN karena base Q" tidak valid
 * secara metodologis.
 *
 * Missing value diganti RSS_MISSING (sama dengan QWKNNAlgorithm) agar
 * basis perbandingan AP antara kedua algoritma identik.
 */
public class WKNNAlgorithm {

    public static final double RSS_MISSING = QWKNNAlgorithm.RSS_MISSING;

    private int K; // K fixed

    public WKNNAlgorithm() {
        this.K = 3; // default K=3 sesuai hasil tuning artikel pada dataset Zenodo
    }

    public WKNNAlgorithm(int k) {
        this.K = k;
    }

    public void setK(int k) { this.K = k; }
    public int getK() { return K; }

    /**
     * WKNN standar (Manhattan distance, RSS langsung tanpa transformasi Q, K fixed).
     * Inilah baseline "noQ" yang dipakai artikel untuk dibandingkan dengan Q-WKNN.
     */
    public PositionResult calculate(Map<String, Double> testRssMap,
                                    List<ReferencePoint> fingerprints,
                                    List<String> selectedBssids) {

        long startTime = System.currentTimeMillis();

        if (fingerprints == null || fingerprints.isEmpty()) return null;
        if (selectedBssids == null || selectedBssids.isEmpty()) return null;

        // ── Step 1: Hitung Manhattan distance (tanpa basis Q) tiap RP ───
        List<double[]> distList = new ArrayList<>();
        // distList[i] = {distance, x, y, index_rp}

        for (int j = 0; j < fingerprints.size(); j++) {
            ReferencePoint rp = fingerprints.get(j);
            double Sj = 0;

            for (String bssid : selectedBssids) {
                Double rssRpObj = rp.getRssMap().get(bssid);
                Double rssTpObj = testRssMap.get(bssid);

                double rssRp = (rssRpObj == null) ? RSS_MISSING : rssRpObj;
                double rssTp = (rssTpObj == null) ? RSS_MISSING : rssTpObj;

                Sj += Math.abs(rssRp - rssTp);
            }

            distList.add(new double[]{Sj, rp.getX(), rp.getY(), j});
        }

        if (distList.isEmpty()) return null;

        // Sort ascending berdasarkan distance
        Collections.sort(distList, (a, b) -> Double.compare(a[0], b[0]));

        // ── Step 2: Ambil K tetangga terdekat ────────────────────────
        int kMin = Math.min(K, distList.size());
        List<double[]> kNearest = distList.subList(0, kMin);

        // ── Step 3: Weighted position w = 1/distance ─────────────────
        double sumW  = 0;
        double sumWX = 0;
        double sumWY = 0;

        for (double[] d : kNearest) {
            double dist = d[0];
            double wj   = (dist == 0) ? 1e6 : 1.0 / dist;
            sumW  += wj;
            sumWX += wj * d[1];
            sumWY += wj * d[2];
        }

        double estX = sumWX / sumW;
        double estY = sumWY / sumW;

        // ── Detail tetangga untuk fitur debug ──────────────────
        StringBuilder detail = new StringBuilder();
        detail.append("K = ").append(kMin).append(" tetangga (fixed)\n\n");
        for (int i = 0; i < kNearest.size(); i++) {
            double[] d = kNearest.get(i);
            double dist = d[0];
            double wj = (dist == 0) ? 1e6 : 1.0 / dist;
            double pct = wj / sumW * 100.0;
            int rpIdx = (int) d[3];
            String rpRoom = fingerprints.get(rpIdx).getRoomName();
            double rpX = fingerprints.get(rpIdx).getX();
            double rpY = fingerprints.get(rpIdx).getY();
            detail.append(String.format(java.util.Locale.US,
                    "[%d] %s (%.2f, %.2f)\n    dist=%.4f | w=%.4f | bobot=%.1f%%\n\n",
                    i + 1, rpRoom, rpX, rpY, dist, wj, pct));
        }
        detail.append(String.format(java.util.Locale.US,
                "→ Estimasi: (%.3f, %.3f)", estX, estY));

        int nearestIdx = (int) kNearest.get(0)[3];
        String roomName = fingerprints.get(nearestIdx).getRoomName();

        long elapsed = System.currentTimeMillis() - startTime;

        PositionResult result = new PositionResult(estX, estY, roomName, K, elapsed, "WKNN");
        result.setNeighborDetails(detail.toString());
        return result;
    }
}