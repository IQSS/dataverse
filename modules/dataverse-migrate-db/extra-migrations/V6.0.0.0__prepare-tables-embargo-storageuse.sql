-- This is a workaround for the missing embargo table
create table if not exists embargo
(
    id            serial primary key,
    dateavailable date not null,
    reason        text
);

-- This is a workaround for the missing storageuse table
create table if not exists storageuse
(
    id                   serial primary key,
    sizeinbytes          bigint,
    dvobjectcontainer_id bigint not null constraint fk_storageuse_dvobjectcontainer_id references dvobject
);
create index if not exists index_storageuse_dvobjectcontainer_id on storageuse (dvobjectcontainer_id);

