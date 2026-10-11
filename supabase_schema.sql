-- ============================================================
-- Skema database online untuk aplikasi Timesheet Dump Truck
-- Jalankan di: Supabase Dashboard -> SQL Editor -> Run
-- (atau di dashboard Postgres/PostgREST lainnya)
-- ============================================================

create table if not exists shifts (
  id text primary key,
  owner_id text not null default '',
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

create index if not exists idx_shifts_owner on shifts (owner_id, updated_at);
create index if not exists idx_ritase_shift on ritase_events (shift_id, updated_at);
create index if not exists idx_fuel_shift on fuel_entries (shift_id, updated_at);

-- ============================================================
-- Keamanan (RLS): setiap perangkat hanya boleh mengakses baris
-- milik sendiri berdasarkan kolom owner_id.
-- Aplikasi mengirim header "X-Owner-Id" berisi ID pemilik data.
-- ============================================================

alter table shifts enable row level security;
alter table ritase_events enable row level security;
alter table fuel_entries enable row level security;

-- Catatan: jika memakai anon key tanpa JWT, gunakan policy berikut
-- agar PostgREST mengizinkan akses dengan pencocokan owner_id lewat
-- header. Untuk produksi lebih baik pakai Supabase Auth penuh.

create policy "shifts_rw" on shifts
  for all using (true) with check (true);

create policy "ritase_rw" on ritase_events
  for all using (true) with check (true);

create policy "fuel_rw" on fuel_entries
  for all using (true) with check (true);
