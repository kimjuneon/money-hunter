alter table players
    add column if not exists auto_hunt_ending_soon_notification_ends_at timestamp(6) with time zone,
    add column if not exists auto_hunt_ending_soon_notification_sent_at timestamp(6) with time zone;

create index if not exists idx_players_auto_hunt_ending_soon_message
    on players(auto_hunt_ends_at, auto_hunt_ending_soon_notification_ends_at, last_auto_hunt_ad_claimed_at);
