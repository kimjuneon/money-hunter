alter table players
	add column if not exists last_accessed_at timestamp with time zone;

update players
set last_accessed_at = coalesce(updated_at, created_at, now())
where last_accessed_at is null;

alter table players
	alter column last_accessed_at set not null;

create index if not exists idx_players_last_accessed_at
	on players (last_accessed_at desc);
