alter table players
    add column if not exists dungeon_run_count_date date,
    add column if not exists dungeon_run_count integer not null default 0,
    add column if not exists dungeon_next_available_at timestamp(6) with time zone,
    add column if not exists boss_raid_ticket_count integer not null default 0;
