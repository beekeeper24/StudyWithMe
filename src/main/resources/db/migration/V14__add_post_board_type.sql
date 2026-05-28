alter table posts
    add column board_type varchar(20) not null default 'FREE';

create index idx_posts_board_status_created_at
    on posts (board_type, status, created_at desc);
