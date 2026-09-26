alter table sku
    add column version bigint not null default 0;
