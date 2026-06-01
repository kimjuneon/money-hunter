alter table players
    add column if not exists auto_hunt_end_settled_gold bigint;

alter table notification_events
    add column if not exists settled_gold bigint;
