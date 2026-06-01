alter table game_economy_policy
    add column if not exists auto_hunt_ad_cooldown_seconds bigint,
    add column if not exists boost_ad_cooldown_seconds bigint;
