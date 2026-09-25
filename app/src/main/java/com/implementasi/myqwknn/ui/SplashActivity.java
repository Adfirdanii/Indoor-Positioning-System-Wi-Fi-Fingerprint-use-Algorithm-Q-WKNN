package com.implementasi.myqwknn.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.implementasi.myqwknn.MainActivity;
import com.implementasi.myqwknn.R;

public class SplashActivity extends AppCompatActivity {

    private static final String DEBUG_PASSWORD = "admin19";
    private static final long SPLASH_DELAY_MS  = 2500;

    // Trigger debug: logo harus di-tap TAP_COUNT_REQUIRED kali dalam
    // jendela waktu TAP_WINDOW_MS. Ini supaya tap tunggal yang tersenggol
    // gak sengaja tidak langsung memunculkan dialog password ke user biasa.
    private static final int  TAP_COUNT_REQUIRED = 5;
    private static final long TAP_WINDOW_MS = 2000;

    private int tapCount = 0;
    private long firstTapTime = 0;

    private boolean debugDialogShown = false;
    private final Handler splashHandler = new Handler(Looper.getMainLooper());
    private final Runnable splashRunnable = () -> {
        if (!debugDialogShown && !isFinishing()) {
            goToClient();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView imgLogo = findViewById(R.id.img_logo_splash);

        // Tap logo 5x cepat berturut-turut → muncul dialog password debug.
        // Tap biasa (1-4x) tidak memberi reaksi apapun ke user, supaya
        // tidak ada indikasi visual yang memancing rasa penasaran.
        imgLogo.setOnClickListener(v -> onLogoTapped());

        // Auto redirect ke client setelah 2.5 detik
        splashHandler.postDelayed(splashRunnable, SPLASH_DELAY_MS);
    }

    private void onLogoTapped() {
        long now = System.currentTimeMillis();

        // Reset hitungan kalau jendela waktu sudah lewat atau ini tap pertama
        if (tapCount == 0 || (now - firstTapTime) > TAP_WINDOW_MS) {
            tapCount = 1;
            firstTapTime = now;
            return;
        }

        tapCount++;
        if (tapCount >= TAP_COUNT_REQUIRED) {
            tapCount = 0;
            showDebugDialog();
        }
    }

    private void showDebugDialog() {
        debugDialogShown = true;
        splashHandler.removeCallbacks(splashRunnable);

        EditText etPassword = new EditText(this);
        etPassword.setHint("Masukkan password");
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPassword.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(this)
                .setTitle("Mode Developer")
                .setMessage("Masukkan password untuk akses debug:")
                .setView(etPassword)
                .setPositiveButton("Masuk", (dialog, which) -> {
                    String input = etPassword.getText().toString().trim();
                    if (DEBUG_PASSWORD.equals(input)) {
                        Toast.makeText(this, "Mode Developer aktif",
                                Toast.LENGTH_SHORT).show();
                        goToDebug();
                    } else {
                        Toast.makeText(this, "Password salah",
                                Toast.LENGTH_SHORT).show();
                        debugDialogShown = false;
                        // Lanjut ke client setelah 1 detik
                        splashHandler.postDelayed(this::goToClient, 1000);
                    }
                })
                .setNegativeButton("Batal", (dialog, which) -> {
                    debugDialogShown = false;
                    goToClient();
                })
                .setCancelable(false)
                .show();
    }

    private void goToClient() {
        if (!isFinishing()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void goToDebug() {
        Intent intent = new Intent(this, GedungAHActivity.class);
        intent.putExtra("debug_mode", true);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        splashHandler.removeCallbacks(splashRunnable);
    }
}