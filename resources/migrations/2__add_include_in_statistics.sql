alter table events
    add column include_in_statistics boolean not null default true;