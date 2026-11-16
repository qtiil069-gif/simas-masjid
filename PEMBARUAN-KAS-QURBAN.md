# Kas Qurban: penggunaan dana peserta

Buka Qurban → Kas Qurban. Ringkasan menampilkan dana shohibul, belanja hewan, biaya operasional, pengeluaran lainnya, total pengeluaran, dan sisa kas. Nominal shohibul yang kosong/0 tidak menambah dana masuk. Penerimaan arsip lama tetap ikut saldo.

Klik **Catat pengeluaran**, isi tahun, tanggal, kategori, jumlah dan keterangan/nama penerima. Kategori tersedia: Pembelian Hewan, Biaya Operasional, Pengeluaran Lainnya. Setoran otomatis dikoreksi melalui Shohibul Qurban agar tidak dicatat dua kali. Transaksi pengeluaran dapat diedit/dihapus oleh peran yang berwenang.

Filter memisahkan pengeluaran, kategori, penerimaan, atau seluruh transaksi. Ekspor Excel/PDF mengikuti tahun, pencarian dan filter aktif, beserta nomor urut dan total pemasukan/pengeluaran. Rekap Qurban pada Laporan juga memuat rincian kategori.

Tidak perlu migrasi SQL baru. Kategori disimpan sebagai awalan terstruktur pada kolom description yang sudah ada. Pengeluaran lama tanpa awalan kategori ditampilkan sebagai Pengeluaran Lainnya; nilainya tidak diubah. RLS dan hak akses Supabase tetap berlaku. Tampilan/fitur ini berlaku pada website; APK native memerlukan pembaruan tersendiri.
