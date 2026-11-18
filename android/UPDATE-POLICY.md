# Jalur pembaruan SIMASJID mulai 2.4

Pemilik memilih APK SIMASJID-2.4-Uji.apk sebagai instalasi dasar pada 18 November 2026.

- Identitas paket tetap: `id.simas.mobile.modernpreview`.
- Versi dasar: versionCode 7. Setiap pembaruan harus lebih tinggi daripada APK terakhir.
- Sertifikat SHA-256: `28240dd1adaa15d784db2bbc51cc1e6883c417612c9c825bfafac035bc7bb9f2`.
- Gunakan kunci asli 2.4 dari cadangan pribadi `SIMASJID-2.4-Kunci-Pembaruan-PRIBADI.zip`. Jangan buat kunci baru, jangan unggah arsip ini, keystore, atau kata sandi ke GitHub.
- Sertifikat uji yang sudah terpasang berlaku sampai 18 November 2027. Rencanakan penanganan masa berlaku sebelum tanggal tersebut; jangan mengganti sertifikat secara sepihak.

Gunakan `build-update.py` untuk APK yang diberikan kepada pemilik. Script ini memeriksa paket dan sertifikat APK sebelumnya, menolak nomor versi yang tidak meningkat, membangun dengan paket tetap, dan memeriksa kembali sertifikat hasil sebelum mengeluarkan APK. Kunci tidak pernah dibuat otomatis.

```sh
python3 android/build-update.py --signing-dir /path/private-signing --previous-apk /path/latest-installed.apk --version-code 8 --version-name 2.5 --output /path/SIMASJID-2.5.apk
```

Sesuaikan angka versi dengan rilis berikutnya. Isi UI dan catatan versi juga harus diperbarui sesuai rilis. Unduh dependensi dengan `setup-build.py` jika diperlukan.

`tests/prepare-test.py` hanya untuk pengujian terisolasi dan menghasilkan kunci sementara. APK yang dihasilkan skrip tersebut tidak boleh didistribusikan sebagai pembaruan bagi pemilik. `build.sh` tetap merupakan build unsigned untuk paket produksi lama; bukan jalur pembaruan instalasi 2.4 pilihan pemilik.

Pengguna mengunduh APK pembaruan dan menyetujui tombol Perbarui Android. Ini tidak menambahkan pemeriksaan versi, unduhan otomatis, atau pemasangan diam-diam di aplikasi. Uji pemasangan di atas APK sebelumnya di emulator sebelum merilis pembaruan yang mengubah aplikasi.
