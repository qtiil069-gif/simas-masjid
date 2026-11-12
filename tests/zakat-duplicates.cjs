const {app}=require('./production.cjs');const assert=require('assert/strict');
(async()=>{const a=app();await a.login();const api=a.w.testApi,doc=a.w.document;api.setPage('zakat');api.setSection('zakat_distributions');
const row=['2026-03-19','Nama <uji>','Krajan','','','Zakat fitrah',3,0,''];let matrix=[api.flowHeaders(true),row,row,row];
let result=api.parseFlowRows(matrix,true);assert.equal(result.records.length,3);assert.equal(result.errors.length,0);assert.deepEqual(JSON.parse(JSON.stringify(result.duplicates)).map(d=>[d.row,d.firstRow]),[[3,2],[4,2]]);assert.equal(new Set(result.records.map(r=>r.id)).size,3);
api.openZakatImport();a.w.XLSX={read:()=>({Sheets:{Data:{}}}),utils:{sheet_to_json:()=>matrix}};
const file=doc.querySelector('#flow-file'),save=doc.querySelector('#flow-save');Object.defineProperty(file,'files',{value:[{name:'test.xlsx',size:100,arrayBuffer:async()=>new ArrayBuffer(0)}]});
await file.onchange();assert(!save.disabled);assert.match(save.textContent,/Tetap lanjut/);assert.equal(doc.querySelector('#flow-duplicates').hidden,false);assert(!doc.querySelector('#flow-duplicates uji'));
a.w.confirm=()=>false;await save.onclick();assert(!a.calls.some(c=>c.url.includes('/rpc/simas_save_zakat')));assert(doc.querySelector('#ui-dialog').open);
// Other validation errors cannot be overridden by accepting duplicates.
matrix=[api.flowHeaders(true),row,row,['','Invalid','Krajan','','','fitrah',3,0,'']];await file.onchange();assert(save.disabled);a.w.confirm=()=>true;await save.onclick();assert(!a.calls.some(c=>c.url.includes('/rpc/simas_save_zakat')));
// Replacing a file clears the old warning.
matrix=[api.flowHeaders(true),row];await file.onchange();assert(doc.querySelector('#flow-duplicates').hidden);assert.equal(save.textContent,'Simpan hasil impor');
matrix=[api.flowHeaders(true),row,row];await file.onchange();a.w.confirm=()=>true;await save.onclick();const calls=a.calls.filter(c=>c.url.includes('/rpc/simas_save_zakat'));assert.equal(calls.length,1);assert.equal(JSON.parse(calls[0].opts.body).p_rows.length,2);
api.openZakatImport();doc.querySelector('#flow-cancel').click();assert(!doc.querySelector('#ui-dialog').open);assert.equal(a.calls.filter(c=>c.url.includes('/rpc/simas_save_zakat')).length,1);
a.close();console.log('PASS: duplicate row references, explicit proceed/cancel, all rows retained, validation errors still block, file reset and HTML escaping.');})().catch(e=>{console.error(e);process.exitCode=1});
