# Pembaruan Distribusi — 10 November 2026

Untuk database SIMAS yang sudah dipasang:
1. Cadangkan database terlebih dahulu.
2. Pastikan migrasi sebelumnya sudah dijalankan: 20261105_zakat_qurban.sql, 20261106_input_updates.sql, dan 20261106_distribution_profile.sql.
3. Buka Supabase SQL Editor. Jalankan HANYA migrations/20261110_distribution_packages.sql satu kali. Jangan jalankan ulang schema.sql pada database yang sudah berisi data.
4. Perbarui kode repositori GitHub dengan isi paket ini, lalu deploy ulang di Vercel. Pertahankan konfigurasi Supabase proyek Anda.
5. Uji tambah distribusi: 25 paket × 2,5 kg harus menghasilkan 62,5 kg. Periksa simpan/edit, impor template baru, dan rekap.

Nama penerima dihapus dari form. Desa/Dusun dan RT/RW dipisah. Nama RT tetap tersedia. Total Berat dihitung otomatis dan digunakan dalam rekap; rekap daging tersalurkan mengikuti status distribusi selesai.

Data historis tidak dihapus. Berat lama dipertahankan sebagai total dengan 1 paket; lengkapi lokasi ketika mengedit data lama. Unduh template Excel baru sebelum impor.

Untuk pemasangan baru, ikuti README.md dan jalankan seluruh migrasi berurutan setelah schema.sql. Paket ini tidak menjalankan SQL di Supabase atau melakukan deploy ke akun Vercel Anda secara otomatis.
