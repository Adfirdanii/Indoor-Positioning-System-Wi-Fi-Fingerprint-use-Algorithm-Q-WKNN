package com.implementasi.myqwknn.scanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;

import com.implementasi.myqwknn.model.AccessPoint;

import java.util.ArrayList;
import java.util.List;

public class WifiScanner {

    public interface ScanCallback {
        void onScanComplete(List<AccessPoint> results);
        void onScanFailed(String reason);
    }

    private Context context;
    private WifiManager wifiManager;
    private ScanCallback callback;
    private BroadcastReceiver receiver;
    private boolean isRegistered = false;

    public WifiScanner(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager)
                context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * Mulai scan WiFi sekali.
     * Hasil dikembalikan lewat callback.onScanComplete().
     */
    public void startScan(ScanCallback callback) {
        this.callback = callback;

        if (wifiManager == null) {
            callback.onScanFailed("WifiManager tidak tersedia");
            return;
        }

        if (!wifiManager.isWifiEnabled()) {
            callback.onScanFailed("WiFi tidak aktif");
            return;
        }

        // Daftarkan receiver untuk terima hasil scan
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                boolean success = intent.getBooleanExtra(
                        WifiManager.EXTRA_RESULTS_UPDATED, false);
                unregisterReceiver();
                if (success) {
                    deliverResults();
                } else {
                    // Android 9+ kadang return false tapi hasil tetap ada
                    deliverResults();
                }
            }
        };

        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter);
        }
        isRegistered = true;

        // Mulai scan
        boolean started = wifiManager.startScan();
        if (!started) {
            // Android 9+ membatasi scan rate — pakai hasil cache
            unregisterReceiver();
            deliverResults();
        }
    }

    /**
     * Scan sebanyak N kali, kumpulkan semua hasil,
     * kembalikan lewat callback setelah scan ke-N selesai.
     * Dipakai saat kalibrasi (fase offline).
     */
    public void startMultiScan(int n, int intervalMs, ScanCallback callback) {
        List<AccessPoint> accumulated = new ArrayList<>();
        final int[] count = {0};

        ScanCallback singleCallback = new ScanCallback() {
            @Override
            public void onScanComplete(List<AccessPoint> results) {
                accumulated.addAll(results);
                count[0]++;
                if (count[0] >= n) {
                    callback.onScanComplete(accumulated);
                } else {
                    // Delay sebelum scan berikutnya
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .postDelayed(() -> startScan(this), intervalMs);
                }
            }

            @Override
            public void onScanFailed(String reason) {
                callback.onScanFailed(reason);
            }
        };

        startScan(singleCallback);
    }

    /**
     * Ambil hasil scan terakhir dari cache WifiManager.
     * Berguna saat scan throttled oleh Android.
     */
    public List<AccessPoint> getCachedResults() {
        List<AccessPoint> list = new ArrayList<>();
        if (wifiManager == null) return list;
        List<ScanResult> results = wifiManager.getScanResults();
        if (results == null) return list;
        for (ScanResult sr : results) {
            list.add(new AccessPoint(sr.BSSID, sr.SSID, sr.level));
        }
        return list;
    }

    private void deliverResults() {
        List<AccessPoint> list = new ArrayList<>();
        List<ScanResult> results = wifiManager.getScanResults();
        if (results != null) {
            for (ScanResult sr : results) {
                list.add(new AccessPoint(sr.BSSID, sr.SSID, sr.level));
            }
        }
        if (callback != null) callback.onScanComplete(list);
    }

    private void unregisterReceiver() {
        if (isRegistered && receiver != null) {
            try {
                context.unregisterReceiver(receiver);
            } catch (Exception ignored) {}
            isRegistered = false;
        }
    }

    public void release() {
        unregisterReceiver();
        callback = null;
    }

    public boolean isWifiEnabled() {
        return wifiManager != null && wifiManager.isWifiEnabled();
    }
}