# SIMASJID 2.3 — ekspor tabel

PDF sebelumnya berupa daftar teks; pilihan Excel sebelumnya hanya CSV dengan kolom umum. Versi ini menggunakan satu model tabel bertipe untuk PDF dan XLSX sesuai kolom website.

- Kop PDF: logo tersimpan masjid, nama, alamat/kota, telepon; judul, tahun Masehi/Hijriyah, konteks filter, tanggal cetak, nomor halaman. Kolom lebar memakai A4 landscape; header tabel berulang dan baris panjang dipecah antarhalaman tanpa membuang teks.
- XLSX OOXML asli: angka numerik, teks literal (bukan formula), tabel bergaris, header, pembungkusan teks, filter, freeze header, dan pengaturan cetak.
- Zakat: tanggal, nama, wilayah, RW/RT, Nama RT, arus, jenis, kg, rupiah, catatan. Penerimaan/penyaluran/saldo terpisah.
- Distribusi Qurban: kolom sesuai jenis penerima, RT/RW terpisah, rincian bungkus campuran, total bungkus dan kg. Nama/jenis shohibul mengikuti relasi peserta.
- Kas Qurban menyertakan setoran otomatis shohibul, kategori, pemasukan/pengeluaran, dan saldo. Ekspor sesuai pencarian termasuk setoran otomatis yang cocok. Daftar UI kas tetap daftar kas tambahan.
- Modul lain memakai kolom masing-masing, bukan kolom umum kg/rupiah.
- Pencarian dan filter modul berlaku pada ekspor; batas tampilan 150 baris tidak membatasi ekspor.
- Pivot Zakat/Distribusi: Desa/Dusun atau Wilayah/Dusun, RW/RT bila ada, Nama RT; jumlah catatan dan jumlah numerik. Excel pivot memiliki sheet Data Sumber, PDF pivot memiliki ruang Pembuat/Mengetahui.

Perbandingan berdasar sumber website pada branch main yang dibaca 17 November 2026. Ukuran/font PDF Android mengikuti renderer native dan tidak identik piksel dengan dialog cetak browser. Ekspor kelompok rinci per halaman, kupon terpilih, serta semua opsi laporan khusus website belum dipindahkan. Impor/template Excel tetap melalui website. XLSX kop berupa teks sebagaimana ekspor website; logo ditampilkan pada PDF.

Paket produksi id.simas.mobile versionCode 6, versionName 2.3-native. APK distribusi uji memakai id.simas.mobile.exportpreview agar tidak bentrok dengan kunci uji sesi sebelumnya, sehingga dipasang terpisah. Seluruh versi tetap memakai backend yang sama. Untuk pembaruan produksi, tandatangani build unsigned menggunakan kunci asli pemilik.
