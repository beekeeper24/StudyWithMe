alter table content_reports
    add column moderation_action varchar(30) not null default 'NONE';
