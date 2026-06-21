create table ad_client_events (
    id bigserial primary key,
    player_id bigint not null references players(id) on delete cascade,
    type varchar(40) not null,
    event_type varchar(40) not null,
    ad_group_key varchar(60),
    ad_group_id varchar(120),
    session_token varchar(120),
    error_message varchar(500),
    occurred_at timestamptz not null,
    constraint chk_ad_client_events_event_type
        check (event_type in ('ATTEMPTED', 'LOAD_FAILED', 'SHOW_FAILED', 'PLAYED'))
);

create index idx_ad_client_events_player_type_time
    on ad_client_events(player_id, type, occurred_at desc);

create index idx_ad_client_events_type_event_time
    on ad_client_events(type, event_type, occurred_at desc);

create index idx_ad_client_events_occurred_at
    on ad_client_events(occurred_at desc);
