# SIMASJID 2.2

Nama tampilan aplikasi, login, dialog, footer, dan kop ekspor diganti menjadi SIMASJID. Logo berasal dari tautan pemilik https://chatgpt.com/s/m_6afbdee7b7d481919be93caab12e78b2 lalu tulisan SIMAS dihapus sesuai arahan. Asset PNG 256x256 dipakai pada launcher dan login.

versionCode 5. Paket produksi tetap id.simas.mobile dan build produksi unsigned untuk ditandatangani dengan kunci lama. APK uji baru memakai id.simas.mobile.preview, label SIMASJID, serta kunci uji tersendiri; dipasang berdampingan dengan aplikasi sebelumnya. Backend tetap sama dan perubahan data memengaruhi data asli.

Verifikasi versi ini: kompilasi aplikasi dan instrumentasi berhasil, metadata paket/label/ikon benar, apksigner verify v1/v2/v3 berhasil, zipalign berhasil. Pengujian emulator 22 pemeriksaan dari versi 2.1 tidak dijalankan ulang untuk perubahan branding ini. Login akun produksi belum diuji.
