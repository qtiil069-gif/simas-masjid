const {app}=require('./production.cjs');const assert=require('assert/strict');
(async()=>{const a=app();await a.login();const w=a.w,api=w.testApi,doc=w.document,y=new Date().getFullYear();api.state.db.zakat_people.push({id:'p',full_name:'Uji',person_type:'mustahik',village:'Krajan'});api.state.db.zakat_entries.push(...[
{id:'a',party_name:'Zaki',zakat_type:'fitrah',rice_kg:3,amount:0,village:'Krajan'},
{id:'b',party_name:'Ani',zakat_type:'mal',rice_kg:0,amount:50000,village:'Krajan'},
{id:'c',party_name:'Budi',zakat_type:'fitrah_mal',rice_kg:2.5,amount:20000,village:'Krajan'}].map(r=>({...r,party_id:'p',direction:'distribution',occurred_on:y+'-03-19',notes:''})));
api.setPage('zakat');api.setSection('zakat_distributions');api.decorateTableSorting();let heads=[...doc.querySelectorAll('#module-list th')];heads[1].querySelector('button').click();let names=()=>[...doc.querySelectorAll('#module-list tbody tr')].map(r=>r.cells[1].textContent);assert.deepEqual(names(),['Ani','Budi','Zaki']);heads[1].querySelector('button').click();assert.deepEqual(names(),['Zaki','Budi','Ani']);heads[7].querySelector('button').click();assert.deepEqual([...doc.querySelectorAll('#module-list tbody tr')].map(r=>r.cells[7].dataset.sortValue),['0','2.5','3']);assert.equal(doc.querySelectorAll('#module-list [data-module-edit]').length,3);
let filter=doc.querySelector('#zakat-export-kind');assert(filter);filter.value='fitrah_mal';filter.onchange();assert.equal(api.filteredModule().length,1);let captured;w.SimasExport.xlsx=s=>{captured=s;return null};w.URL.createObjectURL=()=> 'blob:test';w.HTMLAnchorElement.prototype.click=()=>{};
doc.querySelector('#module-xlsx').click();doc.querySelector('#export-excel').click();assert(captured[0].rows.some(r=>r.includes('Budi')));assert(!captured[0].rows.some(r=>r.includes('Ani')));
api.setPage('laporan');assert(!doc.querySelector('#zakat-export-kind'));assert(doc.querySelector('.toolbar #season'));let report=api.currentReport();assert.equal(report[1][5],2.5);assert.equal(report[2][2],20000);doc.querySelector('#report-xlsx').click();assert(captured[0].rows.some(r=>r.includes(20000)));api.openPivotExport();doc.querySelector('#pivot-excel').click();assert.equal(captured.length,2);const total=captured[0].rows.find(r=>r[0]==='TOTAL');assert.deepEqual(Array.from(total),['TOTAL',1,2.5,20000]);assert.equal(captured[1].name,'Data Sumber');
const choice=doc.querySelector('#report-type');choice.value='qurban';choice.dispatchEvent(new w.Event('change'));assert.equal(api.currentReport()[0][0],'Pemasukan (Rp)');const choice2=doc.querySelector('#report-type');choice2.value='fitrah_mal';choice2.dispatchEvent(new w.Event('change'));const m=api.pivotMatrix([['Desa','Kg','Rp'],['Krajan',2.5,10],['Krajan',3,20],['Wetan',1,50]],0);assert.deepEqual(Array.from(m.at(-1)),['TOTAL',3,6.5,80]);assert.equal(api.pivotMatrix([['Desa','Kg']],0).length,1);api.setPage('zakat');api.setSection('zakat_people');
filter=doc.querySelector('#zakat-export-kind');
assert.equal(filter.getAttribute('aria-label'),'Jenis zakat');
assert(![...filter.parentElement.childNodes].some(n=>n.nodeType===3&&n.textContent.trim()));
for(const [kind,expected] of [['fitrah',['TOTAL',1,3]],['mal',['TOTAL',1,50000]],['fitrah_mal',['TOTAL',1,2.5,20000]],['',['TOTAL',3,5.5,70000]]]){
 filter.value=kind;filter.onchange();api.openPivotExport();
 const group=doc.querySelector('[name=pivot_group]');
 assert.deepEqual([...group.options].map(o=>o.textContent),['Desa/Dusun','RW/RT','Nama RT']);
 for(const option of group.options){group.value=option.value;group.onchange();assert.equal(doc.querySelectorAll('#pivot-preview th')[1].textContent,'Jumlah catatan')}
 doc.querySelector('#pivot-excel').click();
 assert.deepEqual(Array.from(captured[0].rows.find(r=>r[0]==='TOTAL')),expected);
}
api.state.db.zakat_entries.length=0;api.openPivotExport();
assert.deepEqual([...doc.querySelectorAll('#pivot-preview th')].map(h=>h.textContent),['Kelompok: Desa/Dusun','Jumlah catatan','Total fitrah (kg)','Total mal (Rp)']);
a.close();console.log('PASS: header sort directions, numeric sorting, actions preserved, kind filters in exports/reports, pivot totals and empty data.');})().catch(e=>{console.error(e);process.exitCode=1});
