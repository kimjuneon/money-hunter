insert into game_economy_policy (id, updated_at, version)
values (1, now(), 0)
on conflict (id) do nothing;

with previous_policy as (
    select coalesce(nullif(gold_per_toss_point, 0), 100)::numeric as old_gold_per_toss_point
    from game_economy_policy
    where id = 1
      and coalesce(nullif(gold_per_toss_point, 0), 100) < 724
)
update players
set gold = cast(least(9223372036854775807::numeric, floor(greatest(gold, 0)::numeric * 724 / previous_policy.old_gold_per_toss_point)) as bigint),
    hit_gold_remainder_micros = cast(least(9223372036854775807::numeric, floor(greatest(hit_gold_remainder_micros, 0)::numeric * 724 / previous_policy.old_gold_per_toss_point)) as bigint),
    defeat_gold_remainder_micros = cast(least(9223372036854775807::numeric, floor(greatest(defeat_gold_remainder_micros, 0)::numeric * 724 / previous_policy.old_gold_per_toss_point)) as bigint),
    auto_hunt_end_settled_gold = case
        when auto_hunt_end_settled_gold is null then null
        else cast(least(9223372036854775807::numeric, floor(greatest(auto_hunt_end_settled_gold, 0)::numeric * 724 / previous_policy.old_gold_per_toss_point)) as bigint)
    end,
    updated_at = now()
from previous_policy
where gold > 0
   or hit_gold_remainder_micros > 0
   or defeat_gold_remainder_micros > 0
   or coalesce(auto_hunt_end_settled_gold, 0) > 0;

update game_economy_policy
set gold_per_toss_point = case
        when coalesce(nullif(gold_per_toss_point, 0), 100) < 724 then 724
        else gold_per_toss_point
    end,
    auto_hunt_ad_seconds = 14400,
    max_ad_seconds = 43200,
    reward_gold_threshold = 7240,
    updated_at = now()
where id = 1
  and (
      coalesce(nullif(gold_per_toss_point, 0), 100) < 724
      or auto_hunt_ad_seconds is distinct from 14400
      or max_ad_seconds is distinct from 43200
      or reward_gold_threshold is distinct from 7240
  );
