alter table reward_claims
    drop constraint if exists reward_claims_idempotency_key_key;

create unique index if not exists idx_reward_claims_player_idempotency
    on reward_claims(player_id, idempotency_key);
