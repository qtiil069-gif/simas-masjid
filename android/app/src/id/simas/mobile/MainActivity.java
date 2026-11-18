package id.simas.mobile;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.os.*;
import android.text.*;
import android.text.method.PasswordTransformationMethod;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class MainActivity extends Activity {
  final int GREEN = 0xff116b52,
      INK = 0xff172f29,
      MUTED = 0xff72847e,
      BG = 0xfff5f7f6,
      GOLD = 0xffaa8542,
      LINE = 0xffdce5d9;
  final ExecutorService worker = Executors.newSingleThreadExecutor();
  LinearLayout root, body, header;
  String token = "",
      refresh = "",
      mosqueId = "",
      role = "",
      email = "",
      screen = "Beranda",
      tab = "";
  JSONObject mosque = new JSONObject();
  Map<String, JSONArray> data = new HashMap<>();
  Map<String, String> errors = new HashMap<>();
  int year = Calendar.getInstance().get(Calendar.YEAR), epoch = 0;
  boolean busy = false;
  PdfPreview pdfPreview;
  byte[] exportBytes;
  String exportName;
  String zakatFilter = "all", distributionFilter = "all", moduleQuery = "";
  final String[] tables = {
    "finance_transactions",
    "finance_categories",
    "zakat_entries",
    "zakat_people",
    "qurban_people",
    "qurban_cash",
    "qurban_distribution",
    "assets",
    "mosque_officers",
    "events"
  };
  final String[] menu = {
    "Keuangan", "Zakat", "Qurban", "Inventaris", "Pengurus", "Kegiatan", "Laporan", "Pengaturan"
  };

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().setStatusBarColor(BG);
    getWindow().setNavigationBarColor(BG);
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    login();
  }

  @Override
  public void onDestroy() {
    if (pdfPreview != null) pdfPreview.dismiss();
    super.onDestroy();
    epoch++;
    worker.shutdownNow();
  }

  @Override
  public void onBackPressed() {
    if (!token.isEmpty() && !screen.equals("Beranda")) {
      screen = "Beranda";
      tab = "";
      render();
    } else if (!token.isEmpty())
      new AlertDialog.Builder(this)
          .setTitle("Tutup SIMASJID?")
          .setMessage("Data yang sudah disimpan tetap tersedia.")
          .setNegativeButton("Kembali", null)
          .setPositiveButton("Tutup", (d, w) -> finish())
          .show();
    else super.onBackPressed();
  }

  int dp(float n) {
    return (int) (n * getResources().getDisplayMetrics().density + .5f);
  }

  GradientDrawable bg(int color, int radius) {
    GradientDrawable b = new GradientDrawable();
    b.setColor(color);
    b.setCornerRadius(dp(radius));
    return b;
  }

  LinearLayout col() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(1);
    return l;
  }

  LinearLayout row() {
    LinearLayout l = new LinearLayout(this);
    l.setGravity(Gravity.CENTER_VERTICAL);
    return l;
  }

  void pad(View v, int n) {
    v.setPadding(dp(n), dp(n), dp(n), dp(n));
  }

  TextView text(String s, int size, int color, boolean bold) {
    TextView v = new TextView(this);
    v.setText(s);
    v.setTextSize(size);
    v.setTextColor(color);
    if (bold) v.setTypeface(null, 1);
    v.setLineSpacing(dp(3), 1);
    return v;
  }

  void gap(LinearLayout p, int n) {
    Space s = new Space(this);
    p.addView(s, new LinearLayout.LayoutParams(1, dp(n)));
  }

  TextView button(String s, boolean primary, Runnable action) {
    TextView b = text(s, 14, primary ? Color.WHITE : GREEN, true);
    b.setGravity(Gravity.CENTER);
    pad(b, 13);
    b.setMinHeight(dp(48));
    b.setBackground(bg(primary ? GREEN : 0xffe5ede2, 16));
    b.setOnClickListener(
        v -> {
          if (!busy) action.run();
        });
    return b;
  }

  LinearLayout card(LinearLayout parent) {
    LinearLayout c = col();
    pad(c, 18);
    c.setBackground(bg(Color.WHITE, 22));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.bottomMargin = dp(12);
    parent.addView(c, lp);
    return c;
  }

  void note(LinearLayout p, String s) {
    TextView t = text(s, 12, MUTED, false);
    p.addView(t);
  }

  void title(LinearLayout p, String s) {
    TextView t = text(s, 19, INK, true);
    p.addView(t);
    gap(p, 10);
  }

  String str(JSONObject j, String k) {
    return j == null || j.isNull(k) ? "" : j.optString(k, "");
  }

  BigDecimal num(JSONObject j, String k) {
    try {
      return new BigDecimal(str(j, k));
    } catch (Exception e) {
      return BigDecimal.ZERO;
    }
  }

  String qty(BigDecimal n) {
    DecimalFormat f =
        new DecimalFormat("#,##0.###", DecimalFormatSymbols.getInstance(new Locale("id", "ID")));
    return f.format(n);
  }

  String money(BigDecimal n) {
    return "Rp " + qty(n);
  }

  String timestamp() {
    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    f.setTimeZone(TimeZone.getTimeZone("UTC"));
    return f.format(new Date());
  }

  String today() {
    return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
  }

  String yearLabel(int y) {
    if (Build.VERSION.SDK_INT >= 24) {
      android.icu.util.IslamicCalendar i = new android.icu.util.IslamicCalendar();
      Calendar c = Calendar.getInstance();
      c.set(y, 0, 1);
      i.setTime(c.getTime());
      int a = i.get(Calendar.YEAR);
      c.set(y, 11, 31);
      i.setTime(c.getTime());
      return y + " M / " + a + "–" + i.get(Calendar.YEAR) + " H";
    }
    int a = (int) Math.floor((y - 622) * 33.0 / 32);
    return y + " M / ±" + a + "–" + (a + 1) + " H";
  }

  void shell() {
    getWindow().setStatusBarColor(BG);
    getWindow().setNavigationBarColor(BG);
    int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
    if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    else getWindow().setNavigationBarColor(GREEN);
    getWindow().getDecorView().setSystemUiVisibility(flags);
    root = col();
    root.setBackgroundColor(BG);
    setContentView(root);
    if (Build.VERSION.SDK_INT >= 35)
      root.setOnApplyWindowInsetsListener(
          (v, in) -> {
            android.graphics.Insets s =
                in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            v.setPadding(s.left, s.top, s.right, s.bottom);
            return in;
          });
  }

  void login() {
    shell();
    screen = "Login";
    LinearLayout p = ModernUi.loginBody(this);
    EditText em = ModernUi.loginField(this, p, "Alamat email", false);
    EditText pw = ModernUi.loginField(this, p, "Kata sandi", true);
    TextView status = text("", 13, 0xffb34536, false);
    p.addView(status);
    TextView enter =
        button(
            "Masuk ke SIMASJID  →",
            true,
            () -> {
              String e = em.getText().toString().trim(), pass = pw.getText().toString();
              if (e.isEmpty() || pass.isEmpty()) {
                status.setText("Isi email dan kata sandi.");
                return;
              }
              status.setText("Memeriksa akun…");
              job(
                  () -> {
                    JSONObject ses =
                        new JSONObject(
                            request(
                                "POST",
                                "/auth/v1/token?grant_type=password",
                                new JSONObject().put("email", e).put("password", pass),
                                false));
                    token = ses.getString("access_token");
                    refresh = ses.optString("refresh_token");
                    email = e;
                    JSONArray memberships =
                        new JSONArray(
                            request(
                                "GET",
                                "/rest/v1/memberships?user_id=eq."
                                    + ses.getJSONObject("user").getString("id")
                                    + "&select=*",
                                null,
                                true));
                    if (memberships.length() == 0) {
                      token = "";
                      refresh = "";
                      throw new Exception(
                          "Akun belum terdaftar sebagai pengurus masjid. Hubungi administrator.");
                    }
                    return memberships;
                  },
                  result -> {
                    pw.setText("");
                    JSONArray ms = (JSONArray) result;
                    if (ms.length() == 1) {
                      selectMosque(ms.optJSONObject(0));
                    } else {
                      String[] names = new String[ms.length()];
                      for (int a = 0; a < names.length; a++)
                        names[a] =
                            "Masjid " + (a + 1) + " · " + ms.optJSONObject(a).optString("role");
                      new AlertDialog.Builder(this)
                          .setTitle("Pilih keanggotaan masjid")
                          .setItems(names, (d, n) -> selectMosque(ms.optJSONObject(n)))
                          .setCancelable(false)
                          .show();
                    }
                  },
                  e1 -> {
                    token = "";
                    refresh = "";
                    status.setText(e1);
                    pw.setText("");
                  });
            });
    enter.setMinimumHeight(dp(54));
    enter.setBackground(bg(GREEN, 14));
    p.addView(enter);
    status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    pw.setOnEditorActionListener((v, action, event) -> {
      if (action == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) { enter.performClick(); return true; }
      return false;
    });
    ModernUi.loginHelp(this, p);
    p.setAlpha(0);
    p.animate().alpha(1).setDuration(350).start();
  }

  void selectMosque(JSONObject m) {
    mosqueId = m.optString("mosque_id");
    role = m.optString("role");
    screen = "Beranda";
    reload();
  }

  interface Task {
    Object run() throws Exception;
  }

  interface Done {
    void done(Object o);
  }

  interface Failed {
    void fail(String s);
  }

  void job(Task work, Done done, Failed fail) {
    if (busy) return;
    busy = true;
    int generation = epoch;
    worker.execute(
        () -> {
          try {
            Object o = work.run();
            runOnUiThread(
                () -> {
                  if (isFinishing() || generation != epoch) return;
                  busy = false;
                  done.done(o);
                });
          } catch (Exception e) {
            String s = e.getMessage() == null ? "Permintaan gagal. Coba kembali." : e.getMessage();
            runOnUiThread(
                () -> {
                  if (isFinishing() || generation != epoch) return;
                  busy = false;
                  fail.fail(s);
                });
          }
        });
  }

  void failure(String s) {
    new AlertDialog.Builder(this)
        .setTitle("Belum berhasil")
        .setMessage(s)
        .setPositiveButton("Mengerti", null)
        .show();
  }

  String request(String method, String path, JSONObject payload, boolean auth) throws Exception {
    return request(method, path, payload, auth, true);
  }

  String request(String method, String path, JSONObject payload, boolean auth, boolean retry)
      throws Exception {
    HttpURLConnection c = (HttpURLConnection) new URL(Config.URL + path).openConnection();
    c.setRequestMethod(method);
    c.setConnectTimeout(15000);
    c.setReadTimeout(30000);
    c.setRequestProperty("apikey", Config.KEY);
    c.setRequestProperty("Content-Type", "application/json");
    if (auth) c.setRequestProperty("Authorization", "Bearer " + token);
    if (method.equals("PATCH") || method.equals("POST") || method.equals("DELETE"))
      c.setRequestProperty("Prefer", "return=representation");
    try {
      if (payload != null) {
        c.setDoOutput(true);
        try (OutputStream out = c.getOutputStream()) {
          out.write(payload.toString().getBytes("UTF-8"));
        }
      }
      int code = c.getResponseCode();
      InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      if (in != null) {
        try (InputStream stream = in) {
          byte[] b = new byte[4096];
          int n;
          while ((n = stream.read(b)) != -1) out.write(b, 0, n);
        }
      }
      String result = out.toString("UTF-8");
      if (code == 401 && auth && retry && !refresh.isEmpty()) {
        JSONObject ses =
            new JSONObject(
                request(
                    "POST",
                    "/auth/v1/token?grant_type=refresh_token",
                    new JSONObject().put("refresh_token", refresh),
                    false,
                    false));
        token = ses.getString("access_token");
        refresh = ses.optString("refresh_token", refresh);
        return request(method, path, payload, auth, false);
      }
      if (code >= 400) {
        String msg = result;
        try {
          JSONObject j = new JSONObject(result);
          msg =
              j.optString("msg", j.optString("message", j.optString("error_description", result)));
        } catch (Exception ignored) {
        }
        if (code == 401) msg = "Sesi atau kata sandi tidak valid. Silakan masuk kembali.";
        throw new Exception(msg.length() > 350 ? msg.substring(0, 350) : msg);
      }
      return result;
    } catch (java.net.SocketTimeoutException e) {
      throw new Exception(
          "Koneksi melewati batas waktu. Periksa daftar sebelum mencoba menyimpan lagi agar tidak"
              + " menggandakan data.");
    } catch (java.net.UnknownHostException e) {
      throw new Exception("Tidak ada koneksi internet. Periksa jaringan lalu coba lagi.");
    } finally {
      c.disconnect();
    }
  }

  boolean financeAccess() {
    return role.equals("admin") || role.equals("bendahara") || role.equals("takmir");
  }

  boolean canWrite(String t) {
    if (role.equals("admin")) return true;
    if (t.equals("events")) return role.equals("takmir") || role.equals("editor");
    if (t.equals("assets")) return role.equals("takmir");
    return role.equals("bendahara")
        && (t.startsWith("finance_") || t.startsWith("zakat_") || t.startsWith("qurban_"));
  }

  void reload() {
    shell();
    LinearLayout loading = col();
    pad(loading, 28);
    root.addView(loading);
    title(loading, "Menghubungkan data…");
    loading.addView(new ProgressBar(this));
    job(
        () -> {
          JSONObject m =
              new JSONArray(
                      request(
                          "GET", "/rest/v1/mosques?id=eq." + mosqueId + "&select=*", null, true))
                  .getJSONObject(0);
          Map<String, JSONArray> next = new HashMap<>();
          Map<String, String> issues = new HashMap<>();
          for (String t : tables) {
            if (!financeAccess()
                && (t.startsWith("finance_") || t.startsWith("zakat_") || t.startsWith("qurban_")))
              continue;
            try {
              JSONArray all = new JSONArray();
              int offset = 0;
              while (true) {
                JSONArray part =
                    new JSONArray(
                        request(
                            "GET",
                            "/rest/v1/"
                                + t
                                + "?mosque_id=eq."
                                + mosqueId
                                + "&select=*&order=id.asc&limit=1000&offset="
                                + offset,
                            null,
                            true));
                for (int i = 0; i < part.length(); i++) all.put(part.get(i));
                if (part.length() < 1000) break;
                offset += 1000;
              }
              next.put(t, all);
            } catch (Exception e) {
              issues.put(t, e.getMessage());
            }
          }
          return new Object[] {m, next, issues};
        },
        o -> {
          Object[] a = (Object[]) o;
          mosque = (JSONObject) a[0];
          data = (Map<String, JSONArray>) a[1];
          errors = (Map<String, String>) a[2];
          render();
        },
        s -> {
          shell();
          LinearLayout c = col();
          pad(c, 25);
          root.addView(c);
          title(c, "Data belum dapat dimuat");
          note(c, s);
          gap(c, 20);
          c.addView(button("Coba lagi", true, () -> reload()));
          gap(c, 10);
          c.addView(button("Kembali ke login", false, () -> logoutLocal()));
        });
  }

  void logoutLocal() {
    epoch++;
    busy = false;
    token = "";
    refresh = "";
    mosqueId = "";
    data.clear();
    errors.clear();
    login();
  }

  void navigate(String s) {
    screen = s;
    tab = "";
    zakatFilter = "all";
    distributionFilter = "all";
    render();
  }

  void render() {
    shell();
    ModernUi.header(this);
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    body = col();
    pad(body, 20);
    scroll.addView(body);
    switch (screen) {
      case "Beranda":
        home();
        break;
      case "Menu":
        modules(body);
        break;
      case "Laporan":
        reports();
        break;
      case "Pengaturan":
        settings();
        break;
      default:
        module();
    }
    ModernUi.nav(this);
    body.setAlpha(0);
    body.setTranslationY(dp(8));
    body.animate().alpha(1).translationY(0).setDuration(200).start();
  }

  JSONArray arr(String t) {
    return data.containsKey(t) ? data.get(t) : new JSONArray();
  }

  ArrayList<JSONObject> rows(String t, boolean byYear) {
    ArrayList<JSONObject> list = new ArrayList<>();
    JSONArray a = arr(t);
    for (int i = 0; i < a.length(); i++) {
      JSONObject j = a.optJSONObject(i);
      String date = str(j, "occurred_on");
      boolean ok = j.has("year") ? j.optInt("year") == year : date.startsWith(year + "-");
      if (!byYear || ok) list.add(j);
    }
    return list;
  }

  BigDecimal sum(String t, String field, String filter, String value) {
    BigDecimal n = BigDecimal.ZERO;
    for (JSONObject j : rows(t, true))
      if (filter == null || str(j, filter).equals(value)) n = n.add(num(j, field));
    return n;
  }

  boolean ready(LinearLayout p, String... tt) {
    boolean ok = true;
    for (String t : tt)
      if (errors.containsKey(t)) {
        LinearLayout c = card(p);
        c.addView(text("Data belum dimuat", 15, 0xffae493a, true));
        note(c, t + ": " + errors.get(t));
        ok = false;
      }
    return ok;
  }

  void yearPicker(LinearLayout p) {
    ArrayList<String> labels = new ArrayList<>();
    ArrayList<String> values = new ArrayList<>();
    TreeSet<Integer> ys = new TreeSet<>(Collections.reverseOrder());
    int now = Calendar.getInstance().get(Calendar.YEAR);
    ys.add(now);
    ys.add(year);
    for (JSONArray a : data.values())
      for (int i = 0; i < a.length(); i++) {
        JSONObject j = a.optJSONObject(i);
        if (j.has("year") && !j.isNull("year")) ys.add(j.optInt("year"));
        String date = str(j, "occurred_on");
        if (date.length() > 4)
          try {
            ys.add(Integer.parseInt(date.substring(0, 4)));
          } catch (Exception ignored) {
          }
      }
    for (int y : ys) {
      labels.add(yearLabel(y));
      values.add("" + y);
    }
    Spinner s =
        spinner(
            p,
            "Tahun kegiatan",
            labels.toArray(new String[0]),
            values.toArray(new String[0]),
            "" + year);
    s.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(AdapterView<?> a) {}

          public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
            int y = Integer.parseInt(values.get(pos));
            if (year != y) {
              year = y;
              render();
            }
          }
        });
  }

  void stat(LinearLayout p, String label, String value) {
    LinearLayout r = row();
    TextView a = text(label, 13, MUTED, false);
    r.addView(a, new LinearLayout.LayoutParams(0, -2, 1));
    TextView b = text(value, 14, INK, true);
    b.setGravity(Gravity.RIGHT);
    r.addView(b, new LinearLayout.LayoutParams(-2, -2));
    p.addView(r);
    gap(p, 10);
  }

  void home() { ModernUi.home(this); }

  void modules(LinearLayout p) { ModernUi.grid(this, p); }

  void zakatSummary(LinearLayout p) {
    if (!ready(p, "zakat_entries")) return;
    LinearLayout c = card(p);
    title(c, "Informasi Zakat");
    stat(c, "Dana diterima", money(sum("zakat_entries", "amount", "direction", "receipt")));
    stat(c, "Dana disalurkan", money(sum("zakat_entries", "amount", "direction", "distribution")));
    stat(c, "Beras diterima", qty(sum("zakat_entries", "rice_kg", "direction", "receipt")) + " kg");
    stat(
        c,
        "Beras disalurkan",
        qty(sum("zakat_entries", "rice_kg", "direction", "distribution")) + " kg");
  }

  void qurbanSummary(LinearLayout p) {
    if (!ready(p, "qurban_people", "qurban_cash", "qurban_distribution")) return;
    LinearLayout c = card(p);
    title(c, "Informasi Qurban");
    stat(
        c,
        "Dana diterima",
        money(
            sum("qurban_people", "nominal", null, null)
                .add(sum("qurban_cash", "amount", "kind", "income"))));
    stat(c, "Pengeluaran", money(sum("qurban_cash", "amount", "kind", "expense")));
    int sapi = 0, patungan = 0, kambing = 0;
    for (JSONObject j : rows("qurban_people", true)) {
      switch (str(j, "qurban_category")) {
        case "sapi_pribadi":
          sapi++;
          break;
        case "sapi_patungan":
          patungan++;
          break;
        case "kambing":
          kambing++;
          break;
      }
    }
    stat(c, "Sapi pribadi", sapi + " ekor");
    stat(c, "Sapi rombongan lengkap", (patungan / 7) + " ekor");
    if (patungan % 7 > 0) note(c, (patungan % 7) + " peserta belum melengkapi rombongan 7 orang.");
    stat(c, "Kambing pribadi", kambing + " ekor");
    stat(c, "Paket tercatat", qty(sum("qurban_distribution", "package_count", null, null)));
    stat(c, "Daging tercatat", qty(sum("qurban_distribution", "weight_kg", null, null)) + " kg");
    BigDecimal delivered = BigDecimal.ZERO;
    for (JSONObject j : rows("qurban_distribution", true))
      if (!str(j, "collected_at").isEmpty()) delivered = delivered.add(num(j, "weight_kg"));
    stat(c, "Daging sudah diserahkan", qty(delivered) + " kg");
  }

  void tabs(String[] choices) {
    if (tab.isEmpty() || !Arrays.asList(choices).contains(tab)) tab = choices[0];
    HorizontalScrollView hs = new HorizontalScrollView(this);
    hs.setHorizontalScrollBarEnabled(false);
    LinearLayout r = row();
    for (String s : choices) {
      TextView b =
          button(
              s,
              s.equals(tab),
              () -> {
                tab = s;
                render();
              });
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
      lp.rightMargin = dp(8);
      r.addView(b, lp);
    }
    hs.addView(r);
    body.addView(hs);
    gap(body, 18);
  }

  String activeTable() {
    switch (screen) {
      case "Keuangan":
        return "finance_transactions";
      case "Zakat":
        return tab.equals("Jamaah") ? "zakat_people" : "zakat_entries";
      case "Qurban":
        return tab.equals("Shohibul")
            ? "qurban_people"
            : tab.equals("Distribusi") ? "qurban_distribution" : "qurban_cash";
      case "Inventaris":
        return "assets";
      case "Pengurus":
        return "mosque_officers";
      case "Kegiatan":
        return "events";
    }
    return "";
  }

  void filterPicker(boolean zakat) {
    Spinner s =
        spinner(
            body,
            "",
            zakat
                ? new String[] {
                  "Semua jenis zakat", "Zakat fitrah", "Zakat mal", "Zakat fitrah & mal"
                }
                : new String[] {"Kedua jenis penerima", "Shohibul qurban", "Penerima qurban"},
            zakat
                ? new String[] {"all", "fitrah", "mal", "fitrah_mal"}
                : new String[] {"all", "shohibul", "penerima"},
            zakat ? zakatFilter : distributionFilter);
    s.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(AdapterView<?> a) {}

          public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
            String val = pick(s);
            if (!val.equals(zakat ? zakatFilter : distributionFilter)) {
              if (zakat) zakatFilter = val;
              else distributionFilter = val;
              render();
            }
          }
        });
  }

  void module() {
    moduleQuery = "";
    if ((screen.equals("Zakat") || screen.equals("Qurban") || screen.equals("Keuangan"))
        && !financeAccess()) {
      note(body, "Akun Anda tidak memiliki hak akses modul ini.");
      return;
    }
    if (screen.equals("Zakat")) {
      tabs(new String[] {"Penerimaan", "Penyaluran", "Jamaah"});
    }
    if (screen.equals("Qurban")) tabs(new String[] {"Shohibul", "Distribusi", "Kas", "Hewan"});
    if (screen.equals("Qurban") && tab.equals("Hewan")) {
      animalList();
      return;
    }
    String t = activeTable();
    boolean yearly =
        t.startsWith("qurban_") || t.equals("zakat_entries") || t.equals("finance_transactions");
    if (yearly) yearPicker(body);
    gap(body, 10);
    if (!ready(body, t)) return;
    if (t.equals("zakat_entries")) filterPicker(true);
    if (t.equals("qurban_distribution")) filterPicker(false);
    if (screen.equals("Zakat") && !tab.equals("Jamaah")) zakatSummary(body);
    if (screen.equals("Qurban")) qurbanSummary(body);
    LinearLayout actions = row();
    if (canWrite(t) && !t.equals("zakat_people")) {
      TextView b = button("＋ Tambah", true, () -> form(t, null));
      actions.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
    }
    TextView ex = button("Ekspor", false, () -> exportOptions(t, searchRows(t, selected(t, yearly), moduleQuery)));
    LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(0, -2, 1);
    ep.leftMargin = dp(8);
    actions.addView(ex, ep);
    body.addView(actions);
    gap(body, 12);
    if (t.equals("zakat_people"))
      note(body, "Muzakki dan mustahik diperbarui otomatis dari penerimaan dan penyaluran.");
    if (t.equals("qurban_cash"))
      note(
          body,
          "Penerimaan shohibul ditambahkan otomatis ke ringkasan. Daftar ini menampilkan transaksi"
              + " kas tambahan. Ekspor Kas mencakup setoran shohibul seperti website.");
    EditText search = input(body, "Cari data", "", false);
    LinearLayout list = col();
    body.addView(list);
    ArrayList<JSONObject> values = selected(t, yearly);
    renderList(list, t, values, "");
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int a, int c, int f) {}

          public void onTextChanged(CharSequence s, int st, int before, int count) {
            moduleQuery = s.toString();
            renderList(list, t, values, moduleQuery);
          }

          public void afterTextChanged(Editable e) {}
        });
  }

  ArrayList<JSONObject> selected(String t, boolean yearly) {
    ArrayList<JSONObject> all = rows(t, yearly);
    if (t.equals("zakat_entries")) {
      String dir = tab.equals("Penerimaan") ? "receipt" : "distribution";
      for (Iterator<JSONObject> it = all.iterator(); it.hasNext(); )
        if (!str(it.next(), "direction").equals(dir)) it.remove();
    }
    for (Iterator<JSONObject> it = all.iterator(); it.hasNext(); ) {
      JSONObject j = it.next();
      if (t.equals("zakat_entries")
          && !zakatFilter.equals("all")
          && !str(j, "zakat_type").equals(zakatFilter)) it.remove();
      else if (t.equals("qurban_distribution")
          && !distributionFilter.equals("all")
          && !str(j, "recipient_kind").equals(distributionFilter)) it.remove();
    }
    Collections.sort(
        all,
        (a, b) -> {
          int d = str(b, "occurred_on").compareTo(str(a, "occurred_on"));
          return d != 0 ? d : rowTitle(t, a).compareToIgnoreCase(rowTitle(t, b));
        });
    return all;
  }

  String rowTitle(String t, JSONObject j) {
    if (t.equals("zakat_entries")) return str(j, "party_name");
    if (t.equals("qurban_distribution"))
      return str(j, "recipient_kind").equals("shohibul") ? str(j, "recipient") : str(j, "village");
    for (String k : new String[] {"full_name", "name", "title", "description"})
      if (!str(j, k).isEmpty()) return str(j, k);
    return "Transaksi";
  }

  String label(String key) {
    switch (key) {
      case "fitrah":
        return "Zakat fitrah";
      case "mal":
        return "Zakat mal";
      case "fitrah_mal":
        return "Zakat fitrah & mal";
      case "sapi_patungan":
        return "Sapi rombongan";
      case "sapi_pribadi":
        return "Sapi pribadi";
      case "kambing":
        return "Kambing pribadi";
      case "income":
      case "receipt":
        return "Penerimaan";
      case "expense":
        return "Pengeluaran";
      case "distribution":
        return "Penyaluran";
      default:
        return key.replace('_', ' ');
    }
  }

  String details(String t, JSONObject j) {
    switch (t) {
      case "zakat_entries":
        return str(j, "occurred_on")
            + " · "
            + label(str(j, "zakat_type"))
            + "\n"
            + str(j, "village")
            + "  "
            + str(j, "rt_rw")
            + "\n"
            + qty(num(j, "rice_kg"))
            + " kg · "
            + money(num(j, "amount"));
      case "zakat_people":
        return label(str(j, "person_type"))
            + " · "
            + str(j, "village")
            + "\nRW/RT "
            + str(j, "rt_rw")
            + " · "
            + str(j, "rt_name");
      case "qurban_people":
        return label(str(j, "qurban_category")) + "\n" + money(num(j, "nominal"));
      case "qurban_distribution":
        return "RW/RT "
            + str(j, "rt_rw")
            + " · "
            + str(j, "rt_name")
            + "\n"
            + qty(num(j, "package_count"))
            + " paket · "
            + qty(num(j, "weight_kg"))
            + " kg\n"
            + (str(j, "collected_at").isEmpty() ? "Belum diserahkan" : "Sudah diserahkan");
      case "finance_transactions":
      case "qurban_cash":
        return str(j, "occurred_on")
            + " · "
            + label(str(j, "kind"))
            + "\n"
            + money(num(j, "amount"));
      case "assets":
        return qty(num(j, "quantity"))
            + " unit · "
            + label(str(j, "condition"))
            + "\n"
            + str(j, "location");
      case "mosque_officers":
        return str(j, "position") + "\n" + str(j, "phone");
      case "events":
        return str(j, "starts_at") + "\n" + str(j, "description");
    }
    return "";
  }

  void renderList(LinearLayout list, String t, ArrayList<JSONObject> values, String query) {
    list.removeAllViews();
    int count = 0;
    BigDecimal kg = BigDecimal.ZERO, rp = BigDecimal.ZERO;
    String q = query.toLowerCase(new Locale("id"));
    for (JSONObject j : values) {
      if (!searchText(t, j).contains(q))
        continue;
      count++;
      kg = kg.add(num(j, t.equals("qurban_distribution") ? "weight_kg" : "rice_kg"));
      rp = rp.add(num(j, t.equals("qurban_people") ? "nominal" : "amount"));
      if (count > 150) continue;
      LinearLayout c = card(list);
      c.addView(text(count + ". " + rowTitle(t, j), 16, INK, true));
      gap(c, 6);
      note(c, details(t, j));
      if (canWrite(t) && !t.equals("zakat_people")) {
        gap(c, 12);
        LinearLayout r = row();
        TextView edit = button("Edit", false, () -> form(t, j));
        r.addView(edit, new LinearLayout.LayoutParams(0, -2, 1));
        TextView del = button("Hapus", false, () -> delete(t, j));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.leftMargin = dp(8);
        r.addView(del, lp);
        c.addView(r);
        if (t.equals("mosque_officers") && role.equals("admin") && str(j, "user_id").isEmpty()) {
          gap(c, 10);
          c.addView(button("Buat akun login", false, () -> account(j)));
        }
      }
    }
    if (count == 0) note(list, "Belum ada data yang cocok.");
    else {
      LinearLayout c = card(list);
      title(c, "Total · " + count + " catatan");
      if (kg.signum() != 0) stat(c, "Berat", qty(kg) + " kg");
      if (rp.signum() != 0) stat(c, "Nominal tercatat", money(rp));
      if (count > 150)
        note(
            c,
            "Menampilkan 150 catatan pertama. Persempit pencarian untuk melihat data lainnya."
                + " Ekspor mencakup seluruh data pilihan.");
    }
  }

  void animalList() {
    yearPicker(body);
    if (!ready(body, "qurban_people")) return;
    ArrayList<JSONObject> people = rows("qurban_people", true);
    Collections.sort(
        people,
        (a, b) -> {
          int d = str(a, "created_at").compareTo(str(b, "created_at"));
          return d != 0 ? d : str(a, "id").compareTo(str(b, "id"));
        });
    ArrayList<JSONObject> group = new ArrayList<>();
    int sapi = 0, kambing = 0, rombongan = 0;
    for (JSONObject j : people) {
      String kind = str(j, "qurban_category");
      if (kind.equals("sapi_patungan")) {
        group.add(j);
        continue;
      }
      LinearLayout c = card(body);
      title(
          c,
          kind.equals("kambing") ? "Kambing pribadi " + (++kambing) : "Sapi pribadi " + (++sapi));
      note(c, str(j, "full_name"));
    }
    for (int i = 0; i < group.size(); i += 7) {
      LinearLayout c = card(body);
      int count = Math.min(7, group.size() - i);
      title(c, "Sapi rombongan " + (++rombongan));
      note(c, count + " / 7 peserta" + (count < 7 ? " · belum lengkap" : " · lengkap"));
      gap(c, 10);
      for (int k = 0; k < count; k++) note(c, (k + 1) + ". " + str(group.get(i + k), "full_name"));
    }
    if (people.size() == 0) note(body, "Belum ada data shohibul untuk tahun ini.");
  }

  void account(JSONObject officer) {
    LinearLayout c = col();
    pad(c, 20);
    EditText emailInput = input(c, "Email login", "", false);
    emailInput.setInputType(33);
    EditText pass = input(c, "Kata sandi baru (12–128 karakter)", "", false);
    pass.setInputType(129);
    Spinner access =
        spinner(
            c,
            "Peran",
            new String[] {"Viewer", "Editor", "Takmir", "Bendahara", "Admin"},
            new String[] {"viewer", "editor", "takmir", "bendahara", "admin"},
            "viewer");
    TextView error = text("", 12, 0xffae493a, false);
    c.addView(error);
    AlertDialog d =
        new AlertDialog.Builder(this)
            .setTitle("Buat akun · " + str(officer, "full_name"))
            .setView(c)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Buat akun", null)
            .create();
    d.setOnShowListener(
        x ->
            d.getButton(-1)
                .setOnClickListener(
                    v -> {
                      if (busy) return;
                      String em = emailInput.getText().toString().trim(),
                          pw = pass.getText().toString();
                      if (!android.util.Patterns.EMAIL_ADDRESS.matcher(em).matches()
                          || pw.length() < 12
                          || pw.length() > 128) {
                        error.setText("Isi email valid dan kata sandi 12–128 karakter.");
                        return;
                      }
                      error.setText("Membuat akun…");
                      d.setCancelable(false);
                      job(
                          () -> {
                            JSONObject j =
                                new JSONObject()
                                    .put("mosque_id", mosqueId)
                                    .put("officer_id", str(officer, "id"))
                                    .put("email", em)
                                    .put("password", pw)
                                    .put("role", pick(access));
                            return accountRequest(j);
                          },
                          o -> {
                            pass.setText("");
                            d.dismiss();
                            Toast.makeText(this, "Akun pengurus berhasil dibuat", 0).show();
                            reload();
                          },
                          msg -> {
                            d.setCancelable(true);
                            error.setText(msg);
                          });
                    }));
    d.show();
  }

  String accountRequest(JSONObject payload) throws Exception {
    HttpURLConnection c =
        (HttpURLConnection)
            new URL("https://simas-masjid.vercel.app/api/create-officer-account").openConnection();
    c.setRequestMethod("POST");
    c.setConnectTimeout(15000);
    c.setReadTimeout(30000);
    c.setRequestProperty("Authorization", "Bearer " + token);
    c.setRequestProperty("Content-Type", "application/json");
    c.setDoOutput(true);
    try {
      try (OutputStream out = c.getOutputStream()) {
        out.write(payload.toString().getBytes("UTF-8"));
      }
      int code = c.getResponseCode();
      InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      if (in != null)
        try (InputStream stream = in) {
          byte[] b = new byte[4096];
          int n;
          while ((n = stream.read(b)) != -1) out.write(b, 0, n);
        }
      String result = out.toString("UTF-8");
      if (code >= 400) {
        String message = "Pembuatan akun gagal. Periksa konfigurasi akun pengurus pada server.";
        try {
          message = new JSONObject(result).optString("error", message);
        } catch (Exception ignored) {
        }
        throw new Exception(message);
      }
      return result;
    } finally {
      c.disconnect();
    }
  }

  Bitmap mosqueLogo() {
    try {
      String v = str(mosque, "logo_data_url");
      if (v.length() > 300000 || !v.startsWith("data:image/")) return null;
      byte[] b =
          android.util.Base64.decode(v.substring(v.indexOf(',') + 1), android.util.Base64.DEFAULT);
      return BitmapFactory.decodeByteArray(b, 0, b.length);
    } catch (Exception e) {
      return null;
    }
  }

  void chooseLogo() {
    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);
    i.setType("image/*");
    startActivityForResult(i, 82);
  }

  void delete(String t, JSONObject j) {
    new AlertDialog.Builder(this)
        .setTitle("Hapus data?")
        .setMessage(
            rowTitle(t, j) + "\nData yang sudah dihapus tidak dapat dipulihkan melalui aplikasi.")
        .setNegativeButton("Batal", null)
        .setPositiveButton(
            "Hapus",
            (d, w) ->
                job(
                    () -> {
                      String s =
                          request(
                              "DELETE",
                              "/rest/v1/"
                                  + t
                                  + "?id=eq."
                                  + str(j, "id")
                                  + "&mosque_id=eq."
                                  + mosqueId,
                              null,
                              true);
                      if (new JSONArray(s).length() == 0)
                        throw new Exception("Data tidak ditemukan atau akses ditolak.");
                      return null;
                    },
                    o -> {
                      Toast.makeText(this, "Data berhasil dihapus", 0).show();
                      reload();
                    },
                    this::failure))
        .show();
  }

  EditText input(LinearLayout p, String label, String value, boolean numeric) {
    p.addView(text(label, 12, MUTED, true));
    gap(p, 6);
    EditText e = new EditText(this);
    e.setSingleLine(true);
    e.setTextSize(15);
    e.setTextColor(INK);
    e.setText(value);
    e.setBackground(bg(Color.WHITE, 12));
    pad(e, 12);
    e.setMinimumHeight(dp(50));
    if (numeric) e.setInputType(8194);
    p.addView(e, new LinearLayout.LayoutParams(-1, -2));
    gap(p, 14);
    if (label.startsWith("Tanggal")) {
      e.setFocusable(false);
      e.setOnClickListener(
          v -> {
            Calendar cal = Calendar.getInstance();
            try {
              SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
              cal.setTime(fmt.parse(e.getText().toString()));
            } catch (Exception ignored) {
            }
            DatePickerDialog dialog =
                new DatePickerDialog(
                    this,
                    (picker, y, m, d) ->
                        e.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)),
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH));
            if (label.contains("opsional"))
              dialog.setButton(
                  DialogInterface.BUTTON_NEUTRAL, "Kosongkan", (a, b) -> e.setText(""));
            dialog.show();
          });
    }
    return e;
  }

  Spinner spinner(LinearLayout p, String label, String[] labels, String[] values, String selected) {
    if (!label.isEmpty()) {
      p.addView(text(label, 12, MUTED, true));
      gap(p, 6);
    }
    Spinner s = new Spinner(this);
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels);
    s.setAdapter(adapter);
    s.setTag(values);
    s.setBackground(new SpinnerBg());
    s.setPadding(dp(4), 0, dp(24), 0);
    p.addView(s, new LinearLayout.LayoutParams(-1, dp(52)));
    for (int i = 0; i < values.length; i++) if (values[i].equals(selected)) s.setSelection(i);
    gap(p, 14);
    return s;
  }

  String pick(Spinner s) {
    String[] a = (String[]) s.getTag();
    return a.length == 0 ? "" : a[Math.max(0, s.getSelectedItemPosition())];
  }

  class Fields {
    LinearLayout box;
    Map<String, EditText> edits = new LinkedHashMap<>();
    Map<String, Spinner> selects = new LinkedHashMap<>();
    Set<String> numbers = new HashSet<>();
    Set<String> required = new HashSet<>();
    JSONObject old;

    Fields(LinearLayout b, JSONObject j) {
      box = b;
      old = j == null ? new JSONObject() : j;
    }

    EditText add(String k, String l, String def, boolean number, boolean req) {
      EditText e = input(box, l, str(old, k).isEmpty() ? def : str(old, k), number);
      edits.put(k, e);
      if (number) numbers.add(k);
      if (req) required.add(k);
      return e;
    }

    Spinner choose(String k, String l, String[] labs, String[] vals, String def) {
      Spinner s = spinner(box, l, labs, vals, str(old, k).isEmpty() ? def : str(old, k));
      selects.put(k, s);
      return s;
    }

    JSONObject value() throws Exception {
      JSONObject j = new JSONObject();
      for (String k : edits.keySet()) {
        String v = edits.get(k).getText().toString().trim();
        if (required.contains(k) && v.isEmpty())
          throw new Exception("Lengkapi kolom yang wajib diisi.");
        if (numbers.contains(k)) {
          BigDecimal n = v.isEmpty() ? BigDecimal.ZERO : new BigDecimal(v.replace(',', '.'));
          if (n.signum() < 0) throw new Exception("Jumlah tidak boleh negatif.");
          j.put(k, n);
        } else j.put(k, v);
      }
      for (String k : selects.keySet()) j.put(k, pick(selects.get(k)));
      return j;
    }
  }

  void form(String t, JSONObject old) {
    if (busy) return;
    if (t.equals("zakat_entries")
        && old != null
        && str(old, "zakat_type").equals("fitrah")
        && num(old, "amount").signum() > 0) {
      failure(
          "Transaksi fitrah lama dalam rupiah perlu diedit melalui website agar nominal lama tetap"
              + " terjaga.");
      return;
    }
    Dialog dialog = new Dialog(this);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    LinearLayout page = col();
    page.setBackgroundColor(BG);
    pad(page, 20);
    LinearLayout heading = row();
    TextView h = text((old == null ? "Tambah" : "Edit") + " · " + screen, 22, INK, true);
    heading.addView(h, new LinearLayout.LayoutParams(0, -2, 1));
    heading.addView(button("×", false, () -> dialog.dismiss()));
    page.addView(heading);
    gap(page, 12);
    ScrollView scroll = new ScrollView(this);
    LinearLayout contents = col();
    scroll.addView(contents);
    page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    Fields f = new Fields(contents, old);
    TextView help = text("", 12, MUTED, false);
    contents.addView(help);
    if (t.equals("zakat_entries")) {
      f.add(
          "full_name",
          tab.equals("Penerimaan") ? "Nama penyalur *" : "Nama penerima *",
          old == null ? "" : str(old, "party_name"),
          false,
          true);
      f.add("village", "Desa/Dusun *", "", false, true);
      if (tab.equals("Penyaluran")) {
        f.add("rt_rw", "RW/RT (opsional)", "", false, false);
        f.add("rt_name", "Nama RT (opsional)", "", false, false);
      }
      String[] kinds =
          tab.equals("Penerimaan")
              ? new String[] {"fitrah", "mal"}
              : new String[] {"fitrah", "mal", "fitrah_mal"};
      String[] names = new String[kinds.length];
      for (int i = 0; i < kinds.length; i++) names[i] = label(kinds[i]);
      Spinner kind = f.choose("zakat_type", "Jenis zakat", names, kinds, "fitrah");
      EditText kg = f.add("rice_kg", "Fitrah (kg)", "0", true, false),
          cash = f.add("amount", "Mal (Rp)", "0", true, false);
      kind.setOnItemSelectedListener(
          new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> a) {}

            public void onItemSelected(AdapterView<?> a, View v, int n, long id) {
              kg.setEnabled(!pick(kind).equals("mal"));
              cash.setEnabled(!pick(kind).equals("fitrah"));
            }
          });
      f.add("occurred_on", "Tanggal (YYYY-MM-DD) *", today(), false, true);
      f.add("notes", "Catatan (opsional)", "", false, false);
      help.setText(
          "Muzakki/mustahik ikut diperbarui otomatis. Isian RW/RT dan Nama RT boleh kosong.");
    } else if (t.equals("qurban_people")) {
      f.add("full_name", "Nama lengkap *", "", false, true);
      f.add("year", "Tahun Masehi *", "" + year, true, true);
      Spinner type =
          f.choose(
              "qurban_category",
              "Jenis qurban",
              new String[] {"Sapi pribadi", "Sapi rombongan (7 orang)", "Kambing pribadi"},
              new String[] {"sapi_pribadi", "sapi_patungan", "kambing"},
              "sapi_patungan");
      EditText nominal = f.add("nominal", "Nominal per peserta (Rp)", "0", true, true);
      type.setOnItemSelectedListener(
          new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> a) {}

            public void onItemSelected(AdapterView<?> a, View v, int n, long id) {
              nominal.setEnabled(!pick(type).equals("sapi_pribadi"));
            }
          });
      help.setText(
          "Sapi rombongan dicatat per orang, lalu dikelompokkan per 7 peserta. Dana otomatis masuk"
              + " ringkasan qurban.");
    } else if (t.equals("qurban_distribution")) {
      f.add("year", "Tahun Masehi *", "" + year, true, true);
      Spinner kind =
          f.choose(
              "recipient_kind",
              "Jenis penerima",
              new String[] {"Penerima qurban", "Shohibul qurban"},
              new String[] {"penerima", "shohibul"},
              "penerima");
      ArrayList<String> names = new ArrayList<>(), ids = new ArrayList<>();
      names.add("Pilih shohibul");
      ids.add("");
      for (JSONObject j : rows("qurban_people", false)) {
        names.add(str(j, "full_name") + " · " + str(j, "year"));
        ids.add(str(j, "id"));
      }
      Spinner person =
          f.choose(
              "shohibul_id",
              "Shohibul (untuk jenis shohibul)",
              names.toArray(new String[0]),
              ids.toArray(new String[0]),
              "");
      EditText village = f.add("village", "Desa/Dusun (penerima umum)", "", false, false),
          rw = f.add("rt_rw", "RW/RT (penerima umum)", "", false, false),
          rt = f.add("rt_name", "Nama RT (penerima umum)", "", false, false);
      kind.setOnItemSelectedListener(
          new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> a) {}

            public void onItemSelected(AdapterView<?> a, View v, int n, long id) {
              boolean sh = pick(kind).equals("shohibul");
              person.setEnabled(sh);
              village.setEnabled(!sh);
              rw.setEnabled(!sh);
              rt.setEnabled(!sh);
            }
          });
      EditText count = f.add("package_count", "Jumlah paket *", "1", true, true),
          weight = f.add("package_weight_kg", "Berat per paket (kg) *", "2", true, true);
      TextView total = text("", 18, GREEN, true);
      contents.addView(total);
      TextWatcher calc =
          new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int d) {}

            public void onTextChanged(CharSequence s, int a, int b, int c) {
              try {
                total.setText(
                    "Total berat: "
                        + qty(
                            new BigDecimal(count.getText().toString().replace(',', '.'))
                                .multiply(
                                    new BigDecimal(weight.getText().toString().replace(',', '.'))))
                        + " kg");
              } catch (Exception e) {
                total.setText("Isi jumlah paket dan berat.");
              }
            }

            public void afterTextChanged(Editable e) {}
          };
      count.addTextChangedListener(calc);
      weight.addTextChangedListener(calc);
      calc.onTextChanged("", 0, 0, 0);
      f.choose(
          "delivered",
          "Status penyerahan",
          new String[] {"Belum diserahkan", "Sudah diserahkan"},
          new String[] {"no", "yes"},
          old != null && !str(old, "collected_at").isEmpty() ? "yes" : "no");
      f.add("notes", "Catatan", "", false, false);
    } else if (t.equals("finance_transactions") || t.equals("qurban_cash")) {
      f.choose(
          "kind",
          "Arus transaksi",
          new String[] {"Penerimaan", "Pengeluaran"},
          new String[] {"income", "expense"},
          "income");
      if (t.equals("finance_transactions")) {
        String category = "";
        if (old != null)
          for (JSONObject c : rows("finance_categories", false))
            if (str(c, "id").equals(str(old, "category_id"))) category = str(c, "name");
        f.add("category_name", "Kategori (boleh ketik kategori baru) *", category, false, true);
      } else f.add("year", "Tahun Masehi *", "" + year, true, true);
      f.add("amount", "Nominal (Rp) *", "", true, true);
      f.add("occurred_on", "Tanggal (YYYY-MM-DD) *", today(), false, true);
      f.add("description", "Keterangan", "", false, false);
    } else if (t.equals("assets")) {
      f.add("name", "Nama inventaris *", "", false, true);
      f.add("quantity", "Jumlah unit *", "1", true, true);
      f.choose(
          "condition",
          "Kondisi",
          new String[] {"Baik", "Perlu perbaikan", "Rusak"},
          new String[] {"baik", "perlu_perbaikan", "rusak"},
          "baik");
      f.add("location", "Lokasi", "", false, false);
      f.add("acquired_on", "Tanggal perolehan (YYYY-MM-DD, opsional)", "", false, false);
    } else if (t.equals("mosque_officers")) {
      f.add("full_name", "Nama pengurus *", "", false, true);
      f.add("position", "Jabatan *", "", false, true);
      f.add("phone", "Nomor telepon", "", false, false);
      f.add("address", "Alamat", "", false, false);
      help.setText(
          "Form ini mencatat pengurus masjid. Setelah data tersimpan, admin dapat membuat akun"
              + " melalui tombol Buat akun login pada kartu pengurus.");
    } else if (t.equals("events")) {
      f.add("title", "Nama kegiatan *", "", false, true);
      f.add("description", "Keterangan", "", false, false);
      f.add(
          "starts_at",
          "Mulai (contoh 2026-11-20T08:00:00+07:00) *",
          today() + "T08:00:00+07:00",
          false,
          true);
      f.add("ends_at", "Selesai (opsional)", "", false, false);
      f.choose(
          "published",
          "Publikasi",
          new String[] {"Draf", "Diterbitkan"},
          new String[] {"no", "yes"},
          old != null && old.optBoolean("is_published") ? "yes" : "no");
    }
    TextView error = text("", 13, 0xffae493a, false);
    page.addView(error);
    TextView save =
        button(
            "Simpan data",
            true,
            () -> {
              try {
                JSONObject j = f.value();
                validate(t, j, old);
                dialog.setCancelable(false);
                error.setText("Menyimpan…");
                job(
                    () -> saveRecord(t, j, old),
                    o -> {
                      dialog.dismiss();
                      Toast.makeText(this, "Data berhasil disimpan", 0).show();
                      reload();
                    },
                    s -> {
                      dialog.setCancelable(true);
                      error.setText(s);
                    });
              } catch (Exception e) {
                error.setText(e.getMessage());
              }
            });
    page.addView(save);
    dialog.setContentView(page);
    dialog.show();
    dialog.getWindow().setLayout(-1, -1);
    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
  }

  void validate(String t, JSONObject j, JSONObject old) throws Exception {
    for (String key : new String[] {"occurred_on", "acquired_on"})
      if (j.has(key) && !str(j, key).isEmpty()) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setLenient(false);
        String v = str(j, key);
        if (!v.matches("\\d{4}-\\d{2}-\\d{2}") || !sdf.format(sdf.parse(v)).equals(v))
          throw new Exception("Tanggal harus valid dalam format YYYY-MM-DD.");
      }
    for (String k : new String[] {"year", "quantity", "package_count"})
      if (j.has(k) && num(j, k).stripTrailingZeros().scale() > 0)
        throw new Exception("Tahun, jumlah unit, dan paket harus bilangan bulat.");
    if (j.has("year") && (j.optInt("year") < 2000 || j.optInt("year") > 2200))
      throw new Exception("Tahun harus antara 2000 dan 2200.");
    if (t.equals("zakat_entries")) {
      String type = str(j, "zakat_type");
      if (type.equals("fitrah")) j.put("amount", 0);
      if (type.equals("mal")) j.put("rice_kg", 0);
      if ((!type.equals("mal") && num(j, "rice_kg").signum() <= 0)
          || (!type.equals("fitrah") && num(j, "amount").signum() <= 0))
        throw new Exception("Isi jumlah zakat lebih dari nol sesuai jenisnya.");
      if (num(j, "amount").scale() > 2 || num(j, "rice_kg").scale() > 3)
        throw new Exception("Rupiah maksimal 2 angka desimal dan kg maksimal 3 angka desimal.");
    }
    if (t.equals("qurban_people")) {
      if (str(j, "qurban_category").equals("sapi_pribadi")) j.put("nominal", 0);
      else if (num(j, "nominal").signum() <= 0)
        throw new Exception("Nominal harus lebih dari nol.");
    }
    if (t.equals("qurban_distribution")) {
      if (num(j, "package_count").signum() <= 0 || num(j, "package_weight_kg").signum() <= 0)
        throw new Exception("Jumlah paket dan berat wajib lebih dari nol.");
      boolean sh = str(j, "recipient_kind").equals("shohibul");
      if (sh) {
        boolean found = false;
        for (JSONObject p : rows("qurban_people", false))
          if (str(p, "id").equals(str(j, "shohibul_id")) && p.optInt("year") == j.optInt("year"))
            found = true;
        if (!found) throw new Exception("Pilih shohibul sesuai tahun kegiatan.");
      } else if (str(j, "village").isEmpty()
          || str(j, "rt_rw").isEmpty()
          || str(j, "rt_name").isEmpty())
        throw new Exception("Isi Desa/Dusun, RW/RT dan Nama RT untuk penerima umum.");
    }
    if ((t.equals("finance_transactions") || t.equals("qurban_cash"))
        && num(j, "amount").signum() <= 0) throw new Exception("Nominal harus lebih dari nol.");
  }

  Object saveRecord(String t, JSONObject j, JSONObject old) throws Exception {
    if (t.equals("zakat_entries")) {
      j.put("id", old == null ? UUID.randomUUID().toString() : str(old, "id"))
          .put("is_new", old == null)
          .put("direction", tab.equals("Penerimaan") ? "receipt" : "distribution")
          .put("people_count", 1);
      if (old != null) j.put("party_id", str(old, "party_id"));
      return request(
          "POST",
          "/rest/v1/rpc/simas_save_zakat",
          new JSONObject().put("p_mosque", mosqueId).put("p_rows", new JSONArray().put(j)),
          true);
    }
    if (t.equals("finance_transactions")) {
      String name = str(j, "category_name"), kind = str(j, "kind"), id = "";
      for (JSONObject c : rows("finance_categories", false))
        if (str(c, "name").equalsIgnoreCase(name) && str(c, "kind").equals(kind)) id = str(c, "id");
      if (id.isEmpty()) {
        JSONArray a =
            new JSONArray(
                request(
                    "POST",
                    "/rest/v1/finance_categories",
                    new JSONObject().put("mosque_id", mosqueId).put("name", name).put("kind", kind),
                    true));
        id = a.getJSONObject(0).getString("id");
      }
      j.remove("category_name");
      j.put("category_id", id);
    }
    if (t.equals("qurban_distribution")) {
      boolean sh = str(j, "recipient_kind").equals("shohibul");
      if (!sh) j.put("shohibul_id", JSONObject.NULL);
      else {
        j.put("village", JSONObject.NULL)
            .put("rt_rw", JSONObject.NULL)
            .put("rt_name", JSONObject.NULL);
      }
      j.put("weight_kg", num(j, "package_count").multiply(num(j, "package_weight_kg")));
      j.put(
          "collected_at",
          str(j, "delivered").equals("yes")
              ? (old != null && !str(old, "collected_at").isEmpty()
                  ? str(old, "collected_at")
                  : timestamp())
              : JSONObject.NULL);
      j.remove("delivered");
      if (old == null)
        j.put("coupon", "AND-" + UUID.randomUUID().toString()).put("recipient", JSONObject.NULL);
    }
    if (t.equals("events")) {
      j.put("is_published", str(j, "published").equals("yes"));
      j.remove("published");
      if (str(j, "ends_at").isEmpty()) j.put("ends_at", JSONObject.NULL);
    }
    if (t.equals("assets") && str(j, "acquired_on").isEmpty())
      j.put("acquired_on", JSONObject.NULL);
    if (old == null) j.put("mosque_id", mosqueId);
    String response =
        request(
            old == null ? "POST" : "PATCH",
            "/rest/v1/"
                + t
                + (old == null ? "" : "?id=eq." + str(old, "id") + "&mosque_id=eq." + mosqueId),
            j,
            true);
    if (new JSONArray(response).length() == 0)
      throw new Exception("Data tidak berubah. Periksa hak akses akun.");
    return response;
  }

  void reports() {
    if (!financeAccess()) {
      note(body, "Laporan keuangan hanya untuk pengurus yang berwenang.");
      return;
    }
    yearPicker(body);
    tabs(new String[] {"Zakat", "Qurban", "Keuangan"});
    if (tab.equals("Zakat")) {
      zakatSummary(body);
      if (!errors.containsKey("zakat_entries")) {
        chart("zakat_entries", "amount", "Dana zakat (Rp)", "direction", "receipt", "distribution");
        chart(
            "zakat_entries", "rice_kg", "Beras zakat (kg)", "direction", "receipt", "distribution");
        body.addView(
            button(
                "Ekspor laporan zakat",
                true,
                () -> exportOptions("zakat_entries", rows("zakat_entries", true))));
      }
    } else if (tab.equals("Qurban")) {
      qurbanSummary(body);
      if (!errors.containsKey("qurban_cash"))
        chart("qurban_cash", "amount", "Kas tambahan qurban (Rp)", "kind", "income", "expense");
      note(
          body,
          "Grafik kas tambahan tidak memasukkan nominal shohibul; nominal tersebut ada pada"
              + " ringkasan penerimaan.");
      gap(body, 12);
      body.addView(
          button(
              "Ekspor distribusi qurban",
              true,
              () -> exportOptions("qurban_distribution", rows("qurban_distribution", true))));
    } else {
      if (ready(body, "finance_transactions")) {
        LinearLayout c = card(body);
        stat(c, "Penerimaan", money(sum("finance_transactions", "amount", "kind", "income")));
        stat(c, "Pengeluaran", money(sum("finance_transactions", "amount", "kind", "expense")));
        chart("finance_transactions", "amount", "Arus keuangan (Rp)", "kind", "income", "expense");
        body.addView(
            button(
                "Ekspor transaksi keuangan",
                true,
                () -> exportOptions("finance_transactions", rows("finance_transactions", true))));
      }
    }
    gap(body, 16);
    note(
        body,
        "Ekspor PDF berupa tabel dengan kop masjid dan Excel .xlsx. Rekap pivot tersedia untuk"
            + " Zakat dan Distribusi Qurban. Impor dan template tetap tersedia di website.");
  }

  void chart(
      String t, String f, String heading, String direction, String incoming, String outgoing) {
    LinearLayout c = card(body);
    title(c, heading);
    double[][] v = new double[2][12];
    for (JSONObject j : rows(t, true)) {
      String d = str(j, "occurred_on");
      try {
        int m = Integer.parseInt(d.substring(5, 7)) - 1;
        if (m >= 0 && m < 12)
          v[str(j, direction).equals(incoming) ? 0 : 1][m] += num(j, f).doubleValue();
      } catch (Exception ignored) {
      }
    }
    c.addView(new Chart(v), new LinearLayout.LayoutParams(-1, dp(180)));
    note(c, "Hijau: penerimaan · Emas: penyaluran/pengeluaran\nJanuari–Desember " + year);
  }

  void settings() {
    LinearLayout c = card(body);
    Bitmap logo = mosqueLogo();
    if (logo != null) {
      ImageView image = new ImageView(this);
      image.setImageBitmap(logo);
      image.setScaleType(ImageView.ScaleType.FIT_CENTER);
      c.addView(image, new LinearLayout.LayoutParams(dp(64), dp(64)));
      gap(c, 12);
    }
    title(c, str(mosque, "name"));
    note(c, str(mosque, "address"));
    gap(c, 12);
    stat(c, "Akun", email);
    stat(c, "Peran", label(role));
    note(
        c,
        "Sesi disimpan hanya selama aplikasi berjalan. Saat aplikasi ditutup oleh sistem, masuk"
            + " kembali untuk menjaga data.");
    if (role.equals("admin")) {
      gap(c, 16);
      c.addView(button("Ubah informasi masjid", false, () -> editMosque()));
      gap(c, 10);
      c.addView(button("Ubah logo masjid", false, () -> chooseLogo()));
    }
    gap(body, 12);
    body.addView(
        button(
            "Keluar dari akun",
            false,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("Keluar dari SIMASJID?")
                    .setNegativeButton("Batal", null)
                    .setPositiveButton(
                        "Keluar",
                        (d, w) -> {
                          job(
                              () -> {
                                try {
                                  request("POST", "/auth/v1/logout?scope=local", null, true);
                                } catch (Exception ignored) {
                                }
                                return null;
                              },
                              o -> logoutLocal(),
                              s -> logoutLocal());
                        })
                    .show()));
    gap(body, 24);
    note(
        body,
        "SIMASJID Android 2.4.1 · Antarmuka native\n"
            + "Terhubung ke database masjid yang sama. Pembaruan tampilan dan fitur Android"
            + " memerlukan APK baru.\n\n"
            + "Impor Excel dan pivot masih dikelola melalui website untuk versi ini.");
  }

  void editMosque() {
    LinearLayout c = col();
    pad(c, 20);
    EditText name = input(c, "Nama masjid", str(mosque, "name"), false),
        address = input(c, "Alamat", str(mosque, "address"), false);
    AlertDialog d =
        new AlertDialog.Builder(this)
            .setTitle("Informasi masjid")
            .setView(c)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan", null)
            .create();
    d.setOnShowListener(
        x ->
            d.getButton(-1)
                .setOnClickListener(
                    v -> {
                      String nm = name.getText().toString().trim();
                      if (nm.length() < 2) {
                        name.setError("Isi nama masjid");
                        return;
                      }
                      job(
                          () ->
                              request(
                                  "PATCH",
                                  "/rest/v1/mosques?id=eq." + mosqueId,
                                  new JSONObject()
                                      .put("name", nm)
                                      .put("address", address.getText().toString().trim()),
                                  true),
                          o -> {
                            d.dismiss();
                            reload();
                          },
                          this::failure);
                    }));
    d.show();
  }

  String searchText(String t, JSONObject j) {
    return (rowTitle(t,j)+" "+details(t,j)+" "+str(j,"notes")+" "+str(j,"rt_name")+" "+str(j,"coupon")).toLowerCase(new Locale("id"));
  }

  ArrayList<JSONObject> searchRows(String t, ArrayList<JSONObject> values, String q) {
    ArrayList<JSONObject> result=new ArrayList<>();
    for(JSONObject j:values)if(searchText(t,j).contains(q.toLowerCase(new Locale("id"))))result.add(j);
    return result;
  }

  ArrayList<JSONObject> cashExportValues(ArrayList<JSONObject> values) throws Exception {
    ArrayList<JSONObject> out=new ArrayList<>(values);
    for(JSONObject p:rows("qurban_people",true)) {
      if(str(p,"qurban_category").isEmpty()||num(p,"nominal").signum()<=0)continue;
      String date=str(p,"created_at"); if(date.isEmpty())date=today(); if(date.length()>10)date=date.substring(0,10);
      JSONObject r=new JSONObject().put("id","auto-"+str(p,"id")).put("person_id",str(p,"id")).put("kind","income").put("amount",num(p,"nominal")).put("occurred_on",date).put("description","Otomatis · "+label(str(p,"qurban_category")));
      if((searchText("qurban_cash",r)+" "+str(p,"full_name").toLowerCase(new Locale("id"))).contains(moduleQuery.toLowerCase(new Locale("id"))))out.add(r);
    }
    return out;
  }

  ExportTable exportTable(String t, ArrayList<JSONObject> values) {
    String audience=screen.equals("Qurban")?distributionFilter:"all";
    ExportTable report=ExportTable.create(this,t,values,audience);
    if(!screen.equals("Laporan")&&!moduleQuery.isEmpty())report.context+=" · Pencarian: "+moduleQuery;
    if(t.equals("zakat_entries")&&!screen.equals("Laporan"))report.context+=" · "+tab+" · "+(zakatFilter.equals("all")?"Semua jenis":label(zakatFilter));
    return report;
  }

  void exportOptions(String t, ArrayList<JSONObject> values) {
    if(errors.containsKey(t)){failure("Data belum dimuat; ekspor dibatalkan.");return;}
    try {
      if(t.equals("qurban_cash")) {
        if(errors.containsKey("qurban_people"))throw new Exception("Data shohibul belum dimuat; ekspor kas dibatalkan.");
        values = cashExportValues(values);
      }
      final ExportTable source=exportTable(t,values);
      final ArrayList<String> groups=new ArrayList<>();
      groups.add("Data lengkap");
      for(String name:new String[]{"Desa/Dusun","Wilayah/Dusun","RW/RT","Nama RT"})
        if(java.util.Arrays.asList(source.headers).contains(name)&&(t.equals("zakat_entries")||t.equals("qurban_distribution")))groups.add("Rekap pivot · "+name);
      if(groups.size()==1){exportFormat(source,null);return;}
      new AlertDialog.Builder(this).setTitle("Ekspor · "+source.rows.size()+" catatan")
        .setItems(groups.toArray(new String[0]),(d,n)->{
          if(n==0)exportFormat(source,null);
          else exportFormat(source.pivot(groups.get(n).substring("Rekap pivot · ".length())),source);
        }).show();
    }catch(Exception e){failure(e.getMessage());}
  }

  void exportFormat(ExportTable report, ExportTable source) {
    new AlertDialog.Builder(this).setTitle(report.title)
      .setItems(new String[]{"PDF · lihat pratinjau", "Excel (.xlsx)"}, (d, n) -> {
        String name = "SIMASJID-" + report.title.replaceAll("[^a-zA-Z0-9-]+", "-") + "-" + year + (n == 0 ? ".pdf" : ".xlsx");
        job(() -> n == 0 ? ExportFiles.pdf(mosque, report, today()) : source == null ? ExportFiles.xlsx(mosque, report) : ExportFiles.xlsx(mosque, report, source),
          result -> {
            byte[] bytes = (byte[]) result;
            if (n == 0) previewPdf(bytes, name);
            else {
              exportBytes = bytes;
              exportName = name;
              saveExport("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            }
          }, this::failure);
      }).show();
  }

  void previewPdf(byte[] bytes, String name) {
    if (pdfPreview != null) pdfPreview.dismiss();
    pdfPreview = new PdfPreview(this, bytes, name);
    pdfPreview.open();
  }

  void saveExport(String mime) {
    try {
      Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.setType(mime);
      intent.putExtra(Intent.EXTRA_TITLE, exportName);
      startActivityForResult(intent, 81);
    } catch (Exception e) {
      exportBytes = null;
      failure("Pemilih lokasi simpan tidak tersedia. Silakan coba kembali.");
    }
  }

  String csvCell(String s) {
    if (s.matches("^[=+@\\-].*")) s = "'" + s;
    return "\"" + s.replace("\"", "\"\"") + "\"";
  }

  byte[] csv(String t, ArrayList<JSONObject> values) throws Exception {
    StringBuilder s = new StringBuilder("\ufeff");
    s.append(csvCell(str(mosque, "name")))
        .append("\r\n")
        .append(csvCell(str(mosque, "address")))
        .append("\r\n")
        .append(csvCell(yearLabel(year)))
        .append("\r\n");
    s.append("No,Nama/Keterangan,Detail,Berat (kg),Nominal (Rp)\r\n");
    BigDecimal kg = BigDecimal.ZERO, rp = BigDecimal.ZERO;
    int n = 0;
    for (JSONObject j : values) {
      BigDecimal k = num(j, t.equals("qurban_distribution") ? "weight_kg" : "rice_kg"),
          r = num(j, t.equals("qurban_people") ? "nominal" : "amount");
      kg = kg.add(k);
      rp = rp.add(r);
      s.append(++n)
          .append(',')
          .append(csvCell(rowTitle(t, j)))
          .append(',')
          .append(csvCell(details(t, j)))
          .append(',')
          .append(k.toPlainString())
          .append(',')
          .append(r.toPlainString())
          .append("\r\n");
    }
    s.append(",TOTAL,")
        .append(values.size())
        .append(" catatan,")
        .append(kg.toPlainString())
        .append(',')
        .append(rp.toPlainString())
        .append("\r\n");
    return s.toString().getBytes("UTF-8");
  }

  byte[] pdf(String t, ArrayList<JSONObject> values) throws Exception {
    return ExportFiles.pdf(mosque,exportTable(t,values),today());
  }

  String shortText(String s, Paint p, int width) {
    int n = p.breakText(s, true, width, null);
    return n < s.length() ? s.substring(0, Math.max(0, n - 1)) + "…" : s;
  }

  @Override
  protected void onActivityResult(int req, int result, Intent intent) {
    super.onActivityResult(req, result, intent);
    if (req == 82 && result == RESULT_OK && intent != null && intent.getData() != null) {
      final android.net.Uri uri = intent.getData();
      job(
          () -> {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream in = getContentResolver().openInputStream(uri)) {
              if (in == null) throw new IOException("Gambar tidak tersedia.");
              byte[] b = new byte[4096];
              int n;
              while ((n = in.read(b)) != -1) {
                bytes.write(b, 0, n);
                if (bytes.size() > 8 * 1024 * 1024)
                  throw new IOException("Ukuran gambar maksimal 8 MB.");
              }
            }
            byte[] raw = bytes.toByteArray();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(raw, 0, raw.length, options);
            if (options.outWidth < 1)
              throw new IOException("Pilih gambar PNG atau JPEG yang valid.");
            options.inSampleSize = 1;
            while (options.outWidth / options.inSampleSize > 512
                || options.outHeight / options.inSampleSize > 512) options.inSampleSize *= 2;
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length, options);
            if (bitmap == null) throw new IOException("Gambar gagal dibaca.");
            float scale = Math.min(1f, 256f / Math.max(bitmap.getWidth(), bitmap.getHeight()));
            Bitmap small =
                Bitmap.createScaledBitmap(
                    bitmap,
                    Math.max(1, (int) (bitmap.getWidth() * scale)),
                    Math.max(1, (int) (bitmap.getHeight() * scale)),
                    true);
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            small.compress(Bitmap.CompressFormat.PNG, 100, png);
            String dataUrl =
                "data:image/png;base64,"
                    + android.util.Base64.encodeToString(
                        png.toByteArray(), android.util.Base64.NO_WRAP);
            if (dataUrl.length() > 300000)
              throw new IOException("Logo terlalu besar. Pilih gambar yang lebih sederhana.");
            return request(
                "PATCH",
                "/rest/v1/mosques?id=eq." + mosqueId,
                new JSONObject().put("logo_data_url", dataUrl),
                true);
          },
          o -> {
            Toast.makeText(this, "Logo berhasil diperbarui", 0).show();
            reload();
          },
          this::failure);
      return;
    }
    if (req == 81
        && result == RESULT_OK
        && intent != null
        && intent.getData() != null
        && exportBytes != null) {
      try (OutputStream out = getContentResolver().openOutputStream(intent.getData())) {
        if (out == null) throw new IOException("Lokasi berkas tidak tersedia.");
        out.write(exportBytes);
        Toast.makeText(this, "Laporan berhasil disimpan", 0).show();
      } catch (Exception e) {
        failure("Gagal menyimpan berkas: " + e.getMessage());
      }
    }
    if (req == 81) exportBytes = null;
  }

  class SpinnerBg extends GradientDrawable {
    Paint pen = new Paint(3);

    SpinnerBg() {
      setColor(Color.WHITE);
      setCornerRadius(dp(12));
      pen.setColor(GREEN);
      pen.setStrokeWidth(dp(2));
      pen.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void draw(Canvas c) {
      super.draw(c);
      float x = getBounds().right - dp(19), y = getBounds().centerY();
      c.drawLine(x - dp(4), y - dp(2), x, y + dp(2), pen);
      c.drawLine(x, y + dp(2), x + dp(4), y - dp(2), pen);
    }
  }

  class Chart extends View {
    double[][] values;
    Paint p = new Paint(3);

    Chart(double[][] a) {
      super(MainActivity.this);
      values = a;
      setContentDescription("Grafik bulanan penerimaan dan pengeluaran");
    }

    protected void onDraw(Canvas c) {
      super.onDraw(c);
      double max = 1;
      for (double[] a : values) for (double n : a) max = Math.max(max, n);
      float w = getWidth(), h = getHeight() - dp(28), step = w / 12;
      p.setTextSize(dp(9));
      for (int i = 0; i < 12; i++) {
        for (int s = 0; s < 2; s++) {
          p.setColor(s == 0 ? GREEN : GOLD);
          float left = i * step + dp(2) + s * step * .38f;
          float top = (float) (h - values[s][i] / max * (h - dp(20)));
          c.drawRoundRect(left, top, left + step * .3f, h, dp(3), dp(3), p);
        }
        p.setColor(MUTED);
        c.drawText("" + (i + 1), i * step + dp(3), h + dp(17), p);
      }
      p.setColor(MUTED);
      c.drawText("Maks " + qty(BigDecimal.valueOf(max)), 0, dp(12), p);
    }
  }

  class Icon extends View {
    String kind;
    Paint p = new Paint(3);
    int color;

    Icon(String k, int col) {
      super(MainActivity.this);
      kind = k;
      color = col;
      setContentDescription(k);
    }

    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      Canvas c = canvas;
      c.save();
      c.scale(getWidth() / 48f, getHeight() / 48f);
      p.setColor(color);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(2.6f);
      p.setStrokeCap(Paint.Cap.ROUND);
      p.setStrokeJoin(Paint.Join.ROUND);
      Path path = new Path();
      switch (kind) {
        case "Keuangan":
          c.drawRoundRect(5, 12, 43, 38, 5, 5, p);
          c.drawRoundRect(28, 20, 44, 31, 3, 3, p);
          c.drawCircle(34, 25, 1, p);
          c.drawLine(8, 12, 35, 6, p);
          break;
        case "Zakat":
          path.moveTo(18, 15);
          path.lineTo(14, 5);
          path.lineTo(24, 9);
          path.lineTo(34, 5);
          path.lineTo(30, 15);
          path.cubicTo(46, 35, 39, 43, 24, 43);
          path.cubicTo(9, 43, 2, 35, 18, 15);
          c.drawPath(path, p);
          c.drawLine(17, 16, 31, 16, p);
          c.drawLine(24, 23, 24, 36, p);
          for (int i = 0; i < 3; i++) {
            c.drawLine(24, 26 + i * 4, 19, 22 + i * 4, p);
            c.drawLine(24, 28 + i * 4, 29, 24 + i * 4, p);
          }
          break;
        case "Qurban":
          path.moveTo(17, 15);
          path.cubicTo(4, 14, 4, 5, 10, 4);
          path.moveTo(31, 15);
          path.cubicTo(44, 14, 44, 5, 38, 4);
          path.moveTo(15, 15);
          path.lineTo(7, 20);
          path.lineTo(15, 26);
          path.lineTo(19, 38);
          path.lineTo(24, 43);
          path.lineTo(29, 38);
          path.lineTo(33, 26);
          path.lineTo(41, 20);
          path.lineTo(33, 15);
          path.close();
          c.drawPath(path, p);
          c.drawCircle(20, 24, 1, p);
          c.drawCircle(28, 24, 1, p);
          c.drawLine(22, 33, 26, 33, p);
          break;
        case "Inventaris":
          path.moveTo(5, 13);
          path.lineTo(24, 5);
          path.lineTo(43, 13);
          path.lineTo(43, 36);
          path.lineTo(24, 44);
          path.lineTo(5, 36);
          path.close();
          c.drawPath(path, p);
          c.drawLine(5, 13, 24, 22, p);
          c.drawLine(43, 13, 24, 22, p);
          c.drawLine(24, 22, 24, 44, p);
          break;
        case "Pengurus":
          c.drawCircle(18, 14, 7, p);
          c.drawArc(5, 25, 31, 46, 180, 180, false, p);
          c.drawCircle(35, 18, 5, p);
          c.drawArc(28, 29, 46, 43, 180, 150, false, p);
          break;
        case "Kegiatan":
          c.drawRoundRect(5, 10, 43, 43, 5, 5, p);
          c.drawLine(5, 20, 43, 20, p);
          c.drawLine(15, 5, 15, 15, p);
          c.drawLine(33, 5, 33, 15, p);
          c.drawLine(15, 31, 21, 37, p);
          c.drawLine(21, 37, 33, 26, p);
          break;
        case "Laporan":
          c.drawRoundRect(8, 4, 40, 44, 4, 4, p);
          c.drawLine(16, 34, 16, 26, p);
          c.drawLine(24, 34, 24, 19, p);
          c.drawLine(32, 34, 32, 12, p);
          break;
        case "Menu":
          for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++)
              c.drawRoundRect(6 + x * 22, 6 + y * 22, 20 + x * 22, 20 + y * 22, 4, 4, p);
          break;
        case "Pengaturan":
          c.drawCircle(24, 24, 13, p);
          c.drawCircle(24, 24, 5, p);
          for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            c.drawLine(
                24 + (float) Math.cos(a) * 14,
                24 + (float) Math.sin(a) * 14,
                24 + (float) Math.cos(a) * 20,
                24 + (float) Math.sin(a) * 20,
                p);
          }
          break;
        default:
          path.moveTo(8, 23);
          path.quadTo(8, 12, 24, 5);
          path.quadTo(40, 12, 40, 23);
          path.lineTo(40, 43);
          path.lineTo(8, 43);
          path.close();
          c.drawPath(path, p);
          c.drawLine(8, 24, 40, 24, p);
          c.drawRoundRect(19, 30, 29, 43, 4, 4, p);
          break;
      }
      c.restore();
    }
  }
}
