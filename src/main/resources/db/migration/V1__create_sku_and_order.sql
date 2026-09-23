create table sku
(
    id    int primary key auto_increment,
    name  varchar(255) not null,
    stock int          not null
);

create table orders
(
    id       int primary key auto_increment,
    user_id  int not null,
    sku_id   int not null,
    quantity int not null
);