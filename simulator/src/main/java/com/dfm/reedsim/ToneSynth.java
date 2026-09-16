package com.dfm.reedsim;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ToneSynth {
    private static final int SAMPLE_RATE = 24000;
    private final ExecutorService audio = Executors.newCachedThreadPool();

    void play(double frequency) {
        audio.execute(() -> synth(frequency));
    }

    private void synth(double frequency) {
        int durationMs = 260;
        int count = SAMPLE_RATE * durationMs / 1000;
        short[] pcm = new short[count];
        double phase = 0;
        double step = 2 * Math.PI * frequency / SAMPLE_RATE;
        for (int i = 0; i < count; i++) {
            double t = i / (double) count;
            double attack = Math.min(1, i / (SAMPLE_RATE * .018));
            double release = Math.min(1, (count - i) / (SAMPLE_RATE * .09));
            double envelope = attack * release * (1 - .22 * t);
            double reed = Math.sin(phase) + .22 * Math.sin(phase * 2) + .08 * Math.sin(phase * 3);
            pcm[i] = (short) (reed * envelope * 10500);
            phase += step;
        }
        try {
            AudioTrack track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(pcm.length * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();
            track.write(pcm, 0, pcm.length);
            track.play();
            Thread.sleep(durationMs + 30L);
            track.release();
        } catch (Exception ignored) {}
    }

    void release() { audio.shutdownNow(); }
}
