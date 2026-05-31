alter table players
    add column owned_pet_skin_keys varchar(600) not null default 'FIRE_FOX,ICE',
    add column pet_one_skin_key varchar(40) not null default 'FIRE_FOX',
    add column pet_two_skin_key varchar(40) not null default 'ICE';
