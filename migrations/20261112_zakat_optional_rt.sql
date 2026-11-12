-- Run AFTER 20261112_zakat_flow.sql. Optional RT/RW and Nama RT; existing data and RLS preserved.
begin;
create or replace function public.simas_save_zakat(p_mosque uuid,p_rows jsonb) returns integer
language plpgsql security invoker set search_path='' as $$
declare d jsonb; entry_id uuid; person_id uuid; person_role text; dir text; kind text; nm text; village_value text; rt_value text; rt_name_value text; cash numeric; rice numeric; day_value date; old_entry public.zakat_entries; existing_ids uuid[]; result_count integer:=0; is_new boolean;
begin
 if auth.uid() is null or not public.simas_has_role(p_mosque,array['admin','bendahara']::public.simas_role[]) then raise exception 'Akses ditolak'; end if;
 if jsonb_typeof(p_rows) is distinct from 'array' then raise exception 'Data harus berupa daftar'; end if;
 if jsonb_array_length(p_rows) not between 1 and 1000 then raise exception 'Jumlah baris harus 1–1000'; end if;
 -- Serialize RPC writes for the same mosque, including automatic person matching.
 perform pg_advisory_xact_lock(hashtextextended(p_mosque::text,412));
 for d in select value from jsonb_array_elements(p_rows) loop
  entry_id:=(d->>'id')::uuid; is_new:=coalesce((d->>'is_new')::boolean,true);
  dir:=d->>'direction';kind:=d->>'zakat_type';nm:=btrim(d->>'full_name');village_value:=btrim(d->>'village');rt_value:=coalesce(btrim(d->>'rt_rw'),'');rt_name_value:=coalesce(btrim(d->>'rt_name'),'');
  cash:=coalesce((d->>'amount')::numeric,0);rice:=coalesce((d->>'rice_kg')::numeric,0);day_value:=(d->>'occurred_on')::date;
  if entry_id is null or dir is null or dir not in ('receipt','distribution') or kind is null or kind not in ('fitrah','mal','fitrah_mal') or (dir='receipt' and kind='fitrah_mal') then raise exception 'Jenis atau arus zakat tidak valid'; end if;
  if nm is null or length(nm) not between 2 and 150 or village_value is null or length(village_value) not between 1 and 250 then raise exception 'Nama dan Desa/Dusun wajib diisi'; end if;
  if length(rt_value)>50 or length(rt_name_value)>100 then raise exception 'RT/RW maksimal 50 karakter dan Nama RT maksimal 100 karakter'; end if;
  if day_value is null or length(coalesce(d->>'notes',''))>1000 then raise exception 'Tanggal atau catatan tidak valid'; end if;
  if cash::text in ('NaN','Infinity','-Infinity') or rice::text in ('NaN','Infinity','-Infinity') or cash<0 or cash>999999999999.99 or rice<0 or rice>999999999.999 or cash<>round(cash,2) or rice<>round(rice,3) then raise exception 'Jumlah tidak valid'; end if;
  old_entry:=null;
  if not is_new then
   select * into old_entry from public.zakat_entries where id=entry_id and mosque_id=p_mosque for update;
   if not found then raise exception 'Transaksi tidak ditemukan atau akses ditolak'; end if;
   if old_entry.direction<>dir then raise exception 'Arus transaksi tidak dapat diubah'; end if;
  elsif exists(select 1 from public.zakat_entries where id=entry_id and mosque_id=p_mosque) then
   raise exception 'Transaksi ini sudah tersimpan. Muat ulang daftar sebelum mencoba lagi';
  end if;
  if kind='mal' and (cash<=0 or rice<>0) then raise exception 'Mal wajib rupiah positif tanpa beras'; end if;
  if kind='fitrah_mal' and (cash<=0 or rice<=0) then raise exception 'Isi fitrah kg dan mal rupiah'; end if;
  if kind='fitrah' and not (not is_new and old_entry.zakat_type='fitrah' and old_entry.amount>0) and (rice<=0 or cash<>0) then raise exception 'Fitrah baru wajib kg positif tanpa nominal uang'; end if;
  if cash=0 and rice=0 then raise exception 'Jumlah wajib lebih dari nol'; end if;
  person_role:=case when dir='receipt' then 'muzakki' else 'mustahik' end;
  person_id:=nullif(d->>'party_id','')::uuid;
  if person_id is not null then
   perform 1 from public.zakat_people where id=person_id and mosque_id=p_mosque and person_type=person_role for update;
   if not found then raise exception 'Jamaah tidak sesuai masjid atau jenis transaksi'; end if;
  else
   select array_agg(id) into existing_ids from public.zakat_people where mosque_id=p_mosque and person_type=person_role
    and lower(btrim(full_name))=lower(nm) and lower(btrim(coalesce(village,address,'')))=lower(village_value) and lower(btrim(coalesce(rt_rw,'')))=lower(rt_value);
   if coalesce(array_length(existing_ids,1),0)>1 then raise exception 'Ada beberapa jamaah dengan nama dan alamat sama. Pilih jamaah melalui form'; end if;
   person_id:=existing_ids[1];
  end if;
  if person_id is null then
   insert into public.zakat_people(mosque_id,full_name,person_type,asnaf,address,village,rt_rw,rt_name)
    values(p_mosque,nm,person_role,null,village_value,village_value,rt_value,nullif(rt_name_value,'')) returning id into person_id;
  else
   update public.zakat_people set full_name=nm,village=village_value,rt_rw=case when dir='distribution' and rt_value<>'' then rt_value else rt_rw end,rt_name=case when dir='distribution' and rt_name_value<>'' then rt_name_value else rt_name end where id=person_id and mosque_id=p_mosque;
  end if;
  if is_new then
   insert into public.zakat_entries(id,mosque_id,party_id,party_role,direction,zakat_type,amount,rice_kg,people_count,occurred_on,notes,party_name,village,rt_rw,rt_name)
   values(entry_id,p_mosque,person_id,person_role,dir,kind,cash,rice,coalesce((d->>'people_count')::integer,1),day_value,coalesce(d->>'notes',''),nm,village_value,rt_value,rt_name_value);
  else
   update public.zakat_entries set party_id=person_id,party_role=person_role,zakat_type=kind,amount=cash,rice_kg=rice,occurred_on=day_value,notes=coalesce(d->>'notes',''),party_name=nm,village=village_value,rt_rw=rt_value,rt_name=rt_name_value where id=entry_id and mosque_id=p_mosque;
   if not found then raise exception 'Transaksi tidak dapat diperbarui'; end if;
  end if;
  result_count:=result_count+1;
 end loop;
 return result_count;
end $$;
revoke all on function public.simas_save_zakat(uuid,jsonb) from public,anon;
grant execute on function public.simas_save_zakat(uuid,jsonb) to authenticated;
notify pgrst,'reload schema';
commit;
