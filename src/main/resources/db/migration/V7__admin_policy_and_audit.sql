create table game_economy_policy (
    id bigint primary key,
    ad_revenue_per_reward_ad_won integer,
    gold_per_toss_point integer,
    companion_price_won integer,
    skill_point_pack_price_won integer,
    skill_point_pack_amount integer,
    friend_invite_reward_skill_points integer,
    friend_invite_limit integer,
    max_character_slots integer,
    auto_hunt_ad_seconds bigint,
    boost_ad_seconds bigint,
    max_ad_seconds bigint,
    reward_gold_threshold bigint,
    reward_point_amount integer,
    updated_at timestamptz not null,
    version bigint
);

insert into game_economy_policy (id, updated_at, version)
values (1, now(), 0)
on conflict (id) do nothing;

create table admin_audit_logs (
    id bigserial primary key,
    action varchar(40) not null,
    target varchar(80) not null,
    actor_fingerprint varchar(24) not null,
    before_value varchar(4000),
    after_value varchar(4000),
    reason varchar(500),
    client_ip varchar(80),
    user_agent varchar(300),
    created_at timestamptz not null
);

create index idx_admin_audit_logs_created_at on admin_audit_logs(created_at desc);
