-- Apply after 20261105_zakat_qurban.sql. Existing rows are preserved.
begin;
alter table public.zakat_people add column rt_name text check(char_length(rt_name)<=100);
alter table public.qurban_people add column qurban_category text, add column year integer, add column nominal numeric(14,2), add constraint qurban_people_input_check check(
(qurban_category is null and year is null and nominal is null) or
(qurban_category is not null and year is not null and nominal is not null and qurban_category in ('sapi_pribadi','sapi_patungan','kambing') and year between 2000 and 2200 and ((qurban_category='sapi_pribadi' and nominal=0) or (qurban_category in ('sapi_patungan','kambing') and nominal>0))));
grant update(rt_name) on public.zakat_people to authenticated;
grant update(qurban_category,year,nominal) on public.qurban_people to authenticated;
create function public.simas_qurban_single_source() returns trigger language plpgsql security definer set search_path=public as $$
declare category text;
begin
 if tg_table_name='qurban_people' then
  if new.qurban_category is not null and exists(select 1 from public.qurban_cash where mosque_id=new.mosque_id and person_id=new.id and kind='income') then
   raise exception 'Peserta dengan setoran lama tidak dapat dikonversi; gunakan peserta baru untuk kegiatan baru.';
  end if;
 else
  if new.person_id is not null then
   select qurban_category into category from public.qurban_people where mosque_id=new.mosque_id and id=new.person_id for update;
   if new.kind='income' and category is not null then raise exception 'Penerimaan otomatis dari nominal Shohibul Qurban. Edit peserta untuk koreksi.'; end if;
  end if;
 end if;
 return new;
end $$;
revoke all on function public.simas_qurban_single_source() from public;
create trigger qurban_people_single_source before insert or update on public.qurban_people for each row execute function public.simas_qurban_single_source();
create trigger qurban_cash_single_source before insert or update on public.qurban_cash for each row execute function public.simas_qurban_single_source();
-- Existing RLS / tenant restrictions / full-row audit triggers remain in effect.
-- Cash report = legacy ledger + configured participant nominal, with no duplicate inserts.
commit;
