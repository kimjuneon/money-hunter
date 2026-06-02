alter table players
	add column if not exists rookie_event_rewarded_days integer not null default 0;
