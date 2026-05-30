alter table game_economy_policy
    add column anomaly_limit_per_rule integer,
    add column anomaly_ad_events_per_hour_warning bigint,
    add column anomaly_reward_claims_per_day_warning bigint,
    add column anomaly_gold_threshold_multiplier bigint,
    add column anomaly_skill_points_warning integer,
    add column anomaly_timer_grace_seconds bigint;

create table admin_anomaly_cases (
    id bigserial primary key,
    anomaly_key varchar(220) not null unique,
    category varchar(80) not null,
    user_key varchar(180) not null,
    status varchar(20) not null,
    note varchar(1000),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    resolved_at timestamptz
);

create index idx_admin_anomaly_cases_status on admin_anomaly_cases(status);
create index idx_admin_anomaly_cases_user_key on admin_anomaly_cases(user_key);

create table admin_anomaly_actions (
    id bigserial primary key,
    anomaly_case_id bigint not null references admin_anomaly_cases(id) on delete cascade,
    status varchar(20) not null,
    note varchar(1000),
    actor_fingerprint varchar(24) not null,
    created_at timestamptz not null
);

create index idx_admin_anomaly_actions_case_created_at
    on admin_anomaly_actions(anomaly_case_id, created_at desc);
