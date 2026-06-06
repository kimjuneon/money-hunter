alter table players
    add column if not exists dungeon_explore_available_notification_date date,
    add column if not exists dungeon_explore_available_notification_run_count integer,
    add column if not exists dungeon_explore_available_notification_sent_at timestamp(6) with time zone,
    add column if not exists battle_ready_daily_streak_accessed_at timestamp(6) with time zone,
    add column if not exists battle_ready_daily_sent_stage integer not null default 0,
    add column if not exists battle_ready_daily_last_sent_at timestamp(6) with time zone;

create index if not exists idx_players_dungeon_explore_available_message
    on players(dungeon_run_count_date, dungeon_run_count, dungeon_next_available_at);

create index if not exists idx_players_battle_ready_daily_message
    on players(last_accessed_at, battle_ready_daily_sent_stage, battle_ready_daily_last_sent_at);
