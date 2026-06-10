alter table players
    drop column if exists daily_mission_skip_ticket_count;

alter table event_rewards
    drop column if exists daily_mission_skip_ticket_amount;
