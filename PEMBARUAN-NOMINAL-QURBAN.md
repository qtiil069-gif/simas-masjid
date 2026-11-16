# Nominal Shohibul Qurban

Sapi Pribadi (Sapi Perorangan) dan Kambing Pribadi boleh diimpor dengan nominal kosong atau 0. Jika ada setoran uang, nominal positif juga diterima dan masuk rekap kas. Sapi Rombongan tetap wajib nominal lebih dari 0 per peserta. Nilai hewan yang diserahkan tidak dimasukkan sebagai setoran uang.

## Aktivasi database (sekali)

1. Buka proyek Supabase yang digunakan SIMAS.
2. Masuk **SQL Editor → New query**.
3. Salin seluruh isi `migrations/20261116_qurban_optional_nominal.sql`, lalu **Run**.
4. Setelah deployment Vercel selesai, muat ulang website dan pilih ulang berkas impor. Template lama dengan tiga kolom yang sama masih bisa dipakai; template baru memuat petunjuk terbaru.

Migrasi ini tidak menghapus atau mengubah nominal data lama dan tidak mengubah RLS. File dapat dijalankan ulang. Mengunggah file SQL ke GitHub tidak otomatis menjalankannya di Supabase.

Nominal kosong dikirim sebagai angka 0. Baris identik (tahun, nama, jenis, nominal) tetap ditolak untuk mencegah pencatatan ganda; peringatan duplikat tidak berkaitan dengan kewajiban nominal.

Perubahan ini untuk website. APK 2.0 yang sudah terpasang tetap memakai validasi versi sebelumnya sampai APK diperbarui.
