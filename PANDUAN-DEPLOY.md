# Deploy SIMAS produksi ke Vercel

Versi produksi tidak menyediakan demo atau formulir koneksi database. Pengurus cukup masuk dengan email dan kata sandi. Koneksi ditetapkan saat build.

## 1. Siapkan Supabase
Gunakan proyek Supabase yang sudah Anda buat. Pastikan akun Authentication memiliki satu baris memberships dengan user_id yang benar, mosque_id dari tabel mosques, dan role yang sesuai (admin untuk pengelola utama).
Jangan jalankan ulang schema.sql pada database yang sudah berisi tabel/data. Jalankan hanya migrasi yang belum diterapkan, sesuai urutan:
1. migrations/20261105_zakat_qurban.sql
2. migrations/20261106_input_updates.sql
3. migrations/20261106_distribution_profile.sql
4. migrations/20261110_distribution_packages.sql
Untuk database baru kosong saja, jalankan schema.sql sebelum migrasi tersebut. Pertahankan RLS.

## 2. Impor GitHub di Vercel
Impor qtiil069-gif/simas-masjid, branch main. Pilih root ./, preset Other, Build Command node build.mjs, Output Directory dist. Install Command tetap kosong sesuai vercel.json.

Sebelum klik Deploy, buka Environment Variables dan isi:

| Nama | Nilai |
| --- | --- |
| SUPABASE_URL | URL utama proyek, misalnya https://ID-PROYEK.supabase.co, tanpa /rest/v1/ |
| SUPABASE_PUBLISHABLE_KEY | Publishable key proyek yang sama, berawalan sb_publishable_ |

Gunakan environment Production. Jika ingin menguji Preview, isi konfigurasi Preview dengan proyek pengujian yang sesuai. Jangan masukkan password, secret key, atau service_role. Publishable key disertakan pada frontend saat build; RLS dan keanggotaan membatasi akses data.

Klik Deploy. Build akan gagal dengan pesan konfigurasi jika variabel hilang atau formatnya salah. Setelah mengubah variabel, lakukan Redeploy supaya build membaca nilai baru.

## 3. Login dan verifikasi
Buka alamat Vercel. Halaman login menampilkan identitas Masjid Sabilillah Margomulyo, email, dan kata sandi. Tidak perlu mengisi konfigurasi per perangkat.
- Masuk dengan akun Authentication yang sudah memiliki memberships.
- Periksa status Terhubung dan profil masjid.
- Tambahkan satu data inventaris, cocokkan dengan tabel assets di Supabase.
- Uji distribusi: 25 paket x 2,5 kg = 62,5 kg.
- Uji logout, login kembali, dan akun dengan peran terbatas.
- Database kosong menampilkan nilai nol/daftar kosong, tanpa data contoh.

Sesi disimpan di memori halaman. Refresh meminta login kembali; sesi kedaluwarsa juga meminta login. Password/token tidak ditulis ke localStorage. Koneksi lama yang tersimpan di browser diabaikan.

## 4. Pembaruan berikutnya
Push ke main memicu deployment setelah integrasi Git Vercel aktif. Perubahan hanya di Sites tidak otomatis masuk ke repositori ini.
Migrasi database belum otomatis. Riwayat SQL yang dijalankan manual harus dicocokkan sebelum mengaktifkan CI migrasi; jangan menjalankan ulang skema awal.

## Pengujian pengembangan
Gunakan Node.js 22 atau lebih baru. Pasang dependency pengujian dengan npm install --no-save jsdom@26, lalu jalankan node tests/production.cjs. Test menggunakan respons simulasi, tidak login ke proyek Supabase asli.
Build: set dua environment variables di atas lalu jalankan node build.mjs. Sajikan dist melalui HTTP. dist/app.js di GitHub merupakan hasil tanpa konfigurasi aktif; build wajib dilakukan agar koneksi terpasang. Jangan commit output build yang berisi konfigurasi proyek Anda.
