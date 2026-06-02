alter table players
	add column if not exists rookie_event_started_at timestamp with time zone,
	add column if not exists rookie_event_completed_at timestamp with time zone,
	add column if not exists rookie_event_reward_claimed_at timestamp with time zone,
	add column if not exists rookie_event_completed_days integer not null default 0,
	add column if not exists rookie_event_current_date date,
	add column if not exists rookie_event_last_completed_date date,
	add column if not exists rookie_event_daily_hunt_millis bigint not null default 0,
	add column if not exists rookie_event_daily_monsters integer not null default 0,
	add column if not exists rookie_event_daily_boost_monsters integer not null default 0,
	add column if not exists rookie_event_daily_gold bigint not null default 0,
	add column if not exists rookie_event_daily_settlements integer not null default 0,
	add column if not exists rookie_event_daily_skill_points_spent integer not null default 0;
