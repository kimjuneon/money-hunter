create table if not exists rookie_event_settings (
	id bigint primary key,
	enabled boolean not null,
	updated_at timestamptz not null,
	version bigint
);

insert into rookie_event_settings (id, enabled, updated_at, version)
values (1, true, now(), 0)
on conflict (id) do nothing;
