alter table players
    add column if not exists suspended_at timestamptz,
    add column if not exists suspension_reason varchar(500);

create index if not exists idx_players_suspended_at
    on players(suspended_at);
