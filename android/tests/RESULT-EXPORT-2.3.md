# Verifikasi ekspor SIMASJID 2.3 — 17 November 2026

APK SHA256: dcd5ce69444f379ba35f1783f2ee5c7d2fcf04bb4e0c11e2fa78add55017c5d1

- Kompilasi aplikasi dan instrumentasi berhasil.
- Pemasangan aplikasi/instrumentasi Android 10 berhasil.
- Hasil runtime langsung: PASS 39 native checks; runner exit 0.
- Log AndroidRuntime:E tanpa exception tercatat.
- apksigner verify v1/v2/v3 dan zipalign berhasil.
- Pengujian ekspor: kolom Zakat, saldo kg/rupiah, jumlah catatan pivot, nama shohibul dari relasi, rincian bungkus campuran, total bungkus/kg, kolom penerima, pemisahan RT/RW, kategori kas, setoran otomatis shohibul, pencarian, 180 baris, data kosong, baris sangat panjang.
- openpyxl membuka ketiga XLSX fixture, memverifikasi angka dan saldo, string berawalan '=' tetap teks, tanpa sel formula. Pivot memiliki Laporan dan Data Sumber. Library memberi peringatan style default (fallback), bukan kerusakan workbook.
- PyMuPDF membuka tujuh PDF fixture; laporan 180 baris menghasilkan 25 halaman dengan nama terakhir tetap ada; satu baris sangat panjang menghasilkan 11 halaman. Seluruh halaman memiliki footer dan nomor halaman.
- Tampilan halaman pertama PDF Zakat, Distribusi, dan Pivot diperiksa secara visual.

Semua data fixture lokal; tidak login ke produksi atau melakukan CRUD produksi. Pemilih lokasi simpan, Excel Microsoft pada HP pengguna, seluruh versi Android, serta kesamaan piksel dengan cetak browser belum diuji. Batas cakupan fitur dicatat dalam EXPORT-2.3.md.
