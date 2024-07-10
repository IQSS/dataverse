-- This is a workaround for the missing auxiliaryfile table

create table if not exists auxiliaryfile
(
    id            serial primary key,
    checksum      varchar(255),
    contenttype   varchar(255),
    filesize      bigint,
    formattag     varchar(255),
    formatversion varchar(255),
    ispublic      boolean,
    origin        varchar(255),
    type          varchar(255),
    datafile_id   bigint not null constraint fk_auxiliaryfile_datafile_id references dvobject
);
