function numberedExportMatrix(matrix){
 if(!matrix?.length)return matrix;
 if(matrix[0][0]==='No.')return matrix;
 let n=0;return [['No.',...matrix[0]],...matrix.slice(1).map(row=>[/^(TOTAL|SUBTOTAL|SALDO)(\b|\s)/i.test(String(row[0]))?'':++n,...row])]
}
const printWithoutNumbers=printDocument;
printDocument=(title,body)=>{
 const container=document.createElement('div');container.innerHTML=body;
 container.querySelectorAll('table').forEach(table=>{
  const head=table.querySelector('thead tr');if(!head||head.cells[0]?.textContent==='No.'||head.cells[0]?.textContent==='Ringkasan')return;
  const th=document.createElement('th');th.textContent='No.';head.prepend(th);let n=0;
  table.querySelectorAll('tbody tr').forEach(row=>{const spanning=row.querySelector('td[colspan]');if(spanning){spanning.colSpan++;return}const cell=document.createElement('td');cell.textContent=/^(TOTAL|SUBTOTAL|SALDO)(\b|\s)/i.test(row.cells[0]?.textContent||'')?'':String(++n);row.prepend(cell)})
 });return printWithoutNumbers(title,container.innerHTML)
};
const shohibulHeaders=['Nama Lengkap','Jenis Qurban','Nominal (Rp)'];
const templateBeforeShohibul=downloadZakatTemplate;
downloadZakatTemplate=()=>{
 if(section!=='qurban_people')return templateBeforeShohibul();
 downloadBlob(SimasExport.xlsx([{name:'Data',rows:[shohibulHeaders],validations:[{range:'B2:B1001',values:['Sapi Pribadi','Sapi Rombongan','Kambing Pribadi']}]},{name:'Petunjuk',rows:[['Kolom','Ketentuan'],['Tahun','Mengikuti tahun kegiatan yang dipilih di website: '+yearLabel(season)],['Nama Lengkap','Satu baris = satu orang. Isi 7 baris untuk 7 peserta Sapi Rombongan.'],['Jenis Qurban','Pilih dropdown: Sapi Pribadi, Sapi Rombongan, Kambing Pribadi.'],['Nominal (Rp)','Nominal per orang yang sudah diterima panitia. Sapi Rombongan wajib angka positif. Sapi Pribadi dan Kambing Pribadi boleh kosong atau 0; isi hanya jika ada setoran uang. Maksimal 2 desimal.'],['Kelompok','Otomatis per 7 peserta dalam tahun yang sama, melanjutkan kelompok yang belum penuh. Urutan mengikuti pendaftaran.'],['Batas','Maksimal 1000 baris dan 2 MB; tanpa formula. Nama, jenis, tahun dan nominal identik ditolak agar tidak tercatat ganda.']]}]),'Template-Shohibul-Qurban-'+season+'.xlsx')
};
const parseBeforeShohibul=parseZakatRows;
parseZakatRows=(matrix,t)=>{
 if(t!=='qurban_people')return parseBeforeShohibul(matrix,t);
 const records=[],errors=[],seen=new Set(),str=v=>String(v??'').trim();
 if(JSON.stringify((matrix[0]||[]).map(str))!==JSON.stringify(shohibulHeaders))return {records,errors:['Kolom tidak sesuai. Unduh template Shohibul Qurban terbaru.']};
 if(matrix.length>1001)return {records,errors:['Maksimal 1000 baris.']};
 const key=r=>JSON.stringify([r.year,str(r.full_name).toLowerCase(),r.qurban_category,Number(r.nominal)]);
 const existing=new Set((db.qurban_people||[]).map(key));
 const start=Math.max(Date.now(),...(db.qurban_people||[]).map(r=>Date.parse(r.created_at)||0))+1;
 matrix.slice(1).forEach((row,i)=>{if(row.every(v=>!str(v)))return;try{
  if(row.slice(3).some(v=>str(v)))throw Error('Ada kolom tambahan.');
  const full_name=str(row[0]),category={'sapi pribadi':'sapi_pribadi','sapi perorangan':'sapi_pribadi','sapi rombongan':'sapi_patungan','kambing pribadi':'kambing','kambing':'kambing'}[str(row[1]).toLowerCase()],raw=str(row[2]);
  if(full_name.length<2||full_name.length>150)throw Error('Nama wajib 2–150 karakter.');
  if(!category)throw Error('Pilih Sapi Pribadi, Sapi Rombongan, atau Kambing Pribadi.');
  if(raw&&!/^\d+(\.\d{1,2})?$/.test(raw))throw Error('Nominal harus angka tanpa Rp/pemisah ribuan, maksimal 2 desimal.');
  const nominal=Number(raw||0);
  if(!Number.isFinite(nominal)||nominal<0||nominal>999999999999.99)throw Error('Nominal harus nol atau angka positif dalam batas yang diizinkan.');
  if(category==='sapi_patungan'&&nominal<=0)throw Error('Sapi Rombongan: isi nominal setoran per orang lebih dari nol.');
  const r={full_name,qurban_category:category,nominal,year:season,created_at:new Date(start+i).toISOString()};
  const k=key(r);if(seen.has(k)||existing.has(k))throw Error('Data peserta identik sudah ada. Periksa nama, jenis, tahun dan nominal.');seen.add(k);records.push(r);
 }catch(err){errors.push('Baris '+(i+2)+': '+err.message)}});
 if(!records.length&&!errors.length)errors.push('Belum ada data.');return {records,errors}
};
const importBeforeShohibul=openZakatImport;
openZakatImport=()=>{importBeforeShohibul();if(section==='qurban_people'&&$('#import-result'))$('#import-result').before(Object.assign(document.createElement('p'),{textContent:'Tahun kegiatan: '+yearLabel(season)+'. Satu baris untuk satu peserta. Sapi Rombongan otomatis dikelompokkan per 7 orang; nominal dicatat per orang. Kelompok terakhir boleh belum penuh. Nominal Sapi Pribadi dan Kambing Pribadi boleh kosong (dicatat 0); Sapi Rombongan wajib lebih dari 0.'}))};
