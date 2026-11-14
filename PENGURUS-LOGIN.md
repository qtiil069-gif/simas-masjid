# Pengurus masjid dan akun login

1. Jalankan migrations/20261114_pengurus.sql di Supabase SQL Editor setelah schema awal. Data yang sudah ada tetap tersedia.
2. Pada Vercel → Project → Settings → Environment Variables, tambahkan `SUPABASE_SECRET_KEY` berisi secret API key Supabase (`sb_secret_...`) untuk project yang sama dengan `SUPABASE_URL`. Jangan masukkan secret ke SUPABASE_PUBLISHABLE_KEY, source, atau browser. Redeploy.
3. Login sebagai admin → Pengurus → Tambah pengurus. Isi nama, jabatan, telepon dan alamat; simpan.
4. Pilih Buat akun login pada baris pengurus. Isi email baru, kata sandi minimal 12 karakter, dan peran. Berikan kredensial langsung kepada pemilik akun. Tidak ada email yang dikirim.

Data pengurus boleh tanpa akun. Daftar akses lama tetap tersedia untuk mengelola peran. Menghapus biodata tidak mencabut akses; hapus akses di daftar Akses login yang terdaftar jika diperlukan. Email yang sudah digunakan akun lain tidak ditimpa. Akun lama tidak otomatis dipadankan dengan nama pengurus.

Jika pembuatan akun terputus setelah Auth berhasil, ulangi dengan email sama. Server hanya memulihkan akun dengan identitas pengurus dan masjid yang sama; kata sandi dari percobaan pertama tetap berlaku. Kunci rahasia hanya dibaca oleh Vercel Function, tidak oleh build frontend.
