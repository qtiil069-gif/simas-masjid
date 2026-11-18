# SIMASJID 2.4 — tampilan Android

18 November 2026. Login dan beranda diperbarui dengan warna hijau zamrud, logo tanpa tulisan, kartu formulir, tombol tampil/sembunyikan sandi, bantuan akun, kartu saldo, menu empat kolom, ringkasan Zakat/Qurban, transaksi terbaru, dan navigasi bawah dengan penanda halaman aktif. Layar tetap menggunakan Android framework, tanpa membuka browser.

Sumber tampilan terpusat di `app/src/id/simas/mobile/ModernUi.java`. Autentikasi, pemeriksaan keanggotaan, hak akses, database, dan ekspor PDF/XLSX versi 2.3 tetap menggunakan implementasi yang sama. Ringkasan mengikuti tahun terpilih; angka Qurban hanya menghitung penyerahan yang tercatat. Data yang gagal dimuat ditandai.

Build produksi: paket `id.simas.mobile`, versionCode 7, versionName 2.4-native. `build.sh` menghasilkan APK unsigned; pembaruan instalasi produksi harus memakai kunci penandatangan asli pemilik. Kunci tidak disimpan dalam repositori.

APK uji: `id.simas.mobile.modernpreview`, label SIMASJID. Dipasang terpisah dari versi sebelumnya karena memakai sertifikat uji. Login memakai akun yang sama dengan website. Setelah login, perubahan data memengaruhi database masjid yang sama.

Hasil pengujian final: 44 pemeriksaan lulus pada emulator Android 10, termasuk kontras bilah status setelah perpindahan dari login. APK bertanda tangan v1/v2/v3 dan zip alignment valid. Tidak ada exception aplikasi pada log AndroidRuntime.

Pengujian menggunakan emulator Android 10 dengan data contoh dan jaringan dimatikan. Uji meliputi render login/beranda/delapan modul, tampil/sembunyikan sandi, validasi, pembatasan UI berdasarkan peran, perhitungan, serta regresi PDF/XLSX. Screenshot menunjukkan data contoh. Login produksi, CRUD jaringan, dan perangkat fisik tidak termasuk pengujian emulator.

Lihat `EXPORT-2.3.md` untuk cakupan ekspor dan keterbatasan fitur. Dokumen versi sebelumnya disimpan sebagai riwayat.
