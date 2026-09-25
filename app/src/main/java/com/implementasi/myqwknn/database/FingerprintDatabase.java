package com.implementasi.myqwknn.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.implementasi.myqwknn.model.ReferencePoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FingerprintDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "fingerprint.db";
    // Naik ke 5 — menambah tabel tp_raw_scan
    private static final int DB_VERSION = 5;

    // Tabel 1: data scan mentah RP (offline kalibrasi)
    public static final String TABLE_RAW = "fingerprint_raw";
    public static final String COL_RAW_ID = "id";
    public static final String COL_RAW_RP_ID = "rp_id";
    public static final String COL_RAW_ROOM = "room_name";
    public static final String COL_RAW_X = "x";
    public static final String COL_RAW_Y = "y";
    public static final String COL_RAW_BSSID = "bssid";
    public static final String COL_RAW_RSS = "rss";
    public static final String COL_RAW_TIMESTAMP = "timestamp";

    // Tabel 2: fingerprint terproses (setelah Pauta + rata-rata)
    public static final String TABLE_FP = "fingerprint_db";
    public static final String COL_FP_ID = "id";
    public static final String COL_FP_RP_ID = "rp_id";
    public static final String COL_FP_ROOM = "room_name";
    public static final String COL_FP_X = "x";
    public static final String COL_FP_Y = "y";
    public static final String COL_FP_BSSID = "bssid";
    public static final String COL_FP_MEAN_RSS = "mean_rss";

    // Tabel 3: hasil AP Selection (top-L)
    public static final String TABLE_AP = "selected_aps";
    public static final String COL_AP_ID = "id";
    public static final String COL_AP_BSSID = "bssid";
    public static final String COL_AP_SCORE = "score";
    public static final String COL_AP_RANK = "rank";

    // Tabel 4: hasil analisis parameter / pengujian formal
    public static final String TABLE_ANALYSIS = "analysis_result";
    public static final String COL_AN_ID = "id";
    public static final String COL_AN_TP_NAME = "tp_name";
    public static final String COL_AN_SCAN_KE = "scan_ke";
    public static final String COL_AN_ETA = "eta";
    public static final String COL_AN_L = "l_value";
    public static final String COL_AN_TRUE_X = "true_x";
    public static final String COL_AN_TRUE_Y = "true_y";
    public static final String COL_AN_ROOM_Q = "room_qwknn";
    public static final String COL_AN_X_Q = "x_qwknn";
    public static final String COL_AN_Y_Q = "y_qwknn";
    public static final String COL_AN_ERR_Q = "error_qwknn";
    public static final String COL_AN_ROOM_W = "room_wknn";
    public static final String COL_AN_X_W = "x_wknn";
    public static final String COL_AN_Y_W = "y_wknn";
    public static final String COL_AN_ERR_W = "error_wknn";
    public static final String COL_AN_TIME = "timestamp";
    public static final String COL_AN_JENIS = "jenis";
    public static final String COL_AN_K_Q = "k_qwknn";
    public static final String COL_AN_K_W = "k_wknn";

    // ── TABEL 5 (BARU): raw scan per TP untuk sweep offline ──────────────────
    // Satu baris = satu AP yang terdeteksi dalam satu "sesi scan" di satu TP.
    // scan_id membedakan sesi-sesi scan berbeda di TP yang sama.
    public static final String TABLE_TP_RAW = "tp_raw_scan";
    public static final String COL_TP_ID = "id";
    public static final String COL_TP_NAME = "tp_name";       // misal "TP1"
    public static final String COL_TP_SCAN_ID = "scan_id";    // urutan sesi scan di TP ini
    public static final String COL_TP_BSSID = "bssid";
    public static final String COL_TP_RSS = "rss";
    public static final String COL_TP_TIMESTAMP = "timestamp";

    public FingerprintDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_RAW + " (" +
                COL_RAW_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_RAW_RP_ID + " INTEGER, " +
                COL_RAW_ROOM + " TEXT, " +
                COL_RAW_X + " REAL, " +
                COL_RAW_Y + " REAL, " +
                COL_RAW_BSSID + " TEXT, " +
                COL_RAW_RSS + " REAL, " +
                COL_RAW_TIMESTAMP + " INTEGER)");

        db.execSQL("CREATE TABLE " + TABLE_FP + " (" +
                COL_FP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FP_RP_ID + " INTEGER, " +
                COL_FP_ROOM + " TEXT, " +
                COL_FP_X + " REAL, " +
                COL_FP_Y + " REAL, " +
                COL_FP_BSSID + " TEXT, " +
                COL_FP_MEAN_RSS + " REAL)");

        db.execSQL("CREATE TABLE " + TABLE_AP + " (" +
                COL_AP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_AP_BSSID + " TEXT, " +
                COL_AP_SCORE + " REAL, " +
                COL_AP_RANK + " INTEGER)");

        db.execSQL("CREATE TABLE " + TABLE_ANALYSIS + " (" +
                COL_AN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_AN_TP_NAME + " TEXT, " +
                COL_AN_SCAN_KE + " INTEGER, " +
                COL_AN_ETA + " REAL, " +
                COL_AN_L + " INTEGER, " +
                COL_AN_TRUE_X + " REAL, " +
                COL_AN_TRUE_Y + " REAL, " +
                COL_AN_ROOM_Q + " TEXT, " +
                COL_AN_X_Q + " REAL, " +
                COL_AN_Y_Q + " REAL, " +
                COL_AN_ERR_Q + " REAL, " +
                COL_AN_ROOM_W + " TEXT, " +
                COL_AN_X_W + " REAL, " +
                COL_AN_Y_W + " REAL, " +
                COL_AN_ERR_W + " REAL, " +
                COL_AN_TIME + " INTEGER, " +
                COL_AN_JENIS + " TEXT, " +
                COL_AN_K_Q + " INTEGER, " +
                COL_AN_K_W + " INTEGER)");

        db.execSQL(createTpRawTable());
    }

    private String createTpRawTable() {
        return "CREATE TABLE " + TABLE_TP_RAW + " (" +
                COL_TP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TP_NAME + " TEXT, " +
                COL_TP_SCAN_ID + " INTEGER, " +
                COL_TP_BSSID + " TEXT, " +
                COL_TP_RSS + " REAL, " +
                COL_TP_TIMESTAMP + " INTEGER)";
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ANALYSIS + " (" +
                    COL_AN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_AN_TP_NAME + " TEXT, " +
                    COL_AN_SCAN_KE + " INTEGER, " +
                    COL_AN_ETA + " REAL, " +
                    COL_AN_L + " INTEGER, " +
                    COL_AN_TRUE_X + " REAL, " +
                    COL_AN_TRUE_Y + " REAL, " +
                    COL_AN_ROOM_Q + " TEXT, " +
                    COL_AN_X_Q + " REAL, " +
                    COL_AN_Y_Q + " REAL, " +
                    COL_AN_ERR_Q + " REAL, " +
                    COL_AN_ROOM_W + " TEXT, " +
                    COL_AN_X_W + " REAL, " +
                    COL_AN_Y_W + " REAL, " +
                    COL_AN_ERR_W + " REAL, " +
                    COL_AN_TIME + " INTEGER)");
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_ANALYSIS + " ADD COLUMN " + COL_AN_JENIS + " TEXT");
            } catch (Exception e) { /* kolom sudah ada */ }
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_ANALYSIS + " ADD COLUMN " + COL_AN_K_Q + " INTEGER");
            } catch (Exception e) { /* kolom sudah ada */ }
            try {
                db.execSQL("ALTER TABLE " + TABLE_ANALYSIS + " ADD COLUMN " + COL_AN_K_W + " INTEGER");
            } catch (Exception e) { /* kolom sudah ada */ }
        }
        // ── versi 5: tambah tabel tp_raw_scan ──
        if (oldVersion < 5) {
            db.execSQL("CREATE TABLE IF NOT EXISTS tp_raw_scan (id INTEGER PRIMARY KEY AUTOINCREMENT, tp_name TEXT, scan_id INTEGER, bssid TEXT, rss REAL, timestamp INTEGER)");
        }
    }

    // ═══════════════════════════════════════════════════════
    // INSERT RAW RP
    // ═══════════════════════════════════════════════════════
    public void insertRaw(int rpId, String roomName, double x, double y,
                          String bssid, double rss) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_RAW_RP_ID, rpId);
        cv.put(COL_RAW_ROOM, roomName);
        cv.put(COL_RAW_X, x);
        cv.put(COL_RAW_Y, y);
        cv.put(COL_RAW_BSSID, bssid);
        cv.put(COL_RAW_RSS, rss);
        cv.put(COL_RAW_TIMESTAMP, System.currentTimeMillis());
        db.insert(TABLE_RAW, null, cv);
        db.close();
    }

    // ═══════════════════════════════════════════════════════
    // INSERT FINGERPRINT TERPROSES
    // ═══════════════════════════════════════════════════════
    public void insertFingerprint(int rpId, String roomName, double x, double y,
                                  String bssid, double meanRss) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_FP_RP_ID, rpId);
        cv.put(COL_FP_ROOM, roomName);
        cv.put(COL_FP_X, x);
        cv.put(COL_FP_Y, y);
        cv.put(COL_FP_BSSID, bssid);
        cv.put(COL_FP_MEAN_RSS, meanRss);
        db.insert(TABLE_FP, null, cv);
        db.close();
    }

    // ═══════════════════════════════════════════════════════
    // INSERT AP SELECTION
    // ═══════════════════════════════════════════════════════
    public void insertSelectedAp(String bssid, double score, int rank) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_AP_BSSID, bssid);
        cv.put(COL_AP_SCORE, score);
        cv.put(COL_AP_RANK, rank);
        db.insert(TABLE_AP, null, cv);
        db.close();
    }

    // ═══════════════════════════════════════════════════════
    // INSERT ANALYSIS RESULT
    // ═══════════════════════════════════════════════════════
    public void insertAnalysisResult(String tpName, int scanKe, double eta, int l,
                                     double trueX, double trueY,
                                     String roomQ, double xQ, double yQ, double errQ,
                                     String roomW, double xW, double yW, double errW,
                                     String jenis, int kQ, int kW) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_AN_TP_NAME, tpName);
        cv.put(COL_AN_SCAN_KE, scanKe);
        cv.put(COL_AN_ETA, eta);
        cv.put(COL_AN_L, l);
        cv.put(COL_AN_TRUE_X, trueX);
        cv.put(COL_AN_TRUE_Y, trueY);
        cv.put(COL_AN_ROOM_Q, roomQ);
        cv.put(COL_AN_X_Q, xQ);
        cv.put(COL_AN_Y_Q, yQ);
        cv.put(COL_AN_ERR_Q, errQ);
        cv.put(COL_AN_ROOM_W, roomW);
        cv.put(COL_AN_X_W, xW);
        cv.put(COL_AN_Y_W, yW);
        cv.put(COL_AN_ERR_W, errW);
        cv.put(COL_AN_TIME, System.currentTimeMillis());
        cv.put(COL_AN_JENIS, jenis);
        cv.put(COL_AN_K_Q, kQ);
        cv.put(COL_AN_K_W, kW);
        db.insert(TABLE_ANALYSIS, null, cv);
        db.close();
    }

    // ═══════════════════════════════════════════════════════
    // INSERT / QUERY TP RAW SCAN (TABEL BARU)
    // ═══════════════════════════════════════════════════════

    /**
     * Simpan satu batch scan live TP ke tabel tp_raw_scan.
     * scanId = urutan sesi scan di TP itu (1, 2, 3, ...).
     * Dipanggil dari AnalysisActivity setelah scan selesai di lapangan.
     */
    public void insertTpRawScan(String tpName, int scanId,
                                List<String> bssids, List<Double> rssList) {
        SQLiteDatabase db = this.getWritableDatabase();
        long ts = System.currentTimeMillis();
        db.beginTransaction();
        try {
            for (int i = 0; i < bssids.size(); i++) {
                ContentValues cv = new ContentValues();
                cv.put(COL_TP_NAME, tpName);
                cv.put(COL_TP_SCAN_ID, scanId);
                cv.put(COL_TP_BSSID, bssids.get(i));
                cv.put(COL_TP_RSS, rssList.get(i));
                cv.put(COL_TP_TIMESTAMP, ts);
                db.insert(TABLE_TP_RAW, null, cv);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * Ambil semua BSSID + RSS dari satu sesi scan TP (untuk sweep offline).
     * Return Map<bssid, rss>.
     */
    public Map<String, Double> getTpRawScanMap(String tpName, int scanId) {
        Map<String, Double> map = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_TP_RAW,
                new String[]{COL_TP_BSSID, COL_TP_RSS},
                COL_TP_NAME + "=? AND " + COL_TP_SCAN_ID + "=?",
                new String[]{tpName, String.valueOf(scanId)},
                null, null, null);
        while (c.moveToNext()) {
            map.put(c.getString(0), c.getDouble(1));
        }
        c.close();
        db.close();
        return map;
    }

    /**
     * Daftar scan_id yang tersedia untuk suatu TP.
     * Dipakai untuk tahu berapa sesi scan yang sudah ada.
     */
    public List<Integer> getTpScanIds(String tpName) {
        List<Integer> ids = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(true, TABLE_TP_RAW, new String[]{COL_TP_SCAN_ID},
                COL_TP_NAME + "=?", new String[]{tpName},
                COL_TP_SCAN_ID, null, COL_TP_SCAN_ID + " ASC", null);
        while (c.moveToNext()) ids.add(c.getInt(0));
        c.close();
        db.close();
        return ids;
    }

    /**
     * Jumlah sesi scan yang sudah tersimpan untuk suatu TP.
     */
    public int getTpRawScanCount(String tpName) {
        return getTpScanIds(tpName).size();
    }

    /**
     * Ambil semua sesi scan sweep suatu TP, dirata-rata per BSSID
     * jadi satu Map<bssid, meanRss>. Dipakai batch sweep supaya
     * tuning eta/L tidak lagi berbasis 1 scan doang.
     */
    public Map<String, Double> getTpRawScanMapAveraged(String tpName) {
        Map<String, List<Double>> collect = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_TP_RAW,
                new String[]{COL_TP_BSSID, COL_TP_RSS},
                COL_TP_NAME + "=?", new String[]{tpName},
                null, null, null);
        while (c.moveToNext()) {
            String bssid = c.getString(0);
            double rss = c.getDouble(1);
            if (!collect.containsKey(bssid)) collect.put(bssid, new ArrayList<>());
            collect.get(bssid).add(rss);
        }
        c.close();
        db.close();

        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : collect.entrySet()) {
            List<Double> vals = entry.getValue();
            double sum = 0;
            for (double v : vals) sum += v;
            result.put(entry.getKey(), sum / vals.size());
        }
        return result;
    }

    /**
     * Hapus seluruh raw scan untuk satu TP (misal ingin scan ulang).
     */
    public void deleteTpRawScan(String tpName) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TP_RAW, COL_TP_NAME + "=?", new String[]{tpName});
        db.close();
    }

    /**
     * Hapus hasil analysis_result untuk SATU TP dan SATU jenis saja
     * (misal cuma mau hapus jenis="pengujian" milik TP8, tanpa
     * mengganggu data jenis="parameter"/"parameter_eta" atau TP lain).
     */
    public void deleteAnalysisResultByTpAndJenis(String tpName, String jenis) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ANALYSIS,
                COL_TP_NAME + "=? AND " + COL_AN_JENIS + "=?",
                new String[]{tpName, jenis});
        db.close();
    }

    /**
     * Daftar semua TP yang sudah punya raw scan tersimpan.
     */
    public List<String> getAllTpNamesWithRaw() {
        List<String> names = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(true, TABLE_TP_RAW, new String[]{COL_TP_NAME},
                null, null, COL_TP_NAME, null, COL_TP_NAME + " ASC", null);
        while (c.moveToNext()) names.add(c.getString(0));
        c.close();
        db.close();
        return names;
    }

    // ═══════════════════════════════════════════════════════
    // QUERY RP RAW
    // ═══════════════════════════════════════════════════════
    public List<Double> getRawRssList(int rpId, String bssid) {
        List<Double> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_RAW, new String[]{COL_RAW_RSS},
                COL_RAW_RP_ID + "=? AND " + COL_RAW_BSSID + "=?",
                new String[]{String.valueOf(rpId), bssid},
                null, null, null);
        while (c.moveToNext()) result.add(c.getDouble(0));
        c.close();
        db.close();
        return result;
    }

    public List<String> getBssidListForRp(int rpId) {
        List<String> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(true, TABLE_RAW, new String[]{COL_RAW_BSSID},
                COL_RAW_RP_ID + "=?", new String[]{String.valueOf(rpId)},
                COL_RAW_BSSID, null, null, null);
        while (c.moveToNext()) result.add(c.getString(0));
        c.close();
        db.close();
        return result;
    }

    public List<Integer> getAllRpIds() {
        List<Integer> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(true, TABLE_RAW, new String[]{COL_RAW_RP_ID},
                null, null, COL_RAW_RP_ID, null, null, null);
        while (c.moveToNext()) result.add(c.getInt(0));
        c.close();
        db.close();
        return result;
    }

    public String[] getRpInfo(int rpId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_RAW,
                new String[]{COL_RAW_ROOM, COL_RAW_X, COL_RAW_Y},
                COL_RAW_RP_ID + "=?", new String[]{String.valueOf(rpId)},
                null, null, null, "1");
        String[] info = new String[]{"", "0", "0"};
        if (c.moveToFirst()) {
            info[0] = c.getString(0);
            info[1] = String.valueOf(c.getDouble(1));
            info[2] = String.valueOf(c.getDouble(2));
        }
        c.close();
        db.close();
        return info;
    }

    // ═══════════════════════════════════════════════════════
    // QUERY FINGERPRINT & AP
    // ═══════════════════════════════════════════════════════
    public List<ReferencePoint> getAllFingerprints() {
        List<ReferencePoint> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COL_FP_RP_ID + ", " + COL_FP_ROOM + ", " +
                        COL_FP_X + ", " + COL_FP_Y + ", " + COL_FP_BSSID + ", " +
                        COL_FP_MEAN_RSS + " FROM " + TABLE_FP +
                        " ORDER BY " + COL_FP_RP_ID, null);

        int lastRpId = -1;
        ReferencePoint current = null;
        while (c.moveToNext()) {
            int rpId = c.getInt(0);
            if (rpId != lastRpId) {
                current = new ReferencePoint(rpId, c.getString(1),
                        c.getDouble(2), c.getDouble(3));
                result.add(current);
                lastRpId = rpId;
            }
            if (current != null) current.addRss(c.getString(4), c.getDouble(5));
        }
        c.close();
        db.close();
        return result;
    }

    public List<String> getSelectedBssids(int topL) {
        List<String> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_AP, new String[]{COL_AP_BSSID},
                null, null, null, null,
                COL_AP_RANK + " ASC", String.valueOf(topL));
        while (c.moveToNext()) result.add(c.getString(0));
        c.close();
        db.close();
        return result;
    }

    public boolean isFingerprintReady() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_FP, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        db.close();
        return count > 0;
    }

    // ═══════════════════════════════════════════════════════
    // HAPUS DATA
    // ═══════════════════════════════════════════════════════
    public void clearAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_RAW);
        db.execSQL("DELETE FROM " + TABLE_FP);
        db.execSQL("DELETE FROM " + TABLE_AP);
        db.close();
    }

    public void clearFingerprint() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_FP);
        db.execSQL("DELETE FROM " + TABLE_AP);
        db.close();
    }

    public void clearAllAnalysisResults() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ANALYSIS, null, null);
        db.close();
    }

    // ═══════════════════════════════════════════════════════
    // MISC / UTILITY
    // ═══════════════════════════════════════════════════════
    public int getRawScanCount(int rpId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(DISTINCT " + COL_RAW_TIMESTAMP +
                        ") FROM " + TABLE_RAW + " WHERE " + COL_RAW_RP_ID + "=?",
                new String[]{String.valueOf(rpId)});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        db.close();
        return count;
    }

    public void updateRpInfo(int rpId, String newRoomName, double newX, double newY) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_RAW_ROOM, newRoomName);
        cv.put(COL_RAW_X, newX);
        cv.put(COL_RAW_Y, newY);
        db.update(TABLE_RAW, cv, COL_RAW_RP_ID + "=?",
                new String[]{String.valueOf(rpId)});
        db.close();
    }

    public void deleteRawByRpId(int rpId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RAW, COL_RAW_RP_ID + "=?",
                new String[]{String.valueOf(rpId)});
        db.close();
    }

    public int getScanCountForTp(String tpName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ANALYSIS +
                        " WHERE " + COL_AN_TP_NAME + "=?",
                new String[]{tpName});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        db.close();
        return count;
    }

    public void updateAnalysisResult(int id, String tpName, double trueX, double trueY,
                                     double xQ, double yQ, double xW, double yW) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_AN_TP_NAME, tpName);
        cv.put(COL_AN_TRUE_X, trueX);
        cv.put(COL_AN_TRUE_Y, trueY);
        double errQ = Math.sqrt(Math.pow(xQ - trueX, 2) + Math.pow(yQ - trueY, 2));
        double errW = Math.sqrt(Math.pow(xW - trueX, 2) + Math.pow(yW - trueY, 2));
        cv.put(COL_AN_ERR_Q, errQ);
        cv.put(COL_AN_ERR_W, errW);
        db.update(TABLE_ANALYSIS, cv, COL_AN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteAnalysisResult(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ANALYSIS, COL_AN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public List<String[]> getAllAnalysisResults() {
        List<String[]> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT " + COL_AN_ID + ", " + COL_AN_TP_NAME + ", " +
                COL_AN_SCAN_KE + ", " + COL_AN_ETA + ", " + COL_AN_L + ", " +
                COL_AN_TRUE_X + ", " + COL_AN_TRUE_Y + ", " +
                COL_AN_ROOM_Q + ", " + COL_AN_X_Q + ", " + COL_AN_Y_Q + ", " + COL_AN_ERR_Q + ", " +
                COL_AN_ROOM_W + ", " + COL_AN_X_W + ", " + COL_AN_Y_W + ", " + COL_AN_ERR_W + ", " +
                COL_AN_JENIS + ", " + COL_AN_K_Q + ", " + COL_AN_K_W +
                " FROM " + TABLE_ANALYSIS + " ORDER BY " + COL_AN_TP_NAME + ", " + COL_AN_SCAN_KE, null);
        while (c.moveToNext()) {
            String[] row = new String[18];
            for (int i = 0; i < 18; i++) row[i] = c.getString(i);
            result.add(row);
        }
        c.close();
        db.close();
        return result;
    }

    // ── REKAP PARAMETER ──────────────────────────────────────────────────────
    public List<String[]> getRekapParameter() {
        List<String[]> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cEtaL = db.rawQuery(
                "SELECT DISTINCT " + COL_AN_ETA + ", " + COL_AN_L +
                        " FROM " + TABLE_ANALYSIS +
                        " WHERE " + COL_AN_JENIS + "='parameter'" +
                        " ORDER BY " + COL_AN_ETA + ", " + COL_AN_L, null);

        while (cEtaL.moveToNext()) {
            String eta = cEtaL.getString(0);
            String l   = cEtaL.getString(1);

            Cursor cErr = db.rawQuery(
                    "SELECT " + COL_AN_ERR_Q + ", " + COL_AN_ERR_W +
                            " FROM " + TABLE_ANALYSIS +
                            " WHERE " + COL_AN_JENIS + "='parameter'" +
                            " AND " + COL_AN_ETA + "=? AND " + COL_AN_L + "=?" +
                            " ORDER BY " + COL_AN_ERR_Q,
                    new String[]{eta, l});

            List<Double> errQList = new ArrayList<>();
            List<Double> errWList = new ArrayList<>();
            while (cErr.moveToNext()) {
                errQList.add(cErr.getDouble(0));
                errWList.add(cErr.getDouble(1));
            }
            cErr.close();

            if (errQList.isEmpty()) continue;

            int n = errQList.size();
            double meanQ = 0, meanW = 0;
            for (int i = 0; i < n; i++) { meanQ += errQList.get(i); meanW += errWList.get(i); }
            meanQ /= n; meanW /= n;

            java.util.Collections.sort(errQList);
            java.util.Collections.sort(errWList);
            int p75idx = (int) Math.ceil(0.75 * n) - 1;
            if (p75idx < 0) p75idx = 0;
            if (p75idx >= n) p75idx = n - 1;

            result.add(new String[]{
                    eta, l, String.valueOf(n),
                    String.format(java.util.Locale.US, "%.3f", meanQ),
                    String.format(java.util.Locale.US, "%.3f", errQList.get(p75idx)),
                    String.format(java.util.Locale.US, "%.3f", meanW),
                    String.format(java.util.Locale.US, "%.3f", errWList.get(p75idx))
            });
        }
        cEtaL.close();
        db.close();
        return result;
    }

    // ── REKAP FORMAL ─────────────────────────────────────────────────────────
    public String[] getRekapFormal() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COL_AN_ERR_Q + ", " + COL_AN_ERR_W + ", " +
                        COL_AN_ETA + ", " + COL_AN_L +
                        " FROM " + TABLE_ANALYSIS +
                        " WHERE " + COL_AN_JENIS + "='pengujian'" +
                        " ORDER BY " + COL_AN_ERR_Q, null);

        List<Double> errQList = new ArrayList<>();
        List<Double> errWList = new ArrayList<>();
        String eta = "-", l = "-";
        while (c.moveToNext()) {
            errQList.add(c.getDouble(0));
            errWList.add(c.getDouble(1));
            eta = c.getString(2);
            l   = c.getString(3);
        }
        c.close();
        db.close();

        if (errQList.isEmpty()) return null;

        int n = errQList.size();
        double meanQ = 0, meanW = 0;
        for (int i = 0; i < n; i++) { meanQ += errQList.get(i); meanW += errWList.get(i); }
        meanQ /= n; meanW /= n;

        java.util.Collections.sort(errQList);
        java.util.Collections.sort(errWList);
        int p75idx = (int) Math.ceil(0.75 * n) - 1;
        if (p75idx < 0) p75idx = 0;
        if (p75idx >= n) p75idx = n - 1;

        return new String[]{
                String.valueOf(n),
                String.format(java.util.Locale.US, "%.3f", meanQ),
                String.format(java.util.Locale.US, "%.3f", errQList.get(p75idx)),
                String.format(java.util.Locale.US, "%.3f", meanW),
                String.format(java.util.Locale.US, "%.3f", errWList.get(p75idx)),
                eta, l
        };
    }

    // ── DISTRIBUSI K ADAPTIVE ─────────────────────────────────────────────────
    public List<String[]> getDistribusiK(String jenis) {
        List<String[]> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cQ = db.rawQuery(
                "SELECT " + COL_AN_K_Q + ", COUNT(*) FROM " + TABLE_ANALYSIS +
                        " WHERE " + COL_AN_JENIS + "=? AND " + COL_AN_K_Q + " IS NOT NULL" +
                        " GROUP BY " + COL_AN_K_Q + " ORDER BY " + COL_AN_K_Q,
                new String[]{jenis});
        while (cQ.moveToNext()) result.add(new String[]{"Q-WKNN", cQ.getString(0), cQ.getString(1)});
        cQ.close();

        Cursor cW = db.rawQuery(
                "SELECT " + COL_AN_K_W + ", COUNT(*) FROM " + TABLE_ANALYSIS +
                        " WHERE " + COL_AN_JENIS + "=? AND " + COL_AN_K_W + " IS NOT NULL" +
                        " GROUP BY " + COL_AN_K_W + " ORDER BY " + COL_AN_K_W,
                new String[]{jenis});
        while (cW.moveToNext()) result.add(new String[]{"WKNN", cW.getString(0), cW.getString(1)});
        cW.close();
        db.close();
        return result;
    }
}