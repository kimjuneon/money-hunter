alter table players
    add column last_auto_hunt_ad_claimed_at timestamptz,
    add column last_boost_ad_claimed_at timestamptz;

update players p
set last_auto_hunt_ad_claimed_at = latest.last_occurred_at
from (
    select player_id, max(occurred_at) as last_occurred_at
    from ad_events
    where type = 'AUTO_HUNT'
    group by player_id
) latest
where p.id = latest.player_id;

update players p
set last_boost_ad_claimed_at = latest.last_occurred_at
from (
    select player_id, max(occurred_at) as last_occurred_at
    from ad_events
    where type = 'BOOST'
    group by player_id
) latest
where p.id = latest.player_id;
