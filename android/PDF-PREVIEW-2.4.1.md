# SIMASJID 2.4.1 — pratinjau PDF

Menu ekspor Android sekarang menampilkan PDF di dalam aplikasi sebelum meminta lokasi penyimpanan. Berlaku untuk data lengkap dan rekap pivot, karena keduanya memakai jalur ekspor yang sama.

- Pratinjau merender byte PDF final dengan Android PdfRenderer. Berkas yang disimpan sama dengan yang ditampilkan.
- Tombol halaman sebelumnya/berikutnya, perbesar/perkecil (100–300%), Simpan PDF, dan Tutup.
- Hanya satu halaman dirender pada satu waktu; bitmap dibatasi empat juta piksel untuk membatasi memori.
- Menekan Simpan PDF membuka pemilih lokasi Android. Membatalkan pemilih tetap mempertahankan pratinjau sehingga pengguna dapat mencoba lagi.
- File pratinjau berada di cache privat dan dihapus saat dialog ditutup. Menutup pratinjau tidak menyimpan laporan ke folder dokumen pengguna.
- Ekspor Excel tetap langsung menuju pemilih lokasi simpan.

Paket pembaruan: id.simas.mobile.modernpreview, versionCode 8, versionName 2.4.1. Ditandatangani dengan kunci asli 2.4; gunakan build-update.py dan UPDATE-POLICY.md.

Uji tambahan memeriksa rendering PDF, navigasi, zoom, tidak adanya pemilih simpan sebelum tombol ditekan, pembatalan simpan, kesamaan byte hasil simpan, dan penghapusan cache. Pengujian hasil simpan memasukkan URI lokal melalui callback hasil pemilih; pemilihan folder manual di perangkat pengguna tetap perlu dicoba. Instrumentasi memakai data contoh dan jaringan dimatikan.

Verifikasi final: 57 pemeriksaan instrumentasi lulus pada Android 10. Pemasangan 2.4 (kode 7) lalu pembaruan 2.4.1 (kode 8) berhasil tanpa uninstall, dengan UID Android tetap sama. Tidak ada exception aplikasi dalam log AndroidRuntime. Belum diuji pada HP fisik pengguna.
