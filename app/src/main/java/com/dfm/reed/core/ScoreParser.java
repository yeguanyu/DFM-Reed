package com.dfm.reed.core;

import java.util.ArrayList;
import java.util.List;

/** Parses a deliberately small, copy-friendly numbered notation. */
public final class ScoreParser {
    private ScoreParser() {}

    public static final class Event {
        public final float beat;
        public final int[] keys;
        public final int octave; // -1 low, 0 middle, +1 high
        public final boolean sharp;

        Event(float beat, int[] keys, int octave, boolean sharp) {
            this.beat = beat;
            this.keys = keys;
            this.octave = octave;
            this.sharp = sharp;
        }
    }

    public static final class Result {
        public final List<Event> events = new ArrayList<>();
        public float totalBeats;
        public int noteCount;
    }

    public static Result parse(String source) {
        Result out = new Result();
        float beat = 0f;
        int octave = 0;
        boolean sharp = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '^' && i + 1 < source.length() && source.charAt(i + 1) == '1') {
                out.events.add(new Event(beat, new int[]{7}, octave, sharp));
                out.noteCount++;
                beat += 1f;
                octave = 0;
                sharp = false;
                i++;
            } else if (c == 'L' || c == 'l' || c == '<') {
                octave = -1;
            } else if (c == 'H' || c == 'h' || c == '>') {
                octave = 1;
            } else if (c == '#') {
                sharp = true;
            } else if (c >= '0' && c <= '8') {
                if (c != '0') {
                    out.events.add(new Event(beat, new int[]{c - '1'}, octave, sharp));
                    out.noteCount++;
                }
                beat += 1f;
                octave = 0;
                sharp = false;
            } else if (c == '-') {
                beat += 1f;
                octave = 0;
                sharp = false;
            } else if (c == '.') {
                beat += .5f;
                octave = 0;
                sharp = false;
            } else if (c == '[') {
                ArrayList<Integer> chord = new ArrayList<>();
                int j = i + 1;
                while (j < source.length() && source.charAt(j) != ']') {
                    char n = source.charAt(j);
                    if (n >= '1' && n <= '8') chord.add(n - '1');
                    j++;
                }
                if (!chord.isEmpty()) {
                    int[] keys = new int[chord.size()];
                    for (int k = 0; k < keys.length; k++) keys[k] = chord.get(k);
                    out.events.add(new Event(beat, keys, octave, sharp));
                    out.noteCount += keys.length;
                    beat += 1f;
                    octave = 0;
                    sharp = false;
                    i = Math.min(j, source.length() - 1);
                }
            }
        }
        out.totalBeats = beat;
        return out;
    }
}
