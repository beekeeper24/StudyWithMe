alter table outbox_events
    add column kafka_publish_status varchar(20) not null default 'PENDING';

alter table outbox_events
    add column kafka_retry_count integer not null default 0;

alter table outbox_events
    add column kafka_next_attempt_at timestamp not null default current_timestamp;

alter table outbox_events
    add column kafka_published_at timestamp;

alter table outbox_events
    add column kafka_last_error varchar(1000);

create index idx_outbox_events_kafka_publish_status_next_attempt_at
    on outbox_events (kafka_publish_status, kafka_next_attempt_at, occurred_at);
