alter table study_members
    add column status varchar(20) not null default 'JOINED';

alter table study_members
    add column left_at timestamp;

create index idx_study_members_member_status_joined_at
    on study_members (member_id, status, joined_at desc);

alter table chat_room_members
    add column hidden_at timestamp;
