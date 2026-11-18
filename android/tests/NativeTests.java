package id.simas.mobile;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import java.io.*;
import java.math.*;
import java.util.*;
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

  void writeExport(String name, byte[] bytes) throws Exception {
    try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),name))){out.write(bytes);}
  }

  void exportRegression() throws Exception {
    ArrayList<JSONObject> zs=new ArrayList<>();
    zs.add(new JSONObject().put("party_name","=Nama & Uji <A>").put("village","Margomulyo").put("rt_rw","02/01").put("rt_name","Ketua RT").put("direction","receipt").put("zakat_type","fitrah_mal").put("rice_kg",12.5).put("amount",100000).put("occurred_on","2026-03-19").put("notes","Catatan contoh"));
    zs.add(new JSONObject().put("party_name","Penerima contoh").put("village","Margomulyo").put("rt_rw","02/01").put("direction","distribution").put("zakat_type","fitrah_mal").put("rice_kg",2.5).put("amount",20000).put("occurred_on","2026-03-20"));
    ExportTable z=ExportTable.create(a,"zakat_entries",zs,"all");
    check(z.headers.length==11&&z.headers[8].equals("Fitrah (kg)"),"zakat website columns");
    check(z.totals.size()==3&&((BigDecimal)z.totals.get(2)[8]).compareTo(new BigDecimal("10.0"))==0,"zakat kg balance");
    check(((BigDecimal)z.totals.get(2)[9]).compareTo(new BigDecimal("80000"))==0,"zakat cash balance");
    ExportTable pivot=z.pivot("Desa/Dusun");
    check(pivot.rows.size()==1&&((BigDecimal)pivot.rows.get(0)[2]).intValue()==2,"pivot count");
    check(((BigDecimal)pivot.rows.get(0)[3]).compareTo(new BigDecimal("15.0"))==0,"pivot kg sum");
    writeExport("Contoh-Zakat.xlsx",ExportFiles.xlsx(a.mosque,z));
    writeExport("Contoh-Zakat.pdf",ExportFiles.pdf(a.mosque,z,"17 November 2026"));
    writeExport("Contoh-Pivot.xlsx",ExportFiles.xlsx(a.mosque,pivot,z));
    writeExport("Contoh-Pivot.pdf",ExportFiles.pdf(a.mosque,pivot,"17 November 2026"));
    JSONArray saved=a.data.get("qurban_people");
    a.data.put("qurban_people",new JSONArray().put(new JSONObject().put("id","q1").put("full_name","Shohibul Contoh").put("qurban_category","sapi_pribadi").put("nominal",15000000).put("year",2026).put("created_at","2026-05-01")));
    ArrayList<JSONObject> ds=new ArrayList<>();
    ds.add(new JSONObject().put("recipient_kind","shohibul").put("shohibul_id","q1").put("package_count",41).put("secondary_count",40).put("package_weight_kg",2).put("secondary_weight_kg",0.5).put("weight_kg",22).put("notes","Contoh alokasi"));
    ExportTable sho=ExportTable.create(a,"qurban_distribution",ds,"shohibul");
    check(sho.headers.length==7&&sho.rows.get(0)[1].equals("Shohibul Contoh"),"shohibul name lookup");
    check(sho.rows.get(0)[4].equals("1 bks × 2 kg + 40 bks × 0.5 kg"),"mixed composition");
    check(((BigDecimal)sho.totals.get(0)[5]).compareTo(new BigDecimal("22"))==0,"mixed kg total");
    writeExport("Contoh-Shohibul.pdf",ExportFiles.pdf(a.mosque,sho,"17 November 2026"));
    ds.add(new JSONObject().put("recipient_kind","penerima").put("village","Dusun Contoh").put("rt_name","Ketua RT").put("rt_rw","RW 02 RT 03").put("package_count",10).put("package_weight_kg",0.5).put("weight_kg",5));
    ExportTable both=ExportTable.create(a,"qurban_distribution",ds,"all");
    check(both.headers.length==12&&both.rows.get(1)[6].equals("03")&&both.rows.get(1)[7].equals("02"),"distribution both RT RW");
    check(((BigDecimal)both.totals.get(0)[8]).intValue()==51,"distribution package totals");
    writeExport("Contoh-Distribusi.xlsx",ExportFiles.xlsx(a.mosque,both));
    writeExport("Contoh-Distribusi.pdf",ExportFiles.pdf(a.mosque,both,"17 November 2026"));
    ArrayList<JSONObject> peopleOnly=new ArrayList<>();peopleOnly.add(ds.get(1));
    ExportTable recipients=ExportTable.create(a,"qurban_distribution",peopleOnly,"penerima");
    check(recipients.headers.length==8&&recipients.headers[1].equals("Wilayah/Dusun"),"recipient columns");
    writeExport("Contoh-Penerima.pdf",ExportFiles.pdf(a.mosque,recipients,"17 November 2026"));
    ArrayList<JSONObject> cash=new ArrayList<>();cash.add(new JSONObject().put("kind","expense").put("amount",1000000).put("description","[Pembelian Hewan] Pembelian contoh"));
    ArrayList<JSONObject> cashAll=a.cashExportValues(cash);ExportTable c=ExportTable.create(a,"qurban_cash",cashAll,"all");
    check(c.rows.size()==2&&((BigDecimal)c.totals.get(1)[5]).intValue()==14000000,"cash includes automatic contribution balance");
    check(c.rows.get(0)[2].equals("Pembelian Hewan"),"cash category");
    a.data.put("qurban_people",saved);
    ArrayList<JSONObject> many=new ArrayList<>();for(int i=0;i<180;i++)many.add(new JSONObject(zs.get(0).toString()).put("party_name","Nama Contoh "+i).put("notes","Keterangan panjang untuk memastikan teks dibungkus di dalam kolom dan tidak menimpa baris lain."));
    check(a.searchRows("zakat_entries",many,"Nama Contoh 179").size()==1,"export search filter");
    check(ExportTable.create(a,"zakat_entries",many,"all").rows.size()==180,"export exceeds UI 150 limit");
    writeExport("Uji-Multipage.pdf",ExportFiles.pdf(a.mosque,ExportTable.create(a,"zakat_entries",many,"all"),"17 November 2026"));
    check(ExportTable.create(a,"zakat_entries",new ArrayList<JSONObject>(),"all").rows.isEmpty(),"empty export");
    StringBuilder longNote=new StringBuilder();for(int i=0;i<350;i++)longNote.append("Catatan sangat panjang ");
    ArrayList<JSONObject> longRows=new ArrayList<>();longRows.add(new JSONObject(zs.get(0).toString()).put("notes",longNote.toString()));
    writeExport("Uji-Baris-Panjang.pdf",ExportFiles.pdf(a.mosque,ExportTable.create(a,"zakat_entries",longRows,"all"),"17 November 2026"));
    checks++;
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
      main(() -> {
        android.widget.EditText password = a.root.findViewWithTag("login-password");
        android.widget.EditText email = a.root.findViewWithTag("login-email");
        check(password != null && email != null, "modern login fields available");
        check(password.getTransformationMethod() != null, "password concealed by default");
        android.view.View eye = ((android.view.ViewGroup)password.getParent()).getChildAt(2);
        eye.performClick();
        check(password.getTransformationMethod() == null, "password visibility toggle");
        eye.performClick();
        check(password.getTransformationMethod() != null, "password conceal toggle");
      });
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
      main(() -> ((android.widget.ScrollView)a.body.getParent()).fullScroll(android.view.View.FOCUS_DOWN));
      shot("beranda-detail");
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
      main(() -> { try { exportRegression(); } catch(Exception e) { throw new RuntimeException(e); } });
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
