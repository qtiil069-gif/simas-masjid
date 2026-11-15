-- Run after 20261110_distribution_packages.sql. Existing records remain Penerima Qurban.
begin;
alter table public.qurban_distribution add column if not exists recipient_kind text not null default 'penerima' check(recipient_kind in ('penerima','shohibul'));
alter table public.qurban_distribution add column if not exists shohibul_id uuid;
alter table public.qurban_distribution add column if not exists notes text not null default '' check(length(notes)<=1000);
do $$ begin
 if not exists(select 1 from pg_constraint where conname='distribution_shohibul_fk' and conrelid='public.qurban_distribution'::regclass) then
 alter table public.qurban_distribution add constraint distribution_shohibul_fk foreign key(mosque_id,shohibul_id) references public.qurban_people(mosque_id,id);
 end if;
end $$;
create or replace function public.simas_distribution_total() returns trigger
language plpgsql set search_path='' as $$
declare p public.qurban_people;
begin
 if TG_OP='INSERT' and new.package_count is null and new.package_weight_kg is null and new.recipient is not null then new.package_count:=1;new.package_weight_kg:=new.weight_kg;end if;
 if new.package_count is null or new.package_weight_kg is null or new.package_count<1 or new.package_count>1000000 or new.package_weight_kg<=0 then raise exception 'Jumlah paket dan berat per paket harus positif';end if;
 if new.recipient_kind='shohibul' then
  select * into p from public.qurban_people where mosque_id=new.mosque_id and id=new.shohibul_id and year=new.year for key share;
  if not found then raise exception 'Shohibul tidak ditemukan pada masjid dan tahun yang dipilih';end if;
  new.recipient:=p.full_name;new.village:=null;new.rt_rw:=null;new.rt_name:=null;
 else
  if new.shohibul_id is not null then raise exception 'Penerima umum tidak memakai ID shohibul';end if;
  if new.recipient is null and (nullif(btrim(new.village),'') is null or nullif(btrim(new.rt_rw),'') is null or nullif(btrim(new.rt_name),'') is null) then raise exception 'Nama RT, Desa/Dusun dan RW/RT wajib diisi';end if;
 end if;
 new.weight_kg:=new.package_count::numeric*new.package_weight_kg;return new;
end $$;
grant update(recipient_kind,shohibul_id,notes) on public.qurban_distribution to authenticated;
notify pgrst,'reload schema';
commit;
