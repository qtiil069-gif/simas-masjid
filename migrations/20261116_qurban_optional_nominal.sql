-- Jalankan setelah 20261106_input_updates.sql. Aman dijalankan ulang.
-- Tidak mengubah peserta, setoran lama, trigger kas, atau kebijakan akses.
begin;
alter table public.qurban_people
 drop constraint if exists qurban_people_input_check;
alter table public.qurban_people add constraint qurban_people_input_check check (
 (qurban_category is null and year is null and nominal is null)
 or (
  qurban_category is not null and year is not null and nominal is not null
  and qurban_category in ('sapi_pribadi','sapi_patungan','kambing')
  and year between 2000 and 2200
  and nominal >= 0 and nominal <= 999999999999.99
  and (qurban_category <> 'sapi_patungan' or nominal > 0)
 )
);
notify pgrst, 'reload schema';
commit;
