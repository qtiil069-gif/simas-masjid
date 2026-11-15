// Annual Gregorian archives overlap Hijri years; do not assign one Hijri year to a whole Gregorian year.
const hijriYearFormatter=new Intl.DateTimeFormat('en-US-u-ca-islamic-civil',{year:'numeric',timeZone:'UTC'});
function hijriYearOf(date){return hijriYearFormatter.formatToParts(date).find(p=>p.type==='year')?.value||''}
function hijriYearRange(y){y=Number(y);if(!Number.isInteger(y)||y<1900||y>2300)return '';const a=hijriYearOf(new Date(Date.UTC(y,0,1))),b=hijriYearOf(new Date(Date.UTC(y,11,31)));return a===b?a:a+'–'+b}
function yearLabel(y){const h=hijriYearRange(y);return h?y+' M / '+h+' H':String(y)}
const periodBeforeHijri=periodSelect;
periodSelect=()=>{const box=document.createElement('div');box.innerHTML=periodBeforeHijri();box.querySelectorAll('option').forEach(o=>{const v=o.value;o.value=v;o.textContent=yearLabel(v)});return box.innerHTML};
const yearBeforeHijri=yearField;
yearField=r=>yearBeforeHijri(r)+'<small class="hijri-year-hint" data-hijri-for="year">'+esc(yearLabel(r.year||season))+'</small>';
document.addEventListener('input',e=>{const el=e.target;if(el.matches('input[name=year],input[name=from],input[name=to]')){const hint=el.closest('form')?.querySelector('[data-hijri-for="'+el.name+'"]')||el.closest('label')?.querySelector('.hijri-year-hint');if(hint)hint.textContent=yearLabel(el.value)}});
const letterheadBeforeHijri=letterheadRows;
letterheadRows=title=>[...letterheadBeforeHijri(title),['Periode',yearLabel(['zakat','qurban','laporan'].includes(page)?season:new Date().getFullYear())],['Tahun H: rentang kalender Hijriyah sipil otomatis.'],[]];
const printBeforeHijri=printDocument;
printDocument=(title,body)=>printBeforeHijri(title,'<p>Periode: '+esc(yearLabel(['zakat','qurban','laporan'].includes(page)?season:new Date().getFullYear()))+' · Rentang Hijriyah sipil</p>'+body);
const reportBeforeHijri=reportRows;
reportRows=(t,rows=scoped(t))=>{const matrix=reportBeforeHijri(t,rows);const indices=matrix[0].map((h,i)=>/^Tahun/.test(h)?i:-1).filter(i=>i>=0);return [matrix[0],...matrix.slice(1).map(r=>r.map((v,i)=>indices.includes(i)&&/^\d{4}$/.test(String(v))?yearLabel(v):v))]};
const renderBeforeHijri=render;
render=()=>{renderBeforeHijri();const main=$('#main');if(!main)return;
 const top=main.querySelector('.page-top');if(top&&!top.querySelector('.hijri-current'))top.insertAdjacentHTML('beforeend','<small class="hijri-current">'+esc(yearLabel(['zakat','qurban','laporan'].includes(page)?season:new Date().getFullYear()))+'</small>');
 main.querySelectorAll('.year-range input').forEach(el=>{if(!el.closest('label').querySelector('.hijri-year-hint'))el.insertAdjacentHTML('afterend','<small class="hijri-year-hint" data-hijri-for="'+el.name+'">'+esc(yearLabel(el.value))+'</small>')});
};
