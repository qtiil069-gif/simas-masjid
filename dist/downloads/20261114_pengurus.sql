begin;
create table if not exists public.mosque_officers(
 id uuid primary key default gen_random_uuid(),mosque_id uuid not null references public.mosques(id) on delete cascade,
 full_name text not null check(length(btrim(full_name)) between 2 and 150),position text not null check(length(btrim(position)) between 2 and 100),
 phone text not null default '' check(length(phone)<=40),address text not null default '' check(length(address)<=500),
 user_id uuid references auth.users(id) on delete set null,created_at timestamptz not null default now(),unique(mosque_id,user_id)
);
alter table public.mosque_officers enable row level security;
revoke all on public.mosque_officers from anon,authenticated;
grant select,delete on public.mosque_officers to authenticated;
grant insert(id,mosque_id,full_name,position,phone,address),update(full_name,position,phone,address) on public.mosque_officers to authenticated;
drop policy if exists officers_read on public.mosque_officers;
create policy officers_read on public.mosque_officers for select to authenticated using(public.simas_has_role(mosque_id,array['admin','takmir','bendahara','editor','viewer']::public.simas_role[]));
drop policy if exists officers_write on public.mosque_officers;
create policy officers_write on public.mosque_officers for all to authenticated using(public.simas_has_role(mosque_id,array['admin']::public.simas_role[])) with check(public.simas_has_role(mosque_id,array['admin']::public.simas_role[]));
-- Only the authenticated admin can link a provisioned user. Both records commit together.
create or replace function public.simas_link_officer(p_mosque uuid,p_officer uuid,p_user uuid,p_role public.simas_role) returns void
language plpgsql security definer set search_path='' as $$
begin
 if auth.uid() is null or not public.simas_has_role(p_mosque,array['admin']::public.simas_role[]) then raise exception 'Akses ditolak';end if;
 perform 1 from public.mosque_officers where id=p_officer and mosque_id=p_mosque and (user_id is null or user_id=p_user) for update;
 if not found then raise exception 'Pengurus tidak ditemukan atau sudah memiliki akun';end if;
 -- Metadata set only by the server on creation; ordinary callers cannot choose another user's identity.
 perform 1 from auth.users where id=p_user and raw_app_meta_data->>'simas_officer_id'=p_officer::text and raw_app_meta_data->>'simas_mosque_id'=p_mosque::text;
 if not found then raise exception 'Akun tidak sesuai pengurus';end if;
 insert into public.memberships(mosque_id,user_id,role) values(p_mosque,p_user,p_role) on conflict(mosque_id,user_id) do nothing;
 update public.mosque_officers set user_id=p_user where id=p_officer and mosque_id=p_mosque;
end $$;
revoke all on function public.simas_link_officer(uuid,uuid,uuid,public.simas_role) from public,anon;
grant execute on function public.simas_link_officer(uuid,uuid,uuid,public.simas_role) to authenticated;
notify pgrst,'reload schema';
commit;
