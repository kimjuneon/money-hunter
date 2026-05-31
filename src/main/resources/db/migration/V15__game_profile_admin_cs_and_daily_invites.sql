alter table players
    add column friend_invite_reward_date date,
    add column game_profile_nickname varchar(80),
    add column game_profile_updated_at timestamp(6) with time zone,
    add column admin_favorite boolean not null default false,
    add column cumulative_gold_earned bigint not null default 0;

update players
set cumulative_gold_earned = greatest(gold, 0)
where cumulative_gold_earned = 0;

create index idx_players_game_profile_nickname
    on players (lower(game_profile_nickname));

create index idx_players_admin_favorite_updated_at
    on players (admin_favorite, updated_at desc);

update game_economy_policy
set friend_invite_reward_skill_points = 1,
    updated_at = now()
where id = 1
  and (friend_invite_reward_skill_points is null or friend_invite_reward_skill_points = 5);

update game_economy_policy
set friend_invite_limit = 5,
    updated_at = now()
where id = 1
  and (friend_invite_limit is null or friend_invite_limit = 3);
