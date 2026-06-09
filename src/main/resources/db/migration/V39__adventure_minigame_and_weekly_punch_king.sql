alter table players
	add column if not exists adventure_mini_game_entry_started_at timestamp with time zone,
	add column if not exists adventure_mini_game_completed_date date,
	add column if not exists weekly_punch_king_week_start_date date,
	add column if not exists weekly_punch_king_best_score bigint not null default 0,
	add column if not exists weekly_punch_king_rewarded_gold bigint not null default 0,
	add column if not exists weekly_punch_king_rewarded_skill_points integer not null default 0;
