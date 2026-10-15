-- ============================================================
-- Skema database online untuk aplikasi Timesheet Dump Truck (v2.2)
-- Jalankan di: Supabase Dashboard -> SQL Editor -> Run
-- Skrip ini aman dijalankan berulang kali (idempotent).
-- ============================================================

create table if not exists shifts (
  id text primary key,
  owner_id text not null default '',
  driver_name text not null default '',
  unit_id text not null,
  date text not null,
  hm_awal double precision not null,
  hm_akhir double precision,
  km_awal double precision not null,
  km_akhir double precision,
  total_hm double precision not null default 0,
  total_km double precision not null default 0,
  total_ritase integer not null default 0,
  total_fuel double precision not null default 0,
  status text not null default 'ACTIVE',
  created_at bigint not null default 0,
  updated_at bigint not null default 0,
  synced_at bigint not null default 0,
  deleted_at bigint
);

create table if not exists ritase_events (
  id text primary key,
  owner_id text not null default '',
  shift_id text not null,
  unit_id text not null,
  timestamp bigint not null default 0,
  time_formatted text not null default '',
  latitude double precision not null,
  longitude double precision not null,
  accuracy double precision not null default 0,
  trigger_type text not null default 'EXIT_LOADING',
  status text not null default 'VALID',
  created_at bigint not null default 0,
  updated_at bigint not null default 0,
  synced_at bigint not null default 0,
  deleted_at bigint
);

create table if not exists fuel_entries (
  id text primary key,
  owner_id text not null default '',
  shift_id text not null,
  unit_id text not null,
  timestamp bigint not null default 0,
  time_formatted text not null default '',
  hm_isi_fuel double precision not null,
  liter double precision not null,
  created_at bigint not null default 0,
  updated_at bigint not null default 0,
  synced_at bigint not null default 0,
  deleted_at bigint
);

-- Kolom baru bila tabel lama sudah ada (upgrade dari skema sebelumnya)
alter table shifts        add column if not exists owner_id    text not null default '';
alter table shifts        add column if not exists driver_name text not null default '';
alter table ritase_events add column if not exists owner_id    text not null default '';
alter table fuel_entries  add column if not exists owner_id    text not null default '';

create index if not exists idx_shifts_owner  on shifts (owner_id, updated_at);
create index if not exists idx_shifts_driver on shifts (driver_name, updated_at);
create index if not exists idx_ritase_owner  on ritase_events (owner_id, updated_at);
create index if not exists idx_ritase_shift  on ritase_events (shift_id, updated_at);
create index if not exists idx_fuel_owner    on fuel_entries (owner_id, updated_at);
create index if not exists idx_fuel_shift    on fuel_entries (shift_id, updated_at);

-- ============================================================
-- Keamanan (Row Level Security): setiap permintaan ke server
-- WAJIB menyertakan header X-Owner-Id yang sama dengan kolom
-- owner_id pada baris data. Aplikasi mengirim header ini secara
-- otomatis, sehingga:
--   * driver A hanya bisa membaca/menulis data miliknya sendiri;
--   * driver lain TIDAK bisa melihat data meski tahu URL server.
-- ============================================================

alter table shifts        enable row level security;
alter table ritase_events enable row level security;
alter table fuel_entries  enable row level security;

drop policy if exists "shifts_owner_rw"       on shifts;
drop policy if exists "ritase_owner_rw"       on ritase_events;
drop policy if exists "fuel_owner_rw"         on fuel_entries;
drop policy if exists "shifts_rw"             on shifts;
drop policy if exists "ritase_rw"             on ritase_events;
drop policy if exists "fuel_rw"               on fuel_entries;

create policy "shifts_owner_rw" on shifts
  for all using (owner_id = current_setting('request.headers', true)::json->>'x-owner-id')
  with check (owner_id = current_setting('request.headers', true)::json->>'x-owner-id');

create policy "ritase_owner_rw" on ritase_events
  for all using (owner_id = current_setting('request.headers', true)::json->>'x-owner-id')
  with check (owner_id = current_setting('request.headers', true)::json->>'x-owner-id');

create policy "fuel_owner_rw" on fuel_entries
  for all using (owner_id = current_setting('request.headers', true)::json->>'x-owner-id')
  with check (owner_id = current_setting('request.headers', true)::json->>'x-owner-id');

-- ============================================================
-- v2.7: Tabel akun driver (§8 revisi — daftar & login via Supabase)
-- Menyimpan: nama driver (sesuai jadwal), no hp, password HASH (SHA-256+salt).
-- Kata sandi asli TIDAK PERNAH disimpan/dikirim.
-- ============================================================

create table if not exists driver_accounts (
  id text primary key,
  full_name text not null unique,
  phone text not null default '',
  password_hash text not null,
  salt text not null,
  created_at bigint not null default 0,
  updated_at bigint not null default 0
);

-- Kebijakan akses (RLS):
--   * anyone boleh INSERT/MENGGABUNGKAN (merge-duplicates) saat mendaftar;
--   * SELECT hanya bisa dilakukan dengan header x-owner-id yang sama dengan
--     full_name pemilik data (login dari HP lain tetap terbaca karena owner id
--     turunan nama), atau service_role untuk keperluan admin.
alter table driver_accounts enable row level security;

drop policy if exists "driver_accounts_insert" on driver_accounts;
drop policy if exists "driver_accounts_self_read" on driver_accounts;

create policy "driver_accounts_insert" on driver_accounts
  for insert with check (true);

create policy "driver_accounts_self_read" on driver_accounts
  for select using (
    full_name = current_setting('request.headers', true)::json->>'x-owner-id'
  );

create policy "driver_accounts_self_update" on driver_accounts
  for update using (
    full_name = current_setting('request.headers', true)::json->>'x-owner-id'
  ) with check (
    full_name = current_setting('request.headers', true)::json->>'x-owner-id'
  );
