update studies
set status = 'DELETED',
    updated_at = now()
where status in ('RECRUITING', 'CLOSED')
  and owner_member_id in (
    select id
    from members
    where status = 'WITHDRAWN'
  );
