package com.dfm.reedsim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

/** Touch-reactive stand-in for the game's harmonica screen. */
public final class HarmonicaGameView extends View {
    private static final int NONE = -1;
    private static final int MOD_SHARP = 20;
    private static final int MOD_HIGH = 21;
    private static final int MOD_NATURAL = 22;
    private static final int MOD_LOW = 23;
    private static final double[] BASE_HZ = {261.63, 293.66, 329.63, 349.23, 392.00, 440.00, 493.88, 523.25};
    private static final String[] NOTE_NAMES = {"C", "D", "E", "F", "G", "A", "B", "C"};

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mountains = new Path();
    private final SparseIntArray pointerRoles = new SparseIntArray();
    private final boolean[] noteDown = new boolean[8];
    private final ToneSynth synth = new ToneSynth();
    private LinearGradient sky;
    private LinearGradient water;
    private final float[] noteX = new float[8];
    private final float[] modX = new float[4];
    private float noteY, modY, noteRadius, modRadius;
    private boolean sharpDown, highDown, naturalDown, lowDown;
    private long sharpAt, highAt, lowAt;
    private String lastNote = "等待输入";
    private String lastTiming = "请用风簧悬浮球进行标定";
    private int hitCount;
    private long flashUntil;

    public HarmonicaGameView(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);
        setFocusable(true);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        sky = new LinearGradient(0, 0, 0, h * .62f,
                new int[]{0xff91aeb5, 0xffb4beba, 0xff7f938a}, null, Shader.TileMode.CLAMP);
        water = new LinearGradient(0, h * .55f, 0, h,
                new int[]{0xff637c77, 0xff344b49, 0xff172827}, null, Shader.TileMode.CLAMP);
        modY = h * .255f;
        noteY = h * .535f;
        noteRadius = Math.min(w * .031f, h * .064f);
        modRadius = noteRadius * .82f;
        for (int i = 0; i < 8; i++) noteX[i] = w * (.185f + i * .087f);
        modX[0] = w * .34f;
        modX[1] = w * .44f;
        modX[2] = w * .52f;
        modX[3] = w * .60f;
        buildMountains(w, h);
    }

    private void buildMountains(int w, int h) {
        mountains.reset();
        mountains.moveTo(0, h * .48f);
        mountains.lineTo(0, h * .18f);
        mountains.lineTo(w * .08f, h * .08f);
        mountains.lineTo(w * .16f, h * .26f);
        mountains.lineTo(w * .25f, h * .16f);
        mountains.lineTo(w * .34f, h * .39f);
        mountains.lineTo(w * .44f, h * .23f);
        mountains.lineTo(w * .54f, h * .42f);
        mountains.lineTo(w * .66f, h * .20f);
        mountains.lineTo(w * .78f, h * .38f);
        mountains.lineTo(w * .91f, h * .15f);
        mountains.lineTo(w, h * .26f);
        mountains.lineTo(w, h * .6f);
        mountains.close();
    }

    @Override protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        p.setShader(sky); c.drawRect(0, 0, w, h * .62f, p); p.setShader(null);
        p.setColor(0xff53635a); c.drawPath(mountains, p);
        p.setColor(0x55303c35); c.drawPath(mountains, p);
        drawTrees(c, w, h);
        p.setShader(water); c.drawRect(0, h * .53f, w, h, p); p.setShader(null);
        p.setColor(0x227de0da);
        for (int i = 0; i < 11; i++) {
            float y = h * (.60f + i * .029f);
            c.drawRect(w * (.04f + (i % 3) * .035f), y, w * (.72f + (i % 4) * .055f), y + 1.5f, p);
        }
        drawForegroundRocks(c, w, h);
        p.setColor(0x420b1516); c.drawRect(0, 0, w, h, p);
        p.setColor(0x383a4a4a); c.drawRect(0, h * .14f, w, h * .72f, p);
        drawTopStatus(c, w, h);
        drawModifiers(c, h);
        drawNotes(c, w, h);
        drawBottom(c, w, h);
        if (SystemClock.uptimeMillis() < flashUntil) postInvalidateDelayed(30);
    }

    private void drawTrees(Canvas c, int w, int h) {
        p.setColor(0xff263c30);
        for (int i = 0; i < 18; i++) {
            float x = w * (i / 17f);
            float base = h * (.48f + (i % 4) * .018f);
            float r = h * (.022f + (i % 3) * .009f);
            c.drawRect(x-r*.12f, base-r*.2f, x+r*.12f, base+r*.9f, p);
            c.drawCircle(x, base-r*.55f, r, p);
        }
    }

    private void drawForegroundRocks(Canvas c, int w, int h) {
        p.setColor(0xff18201d);
        c.drawOval(w*.28f, h*.79f, w*.52f, h*1.08f, p);
        c.drawOval(w*.49f, h*.82f, w*.75f, h*1.10f, p);
        p.setColor(0xff252b24);
        c.drawOval(w*-.05f, h*.78f, w*.22f, h*1.10f, p);
        c.drawOval(w*.80f, h*.76f, w*1.08f, h*1.10f, p);
    }

    private void drawTopStatus(Canvas c, int w, int h) {
        float d = getResources().getDisplayMetrics().density;
        p.setColor(0x99202a2b);
        c.drawRoundRect(w-285*d, 18*d, w-20*d, 60*d, 7*d, 7*d, p);
        p.setTextAlign(Paint.Align.CENTER); p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        p.setTextSize(13*d); p.setColor(0xffe5ece9);
        c.drawText("口风琴测试场  ·  点击反馈开启", w-152*d, 45*d, p);
        p.setTextAlign(Paint.Align.LEFT); p.setTextSize(12*d); p.setColor(0xffdae1de);
        c.drawText("模拟界面 / 无游戏数据", 24*d, 39*d, p);
    }

    private void drawModifiers(Canvas c, int h) {
        String[] labels = {"半音", "升调", "自然音", "降调"};
        boolean[] states = {sharpDown, highDown, naturalDown, lowDown};
        p.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < 4; i++) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(states[i] ? 0x998e9b9b : 0x88333f41);
            c.drawCircle(modX[i], modY, modRadius, p);
            if (states[i]) {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2.2f);
                p.setColor(0xffdce4e0); c.drawCircle(modX[i], modY, modRadius+3, p);
            }
            p.setStyle(Paint.Style.FILL); p.setColor(0xffd7ddda);
            p.setTypeface(android.graphics.Typeface.DEFAULT); p.setTextSize(h*.030f);
            c.drawText(labels[i], modX[i], modY+h*.010f, p);
        }
        p.setColor(0x88dce4e0); p.setStrokeWidth(1.5f);
        c.drawLine((modX[0]+modX[1])*.5f, modY-modRadius*.65f,
                (modX[0]+modX[1])*.5f, modY+modRadius*.65f, p);
    }

    private void drawNotes(Canvas c, int w, int h) {
        p.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < 8; i++) {
            boolean flash = noteDown[i] || (SystemClock.uptimeMillis() < flashUntil && lastNote.startsWith(String.valueOf(i+1)));
            p.setStyle(Paint.Style.FILL); p.setColor(flash ? 0xccdce6e2 : 0xaa111819);
            c.drawCircle(noteX[i], noteY, noteRadius, p);
            if (flash) {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3f); p.setColor(0xffeffff8);
                c.drawCircle(noteX[i], noteY, noteRadius+5, p);
            }
            p.setStyle(Paint.Style.FILL); p.setColor(flash ? 0xff17201e : 0xfff0f2ef);
            p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); p.setTextSize(h*.053f);
            c.drawText(i == 7 ? "1" : String.valueOf(i+1), noteX[i], noteY+h*.020f, p);
            if (highDown || i == 7) c.drawCircle(noteX[i], noteY-noteRadius*.58f, h*.0045f, p);
            if (lowDown) c.drawCircle(noteX[i], noteY+noteRadius*.62f, h*.0045f, p);
            if (sharpDown) {
                p.setTextSize(h*.023f); p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                c.drawText("♯", noteX[i]+noteRadius*.48f, noteY-noteRadius*.28f, p);
            }
        }
        p.setColor(0x88d8dfdc); p.setStrokeWidth(1f);
        c.drawLine(w*.145f, h*.665f, w*.795f, h*.665f, p);
    }

    private void drawBottom(Canvas c, int w, int h) {
        float d = getResources().getDisplayMetrics().density;
        p.setColor(0x99eef1ed); c.drawCircle(w*.17f, h*.72f, h*.035f, p);
        p.setColor(0x77e5ebe8); p.setStrokeWidth(2*d);
        c.drawLine(w*.17f, h*.72f, w*.75f, h*.72f, p);
        p.setColor(0xaa111919);
        c.drawRoundRect(22*d, h-82*d, 385*d, h-20*d, 10*d, 10*d, p);
        p.setTextAlign(Paint.Align.LEFT); p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        p.setTextSize(14*d); p.setColor(0xffedf4f1);
        c.drawText(lastNote, 38*d, h-54*d, p);
        p.setTypeface(android.graphics.Typeface.DEFAULT); p.setTextSize(11*d); p.setColor(0xffb9c6c1);
        c.drawText(lastTiming + "  ·  累计 " + hitCount + " 次", 38*d, h-32*d, p);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int pointerId = event.getPointerId(index);
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int role = roleAt(event.getX(index), event.getY(index));
            pointerRoles.put(pointerId, role);
            press(role);
            invalidate();
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
            int role = pointerRoles.get(pointerId, NONE);
            releaseRole(role);
            pointerRoles.delete(pointerId);
            if (action == MotionEvent.ACTION_CANCEL) resetTouches();
            invalidate();
            return true;
        }
        return true;
    }

    private int roleAt(float x, float y) {
        for (int i = 0; i < 8; i++) if (Math.hypot(x-noteX[i], y-noteY) <= noteRadius*1.25f) return i;
        for (int i = 0; i < 4; i++) if (Math.hypot(x-modX[i], y-modY) <= modRadius*1.35f)
            return new int[]{MOD_SHARP, MOD_HIGH, MOD_NATURAL, MOD_LOW}[i];
        return NONE;
    }

    private void press(int role) {
        long now = SystemClock.elapsedRealtimeNanos();
        if (role == MOD_SHARP) { sharpDown = true; sharpAt = now; return; }
        if (role == MOD_HIGH) { highDown = true; lowDown = false; highAt = now; return; }
        if (role == MOD_LOW) { lowDown = true; highDown = false; lowAt = now; return; }
        if (role == MOD_NATURAL) { naturalDown = true; return; }
        if (role < 0 || role > 7) return;
        noteDown[role] = true;
        double hz = BASE_HZ[role];
        int octave = highDown ? 1 : lowDown ? -1 : 0;
        if (octave > 0) hz *= 2; else if (octave < 0) hz /= 2;
        if (sharpDown) hz *= Math.pow(2, 1.0/12.0);
        synth.play(hz);
        hitCount++;
        String zone = octave > 0 ? "高音" : octave < 0 ? "低音" : "中音";
        lastNote = (role+1) + "  " + (sharpDown ? "♯" : "") + NOTE_NAMES[role] + "  ·  " + zone +
                String.format(Locale.ROOT, "  %.1f Hz", hz);
        long leadNs = Long.MAX_VALUE;
        if (sharpDown) leadNs = Math.min(leadNs, now-sharpAt);
        if (highDown) leadNs = Math.min(leadNs, now-highAt);
        if (lowDown) leadNs = Math.min(leadNs, now-lowAt);
        lastTiming = leadNs == Long.MAX_VALUE ? "自然音直接点击" :
                String.format(Locale.ROOT, "修饰键提前 %.1f ms", leadNs/1_000_000.0);
        flashUntil = SystemClock.uptimeMillis() + 170;
    }

    private void releaseRole(int role) {
        if (role == MOD_SHARP) sharpDown = false;
        else if (role == MOD_HIGH) highDown = false;
        else if (role == MOD_LOW) lowDown = false;
        else if (role == MOD_NATURAL) naturalDown = false;
        else if (role >= 0 && role < 8) noteDown[role] = false;
    }

    private void resetTouches() {
        pointerRoles.clear(); sharpDown = highDown = naturalDown = lowDown = false;
        for (int i = 0; i < noteDown.length; i++) noteDown[i] = false;
    }

    public void release() { synth.release(); }
}
