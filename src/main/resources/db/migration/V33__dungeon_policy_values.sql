alter table game_economy_policy
    add column if not exists dungeon_free_daily_limit integer,
    add column if not exists dungeon_additional_daily_limit integer,
    add column if not exists dungeon_reentry_cooldown_seconds bigint;
