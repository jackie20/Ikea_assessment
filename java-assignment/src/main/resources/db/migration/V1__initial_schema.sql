-- Sequences used by Hibernate's @GeneratedValue (allocation size = 50)
CREATE SEQUENCE IF NOT EXISTS store_seq     START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS product_seq   START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS warehouse_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS store (
    id                       BIGINT  NOT NULL DEFAULT nextval('store_seq'),
    name                     VARCHAR(40) UNIQUE,
    quantityproductsinstock  INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS product (
    id          BIGINT        NOT NULL DEFAULT nextval('product_seq'),
    name        VARCHAR(40)   UNIQUE,
    description VARCHAR(255),
    price       NUMERIC(10,2),
    stock       INTEGER       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS warehouse (
    id               BIGINT       NOT NULL DEFAULT nextval('warehouse_seq'),
    businessunitcode VARCHAR(255) UNIQUE,
    location         VARCHAR(255),
    capacity         INTEGER,
    stock            INTEGER,
    createdat        TIMESTAMP,
    archivedat       TIMESTAMP,
    PRIMARY KEY (id)
);
