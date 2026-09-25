package com.implementasi.myqwknn.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.implementasi.myqwknn.R;
import com.implementasi.myqwknn.algorithm.PositioningEngine;
import com.implementasi.myqwknn.database.FingerprintDatabase;
import com.implementasi.myqwknn.model.AccessPoint;
import com.implementasi.myqwknn.model.ReferencePoint;
import com.implementasi.myqwknn.scanner.WifiScanner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CalibrationActivity extends AppCompatActivity {

    private static final int SCAN_COUNT    = 20;
    private static final int SCAN_INTERVAL = 1500;
    private static final int PERM_REQ_CODE = 101;

    private EditText etRpId, etRoomName, etX, etY;
    private Button btnScan, btnSaveRp, btnProcess;
    private TextView tvStatus, tvScanCount, tvApList;
    private LinearLayout llRpList;
    private ProgressBar progressBar;

    private WifiScanner wifiScanner;
    private FingerprintDatabase db;
    private PositioningEngine engine;

    private List<AccessPoint> lastScanResult = null;
    private boolean isScanning = false;
    private int currentRpId = -1;
    private String currentRoomName = "";
    private double currentX = 0, currentY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        db          = new FingerprintDatabase(this);
        engine      = new PositioningEngine(db);
        wifiScanner = new WifiScanner(this);

        etRpId     = findViewById(R.id.et_rp_id);
        etRoomName = findViewById(R.id.et_room_name);
        etX        = findViewById(R.id.et_x);
        etY        = findViewById(R.id.et_y);
        btnScan    = findViewById(R.id.btn_scan);
        btnSaveRp  = findViewById(R.id.btn_save_rp);
        btnProcess = findViewById(R.id.btn_process);
        tvStatus   = findViewById(R.id.tv_status);
        tvScanCount= findViewById(R.id.tv_scan_count);
        tvApList   = findViewById(R.id.tv_ap_list);
        llRpList   = findViewById(R.id.ll_rp_list);
        progressBar= findViewById(R.id.progress_bar);

        btnScan.setOnClickListener(v -> {
            if (!isScanning) startCalibration();
        });
        btnSaveRp.setOnClickListener(v -> saveRp());
        btnProcess.setOnClickListener(v -> runPreprocessing());
        Button btnImport = findViewById(R.id.btn_import);
        btnImport.setOnClickListener(v -> importDatabase());

        refreshRpList();
        checkPermissions();
    }

    private void startCalibration() {
        String rpIdStr  = etRpId.getText().toString().trim();
        String roomName = etRoomName.getText().toString().trim();
        String xStr     = etX.getText().toString().trim();
        String yStr     = etY.getText().toString().trim();

        if (rpIdStr.isEmpty() || roomName.isEmpty() ||
                xStr.isEmpty() || yStr.isEmpty()) {
            Toast.makeText(this, "Isi semua field dulu",
                    Toast.LENGTH_SHORT).show();
            Intent restart = new Intent(this, CalibrationActivity.class);
            finish();
            startActivity(restart);
            return;
        }

        currentRpId     = Integer.parseInt(rpIdStr);
        currentRoomName = roomName;
        currentX        = Double.parseDouble(xStr);
        currentY        = Double.parseDouble(yStr);

        isScanning = true;
        lastScanResult = null;
        btnScan.setEnabled(false);
        btnSaveRp.setVisibility(View.GONE);
        tvApList.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setMax(SCAN_COUNT);
        progressBar.setProgress(0);
        tvStatus.setText("Scanning...");

        wifiScanner.startMultiScan(SCAN_COUNT, SCAN_INTERVAL,
                new WifiScanner.ScanCallback() {
                    @Override
                    public void onScanComplete(List<AccessPoint> results) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            lastScanResult = results;
                            isScanning = false;
                            btnScan.setEnabled(true);
                            progressBar.setVisibility(View.GONE);

                            Set<String> uniqueBssids = new HashSet<>();
                            for (AccessPoint ap : results) uniqueBssids.add(ap.getBssid());
                            int apCount = uniqueBssids.size();

                            tvScanCount.setText("20 scan selesai — " +
                                    apCount + " AP unik terdeteksi");

                            StringBuilder sb = new StringBuilder();
                            sb.append("AP terdeteksi: ").append(apCount).append(" AP\n\n");
                            sb.append(String.format("%-20s %s\n", "BSSID", "RSS (terakhir)"));
                            sb.append("─────────────────────────────\n");
                            Map<String, Double> lastRss = new LinkedHashMap<>();
                            for (AccessPoint ap : results) lastRss.put(ap.getBssid(), ap.getRss());
                            for (Map.Entry<String, Double> e : lastRss.entrySet()) {
                                sb.append(String.format("%-20s %.1f dBm\n",
                                        e.getKey(), e.getValue()));
                            }
                            tvApList.setText(sb.toString());
                            tvApList.setVisibility(View.VISIBLE);
                            tvStatus.setText("Scan selesai. " + apCount +
                                    " AP terdeteksi. Belum disimpan.");
                            btnSaveRp.setVisibility(View.VISIBLE);
                        });
                    }

                    @Override
                    public void onScanFailed(String reason) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            tvStatus.setText("Scan gagal: " + reason);
                            isScanning = false;
                            btnScan.setEnabled(true);
                            progressBar.setVisibility(View.GONE);
                        });
                    }
                });
    }

    private void saveRp() {
        if (lastScanResult == null || lastScanResult.isEmpty()) {
            Toast.makeText(this, "Tidak ada data scan",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        engine.saveRawScan(currentRpId, currentRoomName,
                currentX, currentY, lastScanResult);
        tvStatus.setText("RP " + currentRpId + " — " +
                currentRoomName + " tersimpan ✓");
        btnSaveRp.setVisibility(View.GONE);
        tvApList.setVisibility(View.GONE);
        lastScanResult = null;
        etRpId.setText(String.valueOf(currentRpId + 1));
        etRoomName.setText("");
        etX.setText("");
        etY.setText("");
        refreshRpList();
        Toast.makeText(this, "RP tersimpan!", Toast.LENGTH_SHORT).show();
    }

    private void refreshRpList() {
        new Thread(() -> {
            List<Integer> rpIds = db.getAllRpIds();
            new Handler(Looper.getMainLooper()).post(() -> {
                llRpList.removeAllViews();

                if (rpIds.isEmpty()) {
                    TextView tv = new TextView(this);
                    tv.setText("Belum ada RP tersimpan.");
                    tv.setPadding(8, 8, 8, 8);
                    llRpList.addView(tv);
                    return;
                }

                TextView header = new TextView(this);
                header.setText("RP tersimpan: " + rpIds.size() +
                        " titik  |  Tap=edit  Long press=hapus");
                header.setPadding(8, 8, 8, 8);
                header.setTypeface(null, Typeface.BOLD);
                llRpList.addView(header);

                addDivider(0xFFCCCCCC, 2);

                for (int rpId : rpIds) {
                    String[] info = db.getRpInfo(rpId);
                    List<String> bssids = db.getBssidListForRp(rpId);
                    int scanCount = db.getRawScanCount(rpId);

                    String label = String.format(
                            "RP%d — %s\n(%.2f, %.2f) | %d scan | %d AP unik",
                            rpId, info[0],
                            Double.parseDouble(info[1]),
                            Double.parseDouble(info[2]),
                            scanCount, bssids.size());

                    TextView tv = new TextView(this);
                    tv.setText(label);
                    tv.setPadding(8, 20, 8, 20);
                    tv.setTextSize(13);

                    tv.setOnClickListener(v -> showEditDialog(rpId, info));

                    tv.setOnLongClickListener(v -> {
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Hapus RP " + rpId)
                                .setMessage("Hapus semua data scan RP" +
                                        rpId + " — " + info[0] + "?")
                                .setPositiveButton("Hapus", (d, w) -> {
                                    db.deleteRawByRpId(rpId);
                                    refreshRpList();
                                    Toast.makeText(this, "RP " + rpId + " dihapus",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("Batal", null)
                                .show();
                        return true;
                    });

                    llRpList.addView(tv);
                    addDivider(0xFFEEEEEE, 1);
                }
            });
        }).start();
    }

    private void addDivider(int color, int heightDp) {
        View div = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, heightDp);
        div.setLayoutParams(lp);
        div.setBackgroundColor(color);
        llRpList.addView(div);
    }

    private void showEditDialog(int rpId, String[] info) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 16);

        EditText etName = new EditText(this);
        etName.setHint("Nama ruangan");
        etName.setText(info[0]);
        layout.addView(etName);

        EditText etXEdit = new EditText(this);
        etXEdit.setHint("Koordinat X");
        etXEdit.setText(info[1]);
        etXEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL |
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        layout.addView(etXEdit);

        EditText etYEdit = new EditText(this);
        etYEdit.setHint("Koordinat Y");
        etYEdit.setText(info[2]);
        etYEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL |
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        layout.addView(etYEdit);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Edit RP " + rpId)
                .setView(layout)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newX    = etXEdit.getText().toString().trim();
                    String newY    = etYEdit.getText().toString().trim();
                    if (newName.isEmpty() || newX.isEmpty() || newY.isEmpty()) {
                        Toast.makeText(this, "Isi semua field",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.updateRpInfo(rpId, newName,
                            Double.parseDouble(newX),
                            Double.parseDouble(newY));
                    refreshRpList();
                    Toast.makeText(this, "RP " + rpId + " diupdate",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .setNeutralButton("Scan Ulang", (dialog, which) -> {
                    db.deleteRawByRpId(rpId);
                    etRpId.setText(String.valueOf(rpId));
                    etRoomName.setText(info[0]);
                    etX.setText(info[1]);
                    etY.setText(info[2]);
                    refreshRpList();
                    Toast.makeText(this, "Scan ulang RP " + rpId +
                            " — tekan Mulai Scan", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void runPreprocessing() {
        if (db.getAllRpIds().isEmpty()) {
            Toast.makeText(this, "Belum ada RP tersimpan",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        btnProcess.setEnabled(false);
        tvStatus.setText("Memproses Pauta + AP Selection...");

        new Thread(() -> {
            engine.runPreprocessing();
            List<ReferencePoint> fps = db.getAllFingerprints();
            List<String> selectedAps = db.getSelectedBssids(999);
            StringBuilder sb = new StringBuilder();
            sb.append("=== Hasil Fingerprint DB ===\n\n");
            for (ReferencePoint rp : fps) {
                sb.append(String.format("RP%d — %s (%.2f, %.2f) | %d AP lolos Pauta\n",
                        rp.getId(), rp.getRoomName(),
                        rp.getX(), rp.getY(),
                        rp.getRssMap().size()));
            }
            sb.append("\nAP Selection: ")
                    .append(selectedAps.size())
                    .append(" AP terpilih");

            new Handler(Looper.getMainLooper()).post(() -> {
                tvStatus.setText(sb.toString());
                btnProcess.setEnabled(true);
                Toast.makeText(this, "Fingerprint DB siap!",
                        Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, PERM_REQ_CODE);
        }
    }

    private void importDatabase() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Pilih file fingerprint.db"), 200);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                db.close();
                InputStream is = getContentResolver().openInputStream(uri);
                File dbFile = getDatabasePath("fingerprint.db");

                // Pastikan folder ada
                dbFile.getParentFile().mkdirs();

                FileOutputStream fos = new FileOutputStream(dbFile);
                byte[] buffer = new byte[4096];
                int len;
                int total = 0;
                while ((len = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                    total += len;
                }
                is.close();
                fos.flush();
                fos.close();

                Toast.makeText(this, "File tersalin: " + total + " bytes", Toast.LENGTH_LONG).show();

                db = new FingerprintDatabase(this);
                engine = new PositioningEngine(db);

                Intent restart = new Intent(this, CalibrationActivity.class);
                finish();
                startActivity(restart);
            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Request: " + requestCode + " Result: " + resultCode, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wifiScanner != null) wifiScanner.release();
    }
}