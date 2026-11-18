package id.simas.mobile;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Build;
import android.text.method.PasswordTransformationMethod;
import android.view.*;
import android.widget.*;
import java.math.BigDecimal;
import java.util.*;
import org.json.*;

/** Android framework UI; no browser or changes to authentication/data access. */
final class ModernUi {
  static final int EMERALD = 0xff116b52,
      DARK = 0xff123c32,
      INK = 0xff172f29,
      MUTED = 0xff72847e,
      BG = 0xfff5f7f6,
      GOLD = 0xffe2bd71;

  static LinearLayout.LayoutParams lp(int w, int h) {
    return new LinearLayout.LayoutParams(w, h);
  }

  static void rounded(MainActivity a, View v, int color, int radius) {
    v.setBackground(a.bg(color, radius));
    v.setClipToOutline(true);
  }

  static void clickable(MainActivity a, View v, Runnable action) {
    v.setClickable(true);
    v.setFocusable(true);
    v.setForeground(
        new RippleDrawable(ColorStateList.valueOf(0x18116b52), null, a.bg(Color.WHITE, 18)));
    v.setOnClickListener(
        x -> {
          if (!a.busy) action.run();
        });
  }

  static TextView label(MainActivity a, String s, int size, int color, boolean bold) {
    TextView v = a.text(s, size, color, bold);
    v.setFontFeatureSettings("kern");
    return v;
  }

  static ImageView logo(MainActivity a, int size) {
    ImageView v = new ImageView(a);
    v.setImageResource(R.drawable.brand_logo);
    v.setScaleType(ImageView.ScaleType.CENTER_CROP);
    rounded(a, v, DARK, 12);
    v.setContentDescription("Logo SIMASJID");
    v.setLayoutParams(lp(a.dp(size), a.dp(size)));
    return v;
  }

  static LinearLayout loginBody(MainActivity a) {
    a.getWindow().setStatusBarColor(DARK);
    a.getWindow().setNavigationBarColor(BG);
    a.getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            Build.VERSION.SDK_INT >= 26 ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
    ScrollView scroll = new ScrollView(a);
    scroll.setFillViewport(true);
    scroll.setClipToPadding(false);
    scroll.setVerticalScrollBarEnabled(false);
    a.root.addView(scroll, lp(-1, -1));
    LinearLayout page = a.col();
    page.setBackgroundColor(BG);
    scroll.addView(page);
    LinearLayout hero = a.col();
    hero.setPadding(a.dp(24), a.dp(20), a.dp(24), a.dp(44));
    hero.setBackground(new Hero());
    page.addView(hero, lp(-1, -2));
    LinearLayout brand = a.row();
    brand.addView(logo(a, 42));
    LinearLayout brandText = a.col();
    LinearLayout.LayoutParams bp = lp(0, -2);
    bp.weight = 1;
    bp.leftMargin = a.dp(12);
    brand.addView(brandText, bp);
    brandText.addView(label(a, "SIMASJID", 18, Color.WHITE, true));
    brandText.addView(label(a, "Ruang kelola masjid Anda", 11, 0xffbfd8ce, false));
    hero.addView(brand);
    a.gap(hero, 24);
    TextView headline = label(a, "Amanah terjaga.\nJamaah berdaya.", 25, Color.WHITE, true);
    headline.setLineSpacing(a.dp(3), 1);
    hero.addView(headline);
    a.gap(hero, 8);
    hero.addView(label(a, "Lebih tertata, lebih dekat dengan jamaah.", 12, 0xffc5ded3, false));
    LinearLayout form = a.col();
    a.pad(form, 22);
    rounded(a, form, Color.WHITE, 26);
    form.setElevation(a.dp(3));
    LinearLayout.LayoutParams fp = lp(-1, -2);
    fp.setMargins(a.dp(18), -a.dp(20), a.dp(18), a.dp(18));
    page.addView(form, fp);
    form.addView(label(a, "Selamat datang kembali", 22, INK, true));
    a.gap(form, 5);
    form.addView(label(a, "Masuk dengan akun pengurus masjid Anda.", 12, MUTED, false));
    a.gap(form, 22);
    TextView footer = label(a, "SIMASJID 2.4.1  ·  Sistem Informasi Masjid", 10, MUTED, false);
    footer.setGravity(Gravity.CENTER);
    footer.setPadding(a.dp(20), 0, a.dp(20), a.dp(20));
    page.addView(footer, lp(-1, -2));
    return form;
  }

  static EditText loginField(MainActivity a, LinearLayout parent, String title, boolean password) {
    parent.addView(label(a, title, 12, INK, true));
    a.gap(parent, 7);
    LinearLayout field = a.row();
    field.setPadding(a.dp(13), 0, a.dp(4), 0);
    GradientDrawable shape = a.bg(0xfff7f9f8, 14);
    shape.setStroke(a.dp(1), 0xffe0e8e4);
    field.setBackground(shape);
    Glyph glyph = new Glyph(a, password ? "lock" : "mail", MUTED);
    field.addView(glyph, lp(a.dp(20), a.dp(20)));
    EditText input = new EditText(a);
    input.setTag(password ? "login-password" : "login-email");
    input.setSingleLine(true);
    input.setTextSize(14);
    input.setTextColor(INK);
    input.setHintTextColor(0xff9baaa4);
    input.setHint(password ? "Masukkan kata sandi" : "nama@email.com");
    input.setContentDescription(title);
    input.setBackgroundColor(Color.TRANSPARENT);
    input.setPadding(a.dp(12), a.dp(15), a.dp(4), a.dp(15));
    input.setInputType(password ? 129 : 33);
    input.setImeOptions(
        password
            ? android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            : android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
    input.setMinimumHeight(a.dp(54));
    field.addView(input, new LinearLayout.LayoutParams(0, -2, 1));
    if (password) {
      input.setTransformationMethod(PasswordTransformationMethod.getInstance());
      Glyph eye = new Glyph(a, "eye", MUTED);
      eye.setContentDescription("Tampilkan kata sandi");
      eye.setPadding(a.dp(12), a.dp(12), a.dp(12), a.dp(12));
      field.addView(eye, lp(a.dp(48), a.dp(48)));
      clickable(
          a,
          eye,
          () -> {
            boolean hidden = input.getTransformationMethod() != null;
            input.setTransformationMethod(
                hidden ? null : PasswordTransformationMethod.getInstance());
            input.setSelection(input.length());
            eye.setContentDescription(hidden ? "Sembunyikan kata sandi" : "Tampilkan kata sandi");
          });
    }
    parent.addView(field, lp(-1, -2));
    a.gap(parent, 16);
    return input;
  }

  static void loginHelp(MainActivity a, LinearLayout p) {
    TextView help = label(a, "Butuh bantuan masuk?", 12, EMERALD, true);
    help.setGravity(Gravity.CENTER);
    help.setMinHeight(a.dp(48));
    p.addView(help, lp(-1, -2));
    clickable(
        a,
        help,
        () ->
            new AlertDialog.Builder(a)
                .setTitle("Bantuan akun")
                .setMessage(
                    "Untuk akun baru atau pengaturan ulang kata sandi, hubungi administrator masjid"
                        + " Anda.")
                .setPositiveButton("Mengerti", null)
                .show());
  }

  static void header(MainActivity a) {
    LinearLayout h = a.row();
    h.setPadding(a.dp(20), a.dp(12), a.dp(20), a.dp(10));
    if (a.screen.equals("Beranda")) {
      h.addView(logo(a, 38));
      LinearLayout names = a.col();
      LinearLayout.LayoutParams n = lp(0, -2);
      n.weight = 1;
      n.leftMargin = a.dp(10);
      h.addView(names, n);
      names.addView(label(a, "SIMASJID", 16, INK, true));
      names.addView(label(a, "Pengelolaan masjid", 10, MUTED, false));
    } else {
      LinearLayout names = a.col();
      names.addView(label(a, a.screen, 23, INK, true));
      names.addView(label(a, a.yearLabel(a.year), 10, MUTED, false));
      h.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
    }
    Glyph refresh = new Glyph(a, "refresh", EMERALD);
    refresh.setPadding(a.dp(13), a.dp(13), a.dp(13), a.dp(13));
    refresh.setBackground(a.bg(Color.WHITE, 16));
    refresh.setContentDescription("Muat ulang data");
    clickable(a, refresh, () -> a.reload());
    h.addView(refresh, lp(a.dp(48), a.dp(48)));
    a.root.addView(h, lp(-1, -2));
    a.header = h;
  }

  static void nav(MainActivity a) {
    LinearLayout outer = a.col();
    outer.setPadding(a.dp(12), a.dp(7), a.dp(12), a.dp(7));
    outer.setBackgroundColor(Color.WHITE);
    outer.setElevation(a.dp(10));
    LinearLayout nav = a.row();
    String active =
        Arrays.asList("Beranda", "Menu", "Laporan", "Pengaturan").contains(a.screen)
            ? a.screen
            : "Menu";
    for (String name : new String[] {"Beranda", "Menu", "Laporan", "Pengaturan"}) {
      boolean chosen = active.equals(name);
      LinearLayout item = a.col();
      item.setGravity(Gravity.CENTER);
      item.setMinimumHeight(a.dp(58));
      LinearLayout chip = a.row();
      chip.setGravity(Gravity.CENTER);
      chip.setBackground(a.bg(chosen ? 0xffe1f0e8 : Color.WHITE, 16));
      chip.addView(a.new Icon(name, chosen ? EMERALD : MUTED), lp(a.dp(21), a.dp(21)));
      item.addView(chip, lp(a.dp(56), a.dp(30)));
      TextView t = label(a, name, 10, chosen ? EMERALD : MUTED, chosen);
      t.setGravity(Gravity.CENTER);
      item.addView(t);
      item.setContentDescription(name + (chosen ? ", dipilih" : ""));
      item.setSelected(chosen);
      clickable(a, item, () -> a.navigate(name));
      nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
    }
    outer.addView(nav);
    a.root.addView(outer, lp(-1, -2));
  }

  static void section(MainActivity a, LinearLayout p, String title, String action, Runnable go) {
    LinearLayout r = a.row();
    r.addView(label(a, title, 17, INK, true), new LinearLayout.LayoutParams(0, -2, 1));
    if (action != null) {
      TextView b = label(a, action, 11, EMERALD, true);
      b.setGravity(Gravity.CENTER);
      b.setMinHeight(a.dp(48));
      r.addView(b);
      clickable(a, b, go);
    }
    p.addView(r, lp(-1, -2));
  }

  static void year(MainActivity a) {
    TreeSet<Integer> years = new TreeSet<>(Collections.reverseOrder());
    years.add(a.year);
    years.add(Calendar.getInstance().get(Calendar.YEAR));
    for (JSONArray arr : a.data.values())
      for (int i = 0; i < arr.length(); i++) {
        JSONObject j = arr.optJSONObject(i);
        if (j == null) continue;
        if (j.has("year")) years.add(j.optInt("year"));
        String d = a.str(j, "occurred_on");
        if (d.length() >= 4)
          try {
            years.add(Integer.parseInt(d.substring(0, 4)));
          } catch (Exception ignored) {
          }
      }
    ArrayList<Integer> values = new ArrayList<>(years);
    String[] labels = new String[values.size()];
    for (int i = 0; i < labels.length; i++) labels[i] = a.yearLabel(values.get(i));
    new AlertDialog.Builder(a)
        .setTitle("Tahun kegiatan")
        .setItems(
            labels,
            (d, n) -> {
              a.year = values.get(n);
              a.render();
            })
        .show();
  }

  static TextView metric(MainActivity a, String value, int color) {
    TextView t = label(a, value, 24, color, true);
    t.setSingleLine(true);
    if (Build.VERSION.SDK_INT >= 26)
      t.setAutoSizeTextTypeUniformWithConfiguration(
          12, 24, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
    return t;
  }

  static void home(MainActivity a) {
    LinearLayout p = a.body;
    a.gap(p, 4);
    p.addView(label(a, "Assalamu’alaikum", 12, MUTED, false));
    a.gap(p, 3);
    String name = a.str(a.mosque, "name");
    p.addView(label(a, name.isEmpty() ? "Selamat datang" : name, 23, INK, true));
    a.gap(p, 7);
    TextView period = label(a, a.yearLabel(a.year) + "  ▾", 11, EMERALD, true);
    period.setPadding(a.dp(12), 0, a.dp(12), 0);
    period.setGravity(Gravity.CENTER_VERTICAL);
    period.setMinHeight(a.dp(48));
    period.setBackground(a.bg(0xffe7efe9, 14));
    clickable(a, period, () -> year(a));
    p.addView(period, lp(-2, -2));
    a.gap(p, 16);
    if (!a.errors.isEmpty()) {
      TextView warning =
          label(
              a,
              "Sebagian data belum dimuat. Ketuk muat ulang untuk mencoba lagi.",
              12,
              0xff986420,
              false);
      warning.setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10));
      warning.setBackground(a.bg(0xfffff1d9, 12));
      p.addView(warning);
      a.gap(p, 12);
    }
    if (a.financeAccess() && a.ready(p, "finance_transactions")) {
      BigDecimal in = a.sum("finance_transactions", "amount", "kind", "income"),
          out = a.sum("finance_transactions", "amount", "kind", "expense");
      LinearLayout hero = a.col();
      a.pad(hero, 20);
      hero.setBackground(new Hero());
      hero.setClipToOutline(true);
      hero.setElevation(a.dp(2));
      p.addView(hero, lp(-1, -2));
      LinearLayout top = a.row();
      top.addView(
          label(a, "SALDO KAS MASJID", 10, 0xffc3ded1, true),
          new LinearLayout.LayoutParams(0, -2, 1));
      top.addView(a.new Icon("Keuangan", GOLD), lp(a.dp(23), a.dp(23)));
      hero.addView(top);
      a.gap(hero, 10);
      TextView amount = metric(a, a.money(in.subtract(out)), Color.WHITE);
      amount.setTextSize(30);
      hero.addView(amount, lp(-1, a.dp(42)));
      a.gap(hero, 6);
      hero.addView(label(a, "Ringkasan transaksi tahun " + a.year, 10, 0xffb9d5c8, false));
      a.gap(hero, 18);
      View line = new View(a);
      line.setBackgroundColor(0x30ffffff);
      hero.addView(line, lp(-1, a.dp(1)));
      a.gap(hero, 14);
      LinearLayout flow = a.row();
      flow.addView(
          flow(a, "↙ Penerimaan", a.money(in), 0xffa7e0c3),
          new LinearLayout.LayoutParams(0, -2, 1));
      flow.addView(
          flow(a, "↗ Pengeluaran", a.money(out), 0xffffd7b1),
          new LinearLayout.LayoutParams(0, -2, 1));
      hero.addView(flow);
    }
    a.gap(p, 12);
    section(a, p, "Menu utama", "Semua menu  →", () -> a.navigate("Menu"));
    grid(a, p);
    a.gap(p, 4);
    if (a.financeAccess()) {
      section(a, p, "Zakat & Qurban", "Laporan  →", () -> a.navigate("Laporan"));
      LinearLayout summaries = a.row();
      summaries.addView(
          summary(
              a,
              "Zakat",
              "Beras diterima",
              a.errors.containsKey("zakat_entries")
                  ? "Belum dimuat"
                  : a.qty(a.sum("zakat_entries", "rice_kg", "direction", "receipt")) + " kg",
              "Penerimaan tahun " + a.year,
              0xffe6f2ea),
          new LinearLayout.LayoutParams(0, -2, 1));
      Space sp = new Space(a);
      summaries.addView(sp, lp(a.dp(10), 1));
      BigDecimal kg = BigDecimal.ZERO;
      for (JSONObject j : a.rows("qurban_distribution", true))
        if (!a.str(j, "collected_at").isEmpty()) kg = kg.add(a.num(j, "weight_kg"));
      summaries.addView(
          summary(
              a,
              "Qurban",
              "Daging diserahkan",
              a.errors.containsKey("qurban_distribution") ? "Belum dimuat" : a.qty(kg) + " kg",
              "Distribusi tahun " + a.year,
              0xfffaf0dc),
          new LinearLayout.LayoutParams(0, -2, 1));
      p.addView(summaries, lp(-1, -2));
      a.gap(p, 10);
      recent(a, p);
    } else {
      LinearLayout info = a.card(p);
      info.addView(label(a, "Informasi masjid", 16, INK, true));
      a.gap(info, 6);
      info.addView(label(a, "Akses menu mengikuti peran akun Anda.", 12, MUTED, false));
    }
    a.gap(p, 10);
    TextView foot = label(a, "Dikelola dengan amanah, untuk jamaah.", 10, MUTED, false);
    foot.setGravity(Gravity.CENTER);
    p.addView(foot, lp(-1, -2));
    a.gap(p, 8);
  }

  static LinearLayout flow(MainActivity a, String title, String value, int color) {
    LinearLayout c = a.col();
    c.addView(label(a, title, 10, color, false));
    a.gap(c, 5);
    TextView n = metric(a, value, Color.WHITE);
    n.setTextSize(14);
    if (Build.VERSION.SDK_INT >= 26)
      n.setAutoSizeTextTypeUniformWithConfiguration(
          10, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
    c.addView(n, lp(-1, a.dp(25)));
    return c;
  }

  static LinearLayout summary(
      MainActivity a, String title, String sub, String amount, String caption, int color) {
    LinearLayout c = a.col();
    a.pad(c, 14);
    rounded(a, c, Color.WHITE, 20);
    LinearLayout badge = a.row();
    badge.setGravity(Gravity.CENTER);
    badge.setBackground(a.bg(color, 12));
    badge.addView(a.new Icon(title, EMERALD), lp(a.dp(22), a.dp(22)));
    c.addView(badge, lp(a.dp(40), a.dp(40)));
    a.gap(c, 10);
    c.addView(label(a, title, 14, INK, true));
    a.gap(c, 4);
    c.addView(label(a, sub, 10, MUTED, false));
    a.gap(c, 7);
    TextView v = metric(a, amount, INK);
    c.addView(v, lp(-1, a.dp(32)));
    c.addView(label(a, caption, 9, MUTED, false));
    c.setContentDescription(title + ", " + sub + ", " + amount);
    clickable(a, c, () -> a.navigate(title));
    return c;
  }

  static void grid(MainActivity a, LinearLayout p) {
    String[] names = a.menu;
    int[] colors = {
      0xffe0f0e8, 0xffecedff, 0xffffefda, 0xffe3effb, 0xfffce8ec, 0xffe2f1ed, 0xfff4e9ff, 0xffe9edf0
    };
    for (int i = 0; i < names.length; i += 4) {
      LinearLayout row = a.row();
      row.setGravity(Gravity.TOP);
      for (int k = i; k < Math.min(i + 4, names.length); k++) {
        String n = names[k];
        LinearLayout item = a.col();
        item.setGravity(Gravity.CENTER);
        item.setPadding(a.dp(2), a.dp(7), a.dp(2), a.dp(9));
        LinearLayout icon = a.row();
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(a.bg(colors[k], 17));
        icon.addView(a.new Icon(n, k == 2 ? 0xffa47328 : EMERALD), lp(a.dp(25), a.dp(25)));
        item.addView(icon, lp(a.dp(52), a.dp(52)));
        a.gap(item, 7);
        TextView t = label(a, n, 10, INK, true);
        t.setGravity(Gravity.CENTER);
        item.addView(t, lp(-1, -2));
        item.setContentDescription(n);
        clickable(a, item, () -> a.navigate(n));
        row.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
      }
      p.addView(row, lp(-1, -2));
    }
  }

  static void recent(MainActivity a, LinearLayout p) {
    if (a.errors.containsKey("finance_transactions")) return;
    section(a, p, "Transaksi terbaru", "Lihat semua  →", () -> a.navigate("Keuangan"));
    ArrayList<JSONObject> list = a.rows("finance_transactions", true);
    Collections.sort(list, (x, y) -> a.str(y, "occurred_on").compareTo(a.str(x, "occurred_on")));
    LinearLayout c = a.card(p);
    if (list.isEmpty()) {
      c.addView(label(a, "Belum ada transaksi tahun ini", 13, INK, true));
      a.gap(c, 5);
      c.addView(label(a, "Transaksi yang dicatat akan tampil di sini.", 11, MUTED, false));
      return;
    }
    for (int i = 0; i < Math.min(3, list.size()); i++) {
      JSONObject j = list.get(i);
      boolean in = a.str(j, "kind").equals("income");
      LinearLayout r = a.row();
      TextView arrow = label(a, in ? "↙" : "↗", 18, in ? EMERALD : 0xffb77d36, true);
      arrow.setGravity(Gravity.CENTER);
      arrow.setBackground(a.bg(in ? 0xffe8f4ec : 0xfffcf0df, 12));
      r.addView(arrow, lp(a.dp(38), a.dp(38)));
      LinearLayout texts = a.col();
      LinearLayout.LayoutParams ts = new LinearLayout.LayoutParams(0, -2, 1);
      ts.leftMargin = a.dp(10);
      texts.addView(label(a, a.str(j, "description"), 12, INK, true));
      texts.addView(label(a, a.str(j, "occurred_on"), 10, MUTED, false));
      r.addView(texts, ts);
      TextView amount =
          label(
              a,
              (in ? "+ " : "− ") + a.money(a.num(j, "amount")),
              11,
              in ? EMERALD : 0xffa26338,
              true);
      r.addView(amount, lp(-2, -2));
      c.addView(r, lp(-1, -2));
      if (i < Math.min(3, list.size()) - 1) a.gap(c, 14);
    }
  }

  static class Hero extends Drawable {
    final Paint p = new Paint(3);

    public void draw(Canvas c) {
      Rect b = getBounds();
      p.setShader(
          new LinearGradient(
              b.left,
              b.top,
              b.right,
              b.bottom,
              new int[] {0xff123c32, 0xff176a50},
              null,
              Shader.TileMode.CLAMP));
      c.drawRoundRect(new RectF(b), 26, 26, p);
      p.setShader(null);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(1);
      p.setColor(0x16ffffff);
      float w = b.width(), h = b.height();
      for (int i = 0; i < 4; i++) c.drawCircle(w * .96f, h * .12f, 45 + i * 23, p);
      p.setStyle(Paint.Style.FILL);
    }

    public void setAlpha(int a) {}

    public void setColorFilter(ColorFilter f) {}

    public int getOpacity() {
      return PixelFormat.TRANSLUCENT;
    }
  }

  static class Glyph extends View {
    final Paint p = new Paint(3);
    final String kind;
    final int color;

    Glyph(MainActivity a, String k, int color) {
      super(a);
      kind = k;
      this.color = color;
    }

    protected void onDraw(Canvas c) {
      c.save();
      c.translate(getPaddingLeft(), getPaddingTop());
      c.scale(
          (getWidth() - getPaddingLeft() - getPaddingRight()) / 24f,
          (getHeight() - getPaddingTop() - getPaddingBottom()) / 24f);
      p.setColor(color);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(1.7f);
      p.setStrokeCap(Paint.Cap.ROUND);
      p.setStrokeJoin(Paint.Join.ROUND);
      if (kind.equals("mail")) {
        c.drawRoundRect(2, 5, 22, 19, 3, 3, p);
        c.drawLine(3, 7, 12, 13, p);
        c.drawLine(12, 13, 21, 7, p);
      } else if (kind.equals("lock")) {
        c.drawRoundRect(4, 10, 20, 22, 3, 3, p);
        c.drawArc(7, 2, 17, 16, 180, 180, false, p);
        c.drawLine(12, 15, 12, 18, p);
      } else if (kind.equals("eye")) {
        Path path = new Path();
        path.moveTo(2, 12);
        path.quadTo(12, 0, 22, 12);
        path.quadTo(12, 24, 2, 12);
        c.drawPath(path, p);
        c.drawCircle(12, 12, 3, p);
      } else {
        c.drawArc(4, 4, 20, 20, 40, 280, false, p);
        c.drawLine(19, 3, 20, 9, p);
        c.drawLine(20, 9, 14, 8, p);
      }
      c.restore();
    }
  }
}
