alter table chat_messages
    add column deleted_at timestamp;

create index idx_chat_messages_room_deleted_created_id
    on chat_messages (room_id, deleted_at, created_at, id);
