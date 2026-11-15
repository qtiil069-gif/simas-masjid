# Distribusi Qurban dan tahun Hijriyah

Jalankan `migrations/20261115_distribution_recipients.sql` di Supabase SQL Editor setelah migrasi distribusi paket sebelumnya. Skrip dapat dijalankan ulang. Data lama tetap Penerima Qurban; paket dan total berat tidak diubah.

Pada Qurban → Distribusi, pilih Shohibul Qurban, Penerima Qurban, atau Keduanya. Daftar dan ekspor menampilkan nomor urut serta total paket dan kg di bagian bawah. Total mengikuti pencarian, jenis penerima dan tahun. Total daftar menghitung semua alokasi yang tampil; informasi dashboard daging tersalurkan tetap hanya yang telah ditandai diambil.

Tambah distribusi: pilih jenis penerima. Shohibul memilih nama dari peserta pada tahun kegiatan yang sama. Penerima umum mengisi Desa/Dusun, RW/RT dan Nama RT. Isi jumlah paket, berat per paket dan catatan. Total kg dihitung otomatis. Data peserta tidak otomatis berarti daging telah dialokasikan; buat catatan distribusinya dahulu.

Unduh template Distribusi terbaru untuk impor campuran. Untuk Shohibul, salin ID dari sheet Referensi Shohibul. Template lama penerima umum masih diterima. Penghapusan peserta yang telah terkait distribusi dibatasi database; koreksi/hapus distribusinya terlebih dahulu bila memang diperlukan.

Tahun tetap disimpan dan difilter sebagai Masehi. Tahun Hijriyah sipil ditampilkan sebagai rentang pendamping pada menu, formulir tahun, laporan dan kop ekspor; contoh 2026 M / 1447–1448 H. Rentang dihitung dari 1 Januari sampai 31 Desember, bukan tahun khusus Idul Adha dan bukan penetapan tanggal hari raya. Tidak ada perubahan tanggal atau tahun pada data lama.

APK WebView cukup dimuat ulang setelah deployment; tidak perlu memasang APK baru.
