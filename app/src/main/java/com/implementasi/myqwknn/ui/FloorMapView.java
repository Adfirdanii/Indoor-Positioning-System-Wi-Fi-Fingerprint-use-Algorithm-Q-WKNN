package com.implementasi.myqwknn.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.implementasi.myqwknn.R;

public class FloorMapView extends View {

    // Dimensi gedung dalam meter
    private static final float BUILDING_W = 49.12f;
    private static final float BUILDING_H = 18.25f;

    // Batas pixel area gedung di dalam gambar PNG denah_ah.png (2084 x 985 px)
    // Diukur langsung dari analisis pixel gambar
    private static final float IMG_LEFT   = 181f;
    private static final float IMG_TOP    = 194f;
    private static final float IMG_RIGHT  = 1975f;
    private static final float IMG_BOTTOM = 824f;  // garis bawah gedung, bukan sumbu X

    // Selasar/koridor gedung ada di tengah (area yang sama dengan tangga),
    // menghubungkan baris ruangan atas (AH1.1-1.7) & bawah (AH1.8-1.14).
    // Nilai ini dikonversi dari batas piksel dinding koridor (imgY 463-557)
    // ke koordinat meter menggunakan rumus flip Y yang sama seperti meterToScreen.
    private static final float CORRIDOR_Y = 9.09f;

    // Paint
    private final Paint paintUser    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintDest    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintLine    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintLabel   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPulse   = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Bitmap denah
    private Bitmap denahBitmap;
    private Matrix matrix = new Matrix();

    // Zoom & pan
    private float scaleFactor = 1f;
    private float minScale = 0.1f;
    private float transX = 0f, transY = 0f;
    private float lastX, lastY;
    private ScaleGestureDetector scaleDetector;
    private boolean isScaling = false;

    // State posisi
    private float userX = -1, userY = -1;
    private String userRoom = "";
    private float destX = -1, destY = -1;
    private String destName = "";

    // Animasi pulsa
    private float pulseRadius = 0f;
    private boolean pulseGrowing = true;
    private final Runnable pulseRunnable = new Runnable() {
        @Override
        public void run() {
            if (userX >= 0) {
                pulseRadius += pulseGrowing ? 1f : -1f;
                if (pulseRadius > 20f) pulseGrowing = false;
                if (pulseRadius < 0f) pulseGrowing = true;
                invalidate();
                postDelayed(this, 30);
            }
        }
    };

    public FloorMapView(Context context) {
        super(context);
        init(context);
    }

    public FloorMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        // Load bitmap denah
        try {
            // PENTING: cegah Android auto-scale bitmap sesuai densitas layar.
            // Tanpa ini, jika denah_ah.png ada di folder res/drawable/ biasa
            // (tanpa qualifier densitas), BitmapFactory bisa memperbesar bitmap
            // secara otomatis saat decode, sehingga denahBitmap.getWidth()/getHeight()
            // TIDAK LAGI 2084x985 -- padahal semua konstanta kalibrasi (IMG_TOP,
            // IMG_BOTTOM, dll) dihitung berdasarkan ukuran asli 2084x985.
            // Ketidakcocokan ini menyebabkan titik salah posisi (termasuk
            // terlihat pindah baris).
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            denahBitmap = BitmapFactory.decodeResource(
                    context.getResources(), R.drawable.denah_ah, opts);
        } catch (Exception e) {
            denahBitmap = null;
        }

        // Paint user marker (merah)
        paintUser.setAntiAlias(true);

        // Paint dest marker (hijau)
        paintDest.setAntiAlias(true);

        // Paint garis putus-putus
        paintLine.setColor(Color.parseColor("#4CAF50"));
        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setStrokeWidth(4f);
        paintLine.setPathEffect(new android.graphics.DashPathEffect(
                new float[]{15f, 10f}, 0));
        paintLine.setAntiAlias(true);

        // Paint label
        paintLabel.setAntiAlias(true);
        paintLabel.setTextAlign(Paint.Align.CENTER);
        paintLabel.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Paint pulsa
        paintPulse.setAntiAlias(true);
        paintPulse.setStyle(Paint.Style.FILL);

        // Scale gesture
        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float factor = detector.getScaleFactor();
                        float focusX = detector.getFocusX();
                        float focusY = detector.getFocusY();

                        float newScale = scaleFactor * factor;
                        newScale = Math.max(minScale, Math.min(newScale, 8f));

                        // Zoom ke arah fokus jari
                        float scaleChange = newScale / scaleFactor;
                        transX = focusX - scaleChange * (focusX - transX);
                        transY = focusY - scaleChange * (focusY - transY);
                        scaleFactor = newScale;

                        invalidate();
                        return true;
                    }

                    @Override
                    public boolean onScaleBegin(ScaleGestureDetector detector) {
                        isScaling = true;
                        return true;
                    }

                    @Override
                    public void onScaleEnd(ScaleGestureDetector detector) {
                        isScaling = false;
                    }

                });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        if (denahBitmap != null && w > 0 && h > 0) {
            // Denah landscape (2084x985) ditampilkan di layar portrait.
            // Kalau hanya scale berdasarkan lebar (scaleByW), tinggi denah
            // jadi sangat kecil dan menyisakan area kosong besar di atas/bawah
            // (letterboxing) -> baris atas & bawah gedung tampak berdempetan.
            //
            // Solusinya: pakai scale yang lebih besar antara "pas lebar" dan
            // "pas tinggi x faktor", supaya denah mengisi layar lebih penuh
            // (mode "cover" sebagian), lalu sisi kiri-kanan yang terpotong
            // bisa digeser (pan) oleh user.
            float scaleByW = (float) w / denahBitmap.getWidth();
            float scaleByH = (float) h / denahBitmap.getHeight();

            scaleFactor = Math.max(scaleByW, scaleByH * 0.7f);

            transX = (w - denahBitmap.getWidth() * scaleFactor) / 2f;
            transY = (h - denahBitmap.getHeight() * scaleFactor) / 2f;

            // Batas zoom-out minimum: jangan sampai lebih kecil dari kondisi awal
            minScale = Math.min(scaleByW, scaleByH) * 0.5f;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (denahBitmap == null) {
            // Fallback kalau gambar tidak ada
            Paint p = new Paint();
            p.setColor(Color.GRAY);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(40f);
            canvas.drawText("Denah tidak tersedia", getWidth()/2f, getHeight()/2f, p);
            return;
        }

        // Gambar denah PNG dengan transform
        matrix.reset();
        matrix.postScale(scaleFactor, scaleFactor);
        matrix.postTranslate(transX, transY);
        canvas.drawBitmap(denahBitmap, matrix, null);

        // Konversi koordinat meter ke pixel layar
        // user & dest
        if (userX >= 0) {
            float[] screenPt = meterToScreen(userX, userY);
            float sx = screenPt[0];
            float sy = screenPt[1];

            // Lingkaran pulsa
            paintPulse.setColor(Color.parseColor("#F44336"));
            paintPulse.setAlpha(50);
            canvas.drawCircle(sx, sy, 20f + pulseRadius, paintPulse);

            // Titik putih luar
            paintUser.setColor(Color.WHITE);
            paintUser.setStyle(Paint.Style.FILL);
            canvas.drawCircle(sx, sy, 18f, paintUser);

            // Titik merah dalam
            paintUser.setColor(Color.parseColor("#F44336"));
            canvas.drawCircle(sx, sy, 12f, paintUser);

            // Label ruangan
            paintLabel.setColor(Color.parseColor("#B71C1C"));
            paintLabel.setTextSize(28f);
            canvas.drawText(userRoom, sx, sy - 25f, paintLabel);
        }

        // Garis ke tujuan -- lewat selasar tengah (bukan garis lurus tembus tembok)
        if (userX >= 0 && destX >= 0) {
            float[] p1 = meterToScreen(userX, userY);
            float[] p2 = meterToScreen(userX, CORRIDOR_Y);   // keluar ruangan asal ke selasar
            float[] p3 = meterToScreen(destX, CORRIDOR_Y);   // jalan menyusuri selasar
            float[] p4 = meterToScreen(destX, destY);        // masuk ke ruangan tujuan

            Path routePath = new Path();
            routePath.moveTo(p1[0], p1[1]);
            routePath.lineTo(p2[0], p2[1]);
            routePath.lineTo(p3[0], p3[1]);
            routePath.lineTo(p4[0], p4[1]);
            canvas.drawPath(routePath, paintLine);
        }

        // Marker tujuan (hijau)
        if (destX >= 0) {
            float[] screenPt = meterToScreen(destX, destY);
            float sx = screenPt[0];
            float sy = screenPt[1];

            paintDest.setColor(Color.parseColor("#4CAF50"));
            paintDest.setAlpha(60);
            paintDest.setStyle(Paint.Style.FILL);
            canvas.drawCircle(sx, sy, 22f, paintDest);

            paintDest.setAlpha(255);
            canvas.drawCircle(sx, sy, 12f, paintDest);

            paintLabel.setColor(Color.parseColor("#1B5E20"));
            paintLabel.setTextSize(28f);
            canvas.drawText(destName, sx, sy - 28f, paintLabel);
        }
    }

    /**
     * Konversi koordinat meter (sistem koordinat gedung)
     * ke koordinat pixel di layar.
     *
     * Sistem koordinat denah: X ke kanan, Y ke atas (origin pojok kiri bawah)
     * Sistem pixel gambar: X ke kanan, Y ke bawah (origin pojok kiri atas)
     */
    private float[] meterToScreen(float mX, float mY) {
        float imgAreaW = IMG_RIGHT - IMG_LEFT;
        float imgAreaH = IMG_BOTTOM - IMG_TOP;
        float imgX = IMG_LEFT + (mX / BUILDING_W) * imgAreaW;
        float imgY = IMG_TOP + ((BUILDING_H - mY) / BUILDING_H) * imgAreaH;
        float screenX = imgX * scaleFactor + transX;
        float screenY = imgY * scaleFactor + transY;
        return new float[]{screenX, screenY};
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isScaling) {
                    transX += event.getX() - lastX;
                    transY += event.getY() - lastY;
                    invalidate();
                }
                lastX = event.getX();
                lastY = event.getY();
                break;
        }
        return true;
    }

    public void setUserPosition(double x, double y, String room) {
        this.userX = (float) x;
        this.userY = (float) y;
        this.userRoom = room;
        // Tidak ada center — titik muncul di posisi koordinatnya di denah
        // User bisa zoom in untuk melihat lebih jelas
        removeCallbacks(pulseRunnable);
        post(pulseRunnable);
        invalidate();
    }

    // Konversi meter ke koordinat pixel GAMBAR (sebelum transform)
    private float[] meterToImage(float mX, float mY) {
        float imgAreaW = IMG_RIGHT - IMG_LEFT;
        float imgAreaH = IMG_BOTTOM - IMG_TOP;
        float imgX = IMG_LEFT + (mX / BUILDING_W) * imgAreaW;
        float imgY = IMG_TOP + (mY / BUILDING_H) * imgAreaH; // tanpa flip
        return new float[]{imgX, imgY};
    }

    public void setDestination(double x, double y, String name) {
        this.destX = (float) x;
        this.destY = (float) y;
        this.destName = name;
        invalidate();
    }
}