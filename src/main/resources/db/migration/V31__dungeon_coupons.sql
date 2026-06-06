alter table players
	add column if not exists dungeon_coupon_count integer not null default 0,
	add column if not exists dungeon_coupon_hunt_millis bigint not null default 0;
