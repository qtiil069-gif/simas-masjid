# Distribusi Shohibul: dua ukuran bungkus

Website menyediakan alokasi awal berikut, yang dapat dikoreksi sebelum disimpan:

| Jenis | Bungkus utama | Bungkus tambahan | Total bungkus | Total kg |
|---|---|---|---|---|
| Sapi Rombongan | 1 × 1 kg | 7 × 0,5 kg | 8 | 4,5 |
| Kambing Pribadi | 1 × 1 kg | 7 × 0,5 kg | 8 | 4,5 |
| Sapi Pribadi | 1 × 2 kg | 40 × 0,5 kg | 41 | 22 |

## Pembaruan Supabase (sekali saja)

1. Buka berkas `migrations/20261116_distribution_mixed_packages.sql` di GitHub atau unduh dari `/downloads/20261116_distribution_mixed_packages.sql` pada website SIMAS.
2. Salin seluruh isinya. Buka proyek Supabase yang digunakan SIMAS → SQL Editor → New query.
3. Tempel SQL lalu klik Run. Setelah berhasil, muat ulang website.

Prasyarat: migrasi `20261115_distribution_recipients.sql` telah dijalankan (fitur pemilihan Shohibul/Masyarakat sudah berfungsi). SQL baru dapat dijalankan ulang, tidak mengubah jumlah bungkus atau berat data lama, dan mempertahankan RLS serta hak akses. Jangan menjalankan ulang migrasi lama setelah migrasi ini.

## Input dan impor

Buka Qurban → Distribusi → Tambah data → Shohibul Qurban → pilih nama. Komposisi otomatis mengikuti jenis qurban. Saat mengedit arsip lama, komposisi sebelumnya dipertahankan; tombol **Gunakan alokasi sesuai jenis qurban** dapat dipakai untuk menerapkan ketentuan baru secara sengaja.

Unduh ulang template Shohibul. Isi Nama dan Jenis Qurban sesuai sheet Referensi Shohibul. Empat kolom komposisi boleh dikosongkan seluruhnya untuk menggunakan alokasi otomatis saat impor, atau isi keempatnya untuk alokasi khusus. Total Bungkus dan Total Berat boleh kosong dan dihitung saat impor. Tahun kosong mengikuti tahun filter. ID Shohibul digunakan jika nama dan jenis peserta sama lebih dari satu.

Template lama tetap dapat dibaca sebagai alokasi satu ukuran. Template masyarakat tetap menggunakan alokasi × berat per bungkus. Rincian campuran dan total tampil di tabel, ekspor dan kupon; grafik dan laporan menggunakan total berat tersimpan. Tidak ada konversi otomatis atas data lama.

Pembaruan ini untuk website. APK native memerlukan build pembaruan tersendiri.
