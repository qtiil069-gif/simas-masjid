package id.simas.mobile;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import java.io.*;
import java.util.concurrent.*;

/** Displays the exact export bytes, rendering one bounded-size page at a time. */
final class PdfPreview extends Dialog {
  final MainActivity a;
  final byte[] bytes;
  final String filename;
  final ExecutorService renderer = Executors.newSingleThreadExecutor();
  ImageView image;
  TextView status, previous, next, zoomOut, zoomIn, save;
  ScrollView vertical;
  HorizontalScrollView horizontal;
  volatile File cacheFile;
  boolean closed, loading = true;
  int currentPage, pageCount;
  float zoom = 1f;
  Bitmap bitmap;

  PdfPreview(MainActivity activity, byte[] pdf, String name) {
    super(activity, android.R.style.Theme_Material_Light_NoActionBar);
    a = activity;
    bytes = pdf;
    filename = name;
    setOwnerActivity(a);
  }

  void open() {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    LinearLayout layout = a.col();
    layout.setBackgroundColor(a.BG);
    LinearLayout heading = a.row();
    a.pad(heading, 16);
    heading.addView(a.text("Pratinjau PDF", 20, a.INK, true), new LinearLayout.LayoutParams(0, -2, 1));
    heading.addView(a.button("Tutup", false, this::dismiss));
    layout.addView(heading);
    TextView name = a.text(filename, 11, a.MUTED, false);
    name.setPadding(a.dp(16), 0, a.dp(16), a.dp(8));
    layout.addView(name);
    status = a.text("Menyiapkan pratinjau…", 12, a.MUTED, false);
    status.setGravity(Gravity.CENTER);
    status.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);
    layout.addView(status, new LinearLayout.LayoutParams(-1, -2));
    LinearLayout controls = a.row();
    a.pad(controls, 8);
    previous = a.button("‹", false, () -> renderPage(currentPage - 1));
    next = a.button("›", false, () -> renderPage(currentPage + 1));
    zoomOut = a.button("−", false, () -> changeZoom(-0.5f));
    zoomIn = a.button("+", false, () -> changeZoom(0.5f));
    previous.setContentDescription("Halaman sebelumnya");
    next.setContentDescription("Halaman berikutnya");
    zoomOut.setContentDescription("Perkecil PDF");
    zoomIn.setContentDescription("Perbesar PDF");
    for (TextView control : new TextView[]{previous, zoomOut, zoomIn, next}) {
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
      lp.setMargins(a.dp(3), 0, a.dp(3), 0);
      controls.addView(control, lp);
    }
    layout.addView(controls);
    vertical = new ScrollView(a);
    horizontal = new HorizontalScrollView(a);
    image = new ImageView(a);
    image.setAdjustViewBounds(false);
    image.setScaleType(ImageView.ScaleType.FIT_XY);
    image.setBackgroundColor(Color.WHITE);
    horizontal.addView(image, new HorizontalScrollView.LayoutParams(1, 1));
    vertical.addView(horizontal);
    layout.addView(vertical, new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout footer = a.col();
    a.pad(footer, 12);
    save = a.button("Simpan PDF", true, () -> {
      if (!loading && !closed && bitmap != null) {
        a.exportBytes = bytes;
        a.exportName = filename;
        a.saveExport("application/pdf");
      }
    });
    footer.addView(save, new LinearLayout.LayoutParams(-1, -2));
    layout.addView(footer);
    setContentView(layout);
    Window w = getWindow();
    w.setStatusBarColor(a.BG);
    w.setNavigationBarColor(a.BG);
    int flags = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
    if (android.os.Build.VERSION.SDK_INT >= 26) flags |= android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    w.getDecorView().setSystemUiVisibility(flags);
    if (android.os.Build.VERSION.SDK_INT >= 35) layout.setOnApplyWindowInsetsListener((v, in) -> {
      android.graphics.Insets bars = in.getInsets(android.view.WindowInsets.Type.systemBars());
      v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
      return in;
    });
    show();
    w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
    renderer.execute(() -> {
      try {
        cacheFile = File.createTempFile("simas-preview-", ".pdf", a.getCacheDir());
        try (OutputStream out = new FileOutputStream(cacheFile)) { out.write(bytes); }
      } catch (Exception e) { a.runOnUiThread(() -> failed()); }
    });
    renderPage(0);
  }

  void changeZoom(float delta) {
    if (loading || closed) return;
    zoom = Math.max(1f, Math.min(3f, zoom + delta));
    renderPage(currentPage);
  }

  void buttons() {
    previous.setEnabled(!loading && currentPage > 0);
    next.setEnabled(!loading && currentPage + 1 < pageCount);
    zoomOut.setEnabled(!loading && zoom > 1f);
    zoomIn.setEnabled(!loading && zoom < 3f);
    save.setEnabled(!loading && bitmap != null);
    for (TextView v : new TextView[]{previous, next, zoomOut, zoomIn, save}) v.setAlpha(v.isEnabled() ? 1f : 0.4f);
  }

  void renderPage(int index) {
    if (closed || index < 0 || (pageCount > 0 && index >= pageCount)) return;
    loading = true;
    buttons();
    status.setText("Memuat halaman " + (index + 1) + "…");
    final int width = Math.max(240, a.getResources().getDisplayMetrics().widthPixels);
    final float selectedZoom = zoom;
    renderer.execute(() -> {
      try (ParcelFileDescriptor fd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY);
           PdfRenderer pdf = new PdfRenderer(fd);
           PdfRenderer.Page page = pdf.openPage(index)) {
        int count = pdf.getPageCount();
        float scale = width * selectedZoom / page.getWidth();
        scale = Math.min(scale, (float)Math.sqrt(4000000d / ((double)page.getWidth() * page.getHeight())));
        int bw = Math.max(1, (int)(page.getWidth() * scale));
        int bh = Math.max(1, (int)(page.getHeight() * scale));
        Bitmap result = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
        result.eraseColor(Color.WHITE);
        page.render(result, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        a.runOnUiThread(() -> {
          if (closed) { result.recycle(); return; }
          bitmap = result;
          image.setImageBitmap(result);
          image.setLayoutParams(new HorizontalScrollView.LayoutParams(bw, bh));
          image.setContentDescription("Halaman " + (index + 1) + " dari " + count);
          horizontal.scrollTo(0, 0);
          vertical.scrollTo(0, 0);
          currentPage = index;
          pageCount = count;
          loading = false;
          status.setText("Halaman " + (index + 1) + " / " + count + "  ·  " + Math.round(selectedZoom * 100) + "%");
          buttons();
        });
      } catch (Exception | OutOfMemoryError e) { a.runOnUiThread(() -> failed()); }
    });
  }

  void failed() {
    if (closed) return;
    loading = false;
    bitmap = null;
    image.setImageDrawable(null);
    status.setText("Pratinjau belum dapat dibuka. Tutup lalu coba ekspor kembali.");
    buttons();
  }

  @Override public void dismiss() {
    if (!closed) {
      closed = true;
      if (image != null) image.setImageDrawable(null);
      bitmap = null;
      if (a.pdfPreview == this) a.pdfPreview = null;
      renderer.execute(() -> { if (cacheFile != null) cacheFile.delete(); });
      renderer.shutdown();
    }
    super.dismiss();
  }
}
