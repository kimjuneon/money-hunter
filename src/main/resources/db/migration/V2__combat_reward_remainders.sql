alter table players
    add column hit_gold_remainder_micros bigint not null default 0,
    add column defeat_gold_remainder_micros bigint not null default 0;
