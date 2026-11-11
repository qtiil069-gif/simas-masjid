-- Apply after 20261105_zakat_qurban.sql and 20261106_input_updates.sql.
begin;
alter table public.qurban_distribution add column rt_name text check(char_length(rt_name)<=100);
grant update(rt_name) on public.qurban_distribution to authenticated;
alter table public.mosques add column logo_data_url text check(logo_data_url is null or (char_length(logo_data_url)<=300000 and logo_data_url ~ '^data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/]+=*$'));
grant update(logo_data_url) on public.mosques to authenticated;
do $$ declare t text; begin
 foreach t in array array['finance_transactions','finance_categories','zakat_people','zakat_entries','qurban_people','qurban_animals','qurban_shares','qurban_cash','qurban_distribution'] loop
  execute format('grant delete on public.%I to authenticated',t);
  execute format('create policy staff_delete on public.%I for delete to authenticated using(public.simas_has_role(mosque_id,array[''admin'',''bendahara'']::public.simas_role[]))',t);
 end loop;
end $$;
-- Finance already records DELETE in finance_audit. Retain old snapshots for other inputs.
create function public.simas_log_input_delete() returns trigger language plpgsql security definer set search_path='' as $$
begin
 insert into public.simas_module_audit(mosque_id,table_name,record_id,operation,previous_value,current_value,actor_id)
 values(old.mosque_id,tg_table_name,coalesce(to_jsonb(old)->>'id',to_jsonb(old)->>'user_id')::uuid,'DELETE',to_jsonb(old),null,auth.uid());
 return old;
end $$;
revoke all on function public.simas_log_input_delete() from public,anon,authenticated;
do $$ declare t text; begin
 foreach t in array array['finance_categories','events','assets','memberships','zakat_people','zakat_entries','qurban_people','qurban_animals','qurban_shares','qurban_cash','qurban_distribution'] loop
  execute format('create trigger log_input_delete after delete on public.%I for each row execute function public.simas_log_input_delete()',t);
 end loop;
end $$;
create function public.simas_protect_membership() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if tg_op='DELETE' or new.role<>old.role then
  perform 1 from public.mosques where id=old.mosque_id for update;
  if old.user_id=auth.uid() then raise exception 'Keanggotaan/peran akun sendiri tidak dapat dihapus atau diubah.'; end if;
  if old.role='admin' and not exists(select 1 from public.memberships where mosque_id=old.mosque_id and role='admin' and user_id<>old.user_id) then raise exception 'Admin terakhir tidak dapat dihapus.'; end if;
 end if;
 if tg_op='DELETE' then return old; end if;
 return new;
end $$;
revoke all on function public.simas_protect_membership() from public,anon,authenticated;
create trigger protect_membership before delete or update of role on public.memberships for each row execute function public.simas_protect_membership();
commit;
