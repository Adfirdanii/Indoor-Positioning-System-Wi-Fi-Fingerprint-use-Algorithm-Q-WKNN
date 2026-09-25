package com.implementasi.myqwknn.algorithm;

import com.implementasi.myqwknn.database.FingerprintDatabase;
import com.implementasi.myqwknn.model.ReferencePoint;

import java.util.ArrayList;
import java.util.List;

public class DataPreprocessor {

    private FingerprintDatabase db;

    public DataPreprocessor(FingerprintDatabase db) {
        this.db = db;
    }

    /**
     * Jalankan Pauta criterion untuk semua RP di tabel raw,
     * lalu simpan mean RSS bersih ke tabel fingerprint_db.
     * Dipanggil sekali setelah semua RP selesai dikalibrasi.
     */
    public void processAllRp() {
        db.clearFingerprint();

        List<Integer> rpIds = db.getAllRpIds();
        for (int rpId : rpIds) {
            processOneRp(rpId);
        }
    }

    /**
     * Proses satu RP: ambil data raw, terapkan Pauta per BSSID,
     * hitung mean bersih, simpan ke fingerprint_db.
     */
    private void processOneRp(int rpId) {
        String[] info = db.getRpInfo(rpId);
        String roomName = info[0];
        double x = Double.parseDouble(info[1]);
        double y = Double.parseDouble(info[2]);

        List<String> bssidList = db.getBssidListForRp(rpId);

        for (String bssid : bssidList) {
            List<Double> rawList = db.getRawRssList(rpId, bssid);
            if (rawList.isEmpty()) continue;

            List<Double> cleaned = pautaCriterion(rawList);
            if (cleaned.isEmpty()) cleaned = rawList; // fallback

            double meanRss = calculateMean(cleaned);
            db.insertFingerprint(rpId, roomName, x, y, bssid, meanRss);
        }
    }

    /**
     * Pauta Criterion (3-sigma rule):
     * Hitung mean dan standar deviasi dari data,
     * buang nilai yang |rss - mean| > 3 * sigma.
     */
    public List<Double> pautaCriterion(List<Double> data) {
        if (data.size() <= 2) return new ArrayList<>(data);

        double mean = calculateMean(data);
        double sigma = calculateStdDev(data, mean);

        List<Double> cleaned = new ArrayList<>();
        for (double val : data) {
            if (Math.abs(val - mean) <= 3 * sigma) {
                cleaned.add(val);
            }
        }

        // Jika semua dibuang (sigma=0 atau data ekstrem), kembalikan semua
        if (cleaned.isEmpty()) return new ArrayList<>(data);
        return cleaned;
    }

    /**
     * Hitung mean dari list double.
     */
    public double calculateMean(List<Double> data) {
        if (data.isEmpty()) return 0;
        double sum = 0;
        for (double v : data) sum += v;
        return sum / data.size();
    }

    /**
     * Hitung standar deviasi populasi.
     */
    public double calculateStdDev(List<Double> data, double mean) {
        if (data.size() <= 1) return 0;
        double sumSq = 0;
        for (double v : data) {
            double diff = v - mean;
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / data.size());
    }

    /**
     * AP Selection: hitung skor tiap BSSID dari fingerprint_db
     * dan simpan top-L ke tabel selected_aps.
     *
     * Skor R(AP) = M(AP) × P(AP)
     * M(AP) = (maxRSS + U) / U        → kekuatan sinyal puncak
     * P(AP) = S / (S - count_missing) → keandalan kemunculan
     *
     * U = 100 (offset agar RSS negatif jadi positif)
     * S = total jumlah RP
     */
    public void runApSelection(int topL) {
        // Ambil semua fingerprint
        List<ReferencePoint> fps = db.getAllFingerprints();
        if (fps.isEmpty()) return;

        int totalRp = fps.size();
        double U = 100.0;

        // Kumpulkan semua BSSID unik
        List<String> allBssids = new ArrayList<>();
        for (ReferencePoint rp : fps) {
            for (String bssid : rp.getRssMap().keySet()) {
                if (!allBssids.contains(bssid)) allBssids.add(bssid);
            }
        }

        // Hitung skor tiap BSSID
        List<double[]> scores = new ArrayList<>(); // [skor, index]
        for (int i = 0; i < allBssids.size(); i++) {
            String bssid = allBssids.get(i);
            double maxRss = -999;
            int countPresent = 0;

            for (ReferencePoint rp : fps) {
                Double rss = rp.getRssMap().get(bssid);
                if (rss != null) {
                    countPresent++;
                    if (rss > maxRss) maxRss = rss;
                }
            }

            if (countPresent == 0) continue;

            double mAp = (maxRss + U) / U;

            double pAp;
            if (countPresent >= totalRp) {
                // AP muncul di SEMUA RP -> langsung paling reliable.
                // Beri nilai sangat besar (bukan literal Infinity, supaya
                // tetap bisa diurutkan & dikalikan dengan aman).
                pAp = totalRp * 1000.0;
            } else {
                pAp = (double) totalRp / (double) (totalRp - countPresent);
            }

            double score = mAp * pAp;
            scores.add(new double[]{score, i});
        }

        scores.sort((a, b) -> Double.compare(b[0], a[0]));

        int limit = Math.min(topL, scores.size());
        for (int rank = 0; rank < limit; rank++) {
            int idx = (int) scores.get(rank)[1];
            db.insertSelectedAp(allBssids.get(idx), scores.get(rank)[0], rank + 1);
        }
    }
}