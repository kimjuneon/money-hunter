alter table players
	add column if not exists daily_mission_cycle integer not null default 1,
	add column if not exists daily_mission_current_date date,
	add column if not exists daily_mission_last_completed_date date,
	add column if not exists daily_mission_completed_days integer not null default 0,
	add column if not exists daily_mission_daily_hunt_millis bigint not null default 0,
	add column if not exists daily_mission_daily_dungeon_runs integer not null default 0,
	add column if not exists daily_mission_skip_ticket_count integer not null default 0,
	add column if not exists vip_expires_at timestamp with time zone,
	add column if not exists vip_last_daily_reward_date date;

create table if not exists event_rewards (
	id bigserial primary key,
	player_id bigint not null references players(id) on delete cascade,
	reward_key varchar(160) not null,
	source_event_key varchar(80) not null,
	source_event_name varchar(80) not null,
	title varchar(120) not null,
	description varchar(240) not null,
	reward_label varchar(120) not null,
	gold_amount bigint not null default 0,
	skill_point_amount integer not null default 0,
	auto_hunt_seconds bigint not null default 0,
	dungeon_coupon_amount integer not null default 0,
	boss_raid_ticket_amount integer not null default 0,
	daily_mission_skip_ticket_amount integer not null default 0,
	rookie_event_pet_reward boolean not null default false,
	vip_badge_reward boolean not null default false,
	pet_skin_unlock_reward boolean not null default false,
	created_at timestamp with time zone not null,
	expires_at timestamp with time zone not null,
	claimed_at timestamp with time zone,
	constraint uk_event_rewards_player_reward_key unique (player_id, reward_key)
);

create index if not exists idx_event_rewards_player_expires
	on event_rewards(player_id, expires_at);

create index if not exists idx_event_rewards_player_claimed_created
	on event_rewards(player_id, claimed_at, created_at desc);
