create table member_sanctions (
    id bigserial primary key,
    target_member_id bigint not null,
    admin_member_id bigint not null,
    type varchar(30) not null,
    reason varchar(500) not null,
    source_type varchar(30) not null,
    source_id bigint,
    created_at timestamp not null,
    constraint fk_member_sanctions_target foreign key (target_member_id) references members (id),
    constraint fk_member_sanctions_admin foreign key (admin_member_id) references members (id)
);

create index idx_member_sanctions_target_created
    on member_sanctions (target_member_id, created_at desc, id desc);
