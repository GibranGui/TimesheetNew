-- ============================================================
-- KEAMANAN LANJUTAN (RLS KETAT) - v2.2
-- Jalankan HANYA setelah seluruh driver memakai aplikasi versi
-- terbaru (yang mengirim header X-Owner-Id). Aman dijalankan
-- berulang kali.
--
-- Efek: di server, tiap baris data hanya bisa dibaca/diubah
-- oleh pemiliknya. Meski orang lain tahu URL & kunci aplikasi,
-- mereka tidak akan bisa membaca data driver lain.
-- ============================================================

alter table shifts        enable row level security;
alter table ritase_events enable row level security;
alter table fuel_entries  enable row level security;

drop policy if exists "shifts_rw"       on shifts;
drop policy if exists "ritase_rw"       on ritase_events;
drop policy if exists "fuel_rw"         on fuel_entries;
drop policy if exists "shifts_owner_rw" on shifts;
drop policy if exists "ritase_owner_rw" on ritase_events;
drop policy if exists "fuel_owner_rw"   on fuel_entries;

create policy "shifts_owner_rw" on shifts
  for all using (owner_id = current_setting('request.headers', true)::json->>'x-owner-id')
  with check (owner_id = current_setting('request.headers', true)::json->>'x-owner-id');

create policy "ritase_owner_rw" on ritase_events
  for all using (owner_id = current_setting('request.headers', true)::json->>'x-owner-id')
  with check (owner_id = current_setting('request.headers', true)::json->>'x-owner-id');

create policy "fuel_owner_rw" on fuel_entries
  for all using (owner_id = current_setting('request.headers', true)::json->>'x-owner-id')
  with check (owner_id = current_setting('request.headers', true)::json->>'x-owner-id');
