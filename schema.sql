-- SIMas MVP | Supabase/PostgreSQL. Run on a NEW project in a migration.
begin;

create type public.simas_role as enum ('admin', 'takmir', 'bendahara', 'editor', 'viewer');
create type public.simas_finance_kind as enum ('income', 'expense');

create table public.mosques (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique check (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
  name text not null check (char_length(btrim(name)) between 2 and 150),
  address text, city text, phone text,
  created_at timestamptz not null default now()
);

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null default 'Pengguna' check (char_length(full_name) between 1 and 150),
  created_at timestamptz not null default now()
);

create table public.memberships (
  mosque_id uuid not null references public.mosques(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role public.simas_role not null default 'viewer',
  created_at timestamptz not null default now(),
  primary key(mosque_id, user_id)
);
create index memberships_user_id_idx on public.memberships(user_id);

create table public.events (
  id uuid primary key default gen_random_uuid(),
  mosque_id uuid not null references public.mosques(id) on delete cascade,
  title text not null check (char_length(btrim(title)) between 3 and 180),
  description text not null default '',
  starts_at timestamptz not null,
  ends_at timestamptz,
  is_published boolean not null default false,
  created_by uuid not null default auth.uid() references auth.users(id),
  created_at timestamptz not null default now(),
  constraint events_end_after_start check (ends_at is null or ends_at >= starts_at)
);
create index events_mosque_date_idx on public.events(mosque_id, starts_at);

create table public.announcements (
  id uuid primary key default gen_random_uuid(),
  mosque_id uuid not null references public.mosques(id) on delete cascade,
  title text not null check (char_length(btrim(title)) between 3 and 180),
  body text not null default '',
  is_published boolean not null default false,
  created_by uuid not null default auth.uid() references auth.users(id),
  created_at timestamptz not null default now()
);
create index announcements_mosque_created_idx on public.announcements(mosque_id, created_at desc);

create table public.finance_categories (
  id uuid primary key default gen_random_uuid(),
  mosque_id uuid not null references public.mosques(id) on delete cascade,
  kind public.simas_finance_kind not null,
  name text not null check (char_length(btrim(name)) between 2 and 100),
  unique(mosque_id, kind, name),
  unique(mosque_id, id, kind)
);
create index finance_categories_mosque_idx on public.finance_categories(mosque_id);

create table public.finance_transactions (
  id uuid primary key default gen_random_uuid(),
  mosque_id uuid not null references public.mosques(id) on delete cascade,
  category_id uuid not null,
  kind public.simas_finance_kind not null,
  amount numeric(18,2) not null check (amount > 0),
  description text not null default '',
  occurred_on date not null default current_date,
  receipt_path text,
  created_by uuid not null default auth.uid() references auth.users(id),
  created_at timestamptz not null default now(),
  constraint transaction_category_same_mosque_and_kind
    foreign key (mosque_id, category_id, kind)
    references public.finance_categories (mosque_id, id, kind)
);
create index finance_transactions_mosque_date_idx on public.finance_transactions(mosque_id, occurred_on desc);

create table public.assets (
  id uuid primary key default gen_random_uuid(),
  mosque_id uuid not null references public.mosques(id) on delete cascade,
  name text not null check (char_length(btrim(name)) between 2 and 160),
  quantity integer not null default 1 check (quantity >= 0),
  condition text not null default 'baik' check (condition in ('baik', 'perlu_perbaikan', 'rusak')),
  location text,
  acquired_on date,
  created_at timestamptz not null default now()
);
create index assets_mosque_idx on public.assets(mosque_id);

create table public.finance_audit (
  id bigint generated always as identity primary key,
  mosque_id uuid not null,
  transaction_id uuid not null,
  operation text not null check (operation in ('INSERT','UPDATE','DELETE')),
  previous_value jsonb,
  current_value jsonb,
  actor_id uuid,
  changed_at timestamptz not null default now()
);
create index finance_audit_mosque_changed_idx on public.finance_audit(mosque_id, changed_at desc);

-- Signup creates a profile, never an organization membership or role.
create function public.simas_create_profile() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  insert into public.profiles(id, full_name)
  values(new.id, coalesce(nullif(btrim(new.raw_user_meta_data->>'full_name'), ''),
                          nullif(split_part(coalesce(new.email,''),'@',1),''), 'Pengguna'));
  return new;
end; $$;
create trigger on_simas_auth_user_created after insert on auth.users
for each row execute function public.simas_create_profile();
revoke all on function public.simas_create_profile() from public, anon, authenticated;

-- This helper can only inspect the CALLER'S own mosque membership.
-- SECURITY DEFINER breaks recursive RLS evaluation on memberships.
create function public.simas_has_role(p_mosque_id uuid, p_allowed public.simas_role[])
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from public.memberships m
    where m.mosque_id = p_mosque_id
      and m.user_id = (select auth.uid())
      and m.role = any(p_allowed)
  );
$$;
revoke all on function public.simas_has_role(uuid, public.simas_role[]) from public, anon, authenticated;
grant execute on function public.simas_has_role(uuid, public.simas_role[]) to authenticated;

create function public.simas_log_finance_change() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  insert into public.finance_audit(mosque_id, transaction_id, operation, previous_value, current_value, actor_id)
  values(coalesce(new.mosque_id,old.mosque_id), coalesce(new.id,old.id), tg_op,
         case when tg_op <> 'INSERT' then to_jsonb(old) end,
         case when tg_op <> 'DELETE' then to_jsonb(new) end,
         auth.uid());
  return coalesce(new, old);
end; $$;
create trigger on_finance_change after insert or update or delete on public.finance_transactions
for each row execute function public.simas_log_finance_change();
revoke all on function public.simas_log_finance_change() from public, anon, authenticated;

-- Finance aggregate is invoker/RLS-aware, not a bypass view.
create function public.simas_finance_summary(p_mosque_id uuid)
returns table(total_income numeric, total_expense numeric, balance numeric)
language sql stable security invoker set search_path = '' as $$
 select coalesce(sum(amount) filter (where kind = 'income'), 0),
        coalesce(sum(amount) filter (where kind = 'expense'), 0),
        coalesce(sum(case when kind = 'income' then amount else -amount end), 0)
 from public.finance_transactions
 where mosque_id = p_mosque_id
   and public.simas_has_role(p_mosque_id,
     array['admin','takmir','bendahara']::public.simas_role[])
$$;
revoke all on function public.simas_finance_summary(uuid) from public, anon, authenticated;
grant execute on function public.simas_finance_summary(uuid) to authenticated;

-- RLS and explicit grants: authenticated client is NOT a database administrator.
alter table public.mosques enable row level security;
alter table public.profiles enable row level security;
alter table public.memberships enable row level security;
alter table public.events enable row level security;
alter table public.announcements enable row level security;
alter table public.finance_categories enable row level security;
alter table public.finance_transactions enable row level security;
alter table public.assets enable row level security;
alter table public.finance_audit enable row level security;

revoke all on table public.mosques, public.profiles, public.memberships,
  public.events, public.announcements, public.finance_categories,
  public.finance_transactions, public.assets, public.finance_audit
from public, anon, authenticated;

grant select on public.mosques to anon, authenticated;
grant update(name, address, city, phone) on public.mosques to authenticated;
grant select on public.profiles to authenticated;
grant update(full_name) on public.profiles to authenticated;
grant select, insert, update(role), delete on public.memberships to authenticated;
grant select on public.events, public.announcements to anon, authenticated;
grant insert on public.events, public.announcements to authenticated;
grant update(title, description, starts_at, ends_at, is_published) on public.events to authenticated;
grant update(title, body, is_published) on public.announcements to authenticated;
grant delete on public.events, public.announcements to authenticated;
grant select, insert on public.finance_categories to authenticated;
grant update(name) on public.finance_categories to authenticated;
grant select, insert on public.finance_transactions to authenticated;
grant update(category_id, kind, amount, description, occurred_on, receipt_path) on public.finance_transactions to authenticated;
grant select, insert, delete on public.assets to authenticated;
grant update(name, quantity, condition, location, acquired_on) on public.assets to authenticated;
grant select on public.finance_audit to authenticated;

create policy mosque_public_read on public.mosques for select to anon, authenticated using (true);
create policy mosque_admin_update on public.mosques for update to authenticated
using (public.simas_has_role(id, array['admin']::public.simas_role[]))
with check (public.simas_has_role(id, array['admin']::public.simas_role[]));

create policy profile_own_read on public.profiles for select to authenticated
using (id=(select auth.uid()));
create policy profile_own_update on public.profiles for update to authenticated
using (id=(select auth.uid())) with check (id=(select auth.uid()));

create policy membership_self_or_admin_read on public.memberships for select to authenticated
using (user_id=(select auth.uid()) or public.simas_has_role(mosque_id,array['admin']::public.simas_role[]));
create policy membership_admin_insert on public.memberships for insert to authenticated
with check (public.simas_has_role(mosque_id,array['admin']::public.simas_role[]));
create policy membership_admin_update on public.memberships for update to authenticated
using (public.simas_has_role(mosque_id,array['admin']::public.simas_role[]))
with check (public.simas_has_role(mosque_id,array['admin']::public.simas_role[]));
create policy membership_admin_delete on public.memberships for delete to authenticated
using (public.simas_has_role(mosque_id,array['admin']::public.simas_role[]));

create policy events_public_read on public.events for select to anon, authenticated using (is_published);
create policy events_member_read on public.events for select to authenticated
using (public.simas_has_role(mosque_id,array['admin','takmir','bendahara','editor','viewer']::public.simas_role[]));
create policy events_staff_insert on public.events for insert to authenticated
with check (created_by=(select auth.uid()) and
 public.simas_has_role(mosque_id,array['admin','takmir','editor']::public.simas_role[]));
create policy events_staff_update on public.events for update to authenticated
using (public.simas_has_role(mosque_id,array['admin','takmir','editor']::public.simas_role[]))
with check (public.simas_has_role(mosque_id,array['admin','takmir','editor']::public.simas_role[]));
create policy events_staff_delete on public.events for delete to authenticated
using (public.simas_has_role(mosque_id,array['admin','takmir','editor']::public.simas_role[]));

create policy announcements_public_read on public.announcements for select to anon, authenticated using (is_published);
create policy announcements_member_read on public.announcements for select to authenticated
using (public.simas_has_role(mosque_id,array['admin','takmir','bendahara','editor','viewer']::public.simas_role[]));
create policy announcements_staff_insert on public.announcements for insert to authenticated
with check (created_by=(select auth.uid()) and
 public.simas_has_role(mosque_id,array['admin','takmir','editor']::public.simas_role[]));
create policy announcements_staff_update on public.announcements for update to authenticated
using (public.simas_has_role(mosque_id,array['admin','takmir','editor']::public.simas_role[]))
with check (public.simas_has_role(mosque_id,array['admin','takmir','editor']::public.simas_role[]));
create policy announcements_staff_delete on public.announcements for delete to authenticated
using (public.simas_has_role(mosque_id,array['admin','takmir','editor']::public.simas_role[]));

create policy finance_categories_staff_read on public.finance_categories for select to authenticated
using (public.simas_has_role(mosque_id,array['admin','takmir','bendahara']::public.simas_role[]));
create policy finance_categories_staff_insert on public.finance_categories for insert to authenticated
with check (public.simas_has_role(mosque_id,array['admin','bendahara']::public.simas_role[]));
create policy finance_categories_staff_update on public.finance_categories for update to authenticated
using (public.simas_has_role(mosque_id,array['admin','bendahara']::public.simas_role[]))
with check (public.simas_has_role(mosque_id,array['admin','bendahara']::public.simas_role[]));

create policy finance_transactions_staff_read on public.finance_transactions for select to authenticated
using (public.simas_has_role(mosque_id,array['admin','takmir','bendahara']::public.simas_role[]));
create policy finance_transactions_staff_insert on public.finance_transactions for insert to authenticated
with check (created_by=(select auth.uid()) and
 public.simas_has_role(mosque_id,array['admin','bendahara']::public.simas_role[]));
create policy finance_transactions_staff_update on public.finance_transactions for update to authenticated
using (public.simas_has_role(mosque_id,array['admin','bendahara']::public.simas_role[]))
with check (public.simas_has_role(mosque_id,array['admin','bendahara']::public.simas_role[]));
-- DELETE intentionally unavailable: no grant, no policy; corrections are audit-tracked UPDATEs.

create policy assets_member_read on public.assets for select to authenticated
using (public.simas_has_role(mosque_id,array['admin','takmir','bendahara','editor','viewer']::public.simas_role[]));
create policy assets_staff_insert on public.assets for insert to authenticated
with check (public.simas_has_role(mosque_id,array['admin','takmir']::public.simas_role[]));
create policy assets_staff_update on public.assets for update to authenticated
using (public.simas_has_role(mosque_id,array['admin','takmir']::public.simas_role[]))
with check (public.simas_has_role(mosque_id,array['admin','takmir']::public.simas_role[]));
create policy assets_staff_delete on public.assets for delete to authenticated
using (public.simas_has_role(mosque_id,array['admin','takmir']::public.simas_role[]));

create policy finance_audit_staff_read on public.finance_audit for select to authenticated
using (public.simas_has_role(mosque_id,array['admin','bendahara']::public.simas_role[]));

commit;
