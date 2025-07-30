-- This is a workaround for the missing license table in migration V5.9.0.1

create table if not exists license
(
    id               serial  primary key,
    active           boolean not null,
    iconurl          text,
    isdefault        boolean not null,
    name             text constraint unq_license_0 unique,
    shortdescription text,
    sortorder        bigint default 0 not null,
    uri              text constraint unq_license_1 unique
);

create index if not exists license_sortorder_id on license (sortorder, id);
