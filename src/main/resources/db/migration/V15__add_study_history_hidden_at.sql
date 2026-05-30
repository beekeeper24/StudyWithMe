alter table study_members
    add column history_hidden_at timestamp;

create index idx_study_members_member_history_hidden
    on study_members (member_id, history_hidden_at);
