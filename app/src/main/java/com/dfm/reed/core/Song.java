package com.dfm.reed.core;

public final class Song {
    public final String id;
    public final String title;
    public final String subtitle;
    public final String score;
    public final int bpm;
    public final boolean custom;

    public Song(String id, String title, String subtitle, String score, int bpm, boolean custom) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.score = score;
        this.bpm = bpm;
        this.custom = custom;
    }
}
