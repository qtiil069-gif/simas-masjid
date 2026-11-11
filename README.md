# SIMAS — Masjid Sabilillah Margomulyo

Aplikasi pengelolaan masjid dengan login pengurus, keuangan, kegiatan, inventaris, zakat, qurban, distribusi per paket, profil masjid, dan laporan Excel/PDF.

Versi produksi menggunakan Supabase Auth dan database dengan RLS. Tidak tersedia akses demo, data contoh, atau formulir koneksi di halaman login. Konfigurasi Supabase dibaca saat build di Vercel.

Baca **[PANDUAN-DEPLOY.md](PANDUAN-DEPLOY.md)** sebelum deploy. Isi SUPABASE_URL dan SUPABASE_PUBLISHABLE_KEY pada Environment Variables Vercel, kemudian gunakan build command `node build.mjs` dan output `dist`.

## Struktur
- src/: sumber aplikasi.
- dist/: aset web dan bundle aplikasi. Build memperbarui app.js.
- schema.sql: skema awal, hanya untuk database baru kosong.
- migrations/: pembaruan SQL berurutan untuk database yang sudah ada.
- tests/production.cjs: pengujian alur produksi dengan API simulasi.

Database dan akun asli tidak disertakan dalam repositori. Jangan commit password, token, secret key, dump data, atau file .env. Publishable key digunakan di browser; keamanan data bergantung pada Auth, memberships, dan RLS.

## Batas verifikasi
Build, sintaks, login simulasi, penolakan akses, data kosong, penyimpanan melalui REST, logout, sesi kedaluwarsa, dan validasi konfigurasi diuji secara lokal. Deployment Vercel dan login ke Supabase pengguna perlu diverifikasi setelah deployment. Sesi hanya di memori: refresh meminta login ulang.
