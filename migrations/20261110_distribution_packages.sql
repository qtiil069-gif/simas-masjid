-- Run once after 20261106_distribution_profile.sql. Keeps historical totals unchanged.
begin;
alter table public.qurban_distribution
  alter column recipient drop not null,
  add column village text check(village is null or char_length(btrim(village)) between 2 and 200),
  add column rt_rw text check(rt_rw is null or char_length(btrim(rt_rw)) between 1 and 100),
  add column package_count integer check(package_count between 1 and 1000000),
  add column package_weight_kg numeric(12,3) check(package_weight_kg>0);
-- Historical weight was the TOTAL, so migrate as one package, never multiply old totals.
update public.qurban_distribution set package_count=1,package_weight_kg=weight_kg;
alter table public.qurban_distribution alter column package_count set not null,
  alter column package_weight_kg set not null;
create function public.simas_distribution_total() returns trigger
language plpgsql set search_path = '' as $$
begin
  -- Compatibility with old clients inserting recipient + total weight.
  if TG_OP='INSERT' and new.package_count is null and new.package_weight_kg is null and new.recipient is not null then
    new.package_count:=1; new.package_weight_kg:=new.weight_kg;
  end if;
  if new.package_count is null or new.package_weight_kg is null or new.package_count<1 or new.package_count>1000000 or new.package_weight_kg<=0 then
    raise exception 'Jumlah paket dan berat per paket harus positif';
  end if;
  if new.recipient is null and (nullif(btrim(new.village),'') is null or nullif(btrim(new.rt_rw),'') is null or nullif(btrim(new.rt_name),'') is null) then
    raise exception 'Nama RT, Desa/Dusun dan RT/RW wajib diisi';
  end if;
  new.weight_kg:=new.package_count::numeric*new.package_weight_kg;
  return new;
end $$;
revoke all on function public.simas_distribution_total() from public,anon,authenticated;
create trigger distribution_calculate_total before insert or update on public.qurban_distribution
for each row execute function public.simas_distribution_total();
alter table public.qurban_distribution add constraint distribution_total_matches
check(weight_kg=package_count::numeric*package_weight_kg);
grant update(village,rt_rw,package_count,package_weight_kg) on public.qurban_distribution to authenticated;
commit;
