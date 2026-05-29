alter table players
    add column if not exists feature_tutorial_completed_at timestamptz;
