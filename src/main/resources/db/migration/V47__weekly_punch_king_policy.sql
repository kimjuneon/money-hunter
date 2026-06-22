alter table game_economy_policy
    add column if not exists weekly_punch_king_max_gold_reward bigint,
    add column if not exists weekly_punch_king_gold_reward_score_scale bigint,
    add column if not exists weekly_punch_king_base_skill_points integer,
    add column if not exists weekly_punch_king_skill_point_tier2_score bigint,
    add column if not exists weekly_punch_king_skill_point_tier2_reward integer,
    add column if not exists weekly_punch_king_skill_point_tier3_score bigint,
    add column if not exists weekly_punch_king_skill_point_tier3_reward integer,
    add column if not exists weekly_punch_king_skill_point_tier4_score bigint,
    add column if not exists weekly_punch_king_skill_point_tier4_reward integer;
