const {app}=require('./production.cjs');const assert=require('assert/strict');
(async()=>{const a=app();await a.login();const w=a.w,d=w.document,api=w.testApi,y=new Date().getFullYear();w.print=()=>{};let sheets;w.SimasExport.xlsx=s=>{sheets=s;return null};w.URL.createObjectURL=()=> 'blob:test';w.HTMLAnchorElement.prototype.click=()=>{};
api.state.db.zakat_people.push({id:'p',full_name:'Uji',person_type:'mustahik',village:'Krajan'});
api.state.db.zakat_entries.push(...[
{zakat_type:'fitrah',rice_kg:2.125,amount:0,village:'Krajan'},
{zakat_type:'mal',rice_kg:0,amount:12345.67,village:'Wetan'},
{zakat_type:'fitrah_mal',rice_kg:3.25,amount:20000,village:'Krajan'}
].map((r,i)=>({...r,id:'t'+i,party_id:'p',party_name:'Nama '+i,rt_rw:'001/002',rt_name:'Pak RT',direction:'distribution',occurred_on:y+'-03-19'})));
api.setPage('zakat');api.setSection('zakat_distributions');
for(const [kind,total] of [['fitrah',[2.125]],['mal',[12345.67]],['fitrah_mal',[3.25,20000]],['',[5.375,32345.67]]]){
 const filter=d.querySelector('#zakat-export-kind');filter.value=kind;filter.onchange();d.querySelector('#module-xlsx').click();d.querySelector('#export-excel').click();assert.deepEqual(Array.from(sheets[0].rows.find(r=>r[0]==='TOTAL')).slice(1),total);
}
d.querySelector('#module-xlsx').click();let g=d.querySelector('[name=distribution_group]');g.value='village';g.onchange();let v=d.querySelector('[name=distribution_value]');v.value='0';v.onchange();d.querySelector('#export-excel').click();assert.deepEqual(Array.from(sheets[0].rows.find(r=>r[0]==='SUBTOTAL')),['SUBTOTAL',5.375,20000]);assert.deepEqual(Array.from(sheets[0].rows.find(r=>r[0]==='TOTAL')),['TOTAL',5.375,20000]);
d.querySelector('#module-xlsx').click();d.querySelector('#export-pdf').click();assert(d.querySelector('#print-area').textContent.includes('Total sesuai filter'));assert(d.querySelector('#print-area [data-sort-value="32345.67"]'));
api.setSection('zakat_people');d.querySelector('#module-xlsx').click();d.querySelector('#export-excel').click();assert.deepEqual(Array.from(sheets[0].rows.find(r=>r[0]==='TOTAL')),['TOTAL',5.375,32345.67]);
api.state.db.zakat_entries.push({id:'receipt',party_id:'p',party_name:'Uji',village:'Krajan',direction:'receipt',occurred_on:y+'-03-19',zakat_type:'fitrah',rice_kg:10,amount:0});api.setPage('laporan');d.querySelector('#report-xlsx').click();assert.deepEqual(Array.from(sheets[0].rows.find(r=>r[0]==='TOTAL PENERIMAAN')),['TOTAL PENERIMAAN',10,0]);assert.deepEqual(Array.from(sheets[0].rows.find(r=>r[0]==='SALDO')),['SALDO',4.625,-32345.67]);
a.close();console.log('PASS: fitrah/mal/combined totals, group subtotal, selected-only grand total, PDF, master and separated report flows');})().catch(e=>{console.error(e);process.exitCode=1});
