-- SIMAS: additive migration. Run ONCE after schema.sql on the same Supabase project.
-- Existing announcement rows remain intact; their menu is removed from the app.
begin;
create table public.zakat_people (
 id uuid primary key default gen_random_uuid(),
 mosque_id uuid not null references public.mosques(id) on delete cascade,
 full_name text not null check(char_length(btrim(full_name)) between 2 and 150),
 person_type text not null check(person_type in ('muzakki','mustahik')),
 asnaf text check(asnaf in ('fakir','miskin','amil','mualaf','riqab','gharim','fisabilillah','ibnu_sabil')),
 household integer not null default 1 check(household>0), phone text check(char_length(phone)<=40), address text check(char_length(address)<=500),
 created_at timestamptz not null default now(),
 check ((person_type='muzakki' and asnaf is null) or (person_type='mustahik' and asnaf is not null)),
 unique(mosque_id,id,person_type)
);
create table public.zakat_entries (
 id uuid primary key default gen_random_uuid(),
 mosque_id uuid not null references public.mosques(id) on delete cascade,
 party_id uuid not null, party_role text not null,
 direction text not null check(direction in ('receipt','distribution')),
 zakat_type text not null check(zakat_type in ('fitrah','mal')),
 amount numeric(14,2) not null default 0 check(amount>=0),
 rice_kg numeric(12,3) not null default 0 check(rice_kg>=0),
 people_count integer not null default 1 check(people_count>0), occurred_on date not null default current_date,
 notes text not null default '' check(char_length(notes)<=1000),
 created_at timestamptz not null default now(),
 check(amount>0 or rice_kg>0), check(zakat_type='fitrah' or rice_kg=0),
 check((direction='receipt' and party_role='muzakki') or (direction='distribution' and party_role='mustahik')),
 foreign key(mosque_id,party_id,party_role) references public.zakat_people(mosque_id,id,person_type)
);
create table public.qurban_people (
 id uuid primary key default gen_random_uuid(), mosque_id uuid not null references public.mosques(id) on delete cascade,
 full_name text not null check(char_length(btrim(full_name)) between 2 and 150), phone text check(char_length(phone)<=40), address text check(char_length(address)<=500),
 created_at timestamptz not null default now(),unique(mosque_id,id)
);
create table public.qurban_animals (
 id uuid primary key default gen_random_uuid(), mosque_id uuid not null references public.mosques(id) on delete cascade,
 year integer not null check(year between 2000 and 2200),code text not null check(char_length(btrim(code)) between 1 and 40),
 species text not null check(species in ('sapi','kerbau','kambing','domba')),capacity integer not null check(capacity between 1 and 7),
 meat_kg numeric(12,3) not null default 0 check(meat_kg>=0),purchase_price numeric(14,2) not null default 0 check(purchase_price>=0),
 notes text not null default '' check(char_length(notes)<=1000),created_at timestamptz not null default now(),
 check(species in ('sapi','kerbau') or capacity=1),unique(mosque_id,year,code),unique(mosque_id,id,year)
);
create table public.qurban_shares (
 id uuid primary key default gen_random_uuid(),mosque_id uuid not null references public.mosques(id) on delete cascade,
 year integer not null,animal_id uuid not null,person_id uuid not null,shares integer not null check(shares between 1 and 7),created_at timestamptz not null default now(),
 foreign key(mosque_id,animal_id,year) references public.qurban_animals(mosque_id,id,year),
 foreign key(mosque_id,person_id) references public.qurban_people(mosque_id,id),unique(mosque_id,animal_id,person_id)
);
create table public.qurban_cash (
 id uuid primary key default gen_random_uuid(),mosque_id uuid not null references public.mosques(id) on delete cascade,
 year integer not null check(year between 2000 and 2200),person_id uuid,
 kind text not null check(kind in ('income','expense')),amount numeric(14,2) not null check(amount>0),occurred_on date not null default current_date,
 description text not null default '' check(char_length(description)<=1000),created_at timestamptz not null default now(),
 foreign key(mosque_id,person_id) references public.qurban_people(mosque_id,id)
);
create table public.qurban_distribution (
 id uuid primary key default gen_random_uuid(),mosque_id uuid not null references public.mosques(id) on delete cascade,
 year integer not null check(year between 2000 and 2200),coupon text not null check(char_length(btrim(coupon)) between 1 and 60),
 recipient text not null check(char_length(btrim(recipient)) between 2 and 150),address text check(char_length(address)<=500),
 weight_kg numeric(12,3) not null check(weight_kg>0),collected_at timestamptz,
 created_at timestamptz not null default now(),unique(mosque_id,year,coupon)
);
create table public.simas_module_audit (
 id bigint generated always as identity primary key,mosque_id uuid not null,
 table_name text not null,record_id uuid not null,operation text not null,previous_value jsonb,current_value jsonb,
 actor_id uuid,changed_at timestamptz not null default now()
);
-- Lock the parent before evaluating capacity; concurrent shares serialize on the animal.
create function public.simas_check_qurban_capacity() returns trigger
language plpgsql security definer set search_path='' as $$
declare cap integer; used integer;
begin
 if tg_table_name='qurban_shares' then
  select capacity into cap from public.qurban_animals where id=new.animal_id and mosque_id=new.mosque_id and year=new.year for update;
  if cap is null then raise exception 'Hewan/tahun tidak sesuai masjid'; end if;
  select coalesce(sum(shares),0) into used from public.qurban_shares where animal_id=new.animal_id and id<>new.id;
  if used+new.shares>cap then raise exception 'Jumlah bagian melebihi kapasitas hewan'; end if;
 else
  select coalesce(sum(shares),0) into used from public.qurban_shares where animal_id=new.id;
  if used>new.capacity then raise exception 'Kapasitas kurang dari bagian terdaftar'; end if;
 end if;
 return new;
end; $$;
revoke all on function public.simas_check_qurban_capacity() from public,anon,authenticated;
create trigger qurban_share_capacity before insert or update on public.qurban_shares for each row execute function public.simas_check_qurban_capacity();
create trigger qurban_animal_capacity before update on public.qurban_animals for each row execute function public.simas_check_qurban_capacity();
create function public.simas_module_log() returns trigger language plpgsql security definer set search_path='' as $$
begin
 insert into public.simas_module_audit(mosque_id,table_name,record_id,operation,previous_value,current_value,actor_id)
 values(new.mosque_id,tg_table_name,new.id,tg_op,case when tg_op='UPDATE' then to_jsonb(old) end,to_jsonb(new),auth.uid());
 return new;
end;$$;
revoke all on function public.simas_module_log() from public,anon,authenticated;
-- Explicit grants: tenant identity/id/creation time cannot be updated by browser clients.
do $$ declare t text; begin
 foreach t in array array['zakat_people','zakat_entries','qurban_people','qurban_animals','qurban_shares','qurban_cash','qurban_distribution'] loop
  execute format('alter table public.%I enable row level security',t);
  execute format('revoke all on public.%I from public, anon, authenticated',t);
  execute format('grant select, insert on public.%I to authenticated',t);
  execute format('create index %I on public.%I (mosque_id)',t||'_mosque_idx',t);
  execute format('create policy module_staff_read on public.%I for select to authenticated using (public.simas_has_role(mosque_id,array[''admin'',''bendahara'',''takmir'']::public.simas_role[]))',t);
  execute format('create policy module_staff_insert on public.%I for insert to authenticated with check (public.simas_has_role(mosque_id,array[''admin'',''bendahara'']::public.simas_role[]))',t);
  execute format('create policy module_staff_update on public.%I for update to authenticated using (public.simas_has_role(mosque_id,array[''admin'',''bendahara'']::public.simas_role[])) with check (public.simas_has_role(mosque_id,array[''admin'',''bendahara'']::public.simas_role[]))',t);
  execute format('create trigger log_module_change after insert or update on public.%I for each row execute function public.simas_module_log()',t);
 end loop;
end;$$;
grant update(full_name,person_type,asnaf,household,phone,address) on public.zakat_people to authenticated;
grant update(party_id,party_role,direction,zakat_type,amount,rice_kg,people_count,occurred_on,notes) on public.zakat_entries to authenticated;
grant update(full_name,phone,address) on public.qurban_people to authenticated;
grant update(year,code,species,capacity,meat_kg,purchase_price,notes) on public.qurban_animals to authenticated;
grant update(year,animal_id,person_id,shares) on public.qurban_shares to authenticated;
grant update(year,person_id,kind,amount,occurred_on,description) on public.qurban_cash to authenticated;
grant update(year,coupon,recipient,address,weight_kg,collected_at) on public.qurban_distribution to authenticated;
alter table public.simas_module_audit enable row level security;
revoke all on public.simas_module_audit from public,anon,authenticated;
grant select on public.simas_module_audit to authenticated;
create policy module_audit_read on public.simas_module_audit for select to authenticated using(public.simas_has_role(mosque_id,array['admin','bendahara']::public.simas_role[]));
create index module_audit_mosque_idx on public.simas_module_audit(mosque_id,changed_at desc);
create index zakat_entries_date_idx on public.zakat_entries(mosque_id,occurred_on);
create index qurban_cash_year_idx on public.qurban_cash(mosque_id,year);
commit;
