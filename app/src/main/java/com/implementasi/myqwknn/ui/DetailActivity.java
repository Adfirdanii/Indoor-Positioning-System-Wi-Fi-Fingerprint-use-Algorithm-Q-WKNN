package com.implementasi.myqwknn.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_TP_NAME   = "tp_name";
    public static final String EXTRA_ETA        = "eta";
    public static final String EXTRA_L          = "l";
    public static final String EXTRA_TRUE_X     = "true_x";
    public static final String EXTRA_TRUE_Y     = "true_y";
    public static final String EXTRA_ERR_Q      = "err_q";
    public static final String EXTRA_ERR_W      = "err_w";
    public static final String EXTRA_DETAIL_Q   = "detail_q";
    public static final String EXTRA_DETAIL_W   = "detail_w";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(32, 32, 32, 32);
        scroll.addView(ll);
        setContentView(scroll);

        setTitle("Detail Perhitungan");

        Intent intent = getIntent();
        String tpName  = intent.getStringExtra(EXTRA_TP_NAME);
        double eta     = intent.getDoubleExtra(EXTRA_ETA, 0);
        int l          = intent.getIntExtra(EXTRA_L, 0);
        double trueX   = intent.getDoubleExtra(EXTRA_TRUE_X, 0);
        double trueY   = intent.getDoubleExtra(EXTRA_TRUE_Y, 0);
        double errQ    = intent.getDoubleExtra(EXTRA_ERR_Q, -1);
        double errW    = intent.getDoubleExtra(EXTRA_ERR_W, -1);
        String detailQ = intent.getStringExtra(EXTRA_DETAIL_Q);
        String detailW = intent.getStringExtra(EXTRA_DETAIL_W);

        // ── Header info ──────────────────────────────────────────
        addHeader(ll, "Detail Perhitungan");
        addText(ll, "TP       : " + tpName);
        addText(ll, "η        : " + eta + "  |  L: " + l);
        addText(ll, String.format(java.util.Locale.US,
                "Koordinat: (%.2f, %.2f)", trueX, trueY));
        addDivider(ll);

        // ── Q-WKNN detail ────────────────────────────────────────
        addHeader(ll, "Q-WKNN");
        if (errQ >= 0) {
            addText(ll, String.format(java.util.Locale.US, "Error: %.3f m", errQ));
        }
        addText(ll, detailQ != null ? detailQ : "Tidak ada data.");
        addDivider(ll);

        // ── WKNN detail ──────────────────────────────────────────
        addHeader(ll, "WKNN");
        if (errW >= 0) {
            addText(ll, String.format(java.util.Locale.US, "Error: %.3f m", errW));
        }
        addText(ll, detailW != null ? detailW : "Tidak ada data.");
    }

    private void addHeader(LinearLayout ll, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(0xFF1565C0);
        tv.setPadding(0, 16, 0, 8);
        ll.addView(tv);
    }

    private void addText(LinearLayout ll, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        tv.setPadding(0, 4, 0, 4);
        ll.addView(tv);
    }

    private void addDivider(LinearLayout ll) {
        android.view.View v = new android.view.View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        v.setBackgroundColor(0xFFBBBBBB);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
        lp.topMargin = 16;
        lp.bottomMargin = 16;
        v.setLayoutParams(lp);
        ll.addView(v);
    }
}