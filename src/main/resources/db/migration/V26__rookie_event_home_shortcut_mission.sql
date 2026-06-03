alter table players
	add column if not exists rookie_event_daily_home_shortcut_returned boolean not null default false;
