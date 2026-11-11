# Deploy SIMAS dari GitHub

## Vercel
1. Masuk ke Vercel, pilih Add New > Project, lalu impor repositori qtiil069-gif/simas-masjid.
2. Izinkan Vercel mengakses repositori privat ini jika belum tersedia.
3. Gunakan root proyek (jangan memilih folder src atau dist).
4. Framework: Other. Build Command: node build.mjs. Output Directory: dist. Tidak ada dependensi npm yang perlu diinstal.
5. Klik Deploy. Konfigurasi disediakan dalam vercel.json.
6. Setelah siap, buka alamat Vercel. Pada halaman login buka Pengaturan koneksi.
7. Isi Project URL Supabase tanpa /rest/v1/ dan publishable key sb_publishable_. Simpan lalu login dengan akun Supabase Authentication yang mempunyai memberships.

Konfigurasi Supabase versi ini disimpan pada browser dan asal URL. Membuka domain Vercel baru atau perangkat lain memerlukan pengisian ulang. Environment variables Vercel belum dibaca oleh aplikasi ini.

Setelah integrasi Git aktif, perubahan pada branch produksi main memicu deployment Vercel. Perubahan yang dibuat hanya pada Sites tidak otomatis disinkronkan ke GitHub; perubahan harus dikirim ke repositori ini terlebih dahulu.

## Database Supabase yang sudah ada
Jangan jalankan ulang schema.sql. Cadangkan data dan cocokkan migrasi yang sudah diterapkan. Jalankan hanya migrasi yang belum diterapkan melalui SQL Editor, berurutan:
- migrations/20261105_zakat_qurban.sql
- migrations/20261106_input_updates.sql
- migrations/20261106_distribution_profile.sql
- migrations/20261110_distribution_packages.sql

Untuk database baru kosong saja, jalankan schema.sql sebelum keempat migrasi tersebut.

Akun login harus dibuat di Authentication > Users. Tabel mosques harus memiliki data masjid. Tabel memberships menghubungkan user_id (UID Auth), mosque_id (id masjid), dan role admin. Jangan menonaktifkan RLS untuk memperbaiki login.

## Batas otomatisasi
Repositori ini belum mengaktifkan deploy migrasi otomatis ke Supabase. SQL yang sebelumnya dijalankan manual perlu direkonsiliasi dengan riwayat migrasi sebelum menambahkan Supabase CLI atau integrasi GitHub. Menghubungkan repositori saja tidak menjalankan SQL ini.

Jangan memasukkan password, service_role, secret key, dump database, atau data jamaah asli ke repositori. Publishable key diisi di aplikasi; tidak dibundel dalam kode.

## Verifikasi
- Login dan periksa status Supabase terhubung.
- Simpan satu data uji dan cocokkan dengan tabel Supabase.
- Uji distribusi: 25 paket x 2,5 kg = 62,5 kg.
- Uji ekspor dan impor dengan template terbaru.
