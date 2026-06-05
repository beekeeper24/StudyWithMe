create table content_reports (
    id bigserial primary key,
    target_type varchar(20) not null,
    target_id bigint not null,
    post_id bigint not null,
    target_title varchar(100),
    target_content varchar(5000) not null,
    reporter_member_id bigint not null,
    reported_member_id bigint not null,
    reason varchar(500) not null,
    status varchar(20) not null,
    assigned_admin_member_id bigint,
    assigned_at timestamp,
    handler_member_id bigint,
    handling_note varchar(500),
    created_at timestamp not null,
    handled_at timestamp,
    version bigint not null default 0,
    constraint fk_content_reports_post foreign key (post_id) references posts (id),
    constraint fk_content_reports_reporter foreign key (reporter_member_id) references members (id),
    constraint fk_content_reports_reported foreign key (reported_member_id) references members (id),
    constraint fk_content_reports_assigned_admin foreign key (assigned_admin_member_id) references members (id),
    constraint fk_content_reports_handler foreign key (handler_member_id) references members (id),
    constraint uk_content_reports_target_reporter unique (target_type, target_id, reporter_member_id)
);

create index idx_content_reports_status_created
    on content_reports (status, created_at desc, id desc);
