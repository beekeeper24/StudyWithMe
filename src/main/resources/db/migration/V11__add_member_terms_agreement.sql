alter table members
    add column terms_agreed_at timestamp;

alter table members
    add column terms_version varchar(20);

alter table members
    add column privacy_policy_version varchar(20);

update members
set terms_agreed_at = coalesce(created_at, now()),
    terms_version = 'LEGACY',
    privacy_policy_version = 'LEGACY'
where nickname is not null;
