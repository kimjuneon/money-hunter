alter table players
    add column if not exists rookie_event_daily_skill_point_help_claimed boolean not null default false,
    add column if not exists auto_hunt_end_level_gain integer not null default 0,
    add column if not exists auto_hunt_end_skill_point_gain integer not null default 0,
    add column if not exists auto_hunt_end_combat_power_gain bigint not null default 0;

alter table notification_events
    add column if not exists level_gain integer not null default 0,
    add column if not exists skill_point_gain integer not null default 0,
    add column if not exists combat_power_gain bigint not null default 0;
