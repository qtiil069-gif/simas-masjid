# SIMAS — Zakat, Qurban, dan Laporan

Panduan GitHub, Vercel, dan Supabase: [PANDUAN-DEPLOY.md](PANDUAN-DEPLOY.md).

Website statis berbahasa Indonesia dengan demo lokal dan koneksi Supabase Auth / REST berizin RLS. Menu Pengumuman telah dihapus dari navigasi dan dashboard; tabel pengumuman lama tidak dihapus.

## Menjalankan

- Jalankan `node build.mjs` setelah mengubah `src/core.js` atau `src/modules.js`.
- Sajikan direktori `dist` melalui HTTP/HTTPS.
- `dist/app.js` adalah gabungan sumber. `dist/export.js` menulis XLSX asli (OOXML ZIP) tanpa formula dari masukan pengguna.

## Database

Untuk proyek baru: pasang `schema.sql` dahulu dan siapkan akun Auth, masjid, serta membership admin. Untuk proyek dengan skema awal SIMAS: jangan jalankan schema awal ulang.

Jalankan **sekali** `migrations/20261105_zakat_qurban.sql` pada proyek yang sudah memiliki skema awal. Salinan unduhan tersedia pada Pengaturan & koneksi. Migrasi bersifat tambahan, tidak menghapus data lama. Migrasi belum dijalankan pada Supabase pengguna.

Isi URL Supabase dan publishable key pada Pengaturan & koneksi, kemudian login dengan akun anggota masjid. Konfigurasi publik tersimpan lokal; sesi/token hanya di memori halaman. Tidak memakai service-role key. Muat ulang halaman memerlukan login lagi; saat token kedaluwarsa login ulang diperlukan.

Hak akses modul baru:
- admin/bendahara: baca, tambah, koreksi; tanpa hapus ledger.
- takmir: baca/ekspor.
- editor/viewer/anon: tidak dapat membaca data jamaah modul ini.

## Cakupan

- Zakat: master muzakki/mustahik, 8 asnaf, penerimaan/penyaluran fitrah dan mal. Uang terpisah dari beras. Zakat mal pada versi ini dicatat sebagai uang. Tarif/kewajiban zakat tidak dihitung otomatis.
- Qurban: peserta, hewan, bagian per hewan, kas, kupon unik per masjid/tahun, cetak kupon, konfirmasi pengambilan. Bagian maksimal 7 untuk sapi/kerbau dan 1 kambing/domba. Harga hewan bukan pengeluaran otomatis: pembayaran dicatat dalam kas.
- Laporan: filter tahun Masehi, XLSX, cetak/PDF browser. Master orang tidak difilter tahun. Kas qurban difilter tahun kegiatan; zakat berdasarkan tanggal transaksi.
- Dashboard: hitungan setelah simpan; saat terhubung, polling modul baru setiap 30 detik ketika tab aktif. Ini bukan Supabase Realtime/WebSocket.
- Semua perubahan modul diaudit oleh trigger SQL. Nominal tidak boleh negatif; transaksi zakat nol ditolak. Kekurangan stok/dana tampil sebagai peringatan, bukan kalkulasi kelayakan agama atau penolakan penyaluran.
- Database demo hanya untuk latihan, tidak dimigrasikan otomatis ke database asli.

## Verifikasi

- `node --check dist/app.js` dan `node --check dist/export.js`.
- `tests/database.mjs`: skema awal + migrasi dijalankan pada PostgreSQL terisolasi melalui PGlite, 20 pemeriksaan RLS, relasi lintas masjid, batas kapasitas, kupon ganda, pengambilan idempoten, audit, dan larangan hapus. Jalankan dengan `PGLITE_MODULE=<absolute-path-to-pglite-index.js> node tests/database.mjs` atau pasang paket PGlite pada lingkungan uji.
- XLSX dibuka menggunakan openpyxl: angka dan teks formula-like tetap sesuai tipe, karakter XML aman.
- Browser: navigasi, validasi transaksi nol, penyimpanan beras dan rekap, konfirmasi kupon dan berat tersalur, ekspor Excel.
- Belum diuji: proyek Supabase asli, transaksi serentak lintas koneksi produksi, perangkat/printer fisik. Tinjau dan uji akun beberapa role pada staging sebelum operasi nyata.

## Pembaruan tampilan berdasarkan video referensi

Navigasi atas dengan indikator pil yang bergeser, rail ikon charcoal dengan tombol tambah kuning, latar mint/biru/ivory, panel transparan lembut. Transisi halaman memakai Web Animations API dengan fade, pergeseran vertikal dan stagger. `prefers-reduced-motion` menonaktifkan animasi. Di HP rail menjadi dock bawah dan menu atas dapat digeser.

Menu Kegiatan memiliki kalender bulan/minggu, navigasi periode, filter publikasi, detail agenda, serta tambah langsung dari tanggal. Tampilan Daftar mempertahankan pencarian/edit/hapus sebelumnya. Pencarian menu tersedia dari ikon kaca pembesar atau Ctrl/Cmd+K. Tombol tambah cepat mematuhi hak akses yang sudah ada.

Sumber UI: `src/ui.js`; gaya: `dist/ui.css`. `node build.mjs` menggabungkan tiga berkas JS sumber. Tidak ada perubahan skema/aturan akses database pada pembaruan UI ini.

Verifikasi pembaruan UI: navigasi dan pil aktif, pencarian submenu Distribusi & Kupon, tambah agenda dari kalender dan pemunculan pada tanggalnya, responsif pada frame 390 px dengan scrollWidth sama dengan clientWidth, pemeriksaan sintaks JS. Supabase asli tetap belum diuji pada sesi ini.

## Input updates — 6 November 2026
Apply `migrations/20261106_input_updates.sql` after the existing module migration. The update adds RT, participant category, year and nominal with existing RLS and audit coverage. It has NOT been executed against the user's Supabase project.

Qurban input is centralized on participants. Animal, participant-per-animal and cash tabs are derived read-only reports. Sapi Patungan groups follow registration order, max 7 people; private animals have one owner. Changing year/category can regroup participants. Generated groups do not confirm purchases or meat stock. Sapi Pribadi is in-kind (zero cash); other categories require the amount already received. Editing the amount replaces the derived receipt instead of duplicating it. Legacy records remain visible and included; old participants are not automatically converted. SQL prevents duplicate manual receipts for configured participants.

Zakat imports append new records from sheet Data in the downloaded template, max 1000 rows/2 MB. The preview validates RT, party references, directions, amounts, dates, formulas and obvious duplicates. A live import uses one transactional bulk insert. If network outcome is uncertain, inspect records before retrying. PDF export uses browser Print / Save as PDF. The Excel reader is local SheetJS CE 0.18.5, Apache 2.0 (license in dist/vendor).

Finance custom categories are reusable; a newly saved category remains if the subsequent transaction fails.

## Distribution, deletion and mosque identity
Apply `20261106_distribution_profile.sql` AFTER both prior module migrations. It adds distribution RT, a validated PNG/JPEG/WebP data URL for the mosque icon, and delete policies matching edit roles. Deletes are permanent with confirmation. Foreign keys block linked master record deletion; audit snapshots retain deleted values. Own membership / the last admin are protected at DB and UI levels. Derived qurban records follow participant edits/deletion rather than being independently deleted.

Distribution form/template: year, recipient, RT name, hamlet RT/RW, weight. Coupon ID is automatic. Imports append via one atomic bulk request with validation preview. Profile persists to Supabase for admins (separate demo storage in demo mode). Name/address appear in all report sheets and PDFs; icon appears on PDF letterhead and the mosque badge. Import templates intentionally retain machine-readable headers. Core finance, activities, inventory and membership pages export Excel/PDF too. Migration is supplied for the user to apply; it is not executed on their live Supabase.

### Dashboard keuangan dan transisi (7 November 2026)
Dashboard menampilkan kas umum sepanjang waktu, disusul ringkasan zakat dan qurban tahun berjalan. Dua grafik tahunan mempunyai filter terpisah (2000–2200, maksimal 20 tahun inklusif); pilihan bertahan selama sesi navigasi/sinkronisasi. Zakat menggunakan tahun tanggal transaksi, qurban menggunakan tahun kegiatan dan termasuk setoran otomatis peserta. Nilai beras/daging tidak digabung dengan rupiah. Tabel rincian menyediakan nilai persis dan akses pembaca layar. Tidak diperlukan migrasi SQL tambahan.
Transisi memakai opacity/transform pada blok tingkat atas saja; animasi sebelumnya dibatalkan pada navigasi cepat, tab modul turut bertransisi, dan prefers-reduced-motion dihormati. Header tetap di tempat.
Validasi: tests/dashboard.cjs mencakup batas tahun, pemisahan dana, penerimaan otomatis, filter independen/persistensi, izin tampilan, pembatalan animasi, dan reduced motion.

### Informasi Zakat dan Qurban
Dashboard dan halaman modul memakai ringkasan yang sama: dana, beras diterima/disalurkan/sisa, hewan terdaftar per kategori, dan daging yang telah dikonfirmasi diserahkan. Grafik menyediakan pilihan Dana/Beras untuk zakat dan Dana/Hewan/Daging untuk qurban, masing-masing memakai satuan terpisah dan filter rentang tahun. Ringkasan modul mengikuti tahun kegiatan yang dipilih, grafik memakai rentang tahun eksplisit. Sapi patungan dihitung per kelompok maksimal tujuh peserta, termasuk kelompok belum penuh; arsip hewan lama tetap dihitung. Angka hewan bukan bukti pembelian atau penyembelihan. Tidak ada migrasi SQL baru.

### Halaman login
Halaman masuk tampil saat aplikasi dibuka. Email/kata sandi menggunakan Supabase Auth dan keanggotaan masjid yang sudah ada; demo dibuka secara eksplisit. Pengaturan koneksi hanya menerima URL Supabase HTTPS dan publishable key. Kata sandi dibersihkan setelah percobaan login, token sesi tetap di memori (muat ulang meminta masuk kembali), tanpa penyimpanan password/token di localStorage. Animasi menghormati prefers-reduced-motion. Pengujian login memakai respons simulasi untuk kredensial salah, keanggotaan ditolak, berhasil masuk, dan logout; akun Supabase produksi belum diuji.

### Distribusi per paket (10 November 2026)
Form distribusi: Tahun, Nama RT, Desa/Dusun, RT/RW, Jumlah Paket, Berat per Paket, Total Berat otomatis. weight_kg tetap menyimpan total (jumlah × berat per paket), sehingga semua rekap/grafik memakai total yang sama. Impor/ekspor/kupon sudah mengikuti kolom baru; unduh ulang template Excel. Total di template boleh kosong atau harus sesuai hasil hitung. Data lama disimpan sebagai satu paket seberat total lama; nama penerima dan alamat lama tetap tersimpan sebagai arsip. Lengkapi lokasi terpisah ketika mengedit arsip.
Untuk Supabase jalankan migrations/20261110_distribution_packages.sql setelah tiga migrasi sebelumnya. Trigger menghitung ulang total di server, termasuk saat pembaruan; migrasi belum dijalankan pada database pengguna. Deploy kode terbaru ke Vercel sesudah migrasi. Jangan menjalankan ulang schema.sql pada database yang sudah ada.
