alter table players
    add column if not exists tutorial_reward_claimed_at timestamptz;

create table if not exists ad_reward_sessions (
    id bigserial primary key,
    player_id bigint not null references players(id),
    type varchar(40) not null,
    session_token varchar(120) not null unique,
    created_at timestamptz not null,
    expires_at timestamptz not null,
    completed_at timestamptz
);

create index if not exists idx_ad_reward_sessions_player_created_at
    on ad_reward_sessions(player_id, created_at desc);

update game_economy_policy
set reward_gold_threshold = coalesce(reward_gold_threshold, 1000),
    reward_point_amount = coalesce(reward_point_amount, 10),
    updated_at = now()
where id = 1;
