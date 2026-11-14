const {app}=require('./production.cjs');const assert=require('assert/strict');
(async()=>{const a=app();await a.login();const w=a.w,d=w.document,api=w.testApi,y=new Date().getFullYear();api.state.db.zakat_entries.push(...[
{party_name:'Siti',village:'Krajan',rt_rw:'002/001',rt_name:'Pak A'},
{party_name:'Budi',village:'Wetan',rt_rw:'003/002',rt_name:'Pak B'},
{party_name:'Ani',village:'Krajan',rt_rw:'002/001',rt_name:'Pak A'}
].map((r,i)=>({...r,id:'g'+i,occurred_on:y+'-03-19',direction:'distribution',zakat_type:'fitrah',rice_kg:3,amount:0})));
api.setPage('zakat');api.setSection('zakat_distributions');assert(d.querySelector('#module-list').textContent.includes('RW/RT'));assert(!d.querySelector('#module-list').textContent.includes('RT/RW'));
let sheets;w.SimasExport.xlsx=s=>{sheets=s;return null};w.URL.createObjectURL=()=> 'blob:test';w.HTMLAnchorElement.prototype.click=()=>{};
function open(group){d.querySelector('#module-xlsx').click();const select=d.querySelector('[name=distribution_group]');select.value=group;select.onchange();return d.querySelector('[name=distribution_value]')}
let value=open('village');assert.equal(value.options.length,3);value.value='0';value.onchange();d.querySelector('#export-excel').click();let rows=sheets[0].rows;assert(rows.some(r=>r.join('|')==='Dusun/Desa|RW/RT|Nama Penerima'));assert(rows.some(r=>r.includes('Ani')));assert(!rows.some(r=>r.includes('Budi')));
value=open('rt_name');value.value='1';value.onchange();d.querySelector('#export-excel').click();rows=sheets[0].rows;assert(rows.some(r=>r[0]==='Nama RT: Pak B'));assert(rows.some(r=>r.includes('Budi')));assert(!rows.some(r=>r.includes('Siti')));
open('rt_rw');d.querySelector('#export-excel').click();rows=sheets[0].rows;assert(rows.some(r=>r.join('|')==='RW/RT|Dusun/Desa|Nama Penerima'));assert(rows.some(r=>r.join('|')==='002/001|Krajan|Ani'));assert.equal(api.flowHeaders(true)[3],'RW/RT');
const filter=d.querySelector('#zakat-export-kind');filter.value='mal';filter.onchange();open('village');assert(d.querySelector('#distribution-export-preview').textContent.includes('Tidak ada data'));a.close();console.log('PASS: grouped exports by village, RT name and RW/RT, selected group, column ordering, filter and template header');})().catch(e=>{console.error(e);process.exitCode=1});
