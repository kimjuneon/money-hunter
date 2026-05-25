create table players (
    id bigserial primary key,
    user_key varchar(120) not null unique,
    job varchar(30),
    character_slots integer not null,
    gold bigint not null,
    skill_points integer not null,
    level integer not null,
    experience bigint not null,
    current_monster_key varchar(40) not null,
    current_monster_hp integer not null,
    defeated_monsters integer not null,
    auto_hunt_ends_at timestamptz,
    boost_ends_at timestamptz,
    last_settled_at timestamptz not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table player_skills (
    id bigserial primary key,
    player_id bigint not null references players(id),
    type varchar(40) not null,
    level integer not null,
    unique (player_id, type)
);

create table ad_events (
    id bigserial primary key,
    player_id bigint not null references players(id),
    type varchar(40) not null,
    reward_value integer not null,
    occurred_at timestamptz not null
);

create table reward_claims (
    id bigserial primary key,
    player_id bigint not null references players(id),
    gold_spent bigint not null,
    point_amount integer not null,
    status varchar(30) not null,
    idempotency_key varchar(120) not null unique,
    created_at timestamptz not null
);

create index idx_ad_events_player_occurred_at on ad_events(player_id, occurred_at);
create index idx_reward_claims_player_created_at on reward_claims(player_id, created_at);
