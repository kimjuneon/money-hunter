alter table players
	add column if not exists rookie_event_daily_mini_game_attempts integer not null default 0;
