alter table reward_claims
    add column promotion_execution_key varchar(220),
    add column promotion_result_checked_at timestamptz;

create index idx_reward_claims_promotion_execution_key
    on reward_claims (promotion_execution_key);
