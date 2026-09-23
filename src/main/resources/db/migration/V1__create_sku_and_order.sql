create table sku
(
    id    bigint primary key auto_increment,
    name  varchar(255) not null,
    stock int          not null
);

create table orders
(
    id       bigint primary key auto_increment,
    user_id  bigint not null,
    sku_id   bigint not null,
    quantity int    not null
);