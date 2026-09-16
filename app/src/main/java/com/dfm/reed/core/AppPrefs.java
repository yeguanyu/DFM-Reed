package com.dfm.reed.core;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppPrefs {
    private AppPrefs() {}

    public static final String FILE = "reed_settings";
    public static final String SCORE = "score";
    public static final String BPM = "bpm";
    public static final String LOOP = "loop";
    public static final String COUNTDOWN = "countdown";
    public static final String TIMING = "timing";
    public static final String CALIBRATED = "calibrated";
    public static final String CALIBRATION_WIDTH = "calibration_width";
    public static final String CALIBRATION_HEIGHT = "calibration_height";
    public static final String SONG_TITLE = "song_title";
    public static final String SELECTED_SONG_ID = "selected_song_id";
    public static final String CUSTOM_SONGS = "custom_songs";
    public static final String SPEED_PERCENT = "speed_percent";

    public static final String DEFAULT_SCORE =
            "1 1 5 5 | 6 6 5 -\n4 4 3 3 | 2 2 1 -";

    public static SharedPreferences get(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String xKey(int index) { return "key_" + index + "_x"; }
    public static String yKey(int index) { return "key_" + index + "_y"; }
    public static String modXKey(int index) { return "mod_" + index + "_x"; }
    public static String modYKey(int index) { return "mod_" + index + "_y"; }
}
