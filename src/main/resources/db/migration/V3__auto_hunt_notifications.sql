alter table players
    add column auto_hunt_end_notified_at timestamptz;

create table notification_events (
    id bigserial primary key,
    player_id bigint not null references players(id),
    type varchar(40) not null,
    title varchar(120) not null,
    body varchar(500) not null,
    sent_at timestamptz not null,
    read_at timestamptz,
    created_at timestamptz not null
);

create index idx_notification_events_player_read_created
    on notification_events(player_id, read_at, created_at desc);

create index idx_players_auto_hunt_end_notification
    on players(auto_hunt_ends_at, auto_hunt_end_notified_at);
