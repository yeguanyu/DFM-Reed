package com.dfm.reed.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.dfm.reed.ui.Ui;

public final class CalibrationView extends View {
    public interface Listener {
        void onSave(float[] xs, float[] ys);
        void onCancel();
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] xs = new float[11];
    private final float[] ys = new float[11];
    private final Listener listener;
    private final int savedWidth;
    private final int savedHeight;
    private int dragging = -1;
    private boolean initialized;
    private boolean simulatorPreset;

    public CalibrationView(Context context, float[] savedX, float[] savedY,
                           int savedWidth, int savedHeight, Listener listener) {
        super(context);
        this.listener = listener;
        this.savedWidth = savedWidth;
        this.savedHeight = savedHeight;
        System.arraycopy(savedX, 0, xs, 0, 11);
        System.arraycopy(savedY, 0, ys, 0, 11);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (initialized) {
            if (oldw > 0 && oldh > 0 && (w != oldw || h != oldh)) {
                scalePoints(w / (float) oldw, h / (float) oldh);
                invalidate();
            }
            return;
        }
        boolean empty = xs[0] <= 0;
        if (empty) {
            for (int i = 0; i < 8; i++) {
                xs[i] = w * (.15f + i * .10f);
                ys[i] = h * .72f;
            }
            xs[8] = w * .36f; ys[8] = h * .54f;
            xs[9] = w * .50f; ys[9] = h * .54f;
            xs[10] = w * .64f; ys[10] = h * .54f;
        } else if (savedWidth > 0 && savedHeight > 0) {
            int[] location = new int[2];
            getLocationOnScreen(location);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            float sx = screenWidth / (float) savedWidth;
            float sy = screenHeight / (float) savedHeight;
            for (int i = 0; i < 11; i++) {
                xs[i] = xs[i] * sx - location[0];
                ys[i] = ys[i] * sy - location[1];
            }
        } else {
            // Legacy coordinates did not record their canvas size. Keep every point reachable.
            for (int i = 0; i < 11; i++) {
                xs[i] = Math.max(30, Math.min(w - 30, xs[i]));
                ys[i] = Math.max(100, Math.min(h - 90, ys[i]));
            }
        }
        initialized = true;
    }

    private void scalePoints(float sx, float sy) {
        for (int i = 0; i < 11; i++) {
            xs[i] *= sx;
            ys[i] *= sy;
        }
    }

    /** Exact centers used by the companion test-field APK. Safe to call before layout. */
    public void applySimulatorPreset() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            post(this::applySimulatorPreset);
            return;
        }
        int w = getWidth(), h = getHeight();
        for (int i = 0; i < 8; i++) {
            xs[i] = w * (.185f + i * .087f);
            ys[i] = h * .535f;
        }
        // Calibration order is low, sharp, high. Test field order is sharp, high, natural, low.
        xs[8] = w * .60f; ys[8] = h * .255f;
        xs[9] = w * .34f; ys[9] = h * .255f;
        xs[10] = w * .44f; ys[10] = h * .255f;
        simulatorPreset = true;
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        c.drawColor(0x33000000);

        p.setColor(0xdd111512);
        c.drawRoundRect(new RectF(18*d, 18*d, Math.min(getWidth()-18*d, 390*d), 84*d), 16*d, 16*d, p);
        p.setColor(Color.WHITE); p.setTextSize(15*d); p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.LEFT);
        c.drawText("标定音键与修饰键", 34*d, 45*d, p);
        p.setColor(0xffb9c0b9); p.setTextSize(11*d); p.setTypeface(android.graphics.Typeface.DEFAULT);
        c.drawText(simulatorPreset ? "已识别测试场并自动对齐按钮中心" :
                "1–8 是音键；低 / 升 / 高是需提前按住的修饰键", 34*d, 67*d, p);

        String[] labels = {"1","2","3","4","5","6","7","1̇","低","升","高"};
        for (int i = 0; i < 11; i++) {
            p.setStyle(Paint.Style.FILL); p.setColor(i == dragging ? 0xeeffffff : 0xccb9e769);
            c.drawCircle(xs[i], ys[i], 25*d, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2*d); p.setColor(Ui.INK);
            c.drawCircle(xs[i], ys[i], 19*d, p);
            c.drawLine(xs[i]-10*d, ys[i], xs[i]+10*d, ys[i], p);
            c.drawLine(xs[i], ys[i]-10*d, xs[i], ys[i]+10*d, p);
            p.setStyle(Paint.Style.FILL); p.setColor(Color.WHITE);
            c.drawCircle(xs[i], ys[i], 2.5f*d, p);
            p.setStyle(Paint.Style.FILL); p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); p.setTextSize(11*d); p.setColor(Ui.INK);
            c.drawText(labels[i], xs[i], ys[i]-29*d, p);
        }

        float y = getHeight() - 70*d;
        float saveLeft = getWidth()/2f - 108*d;
        float saveRight = getWidth()/2f + 108*d;
        float cancelLeft = getWidth() - 112*d;
        float cancelRight = getWidth() - 20*d;
        float cancelTop = 22*d;
        p.setColor(Ui.INK); c.drawRoundRect(new RectF(cancelLeft, cancelTop, cancelRight, cancelTop+44*d), 13*d, 13*d, p);
        p.setColor(Color.WHITE); p.setTextSize(13*d); p.setTextAlign(Paint.Align.CENTER);
        c.drawText("退出标定", (cancelLeft+cancelRight)/2f, cancelTop+28*d, p);
        p.setColor(Ui.ACCENT); c.drawRoundRect(new RectF(saveLeft, y, saveRight, y+50*d), 14*d, 14*d, p);
        p.setColor(Ui.INK); p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText("保存并依次试按 1–8", getWidth()/2f, y+31*d, p);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float d = getResources().getDisplayMetrics().density;
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float saveY = getHeight() - 70*d;
            float saveLeft = getWidth()/2f - 108*d;
            float saveRight = getWidth()/2f + 108*d;
            float cancelLeft = getWidth() - 112*d;
            if (e.getY() >= saveY && e.getY() <= saveY+50*d &&
                    e.getX() >= saveLeft && e.getX() <= saveRight) {
                int[] location = new int[2];
                getLocationOnScreen(location);
                float[] globalX = xs.clone();
                float[] globalY = ys.clone();
                for (int i = 0; i < 11; i++) {
                    globalX[i] += location[0];
                    globalY[i] += location[1];
                }
                listener.onSave(globalX, globalY);
                return true;
            }
            if (e.getY() >= 22*d && e.getY() <= 66*d && e.getX() >= cancelLeft) {
                listener.onCancel();
                return true;
            }
            float best = 50*d;
            for (int i = 0; i < 11; i++) {
                float dist = (float)Math.hypot(e.getX()-xs[i], e.getY()-ys[i]);
                if (dist < best) { best = dist; dragging = i; }
            }
            invalidate();
            return true;
        }
        if (e.getActionMasked() == MotionEvent.ACTION_MOVE && dragging >= 0) {
            xs[dragging] = e.getX(); ys[dragging] = e.getY(); invalidate(); return true;
        }
        if (e.getActionMasked() == MotionEvent.ACTION_UP || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            dragging = -1; invalidate(); return true;
        }
        return true;
    }
}
