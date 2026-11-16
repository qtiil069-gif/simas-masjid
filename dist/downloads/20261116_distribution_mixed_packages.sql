-- Run after 20261115_distribution_recipients.sql. Repeatable; historical allocations are unchanged.
begin;
alter table public.qurban_distribution
 add column if not exists secondary_count integer not null default 0,
 add column if not exists secondary_weight_kg numeric(12,3) not null default 0;
alter table public.qurban_distribution drop constraint if exists distribution_total_matches;
alter table public.qurban_distribution drop constraint if exists distribution_mixed_packages_valid;
alter table public.qurban_distribution add constraint distribution_mixed_packages_valid check(
 secondary_count>=0 and secondary_count<package_count
 and ((secondary_count=0 and secondary_weight_kg=0) or
      (recipient_kind='shohibul' and secondary_count>0 and secondary_weight_kg>0))
);
create or replace function public.simas_distribution_total() returns trigger
language plpgsql set search_path='' as $$
declare p public.qurban_people;
begin
 if TG_OP='INSERT' and new.package_count is null and new.package_weight_kg is null and new.recipient is not null then new.package_count:=1;new.package_weight_kg:=new.weight_kg;end if;
 if new.package_count is null or new.package_weight_kg is null or new.package_count<1 or new.package_count>1000000 or new.package_weight_kg<=0 then raise exception 'Jumlah paket dan berat per paket harus positif';end if;
 if new.secondary_count is null or new.secondary_weight_kg is null or new.secondary_count<0 or new.secondary_count>=new.package_count or (new.secondary_count=0 and new.secondary_weight_kg<>0) or (new.secondary_count>0 and new.secondary_weight_kg<=0) then raise exception 'Komposisi bungkus tambahan tidak valid';end if;
 if new.recipient_kind='shohibul' then
  select * into p from public.qurban_people where mosque_id=new.mosque_id and id=new.shohibul_id and year=new.year for key share;
  if not found then raise exception 'Shohibul tidak ditemukan pada masjid dan tahun yang dipilih';end if;
  new.recipient:=p.full_name;new.village:=null;new.rt_rw:=null;new.rt_name:=null;
 else
  if new.secondary_count<>0 then raise exception 'Paket campuran hanya untuk Shohibul Qurban';end if;
  if new.shohibul_id is not null then raise exception 'Penerima umum tidak memakai ID shohibul';end if;
  if new.recipient is null and (nullif(btrim(new.village),'') is null or nullif(btrim(new.rt_rw),'') is null or nullif(btrim(new.rt_name),'') is null) then raise exception 'Nama RT, Desa/Dusun dan RW/RT wajib diisi';end if;
 end if;
 new.weight_kg:=(new.package_count-new.secondary_count)::numeric*new.package_weight_kg+new.secondary_count::numeric*new.secondary_weight_kg;
 return new;
end $$;
alter table public.qurban_distribution add constraint distribution_total_matches check(weight_kg=(package_count-secondary_count)::numeric*package_weight_kg+secondary_count::numeric*secondary_weight_kg);
grant update(secondary_count,secondary_weight_kg) on public.qurban_distribution to authenticated;
notify pgrst,'reload schema';
commit;
