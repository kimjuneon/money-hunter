alter table players
    add column if not exists auto_hunt_end_smart_message_attempted_at timestamptz;

create index if not exists idx_players_auto_hunt_smart_message_retry
    on players(auto_hunt_ends_at, auto_hunt_end_notified_at, auto_hunt_end_smart_message_attempted_at);
