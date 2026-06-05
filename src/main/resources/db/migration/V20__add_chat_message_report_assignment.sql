alter table chat_message_reports
    add column assigned_admin_member_id bigint;

alter table chat_message_reports
    add column assigned_at timestamp;

alter table chat_message_reports
    add constraint fk_chat_message_reports_assigned_admin
        foreign key (assigned_admin_member_id) references members (id);
