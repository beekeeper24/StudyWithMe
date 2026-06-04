alter table chat_message_reports
    add column version bigint not null default 0;
