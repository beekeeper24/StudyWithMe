alter table chat_room_members
    add column last_read_message_id bigint;

create index idx_chat_room_members_room_last_read
    on chat_room_members (room_id, last_read_message_id);
