# Pembaruan penerimaan dan penyaluran zakat

1. Buka proyek Supabase yang dipakai SIMAS, lalu **SQL Editor → New query**.
2. Salin seluruh isi `migrations/20261112_zakat_flow.sql` dari GitHub, lalu klik **Run**. Jalankan pada database yang sudah memiliki tabel SIMAS dan migrasi sebelumnya. Jangan jalankan ulang schema.sql.
3. Setelah berhasil, muat ulang website. Menu Zakat berisi **Muzakki & Mustahik**, **Penerimaan**, dan **Penyaluran**.
4. Penerimaan: pilih jamaah yang sudah ada atau tambah nama; isi Desa/Dusun; pilih Fitrah untuk total berat kg atau Mal untuk rupiah.
5. Penyaluran: isi penerima, Desa/Dusun, RT/RW, Nama RT; pilih Fitrah, Mal, atau Fitrah & Mal. Gabungan menampilkan kedua isian.
6. Simpan. Transaksi dan master jamaah diperbarui dalam satu transaksi database. Penerimaan menjadi Muzakki dan Penyaluran menjadi Mustahik. Asnaf tidak ditebak: lengkapi klasifikasi mustahik melalui Edit pada master jamaah.
7. Excel: unduh template baru dari menu yang sesuai. Jangan gunakan template gabungan lama untuk dua menu baru. Impor menambah transaksi; jangan unggah ulang berkas yang sudah berhasil.

Tidak ada konversi rupiah ke kg. Berat dimasukkan sebagai **total kg**, bukan jumlah kantong. Data lama tetap dihitung; fitrah uang lama tetap bisa dikoreksi. Rekap gabungan mengalokasikan kg ke fitrah dan rupiah ke mal. Grafik tetap memisahkan unit.

Nama dan lokasi transaksi disimpan sebagai arsip transaksi. Edit identitas master secara langsung tidak menulis ulang arsip transaksi lama. Menghapus transaksi menghitung ulang rekap, tetapi tidak menghapus master jamaah yang mungkin dipakai transaksi lain. Master tanpa transaksi dapat dihapus lewat tombol Hapus.

Nama, jenis jamaah, Desa/Dusun, dan RT/RW yang cocok dipakai kembali. Orang berbeda dengan nama sama: pilih identitas yang benar; jangan menebak hanya dari nama.

Jika terlihat pesan pembaruan database belum dipasang, pastikan skrip dijalankan pada proyek yang sama dengan SUPABASE_URL di Vercel. Tidak perlu mengubah environment variables atau kunci akses.

## Perbaikan kolom opsional

Jalankan `migrations/20261112_zakat_optional_rt.sql` setelah migrasi zakat sebelumnya. RT/RW, Nama RT, dan Catatan sekarang boleh kosong pada penyaluran. Tanggal, nama, Desa/Dusun, jenis dan jumlah zakat tetap wajib. Kolom template lama tetap sama, sehingga berkas yang sudah diisi bisa dipakai kembali. Tanggal Excel dan teks DD-MM-YY (26 = 2026), DD-MM-YYYY, serta YYYY-MM-DD diterima.

## Duplikat dalam berkas impor

Baris identik ditampilkan sebagai peringatan beserta nomor baris asal. Pilih **Tetap lanjut · impor semua** dan konfirmasi untuk menyimpan seluruh baris, termasuk duplikat; semuanya menambah rekap. Pilih **Batal / perbaiki berkas** untuk menutup tanpa menyimpan. Kesalahan kolom, tanggal atau jumlah tetap harus diperbaiki. Pemeriksaan ini hanya dalam berkas terpilih, bukan pembandingan dengan riwayat impor. Tidak memerlukan migrasi SQL tambahan.

## Pengurutan, filter dan rekap pivot

Klik judul kolom untuk urutan naik/turun; tombol Edit/Hapus tetap mengikuti barisnya. Pilihan jenis zakat di dekat Ekspor menyaring tabel dan hasil ekspor. Fitrah saja/Mal saja adalah jenis transaksi tepat; transaksi gabungan tersedia pada pilihan Fitrah & Mal. Filter jamaah mengikuti transaksi pada tahun rekap.

Ekspor pivot membuka pilihan pengelompokan kolom dan pratinjau. Excel berisi rekap pivot (nilai hasil perhitungan) dan sheet Data Sumber; PDF melalui Cetak / Simpan sebagai PDF. Ini tabel rekap, bukan objek PivotTable interaktif Excel. Untuk mengganti kelompok atau memperbarui angka, lakukan di aplikasi lalu ekspor ulang. Uang dan kg dijumlahkan terpisah. Data kosong menampilkan judul tanpa total palsu. Tidak perlu migrasi SQL.
