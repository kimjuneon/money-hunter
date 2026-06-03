alter table players
	add column if not exists dormant_sp_reward_streak_accessed_at timestamptz,
	add column if not exists dormant_sp_reward_sent_stage integer not null default 0,
	add column if not exists dormant_sp_reward_last_sent_at timestamptz;

create index if not exists idx_players_dormant_sp_reward
	on players(last_accessed_at, dormant_sp_reward_sent_stage, dormant_sp_reward_last_sent_at);
