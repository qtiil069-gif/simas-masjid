PEMBARUAN 2.4.1 — PRATINJAU PDF
Ekspor PDF menampilkan pratinjau di dalam aplikasi sebelum tombol Simpan PDF membuka pemilih lokasi Android. Lihat PDF-PREVIEW-2.4.1.md. APK ini memperbarui instalasi 2.4 dengan paket dan kunci yang sama.

JALUR PEMBARUAN MULAI 2.4
Pemilik memilih instalasi 2.4 sebagai versi dasar. Gunakan build-update.py dan UPDATE-POLICY.md untuk semua APK pembaruan; jangan bagikan keluaran prepare-test.py sebagai pembaruan. Kunci asli disimpan dalam cadangan pribadi di luar GitHub.

PEMBARUAN 2.4 — LOGIN DAN DASHBOARD
Lihat UI-2.4.md untuk tampilan terbaru, build dan pemasangan APK uji. Panduan versi 2.0 di bawah adalah riwayat; paket, versi, dan penandatangan versi terbaru mengikuti UI-2.4.md.

PEMBARUAN 2.3 — EKSPOR
Lihat EXPORT-2.3.md untuk perilaku ekspor terbaru dan batas pengujian. PDF tabel dan XLSX asli menggantikan PDF daftar teks dan CSV pada menu ekspor. Rekap pivot Zakat/Distribusi tersedia. Catatan versi 2.0 di bawah adalah riwayat dan tidak menggambarkan ekspor terbaru.

SIMAS Android 2.0 — Native (versi awal untuk uji perangkat)

Aplikasi Android dengan layar dan komponen native, bukan WebView atau pembuka browser.
Paket: id.simas.mobile | versionCode: 3 | Minimum Android 6 (API 23).
Login memakai akun pengurus yang sama dengan website. Koneksi Supabase mengikuti konfigurasi publik proyek produksi ketika APK ini dibuat. Tidak ada service-role/secret key dalam aplikasi.

FITUR
- Login email/kata sandi Supabase; pemeriksaan keanggotaan masjid dan hak akses.
- Beranda dengan saldo keuangan, ringkasan zakat (Rp/kg), qurban, grid ikon, navigasi bawah, dan animasi perpindahan.
- Keuangan: lihat/cari/tambah/edit/hapus transaksi, kategori manual, tanggal dengan pemilih kalender.
- Zakat: penerimaan dan penyaluran terpisah; fitrah kg, mal Rp, gabungan untuk penyaluran; RW/RT serta Nama RT opsional; jamaah otomatis lewat RPC yang sudah digunakan website.
- Qurban: shohibul, kas tambahan, distribusi, serta kelompok hewan dari peserta. Sapi rombongan dikelompokkan per 7 orang; kelompok belum lengkap ditandai.
- Distribusi: filter shohibul/penerima/keduanya, jumlah paket x berat, status penyerahan. Data tercatat dan data yang telah diserahkan dibedakan.
- Inventaris, pengurus, kegiatan: lihat/cari/tambah/edit/hapus sesuai peran akun.
- Admin: ubah nama/alamat/logo masjid dan buat akun login dari kartu pengurus. Pembuatan akun membutuhkan endpoint server website dan konfigurasi SUPABASE_SECRET_KEY yang sudah benar di Vercel; secret tidak dimasukkan ke APK.
- Laporan: tahun, ringkasan, grafik bulanan, PDF dengan kop nama/alamat masjid, CSV yang dapat dibuka di Excel, nomor urut dan total. Grafik qurban menampilkan kas tambahan; nominal shohibul termasuk dalam ringkasan penerimaan.
- Tahun Masehi dan rentang Hijriyah. Android 6 menggunakan perkiraan Hijriyah; Android 7+ menggunakan kalender Islam sistem.
- Dokumen disimpan melalui pemilih lokasi Android; tidak meminta akses semua penyimpanan.

BATAS VERSI NATIVE AWAL
Impor XLSX, unduh template dengan dropdown, ekspor XLSX asli, pivot, pengelompokan ekspor lanjutan, serta pengeditan transaksi fitrah lama dalam rupiah belum dipindahkan ke versi Android. Fitur tersebut tetap tersedia di website: https://simas-masjid.vercel.app/
CSV bukan XLSX. APK tidak membuka website secara otomatis. Kegiatan menggunakan input waktu ISO8601; pemilih kalender tersedia untuk tanggal transaksi.
Daftar menampilkan maksimal 150 hasil pencarian supaya ringan; pencarian memakai seluruh data yang dimuat. Ekspor mengikuti tahun/jenis yang dipilih, tidak mengikuti teks pencarian. Tombol muat ulang mengambil perubahan dari website; belum memakai langganan realtime.
Sesi berada dalam memori, sehingga perlu masuk kembali jika proses aplikasi ditutup sistem. Data tidak disimpan offline.

CARA MEMASANG
1. Unduh SIMAS-2.0-native.apk, kemudian buka dari aplikasi File/Unduhan di HP.
2. Jika diminta Android, izinkan aplikasi File tersebut memasang APK ini.
3. Pilih Instal/Perbarui. Versi ini memakai identitas paket dan sertifikat yang sama dengan APK SIMAS 1.1 sebelumnya.
4. Buka SIMAS, masuk memakai email/kata sandi pengurus, lalu uji satu alur yang diperlukan.
5. Jika gagal dipasang, catat pesan persisnya, merek HP, versi Android, dan apakah ada SIMAS versi lain terpasang. Jangan menghapus aplikasi atau menonaktifkan pemeriksaan keamanan tanpa mengetahui penyebabnya.

PEMBARUAN
Perubahan data melalui aplikasi atau website memakai database yang sama. Perubahan fitur/desain Android memerlukan APK baru dengan versionCode lebih tinggi dan kunci tanda tangan yang sama. Perubahan kode website tidak otomatis memperbarui antarmuka native.

SUMBER & BUILD
Cadangan ini memuat kode sumber, skrip build, uji instrumentasi, dan kunci penandatangan. SIMPAN PRIBADI. Jangan unggah signing.p12 atau signing-password.txt ke GitHub atau membagikan cadangan kepada jamaah; bagikan hanya APK.
Kunci yang sama dibutuhkan untuk pembaruan berikutnya.
Lingkungan Linux: JRE 21, zip, Python 3; Eclipse ECJ 3.38.0; Android platform 35 r02 dan build-tools 35.0.0. Jalankan python3 setup-build.py, kemudian bash build.sh. SDK/compiler diunduh terpisah, tidak dimasukkan dalam cadangan. Periksa dan terima lisensi Android SDK sesuai ketentuan Google sebelum menggunakan alat build.
Build mengeluarkan APK ke ../deliverables/android/. Naikkan versionCode sebelum pembaruan berikutnya.
Config.java hanya berisi project URL dan publishable key publik. RLS Supabase tetap menjadi batas hak akses server. Jangan mengganti dengan secret key.
Uji di folder tests menggunakan data fixture lokal dan tidak menulis data produksi.
Status uji terinci ada pada VERIFIKASI.txt.
