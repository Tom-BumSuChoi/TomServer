alter table orders
    add column status varchar(32) not null default 'PENDING_PAYMENT';
