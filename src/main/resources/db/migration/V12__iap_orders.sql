create table iap_orders (
    id bigserial primary key,
    order_id varchar(120) not null unique,
    user_key varchar(120) not null,
    product_id varchar(180) not null,
    product_type varchar(60) not null,
    granted_at timestamptz,
    created_at timestamptz not null
);

create index idx_iap_orders_user_key on iap_orders(user_key);
