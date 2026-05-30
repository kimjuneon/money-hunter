alter table players
    add column if not exists last_skill_point_ad_claimed_at timestamptz;

alter table game_economy_policy
    add column if not exists skill_point_ad_cooldown_seconds bigint;
