create table admin_sessions (
    id bigserial primary key,
    token_hash varchar(96) not null unique,
    username varchar(80) not null,
    created_at timestamptz not null,
    expires_at timestamptz not null,
    revoked_at timestamptz
);

create index idx_admin_sessions_token_hash on admin_sessions(token_hash);
create index idx_admin_sessions_expires_at on admin_sessions(expires_at);
