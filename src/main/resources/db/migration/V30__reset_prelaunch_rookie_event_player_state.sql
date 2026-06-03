with updated_players as (
	update players
	set rookie_event_started_at = null,
		rookie_event_completed_at = null,
		rookie_event_reward_claimed_at = null,
		rookie_event_completed_days = 0,
		rookie_event_rewarded_days = 0,
		rookie_event_current_date = null,
		rookie_event_last_completed_date = null,
		rookie_event_daily_hunt_millis = 0,
		rookie_event_daily_monsters = 0,
		rookie_event_daily_boost_monsters = 0,
		rookie_event_daily_gold = 0,
		rookie_event_daily_settlements = 0,
		rookie_event_daily_skill_points_spent = 0,
		rookie_event_daily_home_shortcut_returned = false,
		rookie_event_mission_notification_agreed_at = null,
		rookie_event_mission_message_sent_date = null,
		rookie_event_mission_message_sent_day = 0
	where rookie_event_started_at is not null
		or rookie_event_completed_at is not null
		or rookie_event_reward_claimed_at is not null
		or rookie_event_completed_days <> 0
		or rookie_event_rewarded_days <> 0
		or rookie_event_current_date is not null
		or rookie_event_last_completed_date is not null
		or rookie_event_daily_hunt_millis <> 0
		or rookie_event_daily_monsters <> 0
		or rookie_event_daily_boost_monsters <> 0
		or rookie_event_daily_gold <> 0
		or rookie_event_daily_settlements <> 0
		or rookie_event_daily_skill_points_spent <> 0
		or rookie_event_daily_home_shortcut_returned = true
		or rookie_event_mission_notification_agreed_at is not null
		or rookie_event_mission_message_sent_date is not null
		or rookie_event_mission_message_sent_day <> 0
	returning id
)
insert into admin_audit_logs (
	action,
	target,
	actor_fingerprint,
	before_value,
	after_value,
	reason,
	client_ip,
	user_agent,
	created_at
)
select
	'ROOKIE_EVENT_RESET',
	'rookie-event',
	'migration-v30',
	'affectedPlayers=' || count(*),
	'rookie_event_player_state=cleared',
	'정식 출시 전 자동 시작/테스트 이벤트 상태 초기화',
	null,
	'flyway',
	now()
from updated_players;
