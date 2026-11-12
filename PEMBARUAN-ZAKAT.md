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
