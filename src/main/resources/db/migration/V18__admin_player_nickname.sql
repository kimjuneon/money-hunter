alter table players
    add column admin_nickname varchar(80);

create index idx_players_admin_nickname
    on players (lower(admin_nickname));
