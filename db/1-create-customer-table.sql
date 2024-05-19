CREATE TABLE customer
(
    id    serial primary key,
    name        VARCHAR(40) not null,
    email        VARCHAR(40) not null UNIQUE,
    vat        VARCHAR(40) not null UNIQUE,
    phone        VARCHAR(40) null
);