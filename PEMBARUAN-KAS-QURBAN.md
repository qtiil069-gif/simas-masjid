# Kas Qurban: penggunaan dana peserta

Buka Qurban → Kas Qurban. Ringkasan menampilkan dana shohibul, belanja hewan, biaya operasional, pengeluaran lainnya, total pengeluaran, dan sisa kas. Nominal shohibul yang kosong/0 tidak menambah dana masuk. Penerimaan arsip lama tetap ikut saldo.

Klik **Input transaksi**, pilih **Pengeluaran**, lalu isi tahun, tanggal, kategori, jumlah dan keterangan/nama penerima. Kategori tersedia: Pembelian Hewan, Biaya Operasional, Pengeluaran Lainnya. Setoran otomatis dikoreksi melalui Shohibul Qurban agar tidak dicatat dua kali. Transaksi pengeluaran dapat diedit/dihapus oleh peran yang berwenang.

Filter memisahkan pengeluaran, kategori, penerimaan, atau seluruh transaksi. Ekspor Excel/PDF mengikuti tahun, pencarian dan filter aktif, beserta nomor urut dan total pemasukan/pengeluaran. Rekap Qurban pada Laporan juga memuat rincian kategori.

Tidak perlu migrasi SQL baru. Kategori disimpan sebagai awalan terstruktur pada kolom description yang sudah ada. Pengeluaran lama tanpa awalan kategori ditampilkan sebagai Pengeluaran Lainnya; nilainya tidak diubah. RLS dan hak akses Supabase tetap berlaku. Tampilan/fitur ini berlaku pada website; APK native memerlukan pembaruan tersendiri.


## Penerimaan operasional shohibul

Pada **Input transaksi**, pilih **Penerimaan Operasional Shohibul Qurban**, kemudian pilih **Sapi Perorangan** atau **Kambing**. Isi nama shohibul, tanggal, dan nominal lebih dari nol; keterangan boleh kosong. Penerimaan ini menambah saldo kas dan ditampilkan pada ringkasan Penerimaan Operasional, filter transaksi, serta ekspor dan laporan.

Nama pemberi disimpan sebagai catatan pada transaksi kas, tanpa mengubah nominal utama atau membuat data peserta baru. Transaksi ini tidak dihubungkan melalui person_id agar tidak menggandakan setoran utama yang dihitung otomatis dari peserta. Data jenis qurban, nama, dan catatan disimpan terstruktur dalam description. Tidak ada migrasi SQL tambahan.
