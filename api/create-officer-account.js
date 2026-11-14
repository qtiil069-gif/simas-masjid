// Server only: never include this file or SUPABASE_SECRET_KEY in dist.
module.exports=async(req,res)=>{
 res.setHeader('Cache-Control','no-store');
 if(req.method!=='POST')return res.status(405).json({error:'Metode tidak tersedia.'});
 const url=process.env.SUPABASE_URL,key=process.env.SUPABASE_PUBLISHABLE_KEY,secret=process.env.SUPABASE_SECRET_KEY;
 if(!url||!key||!secret)return res.status(503).json({error:'Administrator perlu mengatur SUPABASE_SECRET_KEY di Vercel lalu redeploy.'});
 const token=req.headers.authorization;
 if(!/^Bearer \S+$/.test(token||''))return res.status(401).json({error:'Silakan login kembali.'});
 const b=typeof req.body==='object'&&req.body||{};
 const uuid=/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
 if(!uuid.test(b.mosque_id)||!uuid.test(b.officer_id)||!['admin','takmir','bendahara','editor','viewer'].includes(b.role)||typeof b.email!=='string'||b.email.length>254||!/^\S+@\S+\.\S+$/.test(b.email)||typeof b.password!=='string'||b.password.length<12||b.password.length>128)return res.status(400).json({error:'Isi email valid, peran, dan kata sandi 12–128 karakter.'});
 async function call(path,opts={},admin=false){const r=await fetch(url+path,{...opts,headers:{'Content-Type':'application/json',apikey:admin?secret:key,Authorization:admin?'Bearer '+secret:token},signal:AbortSignal.timeout(15000)});const data=await r.json().catch(()=>null);return {ok:r.ok,status:r.status,data};}
 try{
  const auth=await call('/auth/v1/user');if(!auth.ok||!auth.data?.id)return res.status(401).json({error:'Sesi tidak valid.'});
  const membership=await call('/rest/v1/memberships?mosque_id=eq.'+b.mosque_id+'&user_id=eq.'+auth.data.id+'&role=eq.admin&select=role');
  if(!membership.ok||!membership.data?.length)return res.status(403).json({error:'Hanya admin masjid yang dapat membuat akun.'});
  const officer=await call('/rest/v1/mosque_officers?id=eq.'+b.officer_id+'&mosque_id=eq.'+b.mosque_id+'&select=*');
  if(!officer.ok)return res.status(409).json({error:'Jalankan migrasi 20261114_pengurus.sql terlebih dahulu.'});
  const row=officer.data?.[0];if(!row)return res.status(404).json({error:'Data pengurus tidak ditemukan.'});
  if(row.user_id)return res.status(409).json({error:'Pengurus ini sudah memiliki akun login.'});
  const created=await call('/auth/v1/admin/users',{method:'POST',body:JSON.stringify({email:b.email.trim(),password:b.password,email_confirm:true,user_metadata:{full_name:row.full_name},app_metadata:{simas_officer_id:row.id,simas_mosque_id:b.mosque_id}})},true);
  let user=created.data?.id?created.data:created.data?.user;
  if(!created.ok){
   // Recover only accounts previously created for this exact officer, never arbitrary existing accounts.
   if(!['email_exists','user_already_exists','email_already_exists'].includes(created.data?.error_code))return res.status(409).json({error:'Akun belum dibuat. Periksa kekuatan kata sandi dan pastikan email belum digunakan.'});
   for(let page=1;page<=100&&!user;page++){const list=await call('/auth/v1/admin/users?page='+page+'&per_page=100',{},true);if(!list.ok)break;const users=list.data?.users||[];user=users.find(u=>u.email?.toLowerCase()===b.email.trim().toLowerCase()&&u.app_metadata?.simas_officer_id===row.id&&u.app_metadata?.simas_mosque_id===b.mosque_id);if(users.length<100)break;}
   if(!user)return res.status(409).json({error:'Email sudah digunakan akun lain. Gunakan email baru; akun lama tidak diubah.'});
  }
  if(!user?.id)throw Error('Invalid auth response');
  const link=await call('/rest/v1/rpc/simas_link_officer',{method:'POST',body:JSON.stringify({p_mosque:b.mosque_id,p_officer:row.id,p_user:user.id,p_role:b.role})});
  if(!link.ok)return res.status(409).json({error:'Akun dibuat, tetapi pengaitan belum selesai. Coba lagi dengan email yang sama; kata sandi pertama tetap berlaku.'});
  return res.status(200).json({success:true});
 }catch{return res.status(503).json({error:'Proses belum dapat dipastikan. Muat ulang daftar; bila belum terhubung, coba lagi dengan email yang sama dan kata sandi pertama.'});}
};
