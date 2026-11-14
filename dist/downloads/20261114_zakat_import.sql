-- Run after 20261112_zakat_optional_rt.sql. Does not delete existing data.
begin;
create index if not exists zakat_import_lookup on public.zakat_entries(mosque_id,occurred_on,direction,zakat_type);
create index if not exists zakat_people_import_lookup on public.zakat_people(mosque_id,person_type,lower(btrim(full_name)));
create or replace function public.simas_import_zakat(p_mosque uuid,p_rows jsonb) returns jsonb
language plpgsql security invoker set search_path='' as $$
declare d jsonb; saved integer:=0; skipped integer:=0;
begin
 if auth.uid() is null or not public.simas_has_role(p_mosque,array['admin','bendahara']::public.simas_role[]) then raise exception 'Akses ditolak'; end if;
 if jsonb_typeof(p_rows) is distinct from 'array' then raise exception 'Data harus berupa daftar'; end if;
 if jsonb_array_length(p_rows) not between 1 and 25 then raise exception 'Maksimal 25 baris per proses'; end if;
 perform pg_advisory_xact_lock(hashtextextended(p_mosque::text,412));
 for d in select value from jsonb_array_elements(p_rows) loop
  if exists(select 1 from public.zakat_entries e where e.mosque_id=p_mosque
   and e.occurred_on=(d->>'occurred_on')::date and e.direction=d->>'direction' and e.zakat_type=d->>'zakat_type'
   and coalesce(e.party_name,'')=btrim(d->>'full_name') and coalesce(e.village,'')=btrim(d->>'village')
   and coalesce(e.rt_rw,'')=coalesce(btrim(d->>'rt_rw'),'') and coalesce(e.rt_name,'')=coalesce(btrim(d->>'rt_name'),'')
   and e.amount=coalesce((d->>'amount')::numeric,0) and e.rice_kg=coalesce((d->>'rice_kg')::numeric,0)
   and coalesce(e.people_count,1)=coalesce((d->>'people_count')::integer,1)
   and coalesce(e.notes,'')=coalesce(d->>'notes','')) then
   skipped:=skipped+1;
  else
   perform public.simas_save_zakat(p_mosque,jsonb_build_array(d||jsonb_build_object('is_new',true,'id',gen_random_uuid())));
   saved:=saved+1;
  end if;
 end loop;
 return jsonb_build_object('saved',saved,'skipped',skipped);
end $$;
revoke all on function public.simas_import_zakat(uuid,jsonb) from public,anon;
grant execute on function public.simas_import_zakat(uuid,jsonb) to authenticated;
notify pgrst,'reload schema';
commit;
