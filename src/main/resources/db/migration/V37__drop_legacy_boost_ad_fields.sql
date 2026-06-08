delete from ad_reward_sessions
where type = 'BOOST';

delete from ad_events
where type = 'BOOST';

alter table players
    drop column if exists boost_ends_at,
    drop column if exists last_boost_ad_claimed_at,
    drop column if exists rookie_event_daily_boost_monsters;

alter table game_economy_policy
    drop column if exists boost_ad_seconds,
    drop column if exists boost_ad_cooldown_seconds;
