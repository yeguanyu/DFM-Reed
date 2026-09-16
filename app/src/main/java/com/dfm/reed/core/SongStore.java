package com.dfm.reed.core;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SongStore {
    private static final List<Song> PRESETS;
    static {
        ArrayList<Song> songs = new ArrayList<>();
        songs.add(new Song("preset_star", "小星星", "经典练习曲",
                "1 1 5 5 | 6 6 5 -\n4 4 3 3 | 2 2 1 -\n5 5 4 4 | 3 3 2 -\n5 5 4 4 | 3 3 2 -\n1 1 5 5 | 6 6 5 -\n4 4 3 3 | 2 2 1 -", 96, false));
        songs.add(new Song("preset_ode", "欢乐颂", "贝多芬 · 简易版",
                "3 3 4 5 | 5 4 3 2 | 1 1 2 3 | 3 2 2 -\n3 3 4 5 | 5 4 3 2 | 1 1 2 3 | 2 1 1 -", 108, false));
        songs.add(new Song("preset_tiger", "两只老虎", "童谣",
                "1 2 3 1 | 1 2 3 1 | 3 4 5 - | 3 4 5 -\n5 6 5 4 3 1 | 5 6 5 4 3 1 | 1 5 1 - | 1 5 1 -", 116, false));
        songs.add(new Song("preset_birthday", "生日快乐", "祝福旋律",
                "5 5 6 5 ^1 7 | 5 5 6 5 H2 ^1 | 5 5 H5 H3 ^1 7 6 | H4 H4 H3 ^1 H2 ^1", 92, false));
        PRESETS = Collections.unmodifiableList(songs);
    }

    private SongStore() {}

    public static List<Song> presets() { return PRESETS; }

    public static List<Song> all(Context context) {
        ArrayList<Song> result = new ArrayList<>(PRESETS);
        result.addAll(custom(context));
        return result;
    }

    public static List<Song> custom(Context context) {
        ArrayList<Song> result = new ArrayList<>();
        String raw = AppPrefs.get(context).getString(AppPrefs.CUSTOM_SONGS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                result.add(new Song(o.getString("id"), o.getString("title"),
                        o.optString("subtitle", "自定义曲谱"), o.getString("score"),
                        o.optInt("bpm", 96), true));
            }
        } catch (Exception ignored) {}
        return result;
    }

    public static void saveCustom(Context context, Song song) {
        List<Song> songs = custom(context);
        ArrayList<Song> next = new ArrayList<>();
        for (Song old : songs) if (!old.id.equals(song.id)) next.add(old);
        next.add(0, song);
        writeCustom(context, next);
    }

    public static void deleteCustom(Context context, String id) {
        ArrayList<Song> next = new ArrayList<>();
        for (Song song : custom(context)) if (!song.id.equals(id)) next.add(song);
        writeCustom(context, next);
    }

    private static void writeCustom(Context context, List<Song> songs) {
        JSONArray array = new JSONArray();
        try {
            for (Song song : songs) {
                JSONObject o = new JSONObject();
                o.put("id", song.id); o.put("title", song.title); o.put("subtitle", song.subtitle);
                o.put("score", song.score); o.put("bpm", song.bpm);
                array.put(o);
            }
        } catch (Exception ignored) {}
        AppPrefs.get(context).edit().putString(AppPrefs.CUSTOM_SONGS, array.toString()).apply();
    }

    public static Song selected(Context context) {
        SharedPreferences p = AppPrefs.get(context);
        String id = p.getString(AppPrefs.SELECTED_SONG_ID, PRESETS.get(0).id);
        for (Song song : all(context)) if (song.id.equals(id)) return song;
        return PRESETS.get(0);
    }

    public static void select(Context context, Song song) {
        AppPrefs.get(context).edit()
                .putString(AppPrefs.SELECTED_SONG_ID, song.id)
                .putString(AppPrefs.SONG_TITLE, song.title)
                .putString(AppPrefs.SCORE, song.score)
                .putInt(AppPrefs.BPM, song.bpm)
                .apply();
    }

    public static Song selectAdjacent(Context context, int delta) {
        List<Song> songs = all(context);
        Song current = selected(context);
        int index = 0;
        for (int i = 0; i < songs.size(); i++) if (songs.get(i).id.equals(current.id)) index = i;
        index = (index + delta + songs.size()) % songs.size();
        Song next = songs.get(index);
        select(context, next);
        return next;
    }

    public static String displayScore(String score) {
        return score.replace("^1", "1̇").replace("8", "1̇");
    }
}
