create table login_sessions (
    id bigserial primary key,
    token_hash varchar(96) not null unique,
    user_key varchar(120) not null,
    created_at timestamptz not null,
    expires_at timestamptz not null
);

create index idx_login_sessions_user_key on login_sessions(user_key);
create index idx_login_sessions_expires_at on login_sessions(expires_at);
