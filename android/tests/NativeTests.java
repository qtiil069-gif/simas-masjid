package id.simas.mobile;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import java.io.*;
import java.math.*;
import org.json.*;

public class NativeTests extends Instrumentation {
  MainActivity a;
  Throwable problem;
  int checks = 0;

  void check(boolean result, String msg) {
    checks++;
    if (!result) throw new AssertionError(msg);
  }

  void main(Runnable r) {
    runOnMainSync(
        () -> {
          try {
            r.run();
          } catch (Throwable t) {
            problem = t;
          }
        });
    if (problem != null) throw new RuntimeException(problem);
    waitForIdleSync();
  }

  void shot(String name) throws Exception {
    Thread.sleep(700);
    android.view.accessibility.AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
    if(root != null && !root.findAccessibilityNodeInfosByText("System UI isn't responding").isEmpty()) {
      for(android.view.accessibility.AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByText("Wait")) {
        node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
      }
      Thread.sleep(800);
    }
    Bitmap b = getUiAutomation().takeScreenshot();
    File f = new File(getTargetContext().getExternalFilesDir(null), name + ".png");
    try (FileOutputStream out = new FileOutputStream(f)) {
      b.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
  }

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    start();
  }

  @Override
  public void onStart() {
    Bundle result = new Bundle();
    try {
      Intent intent = new Intent(getTargetContext(), MainActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      a = (MainActivity) startActivitySync(intent);
      check(a.screen.equals("Login"), "starts login");
      shot("login");
      main(
          () -> {
            try {
              a.token = "fixture-only-no-network";
              a.mosqueId = "00000000-0000-0000-0000-000000000001";
              a.role = "admin";
              a.email = "pengurus@example.test";
              a.year = 2026;
              a.mosque =
                  new JSONObject()
                      .put("name", "Masjid Sabilillah")
                      .put("address", "Margomulyo, Kerek, Tuban");
              for (String t : a.tables) a.data.put(t, new JSONArray());
              a.data.put(
                  "finance_transactions",
                  new JSONArray(
                      "[{\"id\":\"f1\",\"kind\":\"income\",\"amount\":12500000,\"occurred_on\":\"2026-05-01\",\"description\":\"Infak"
                          + " jamaah\"},{\"id\":\"f2\",\"kind\":\"expense\",\"amount\":2500000,\"occurred_on\":\"2026-05-02\",\"description\":\"Operasional"
                          + " masjid\"}]"));
              a.data.put(
                  "zakat_entries",
                  new JSONArray(
                      "[{\"id\":\"z1\",\"direction\":\"receipt\",\"zakat_type\":\"fitrah\",\"party_name\":\"Jamaah"
                          + " Uji\",\"village\":\"Margomulyo\",\"rice_kg\":120,\"amount\":0,\"occurred_on\":\"2026-03-19\"}]"));
              a.navigate("Beranda");
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
      shot("beranda");
      main(
          () -> {
            check(
                a.sum("finance_transactions", "amount", "kind", "income")
                        .compareTo(new BigDecimal("12500000"))
                    == 0,
                "income exact");
            check(
                a.sum("finance_transactions", "amount", "kind", "expense")
                        .compareTo(new BigDecimal("2500000"))
                    == 0,
                "expense exact");
            check(a.yearLabel(2026).contains("H"), "Hijri label");
            check(a.csvCell("=1+1").startsWith("\"'"), "CSV formula safety");
            try {
              JSONObject z =
                  new JSONObject(
                      "{\"full_name\":\"Uji\",\"village\":\"Desa\",\"zakat_type\":\"fitrah\",\"rice_kg\":3,\"amount\":0,\"occurred_on\":\"2026-03-19\"}");
              a.validate("zakat_entries", z, null);
              checks++;
              boolean bad = false;
              try {
                z.put("rice_kg", 0);
                a.validate("zakat_entries", z, null);
              } catch (Exception e) {
                bad = true;
              }
              check(bad, "zero rejected");
              z.put("rice_kg", 3).put("occurred_on", "2026-02-30");
              bad = false;
              try {
                a.validate("zakat_entries", z, null);
              } catch (Exception e) {
                bad = true;
              }
              check(bad, "invalid date rejected");
              byte[] pdf = a.pdf("zakat_entries", a.rows("zakat_entries", true));
              check(new String(pdf, 0, 4, "UTF-8").equals("%PDF"), "PDF valid signature");
              check(
                  new String(a.csv("zakat_entries", a.rows("zakat_entries", true)), "UTF-8")
                      .contains("TOTAL"),
                  "CSV totals");
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
      for (String screen : a.menu) {
        main(() -> a.navigate(screen));
        shot(screen.toLowerCase());
        checks++;
      }
      main(
          () -> {
            a.navigate("Zakat");
            a.tab = "Penyaluran";
            a.render();
            a.form("zakat_entries", null);
          });
      shot("form-zakat");
      main(
          () -> {
            a.role = "viewer";
            check(!a.canWrite("assets"), "viewer no write");
            check(!a.financeAccess(), "viewer no finance");
            a.role = "bendahara";
            check(a.canWrite("zakat_entries"), "treasurer zakat");
            check(!a.canWrite("mosque_officers"), "treasurer no officers");
          });
      result.putString(
          "stream", "PASS " + checks + " native checks; screenshots in app external files\n");
      finish(Activity.RESULT_OK, result);
    } catch (Throwable e) {
      result.putString("stream", "FAIL " + android.util.Log.getStackTraceString(e));
      finish(Activity.RESULT_CANCELED, result);
    }
  }
}
