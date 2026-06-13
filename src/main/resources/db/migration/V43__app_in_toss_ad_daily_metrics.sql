create table app_in_toss_ad_daily_metrics (
    id bigserial primary key,
    metric_date date not null unique,
    ad_impressions bigint not null,
    ad_watch_rate_percent numeric(5, 2) not null,
    ecpm_won numeric(12, 2) not null,
    note varchar(500),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint chk_app_in_toss_ad_daily_metrics_impressions_non_negative
        check (ad_impressions >= 0),
    constraint chk_app_in_toss_ad_daily_metrics_watch_rate_range
        check (ad_watch_rate_percent >= 0 and ad_watch_rate_percent <= 100),
    constraint chk_app_in_toss_ad_daily_metrics_ecpm_non_negative
        check (ecpm_won >= 0)
);

create index idx_app_in_toss_ad_daily_metrics_metric_date
    on app_in_toss_ad_daily_metrics(metric_date desc);
