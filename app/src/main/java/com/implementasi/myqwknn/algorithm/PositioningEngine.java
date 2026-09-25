package com.implementasi.myqwknn.algorithm;

import com.implementasi.myqwknn.database.FingerprintDatabase;
import com.implementasi.myqwknn.model.AccessPoint;
import com.implementasi.myqwknn.model.PositionResult;
import com.implementasi.myqwknn.model.ReferencePoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PositioningEngine {

    private FingerprintDatabase db;
    private QWKNNAlgorithm qwknn;
    private WKNNAlgorithm wknn;
    private DataPreprocessor preprocessor;

    // Parameter default
    private double eta = 7.0;
    private int topL = 999;  // jumlah AP yang dipilih
    private int kFixed = 3; // K untuk WKNN

    public PositioningEngine(FingerprintDatabase db) {
        this.db = db;
        this.qwknn = new QWKNNAlgorithm(eta);
        this.wknn = new WKNNAlgorithm(kFixed);
        this.preprocessor = new DataPreprocessor(db);
    }

    // ── Setter parameter ─────────────────────────────────────
    public void setEta(double eta) {
        this.eta = eta;
        this.qwknn.setEta(eta);
    }

    public void setTopL(int topL) {
        this.topL = topL;
    }

    public void setKFixed(int kFixed) {
        this.kFixed = kFixed;
        this.wknn.setK(kFixed);
    }

    public double getEta() { return eta; }
    public int getTopL()   { return topL; }
    public int getKFixed() { return kFixed; }

    // ── FASE OFFLINE ─────────────────────────────────────────

    /**
     * Simpan satu batch scan mentah ke DB.
     * Dipanggil dari CalibrationActivity tiap scan selesai.
     */
    public void saveRawScan(int rpId, String roomName, double x, double y,
                            List<AccessPoint> scanResults) {
        for (AccessPoint ap : scanResults) {
            db.insertRaw(rpId, roomName, x, y, ap.getBssid(), ap.getRss());
        }
    }

    /**
     * Jalankan preprocessing + AP Selection.
     * Dipanggil setelah semua RP selesai dikalibrasi.
     */
    public void runPreprocessing() {
        preprocessor.processAllRp();
        preprocessor.runApSelection(topL);
    }

    // ── FASE ONLINE ──────────────────────────────────────────

    /**
     * Jalankan Q-WKNN dari hasil scan test point.
     * Return null jika fingerprint belum siap.
     */
    public PositionResult runQWKNN(List<AccessPoint> scanResults) {
        if (!db.isFingerprintReady()) return null;

        Map<String, Double> testRssMap = toRssMap(scanResults);
        List<ReferencePoint> fingerprints = db.getAllFingerprints();
        List<String> selectedBssids = db.getSelectedBssids(topL);

        if (selectedBssids.isEmpty()) {
            // Fallback: pakai semua BSSID yang ada
            selectedBssids = getAllBssids(fingerprints);
        }

        return qwknn.calculate(testRssMap, fingerprints, selectedBssids);
    }

    /**
     * Jalankan WKNN dari hasil scan test point.
     * Memakai data dan AP yang sama dengan Q-WKNN.
     */
    public PositionResult runWKNN(List<AccessPoint> scanResults) {
        if (!db.isFingerprintReady()) return null;

        Map<String, Double> testRssMap = toRssMap(scanResults);
        List<ReferencePoint> fingerprints = db.getAllFingerprints();
        List<String> selectedBssids = db.getSelectedBssids(topL);

        if (selectedBssids.isEmpty()) {
            selectedBssids = getAllBssids(fingerprints);
        }

        return wknn.calculate(testRssMap, fingerprints, selectedBssids);
    }

    /**
     * Jalankan keduanya sekaligus, return array [qwknnResult, wknnResult].
     */
    public PositionResult[] runBoth(List<AccessPoint> scanResults) {
        if (!db.isFingerprintReady()) return null;

        Map<String, Double> testRssMap = toRssMap(scanResults);
        List<ReferencePoint> fingerprints = db.getAllFingerprints();
        List<String> selectedBssids = db.getSelectedBssids(topL);

        if (selectedBssids.isEmpty()) {
            selectedBssids = getAllBssids(fingerprints);
        }

        PositionResult qResult = qwknn.calculate(testRssMap, fingerprints, selectedBssids);
        PositionResult wResult = wknn.calculate(testRssMap, fingerprints, selectedBssids);

        return new PositionResult[]{qResult, wResult};
    }

    // ── UNTUK MODE ZENODO ─────────────────────────────────────

    /**
     * Jalankan Q-WKNN langsung dari ReferencePoint list dan test RSS map.
     * Dipakai di mode Zenodo yang tidak pakai DB.
     */
    public PositionResult runQWKNNDirect(Map<String, Double> testRssMap,
                                         List<ReferencePoint> fingerprints,
                                         List<String> selectedBssids) {
        return qwknn.calculate(testRssMap, fingerprints, selectedBssids);
    }

    public PositionResult runWKNNDirect(Map<String, Double> testRssMap,
                                        List<ReferencePoint> fingerprints,
                                        List<String> selectedBssids) {
        return wknn.calculate(testRssMap, fingerprints, selectedBssids);
    }

    // ── Helper ───────────────────────────────────────────────

    /**
     * Konversi List<AccessPoint> ke Map<bssid, rss>.
     * Jika ada bssid duplikat, ambil rata-ratanya.
     */
    private Map<String, Double> toRssMap(List<AccessPoint> scanResults) {
        Map<String, Double> map = new HashMap<>();
        Map<String, Integer> count = new HashMap<>();

        for (AccessPoint ap : scanResults) {
            String bssid = ap.getBssid();
            if (map.containsKey(bssid)) {
                map.put(bssid, map.get(bssid) + ap.getRss());
                count.put(bssid, count.get(bssid) + 1);
            } else {
                map.put(bssid, ap.getRss());
                count.put(bssid, 1);
            }
        }

        // Rata-rata jika ada duplikat
        for (String bssid : map.keySet()) {
            if (count.get(bssid) > 1) {
                map.put(bssid, map.get(bssid) / count.get(bssid));
            }
        }

        return map;
    }

    private List<String> getAllBssids(List<ReferencePoint> fingerprints) {
        List<String> bssids = new java.util.ArrayList<>();
        for (ReferencePoint rp : fingerprints) {
            for (String bssid : rp.getRssMap().keySet()) {
                if (!bssids.contains(bssid)) bssids.add(bssid);
            }
        }
        return bssids;
    }
}