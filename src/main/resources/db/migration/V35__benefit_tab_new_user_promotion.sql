alter table players
    add column benefit_tab_new_user_entered_at timestamptz,
    add column benefit_tab_new_user_promotion_execution_key varchar(220),
    add column benefit_tab_new_user_promotion_result_checked_at timestamptz,
    add column benefit_tab_new_user_promotion_granted_at timestamptz;

create index idx_players_benefit_tab_new_user_entered_at
    on players (benefit_tab_new_user_entered_at);

create index idx_players_benefit_tab_new_user_promotion_execution_key
    on players (benefit_tab_new_user_promotion_execution_key);
