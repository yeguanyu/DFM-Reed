package com.dfm.reed.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import com.dfm.reed.core.AppPrefs;
import com.dfm.reed.core.ScoreParser;
import com.dfm.reed.core.Song;
import com.dfm.reed.core.SongStore;
import com.dfm.reed.overlay.CalibrationView;
import com.dfm.reed.overlay.FloatBarView;
import com.dfm.reed.ui.Ui;

import java.util.Collections;
import java.util.List;

public final class HarmonicaAccessibilityService extends AccessibilityService {
    public static final String ACTION_SHOW = "com.dfm.reed.SHOW_FLOAT_BAR";

    private WindowManager wm;
    private FloatBarView bar;
    private WindowManager.LayoutParams barParams;
    private CalibrationView calibration;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private List<ScoreParser.Event> events = Collections.emptyList();
    private float totalBeats;
    private int eventIndex;
    private long countdownUntil;
    private long countdownRemaining;
    private long lastTickAt;
    private float baseBeatMs;
    private float currentBeat;
    private float speedMultiplier = 1f;
    private int gestureMs;
    private int modifierLeadMs;
    private boolean playing;
    private boolean paused;
    private boolean loop;
    private String lastForegroundPackage = "";

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { showBar(); }
    };

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!playing || paused) return;
            long now = SystemClock.uptimeMillis();
            if (now < countdownUntil) {
                long left = (countdownUntil - now + 999) / 1000;
                updateBar("倒计时 " + left);
                handler.postDelayed(this, 40);
                return;
            }
            if (lastTickAt < countdownUntil) lastTickAt = countdownUntil;
            long elapsed = Math.max(0, now - lastTickAt);
            lastTickAt = now;
            currentBeat += elapsed * speedMultiplier / baseBeatMs;
            while (eventIndex < events.size() && events.get(eventIndex).beat <= currentBeat) {
                clickEvent(events.get(eventIndex));
                eventIndex++;
            }
            if (currentBeat >= totalBeats) {
                if (loop) {
                    eventIndex = 0;
                    currentBeat %= totalBeats;
                    updateBar("循环演奏");
                } else {
                    stopPlayback("演奏完成");
                    return;
                }
            } else {
                int pct = totalBeats <= 0 ? 0 : Math.min(99, Math.round(currentBeat / totalBeats * 100));
                updateBar(String.format(java.util.Locale.ROOT, "演奏中 %d%% · %.2f×", pct, speedMultiplier));
            }
            handler.postDelayed(this, 8);
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        prefs = AppPrefs.get(this);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        IntentFilter filter = new IntentFilter(ACTION_SHOW);
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver, filter);
        showBar();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null) return;
        lastForegroundPackage = event.getPackageName().toString();
        if (calibration != null && "com.dfm.reedsim".equals(lastForegroundPackage)) {
            calibration.applySimulatorPreset();
        }
    }
    @Override public void onInterrupt() { pausePlayback(); }

    private void showBar() {
        if (wm == null || calibration != null) return;
        if (bar != null) {
            bar.setSongTitle(SongStore.selected(this).title);
            return;
        }
        bar = new FloatBarView(this, new FloatBarView.Listener() {
            @Override public void onMove(float dx, float dy) {
                barParams.x += Math.round(dx); barParams.y += Math.round(dy);
                try { wm.updateViewLayout(bar, barParams); } catch (Exception ignored) {}
            }
            @Override public void onExpandedChanged(boolean expanded) {
                int width = Ui.dp(HarmonicaAccessibilityService.this, expanded ? 331 : 58);
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                barParams.width = width;
                barParams.height = Ui.dp(HarmonicaAccessibilityService.this, expanded ? 112 : 64);
                barParams.x = Math.max(0, Math.min(barParams.x, screenWidth - width));
                try { wm.updateViewLayout(bar, barParams); } catch (Exception ignored) {}
            }
            @Override public void onPrevious() { changeSong(-1); }
            @Override public void onPlayPause() {
                if (!playing) startPlayback(); else if (paused) resumePlayback(); else pausePlayback();
            }
            @Override public void onNext() { changeSong(1); }
            @Override public void onStop() { stopPlayback("已停止"); }
            @Override public void onCalibrate() { showCalibration(); }
            @Override public void onSpeedChanged(float speed) { setPlaybackSpeed(speed); }
        });
        bar.setSongTitle(SongStore.selected(this).title);
        setPlaybackSpeed(prefs.getInt(AppPrefs.SPEED_PERCENT, 100) / 100f);
        barParams = new WindowManager.LayoutParams(
                Ui.dp(this, 58), Ui.dp(this, 64),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        barParams.gravity = Gravity.TOP | Gravity.LEFT;
        barParams.x = Math.max(0, getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 72));
        barParams.y = Ui.dp(this, 180);
        try { wm.addView(bar, barParams); } catch (Exception e) { bar = null; }
    }

    private void hideBar() {
        if (bar != null) {
            try { wm.removeView(bar); } catch (Exception ignored) {}
            bar = null;
        }
    }

    private void showCalibration() {
        stopPlayback("已停止");
        hideBar();
        float[] xs = new float[11], ys = new float[11];
        for (int i = 0; i < 8; i++) {
            xs[i] = prefs.getFloat(AppPrefs.xKey(i), 0);
            ys[i] = prefs.getFloat(AppPrefs.yKey(i), 0);
        }
        for (int i = 0; i < 3; i++) {
            xs[i+8] = prefs.getFloat(AppPrefs.modXKey(i), 0);
            ys[i+8] = prefs.getFloat(AppPrefs.modYKey(i), 0);
        }
        int savedWidth = prefs.getInt(AppPrefs.CALIBRATION_WIDTH, 0);
        int savedHeight = prefs.getInt(AppPrefs.CALIBRATION_HEIGHT, 0);
        calibration = new CalibrationView(this, xs, ys, savedWidth, savedHeight, new CalibrationView.Listener() {
            @Override public void onSave(float[] outX, float[] outY) {
                SharedPreferences.Editor e = prefs.edit()
                        .putBoolean(AppPrefs.CALIBRATED, true)
                        .putInt(AppPrefs.CALIBRATION_WIDTH, getResources().getDisplayMetrics().widthPixels)
                        .putInt(AppPrefs.CALIBRATION_HEIGHT, getResources().getDisplayMetrics().heightPixels);
                for (int i = 0; i < 8; i++) e.putFloat(AppPrefs.xKey(i), outX[i]).putFloat(AppPrefs.yKey(i), outY[i]);
                for (int i = 0; i < 3; i++) e.putFloat(AppPrefs.modXKey(i), outX[i+8]).putFloat(AppPrefs.modYKey(i), outY[i+8]);
                e.apply();
                closeCalibration();
                Toast.makeText(HarmonicaAccessibilityService.this,
                        "已保存绝对圆心，开始依次试按 1–8", Toast.LENGTH_SHORT).show();
                handler.postDelayed(HarmonicaAccessibilityService.this::runPointTest, 500);
            }
            @Override public void onCancel() { closeCalibration(); }
        });
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.LEFT;
        try { wm.addView(calibration, p); }
        catch (Exception e) { calibration = null; showBar(); }
        if (calibration != null && "com.dfm.reedsim".equals(lastForegroundPackage)) {
            calibration.applySimulatorPreset();
        }
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        handler.postDelayed(() -> {
            if (bar == null || barParams == null) return;
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            barParams.x = Math.max(0, screenWidth - barParams.width - Ui.dp(this, 12));
            barParams.y = Math.max(Ui.dp(this, 72), (screenHeight - barParams.height) / 2);
            try { wm.updateViewLayout(bar, barParams); } catch (Exception ignored) {}
        }, 120);
    }

    private void closeCalibration() {
        if (calibration != null) {
            try { wm.removeView(calibration); } catch (Exception ignored) {}
            calibration = null;
        }
        showBar();
    }

    private void startPlayback() {
        if (!prefs.getBoolean(AppPrefs.CALIBRATED, false)) {
            Toast.makeText(this, "请先在游戏口风琴界面标定 8 个琴键", Toast.LENGTH_LONG).show();
            showCalibration();
            return;
        }
        ScoreParser.Result score = ScoreParser.parse(prefs.getString(AppPrefs.SCORE, AppPrefs.DEFAULT_SCORE));
        if (score.events.isEmpty()) {
            Toast.makeText(this, "曲谱为空，请回到风簧编辑", Toast.LENGTH_SHORT).show();
            return;
        }
        int bpm = Math.max(20, Math.min(480, prefs.getInt(AppPrefs.BPM, 96)));
        int timing = prefs.getInt(AppPrefs.TIMING, 1);
        int countdown = prefs.getInt(AppPrefs.COUNTDOWN, 1);
        gestureMs = timing == 0 ? 45 : timing == 2 ? 8 : 32;
        modifierLeadMs = timing == 0 ? 70 : timing == 2 ? 8 : 40;
        long countdownMs = countdown == 2 ? 5000 : countdown == 1 ? 3000 : 0;
        baseBeatMs = 60000f / bpm;
        loop = prefs.getBoolean(AppPrefs.LOOP, false);
        events = score.events;
        if (bar != null) bar.setSongTitle(prefs.getString(AppPrefs.SONG_TITLE, "我的曲谱"));
        totalBeats = score.totalBeats;
        eventIndex = 0;
        currentBeat = 0;
        paused = false;
        playing = true;
        long now = SystemClock.uptimeMillis();
        countdownUntil = now + countdownMs;
        countdownRemaining = 0;
        lastTickAt = countdownUntil;
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        updateBar(countdownMs > 0 ? "准备开始" : "演奏中");
    }

    private void pausePlayback() {
        if (!playing || paused) return;
        paused = true;
        countdownRemaining = Math.max(0, countdownUntil - SystemClock.uptimeMillis());
        handler.removeCallbacks(ticker);
        updateBar("已暂停");
    }

    private void resumePlayback() {
        if (!playing || !paused) return;
        long now = SystemClock.uptimeMillis();
        countdownUntil = now + countdownRemaining;
        lastTickAt = countdownRemaining > 0 ? countdownUntil : now;
        countdownRemaining = 0;
        paused = false;
        updateBar("继续演奏");
        handler.post(ticker);
    }

    private void stopPlayback(String status) {
        playing = false; paused = false;
        handler.removeCallbacks(ticker);
        updateBar(status);
    }

    private void updateBar(String status) {
        if (bar != null) bar.setPlaybackState(playing, paused, status);
    }

    private void setPlaybackSpeed(float speed) {
        speedMultiplier = Math.max(.25f, Math.min(4f, speed));
        prefs.edit().putInt(AppPrefs.SPEED_PERCENT, Math.round(speedMultiplier * 100)).apply();
        if (bar != null) bar.setSpeed(speedMultiplier);
    }

    private void changeSong(int delta) {
        stopPlayback("已切歌");
        Song song = SongStore.selectAdjacent(this, delta);
        if (bar != null) bar.setSongTitle(song.title);
        Toast.makeText(this, "已选择：" + song.title, Toast.LENGTH_SHORT).show();
    }

    private void clickEvent(ScoreParser.Event event) {
        GestureDescription.Builder builder = new GestureDescription.Builder();
        boolean modified = event.octave != 0 || event.sharp;
        int noteStart = modified ? modifierLeadMs : 0;
        if (event.octave < 0) addStroke(builder, 8, 0, modifierLeadMs + gestureMs + 10);
        if (event.sharp) addStroke(builder, 9, 0, modifierLeadMs + gestureMs + 10);
        if (event.octave > 0) addStroke(builder, 10, 0, modifierLeadMs + gestureMs + 10);
        for (int key : event.keys) {
            float x = prefs.getFloat(AppPrefs.xKey(key), 0);
            float y = prefs.getFloat(AppPrefs.yKey(key), 0);
            if (x <= 0 || y <= 0) continue;
            Path path = new Path(); path.moveTo(x, y);
            builder.addStroke(new GestureDescription.StrokeDescription(path, noteStart, gestureMs));
        }
        try { dispatchGesture(builder.build(), null, null); } catch (Exception ignored) {}
    }

    private void addStroke(GestureDescription.Builder builder, int calibrationIndex, int start, int duration) {
        int mod = calibrationIndex - 8;
        float x = prefs.getFloat(AppPrefs.modXKey(mod), 0);
        float y = prefs.getFloat(AppPrefs.modYKey(mod), 0);
        if (x <= 0 || y <= 0) return;
        Path path = new Path(); path.moveTo(x, y);
        builder.addStroke(new GestureDescription.StrokeDescription(path, start, duration));
    }

    private void runPointTest() {
        stopPlayback("点位自检 0/8");
        for (int i = 0; i < 8; i++) {
            final int key = i;
            handler.postDelayed(() -> {
                if (calibration != null) return;
                GestureDescription.Builder builder = new GestureDescription.Builder();
                float x = prefs.getFloat(AppPrefs.xKey(key), 0);
                float y = prefs.getFloat(AppPrefs.yKey(key), 0);
                if (x > 0 && y > 0) {
                    Path path = new Path();
                    path.moveTo(x, y);
                    builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 45));
                    try { dispatchGesture(builder.build(), null, null); } catch (Exception ignored) {}
                }
                updateBar("点位自检 " + (key + 1) + "/8");
            }, i * 520L);
        }
        handler.postDelayed(() -> updateBar("自检完成：应依次亮 1–8"), 8 * 520L);
    }

    @Override public void onDestroy() {
        stopPlayback("服务关闭");
        try { unregisterReceiver(receiver); } catch (Exception ignored) {}
        if (calibration != null) try { wm.removeView(calibration); } catch (Exception ignored) {}
        hideBar();
        super.onDestroy();
    }
}
