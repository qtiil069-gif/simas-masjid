const {JSDOM}=require('jsdom');
const fs=require('node:fs');
const assert=require('node:assert/strict');
const {spawnSync}=require('node:child_process');
const html=fs.readFileSync('dist/index.html','utf8');
const names=['core','modules','input-updates','dashboard','fund-information','ui','distribution-profile','distribution-packages','zakat-flow','table-tools','officers','qurban-import','distribution-recipients','hijri-years','login'];
const raw=names.map(n=>fs.readFileSync('src/'+n+'.js','utf8')).join('\n');
function app(mode='ok',configured=true){
 const d=new JSDOM(html,{url:'https://simas.example',runScripts:'outside-only',pretendToBeVisual:true});
 const w=d.window;w.TextEncoder=TextEncoder;w.TextDecoder=TextDecoder;
 w.matchMedia=()=>({matches:true,addEventListener(){}});w.scrollTo=()=>{};w.ResizeObserver=class{observe(){}disconnect(){}};
 w.HTMLElement.prototype.scrollTo=()=>{};w.HTMLElement.prototype.animate=()=>({finished:Promise.resolve(),cancel(){}});
 w.HTMLDialogElement.prototype.showModal=function(){this.open=true};w.HTMLDialogElement.prototype.close=function(){this.open=false};
 w.AbortSignal.timeout=()=>new w.AbortController().signal;
 const calls=[];let current=mode;
 w.fetch=async(url,opts={})=>{calls.push({url,opts});let status=200,data=[];
 if(url.includes('/auth/v1/token')){if(current==='bad'){status=400;data={message:'Invalid login credentials'}}else if(current==='network')throw Error('Failed to fetch');else if(current==='unconfirmed'){status=400;data={message:'Email not confirmed'}}else data={access_token:'test-session',user:{id:'u1',email:'admin@example.com'}};}
 else if(current==='expired'){status=401;data={message:'JWT expired'}}
 else if(url.includes('/memberships'))data=current==='nomember'?[]:[{user_id:'u1',mosque_id:'m1',role:'admin'}];
 else if(url.includes('/mosques'))data=[{id:'m1',name:'Masjid Sabilillah',address:'Margomulyo',city:'Tuban'}];
 else if(url.includes('/assets')&&opts.method==='POST')data=[{id:'a1',...JSON.parse(opts.body)}];
 return {ok:status<400,status,text:async()=>JSON.stringify(data)};
 };
 w.localStorage.setItem('simas-config',JSON.stringify({url:'https://wrong.supabase.co',key:'old'}));
 w.localStorage.setItem('simas-demo','{"assets":[{"name":"OLD DEMO"}]}');
 w.eval(fs.readFileSync('dist/export.js','utf8'));
 let source=raw.replace('/* SIMAS_DEPLOYMENT */null',configured?JSON.stringify({url:'https://example.supabase.co',key:'sb_publishable_test'}):'null');
 source=source.replace(/\}\)\(\);\s*$/,`window.testApi={get state(){return {db,live,session,config}},setPage(p){page=p;render()},setIdentity(data){Object.assign(mosque,data);render()},save,api,logout,yearLabel,distributionAudienceMatrix,validateDistribution,parseZakatRows,qurbanAutomatic,numberedExportMatrix,pivotMatrix,currentReport,filteredModule,decorateTableSorting,openPivotExport,openZakatImport,downloadZakatTemplate,fillModuleForm,validateZakatFlow,parseFlowRows,flowHeaders,summaryMatrix,scoped,setSection(s){section=s;render()},setRole(r){role=r;render()}};})();`);
 w.eval(source);
 return {w,calls,setMode:m=>current=m,async login(){w.document.querySelector('#auth-email').value='admin@example.com';w.document.querySelector('#auth-password').value='test-password';await w.document.querySelector('#auth-form').onsubmit({preventDefault(){}})},close:()=>w.close()};
}
module.exports={app};
if(require.main===module)(async()=>{
 for(const [mode,match] of [['bad',/tidak sesuai/],['nomember',/belum memiliki akses/],['network',/tidak tersedia/],['unconfirmed',/belum dikonfirmasi/]]){
 const a=app(mode);await a.login();assert.match(a.w.document.querySelector('#auth-error').textContent,match);assert.equal(a.w.testApi.state.session,null);assert.equal(a.w.document.querySelector('#auth-password').value,'');assert.equal(a.w.document.querySelector('#main').textContent,'');a.close();}
 const n=app('ok',false);await n.login();assert.equal(n.calls.length,0);assert.match(n.w.document.querySelector('#auth-error').textContent,/belum tersedia/);n.close();
 const a=app();assert.equal(a.w.document.querySelector('#auth-demo'),null);assert.equal(a.w.document.querySelector('#auth-config'),null);assert.equal(a.w.testApi.state.db.assets.length,0);assert.equal(a.w.testApi.state.config.url,'https://example.supabase.co');
 await a.login();assert.equal(a.w.testApi.state.live,true);assert.equal(a.w.document.querySelector('#login-screen').hidden,true);assert.equal(a.w.testApi.state.db.finance_transactions.length,0);assert(!/NaN|OLD DEMO/.test(a.w.document.querySelector('#main').textContent));
 for(const page of ['keuangan','kegiatan','inventaris','pengurus','zakat','qurban','laporan','pengaturan']){a.w.testApi.setPage(page);assert(!/NaN|Mode demo|Pengaturan koneksi|Unduh.*SQL/.test(a.w.document.querySelector('#main').textContent),page);}
 const logo='data:image/png;base64,iVBORw0KGgo=';
 a.w.testApi.setIdentity({name:'Masjid Sabilillah Margomulyo',address:'Jl. Raya Margomulyo, Dusun Kerek, Margomulyo, Kec. Kerek, Kabupaten Tuban, Jawa Timur 62356',logo_data_url:logo});
 assert.equal(a.w.document.querySelector('#home-link .brand-icon img').getAttribute('src'),logo);
 assert(a.w.document.querySelector('#home-link').classList.contains('has-mosque-logo'));
 assert.equal(a.w.document.querySelector('#mosque-name').textContent,'Masjid Sabilillah Margomulyo');
 assert.match(a.w.document.querySelector('#mosque-city').textContent,/62356/);
 a.w.testApi.setIdentity({logo_data_url:null});
 assert(!a.w.document.querySelector('#home-link .brand-icon img'));
 assert(a.w.document.querySelector('#home-link .brand-icon svg'));
 assert(a.w.document.querySelector('#profile-form'));assert(!a.w.document.querySelector('#connect-form'));
 await a.w.testApi.save('assets',null,{name:'Barang uji',quantity:1,condition:'baik'});
 const saved=a.calls.find(c=>c.url.includes('/assets')&&c.opts.method==='POST');assert.equal(JSON.parse(saved.opts.body).mosque_id,'m1');assert.equal(saved.opts.headers.Authorization,'Bearer test-session');
 await a.w.testApi.logout();assert.equal(a.w.testApi.state.session,null);assert.equal(a.w.document.querySelector('#main').textContent,'');assert.equal(a.w.document.querySelector('#login-screen').hidden,false);assert(!a.w.localStorage.getItem('token'));a.close();
 const e=app();await e.login();e.setMode('expired');await assert.rejects(e.w.testApi.api('/rest/v1/assets'));assert.equal(e.w.testApi.state.session,null);assert.match(e.w.document.querySelector('#auth-error').textContent,/Sesi Anda berakhir/);e.close();
 for(const env of [{SUPABASE_URL:'',SUPABASE_PUBLISHABLE_KEY:''},{SUPABASE_URL:'https://example.supabase.co/rest/v1/',SUPABASE_PUBLISHABLE_KEY:'sb_publishable_test'},{SUPABASE_URL:'https://example.supabase.co',SUPABASE_PUBLISHABLE_KEY:'sb_secret_invalid'}]){const r=spawnSync(process.execPath,['build.mjs'],{env:{...process.env,...env},encoding:'utf8'});assert.notEqual(r.status,0);assert(!r.stderr.includes('sb_secret_invalid'));}
 console.log('PASS: production login, errors, missing config, legacy storage ignored, empty modules, settings, save authorization, logout, expiry and build validation.');
})().catch(e=>{console.error(e);process.exitCode=1});
