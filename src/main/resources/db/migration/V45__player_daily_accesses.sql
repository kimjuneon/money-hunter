create table player_daily_accesses (
    id bigserial primary key,
    player_id bigint not null references players(id) on delete cascade,
    access_date date not null,
    first_accessed_at timestamptz not null,
    last_accessed_at timestamptz not null,
    constraint uk_player_daily_accesses_player_date unique (player_id, access_date)
);

create index idx_player_daily_accesses_date_player
    on player_daily_accesses(access_date, player_id);

create index idx_player_daily_accesses_player_date
    on player_daily_accesses(player_id, access_date);

insert into player_daily_accesses (player_id, access_date, first_accessed_at, last_accessed_at)
select
    p.id,
    (s.created_at at time zone 'Asia/Seoul')::date,
    min(s.created_at),
    max(s.created_at)
from login_sessions s
join players p on p.user_key = s.user_key
where s.created_at >= (((now() at time zone 'Asia/Seoul')::date - interval '6 days') at time zone 'Asia/Seoul')
group by p.id, (s.created_at at time zone 'Asia/Seoul')::date
on conflict (player_id, access_date) do update
set first_accessed_at = least(player_daily_accesses.first_accessed_at, excluded.first_accessed_at),
    last_accessed_at = greatest(player_daily_accesses.last_accessed_at, excluded.last_accessed_at);

insert into player_daily_accesses (player_id, access_date, first_accessed_at, last_accessed_at)
select
    id,
    (last_accessed_at at time zone 'Asia/Seoul')::date,
    last_accessed_at,
    last_accessed_at
from players
where last_accessed_at >= (((now() at time zone 'Asia/Seoul')::date - interval '6 days') at time zone 'Asia/Seoul')
on conflict (player_id, access_date) do update
set first_accessed_at = least(player_daily_accesses.first_accessed_at, excluded.first_accessed_at),
    last_accessed_at = greatest(player_daily_accesses.last_accessed_at, excluded.last_accessed_at);
