package com.dfm.reed;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.dfm.reed.core.AppPrefs;
import com.dfm.reed.core.ScoreParser;
import com.dfm.reed.core.Song;
import com.dfm.reed.core.SongStore;
import com.dfm.reed.service.HarmonicaAccessibilityService;
import com.dfm.reed.ui.Ui;

import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String OFFICIAL_WEBSITE = "https://1nvweb.top/dfm";
    private SharedPreferences prefs;
    private TextView servicePill;
    private TextView heroTitle;
    private TextView heroSubtitle;
    private TextView heroMeta;
    private TextView heroScore;
    private LinearLayout presetList;
    private LinearLayout customList;
    private Song selected;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = AppPrefs.get(this);
        migrateLegacyScore();
        selected = SongStore.selected(this);
        Window w = getWindow();
        w.setStatusBarColor(Ui.INK);
        w.setNavigationBarColor(Ui.INK);
        setContentView(buildScreen());
    }

    private void migrateLegacyScore() {
        if (prefs.contains(AppPrefs.SELECTED_SONG_ID)) return;
        String score = prefs.getString(AppPrefs.SCORE, AppPrefs.DEFAULT_SCORE);
        if (!AppPrefs.DEFAULT_SCORE.equals(score)) {
            Song legacy = new Song("custom_imported", "此前的曲谱", "已从旧版本保留",
                    score, prefs.getInt(AppPrefs.BPM, 96), true);
            SongStore.saveCustom(this, legacy);
            SongStore.select(this, legacy);
        } else SongStore.select(this, SongStore.presets().get(0));
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xfff3f1ea);
        scroll.setClipToPadding(false);
        LinearLayout page = column();
        page.setPadding(dp(20), dp(20), dp(20), dp(42));
        scroll.addView(page, new ViewGroup.LayoutParams(-1, -2));
        page.addView(header());
        page.addView(gap(22));
        page.addView(nowPlaying());
        page.addView(gap(26));
        page.addView(sectionHeader("为你准备", "预设曲目"));
        page.addView(gap(10));
        presetList = column(); page.addView(presetList);
        page.addView(gap(25));
        page.addView(customHeader());
        page.addView(gap(10));
        customList = column(); page.addView(customList);
        page.addView(gap(20));
        page.addView(settingsEntry());
        page.addView(gap(10));
        page.addView(websiteEntry());
        renderLibrary();
        return scroll;
    }

    private View header() {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = Ui.text(this, "♫", 24, Ui.INK, true);
        logo.setGravity(Gravity.CENTER); logo.setBackground(Ui.bg(Ui.ACCENT, 14, this));
        row.addView(logo, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout copy = column(); copy.setPadding(dp(12), 0, 0, 0);
        copy.addView(Ui.text(this, "风簧", 24, Ui.INK, true));
        TextView sub = Ui.text(this, "随身口风琴播放器", 11, Ui.MUTED, false); sub.setLetterSpacing(.08f);
        copy.addView(sub); row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        servicePill = Ui.text(this, "", 11, Ui.INK, true); servicePill.setGravity(Gravity.CENTER);
        servicePill.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        row.addView(servicePill, new LinearLayout.LayoutParams(dp(88), dp(34)));
        return row;
    }

    private View nowPlaying() {
        LinearLayout card = column(); card.setBackground(Ui.bg(Ui.INK, 21, this));
        card.setPadding(dp(18), dp(17), dp(18), dp(16));
        TextView overline = Ui.text(this, "当前选择  ·  NOW PLAYING", 10, 0xff9da69f, true);
        overline.setLetterSpacing(.12f); card.addView(overline);
        LinearLayout main = new LinearLayout(this); main.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams mainLp = new LinearLayout.LayoutParams(-1, -2); mainLp.topMargin = dp(14);
        card.addView(main, mainLp);
        TextView art = Ui.text(this, "♪", 35, Ui.INK, true); art.setGravity(Gravity.CENTER);
        art.setBackground(Ui.bg(Ui.ACCENT, 17, this)); main.addView(art, new LinearLayout.LayoutParams(dp(78), dp(78)));
        LinearLayout info = column(); info.setPadding(dp(15), 0, 0, 0);
        heroTitle = Ui.text(this, "", 21, Color.WHITE, true);
        heroSubtitle = Ui.text(this, "", 12, 0xffb5bdb7, false);
        heroMeta = Ui.text(this, "", 11, Ui.ACCENT, true);
        info.addView(heroTitle); info.addView(heroSubtitle);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(-1, -2); metaLp.topMargin = dp(7);
        info.addView(heroMeta, metaLp); main.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        heroScore = Ui.text(this, "", 15, 0xffd9ded9, false); heroScore.setTypeface(Typeface.MONOSPACE);
        heroScore.setMaxLines(2); heroScore.setSingleLine(false);
        LinearLayout.LayoutParams scoreLp = new LinearLayout.LayoutParams(-1, -2); scoreLp.topMargin = dp(14);
        card.addView(heroScore, scoreLp);
        Button show = button("显示悬浮播放器", Ui.ACCENT, Ui.INK); show.setOnClickListener(v -> showPlayer());
        LinearLayout.LayoutParams showLp = new LinearLayout.LayoutParams(-1, dp(48)); showLp.topMargin = dp(15);
        card.addView(show, showLp); return card;
    }

    private View sectionHeader(String title, String caption) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.BOTTOM);
        row.addView(Ui.text(this, title, 18, Ui.INK, true), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(Ui.text(this, caption, 11, Ui.MUTED, false)); return row;
    }

    private View customHeader() {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(Ui.text(this, "我的曲谱", 18, Ui.INK, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button add = button("＋ 添加", Ui.INK, Color.WHITE); add.setOnClickListener(v -> editSong(null));
        row.addView(add, new LinearLayout.LayoutParams(dp(88), dp(38))); return row;
    }

    private void renderLibrary() {
        selected = SongStore.selected(this);
        ScoreParser.Result parsed = ScoreParser.parse(selected.score);
        heroTitle.setText(selected.title); heroSubtitle.setText(selected.subtitle);
        heroMeta.setText(parsed.noteCount + " 个音符  ·  " + selected.bpm + " BPM  ·  " + duration(parsed, selected.bpm));
        String display = SongStore.displayScore(selected.score).replace('\n', ' ');
        heroScore.setText(display.length() > 76 ? display.substring(0, 76) + "…" : display);
        presetList.removeAllViews();
        List<Song> presets = SongStore.presets();
        for (int i = 0; i < presets.size(); i++) {
            presetList.addView(songRow(presets.get(i), i)); if (i < presets.size()-1) presetList.addView(gap(9));
        }
        customList.removeAllViews();
        List<Song> custom = SongStore.custom(this);
        if (custom.isEmpty()) {
            TextView empty = Ui.text(this, "还没有自定义曲谱\n点右上角“添加”，粘贴数字谱即可。", 13, Ui.MUTED, false);
            empty.setGravity(Gravity.CENTER); empty.setLineSpacing(dp(4), 1f);
            empty.setBackground(Ui.outlined(0xfffaf9f5, Ui.LINE, 16, this));
            customList.addView(empty, new LinearLayout.LayoutParams(-1, dp(92)));
        } else for (int i = 0; i < custom.size(); i++) {
            customList.addView(songRow(custom.get(i), i + 4)); if (i < custom.size()-1) customList.addView(gap(9));
        }
    }

    private View songRow(Song song, int colorIndex) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(11), dp(12), dp(11)); row.setBackground(Ui.outlined(0xfffffefa, Ui.LINE, 16, this));
        int[] colors = {0xffd9efad, 0xffd8e5ee, 0xffeadcc7, 0xffddd8ef, 0xffd6e9df};
        TextView art = Ui.text(this, song.title.substring(0, 1), 18, Ui.INK, true);
        art.setGravity(Gravity.CENTER); art.setBackground(Ui.bg(colors[colorIndex % colors.length], 12, this));
        row.addView(art, new LinearLayout.LayoutParams(dp(52), dp(52)));
        ScoreParser.Result parsed = ScoreParser.parse(song.score);
        LinearLayout copy = column(); copy.setPadding(dp(13), 0, dp(8), 0);
        copy.addView(Ui.text(this, song.title, 15, Ui.INK, true));
        copy.addView(Ui.text(this, song.subtitle + "  ·  " + parsed.noteCount + " 音  ·  " + song.bpm + " BPM", 11, Ui.MUTED, false));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        boolean active = song.id.equals(selected.id);
        TextView state = Ui.text(this, active ? "●" : "›", active ? 17 : 25, active ? 0xff78a32d : Ui.MUTED, true);
        state.setGravity(Gravity.CENTER); row.addView(state, new LinearLayout.LayoutParams(dp(34), dp(42)));
        row.setOnClickListener(v -> { SongStore.select(this, song); renderLibrary(); });
        if (song.custom) row.setOnLongClickListener(v -> { customMenu(song); return true; });
        return row;
    }

    private View settingsEntry() {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(15), dp(13), dp(13), dp(13)); row.setBackground(Ui.outlined(Color.TRANSPARENT, Ui.LINE, 15, this));
        LinearLayout copy = column(); copy.addView(Ui.text(this, "播放与标定设置", 14, Ui.INK, true));
        copy.addView(Ui.text(this, "倒计时、点击时序、循环和无障碍", 11, Ui.MUTED, false));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1)); row.addView(Ui.text(this, "›", 26, Ui.MUTED, false));
        row.setOnClickListener(v -> settingsDialog()); return row;
    }

    private View websiteEntry() {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(15), dp(13), dp(13), dp(13)); row.setBackground(Ui.outlined(0xffe8f5d1, 0xffc5dda0, 15, this));
        TextView icon = Ui.text(this, "↗", 18, Ui.INK, true); icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.bg(Ui.ACCENT, 11, this)); row.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout copy = column(); copy.setPadding(dp(12), 0, 0, 0);
        copy.addView(Ui.text(this, "官方网站", 14, Ui.INK, true));
        copy.addView(Ui.text(this, "1nvweb.top/dfm  ·  下载与更新", 11, Ui.MUTED, false));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1)); row.addView(Ui.text(this, "›", 26, Ui.MUTED, false));
        row.setContentDescription("打开风簧官方网站 " + OFFICIAL_WEBSITE);
        row.setOnClickListener(v -> openWebsite()); return row;
    }

    private void openWebsite() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(OFFICIAL_WEBSITE)));
        } catch (Exception e) {
            Toast.makeText(this, "无法打开浏览器：" + OFFICIAL_WEBSITE, Toast.LENGTH_LONG).show();
        }
    }

    private void showPlayer() {
        SongStore.select(this, selected);
        if (!isServiceEnabled()) {
            Toast.makeText(this, "请先开启“风簧演奏服务”", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); return;
        }
        sendBroadcast(new Intent(HarmonicaAccessibilityService.ACTION_SHOW).setPackage(getPackageName()));
        Toast.makeText(this, "已显示悬浮播放器", Toast.LENGTH_SHORT).show();
    }

    private void editSong(Song existing) {
        LinearLayout form = column(); form.setPadding(dp(20), dp(4), dp(20), 0);
        EditText title = field("曲名", existing == null ? "" : existing.title);
        EditText bpm = field("BPM（20–480）", String.valueOf(existing == null ? 96 : existing.bpm));
        bpm.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText score = field("数字谱：1–7，^1 是上方带点的 1", existing == null ? "" : existing.score);
        score.setGravity(Gravity.TOP | Gravity.LEFT); score.setMinHeight(dp(150)); score.setTypeface(Typeface.MONOSPACE);
        form.addView(title); form.addView(gap(10)); form.addView(bpm); form.addView(gap(10)); form.addView(score);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(existing == null ? "添加自定义曲谱" : "编辑曲谱")
                .setView(form).setNegativeButton("取消", null).setPositiveButton("保存", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = title.getText().toString().trim(); String notation = score.getText().toString().trim();
            ScoreParser.Result result = ScoreParser.parse(notation);
            if (name.isEmpty() || result.events.isEmpty()) { Toast.makeText(this, "请填写曲名和有效数字谱", Toast.LENGTH_SHORT).show(); return; }
            int tempo; try { tempo = Math.max(20, Math.min(480, Integer.parseInt(bpm.getText().toString()))); }
            catch (Exception e) { tempo = 96; }
            String id = existing == null ? "custom_" + System.currentTimeMillis() : existing.id;
            Song song = new Song(id, name, "我的曲谱", notation, tempo, true);
            SongStore.saveCustom(this, song); SongStore.select(this, song); dialog.dismiss(); renderLibrary();
        })); dialog.show();
    }

    private void customMenu(Song song) {
        new AlertDialog.Builder(this).setTitle(song.title).setItems(new String[]{"编辑曲谱", "删除"}, (d, which) -> {
            if (which == 0) editSong(song);
            else new AlertDialog.Builder(this).setMessage("删除“" + song.title + "”？").setNegativeButton("取消", null)
                    .setPositiveButton("删除", (x, y) -> { SongStore.deleteCustom(this, song.id);
                        if (selected.id.equals(song.id)) SongStore.select(this, SongStore.presets().get(0)); renderLibrary(); }).show();
        }).show();
    }

    private void settingsDialog() {
        LinearLayout form = column(); form.setPadding(dp(20), 0, dp(20), 0);
        Spinner timing = spinner(new String[]{"稳健：70ms 提前 / 45ms 点击", "标准：40ms / 32ms", "极速：8ms 提前 / 8ms 点击"});
        timing.setSelection(prefs.getInt(AppPrefs.TIMING, 1));
        Spinner countdown = spinner(new String[]{"无倒计时", "3 秒倒计时", "5 秒倒计时"});
        countdown.setSelection(prefs.getInt(AppPrefs.COUNTDOWN, 1));
        Switch loop = new Switch(this); loop.setText("循环演奏"); loop.setChecked(prefs.getBoolean(AppPrefs.LOOP, false));
        form.addView(labeled("点击时序", timing)); form.addView(gap(8)); form.addView(labeled("开始等待", countdown)); form.addView(gap(8)); form.addView(loop);
        new AlertDialog.Builder(this).setTitle("播放设置").setView(form)
                .setNeutralButton("无障碍设置", (d, w) -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                .setNegativeButton("取消", null).setPositiveButton("保存", (d, w) -> prefs.edit()
                        .putInt(AppPrefs.TIMING, timing.getSelectedItemPosition()).putInt(AppPrefs.COUNTDOWN, countdown.getSelectedItemPosition())
                        .putBoolean(AppPrefs.LOOP, loop.isChecked()).apply()).show();
    }

    private View labeled(String label, View view) { LinearLayout box = column(); box.addView(Ui.text(this, label, 12, Ui.MUTED, true)); box.addView(view); return box; }
    private Spinner spinner(String[] values) { Spinner s = new Spinner(this); s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values)); return s; }
    private EditText field(String hint, String value) { EditText e = new EditText(this); e.setHint(hint); e.setText(value); e.setTextColor(Ui.INK); e.setTextSize(14); e.setBackground(Ui.outlined(0xfff6f5f0, Ui.LINE, 11, this)); e.setPadding(dp(12), dp(10), dp(12), dp(10)); return e; }
    private String duration(ScoreParser.Result r, int bpm) { long sec = Math.round(r.totalBeats * 60f / bpm); return String.format(Locale.CHINA, "%d:%02d", sec/60, sec%60); }

    @Override protected void onResume() { super.onResume(); updateServicePill(); }
    private void updateServicePill() { if (servicePill == null) return; boolean on = isServiceEnabled(); servicePill.setText(on ? "● 服务已开启" : "○ 开启服务"); servicePill.setTextColor(on ? 0xff315c2e : Ui.MUTED); servicePill.setBackground(Ui.outlined(on ? 0xffe3f1c9 : Color.TRANSPARENT, Ui.LINE, 18, this)); }
    private boolean isServiceEnabled() { android.view.accessibility.AccessibilityManager manager = (android.view.accessibility.AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE); List<AccessibilityServiceInfo> list = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK); for (AccessibilityServiceInfo info : list) if (info.getResolveInfo() != null && getPackageName().equals(info.getResolveInfo().serviceInfo.packageName)) return true; return false; }
    private Button button(String text, int bg, int fg) { Button b = new Button(this); b.setText(text); b.setTextSize(13); b.setTextColor(fg); b.setAllCaps(false); b.setTypeface(Typeface.DEFAULT_BOLD); b.setBackground(Ui.bg(bg, 12, this)); b.setStateListAnimator(null); return b; }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private View gap(int value) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(value))); return v; }
    private int dp(float value) { return Ui.dp(this, value); }
}
