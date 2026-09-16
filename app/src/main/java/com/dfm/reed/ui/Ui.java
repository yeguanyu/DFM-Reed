package com.dfm.reed.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

public final class Ui {
    public static final int INK = Color.rgb(17, 21, 18);
    public static final int PAPER = Color.rgb(242, 240, 232);
    public static final int CARD = Color.rgb(255, 254, 249);
    public static final int MUTED = Color.rgb(103, 108, 101);
    public static final int LINE = Color.rgb(216, 216, 207);
    public static final int ACCENT = Color.rgb(185, 231, 105);
    public static final int GREEN = Color.rgb(44, 113, 71);

    private Ui() {}

    public static int dp(Context c, float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                c.getResources().getDisplayMetrics());
    }

    public static GradientDrawable bg(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static GradientDrawable outlined(int color, int stroke, float radiusDp, Context c) {
        GradientDrawable d = bg(color, radiusDp, c);
        d.setStroke(dp(c, 1), stroke);
        return d;
    }

    public static TextView text(Context c, String value, float sp, int color, boolean bold) {
        TextView v = new TextView(c);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setGravity(android.view.Gravity.CENTER_VERTICAL);
        if (bold) v.setTypeface(Typeface.create("sans", Typeface.BOLD));
        return v;
    }

    public static void pad(View v, Context c, int l, int t, int r, int b) {
        v.setPadding(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
    }
}
