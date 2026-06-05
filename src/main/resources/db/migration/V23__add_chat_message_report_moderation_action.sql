alter table chat_message_reports
    add column moderation_action varchar(30) not null default 'NONE';
