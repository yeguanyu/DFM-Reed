package com.dfm.reed.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import com.dfm.reed.ui.Ui;

public final class FloatBarView extends View {
    public interface Listener {
        void onMove(float dx, float dy);
        void onExpandedChanged(boolean expanded);
        void onPrevious();
        void onPlayPause();
        void onNext();
        void onStop();
        void onCalibrate();
        void onSpeedChanged(float speed);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Listener listener;
    private boolean expanded;
    private boolean playing;
    private boolean paused;
    private String status = "就绪";
    private String songTitle = "选择一首歌";
    private float speed = 1f;
    private boolean adjustingSpeed;
    private float downRawX, downRawY, lastRawX, lastRawY;
    private boolean moved;

    public FloatBarView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setSongTitle(String value) { songTitle = value == null ? "未命名曲谱" : value; invalidate(); }
    public void setSpeed(float value) { speed = Math.max(.25f, Math.min(4f, value)); invalidate(); }

    public void setPlaybackState(boolean playing, boolean paused, String status) {
        this.playing = playing; this.paused = paused; this.status = status == null ? "" : status; invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        paint.setShadowLayer(12*d, 0, 4*d, 0x55000000); paint.setColor(Ui.INK);
        c.drawRoundRect(4*d, 4*d, getWidth()-4*d, getHeight()-4*d, 20*d, 20*d, paint);
        paint.clearShadowLayer();
        float centerY = getHeight()/2f;
        paint.setColor(Ui.ACCENT); c.drawCircle(31*d, centerY, expanded ? 24*d : 21*d, paint);
        paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(expanded ? 19*d : 13*d); paint.setColor(Ui.INK);
        c.drawText(expanded ? "‹" : "♫", 31*d, centerY + (expanded ? 7*d : 5*d), paint);
        if (!expanded) return;

        paint.setTextAlign(Paint.Align.LEFT); paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(13*d); paint.setColor(Color.WHITE);
        c.drawText(ellipsize(songTitle, 17), 62*d, 25*d, paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT); paint.setTextSize(9*d); paint.setColor(0xffaeb6b0);
        c.drawText(status, 62*d, 42*d, paint);

        drawControl(c, 61, 107, "上一首", false);
        drawControl(c, 111, 173, playing && !paused ? "暂停" : "播放", true);
        drawControl(c, 177, 223, "下一首", false);
        drawControl(c, 227, 273, "停止", false);
        drawControl(c, 277, 327, "标定", false);
        drawSpeed(c);
    }

    private void drawSpeed(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        float left = 76*d, right = 306*d, y = 96*d;
        paint.setTextAlign(Paint.Align.LEFT); paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(9.5f*d); paint.setColor(0xffdce3de);
        c.drawText(String.format(java.util.Locale.ROOT, "%.2f×", speed), 18*d, 99*d, paint);
        paint.setStrokeWidth(3*d); paint.setStrokeCap(Paint.Cap.ROUND); paint.setColor(0xff444b46);
        c.drawLine(left, y, right, y, paint);
        float knobX = left + (speed-.25f)/3.75f*(right-left);
        paint.setColor(Ui.ACCENT); c.drawLine(left, y, knobX, y, paint); c.drawCircle(knobX, y, 7*d, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawControl(Canvas c, float leftDp, float rightDp, String label, boolean accent) {
        float d = getResources().getDisplayMetrics().density;
        paint.setStyle(Paint.Style.FILL); paint.setColor(accent ? Ui.ACCENT : 0xff303632);
        c.drawRoundRect(leftDp*d, 51*d, rightDp*d, 77*d, 8*d, 8*d, paint);
        paint.setColor(accent ? Ui.INK : 0xffe4e9e5); paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); paint.setTextSize(9.5f*d);
        c.drawText(label, (leftDp+rightDp)*.5f*d, 68.5f*d, paint);
    }

    private String ellipsize(String text, int max) { return text.length() <= max ? text : text.substring(0, max-1) + "…"; }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (expanded && e.getY()/getResources().getDisplayMetrics().density > 82) {
                    adjustingSpeed = true; updateSpeed(e.getX()); return true;
                }
                downRawX = lastRawX = e.getRawX(); downRawY = lastRawY = e.getRawY(); moved = false; return true;
            case MotionEvent.ACTION_MOVE:
                if (adjustingSpeed) { updateSpeed(e.getX()); return true; }
                float dx = e.getRawX()-lastRawX, dy = e.getRawY()-lastRawY;
                if (Math.hypot(e.getRawX()-downRawX, e.getRawY()-downRawY) > Ui.dp(getContext(), 7)) moved = true;
                if (moved) listener.onMove(dx, dy);
                lastRawX = e.getRawX(); lastRawY = e.getRawY(); return true;
            case MotionEvent.ACTION_UP:
                if (adjustingSpeed) { updateSpeed(e.getX()); adjustingSpeed = false; return true; }
                if (moved) return true;
                float d = getResources().getDisplayMetrics().density;
                float x = e.getX()/d, y = e.getY()/d;
                if (!expanded || x < 58) {
                    expanded = !expanded; listener.onExpandedChanged(expanded); return true;
                }
                if (y < 47) return true;
                if (x < 109) listener.onPrevious();
                else if (x < 175) listener.onPlayPause();
                else if (x < 225) listener.onNext();
                else if (x < 275) listener.onStop();
                else listener.onCalibrate();
                return true;
            default: return true;
        }
    }

    private void updateSpeed(float touchX) {
        float d = getResources().getDisplayMetrics().density;
        float fraction = Math.max(0, Math.min(1, (touchX-76*d)/(230*d)));
        speed = .25f + fraction*3.75f;
        // Snap around common listening speeds without limiting the full range.
        float[] snaps = {.5f, .75f, 1f, 1.25f, 1.5f, 2f, 3f, 4f};
        for (float snap : snaps) if (Math.abs(speed-snap) < .035f) speed = snap;
        listener.onSpeedChanged(speed);
        invalidate();
    }
}
