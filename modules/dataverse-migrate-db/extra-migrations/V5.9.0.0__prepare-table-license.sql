-- This is a workaround for the missing license table

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

-- Save information about CCBY license to temporary table (to be used in a migration after the main license migration)
CREATE TEMPORARY TABLE ccby(
   id serial  primary key
);
INSERT INTO ccby (id) SELECT id FROM termsofuseandaccess WHERE license = 'CCBY';
