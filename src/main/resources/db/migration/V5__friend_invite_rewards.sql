alter table players
    add column if not exists friend_invite_reward_count integer not null default 0;
