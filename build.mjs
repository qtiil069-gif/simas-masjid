import fs from 'node:fs';
const url=(process.env.SUPABASE_URL||'').trim().replace(/\/$/,'');
const key=(process.env.SUPABASE_PUBLISHABLE_KEY||'').trim();
if(!/^https:\/\/[a-z0-9-]+\.supabase\.co$/.test(url)||!/^sb_publishable_[A-Za-z0-9_-]+$/.test(key)) {
  console.error('Konfigurasi belum lengkap: isi SUPABASE_URL (tanpa /rest/v1/) dan SUPABASE_PUBLISHABLE_KEY (sb_publishable_) di Environment Variables Vercel, lalu deploy ulang. Jangan gunakan secret key atau service_role.');
  process.exit(1);
}
const files=['core','modules','input-updates','dashboard','fund-information','ui','distribution-profile','distribution-packages','zakat-flow','login'];
const source=files.map(name=>fs.readFileSync(`src/${name}.js`,'utf8')).join('\n');
const marker='/* SIMAS_DEPLOYMENT */null';
if(!source.includes(marker))throw Error('Penanda konfigurasi build tidak ditemukan.');
fs.writeFileSync('dist/app.js',source.replace(marker,()=>JSON.stringify({url,key}).replace(/</g,'\\u003c')));
console.log('SIMAS production build selesai.');
