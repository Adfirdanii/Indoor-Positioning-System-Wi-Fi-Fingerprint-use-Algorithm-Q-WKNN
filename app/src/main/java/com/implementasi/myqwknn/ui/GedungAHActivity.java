package com.implementasi.myqwknn.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.implementasi.myqwknn.R;
import com.implementasi.myqwknn.ui.ZenodoActivity;

public class GedungAHActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gedung_ah);

        // Cek apakah masuk dari mode debug
        boolean debugMode = getIntent().getBooleanExtra("debug_mode", false);
        if (!debugMode) {
            // Kalau bukan dari debug, balik ke splash
            Toast.makeText(this, "Akses tidak diizinkan.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView tvDebugLabel = findViewById(R.id.tv_debug_label);
        if (tvDebugLabel != null) {
            tvDebugLabel.setText("🛠 Mode Developer");
        }

        Button btnKalibrasi   = findViewById(R.id.btn_calibration);
        Button btnPositioning = findViewById(R.id.btn_positioning);
        Button btnAnalysis    = findViewById(R.id.btn_analysis);
        Button btnZenodo      = findViewById(R.id.btn_zenodo);

        btnKalibrasi.setOnClickListener(v ->
                startActivity(new Intent(this, CalibrationActivity.class)));
        btnPositioning.setOnClickListener(v ->
                startActivity(new Intent(this, PositioningActivity.class)));
        btnAnalysis.setOnClickListener(v ->
                startActivity(new Intent(this, AnalysisActivity.class)));
        btnZenodo.setOnClickListener(v ->
                startActivity(new Intent(this, ZenodoActivity.class)));
    }
}