alter table players
	add column if not exists rookie_event_mission_notification_agreed_at timestamptz,
	add column if not exists rookie_event_mission_message_sent_date date,
	add column if not exists rookie_event_mission_message_sent_day integer not null default 0;

create index if not exists idx_players_rookie_event_mission_message
	on players(rookie_event_started_at, rookie_event_reward_claimed_at, rookie_event_mission_message_sent_date);
