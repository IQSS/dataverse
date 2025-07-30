-- This is a workaround for the missing tables in migration V6.6.0.2

create table if not exists dataversefeatureditem
(
    id            serial primary key,
    content       text,
    displayorder  integer not null,
    imagefilename varchar(255),
    type          text,
    dataverse_id  bigint not null constraint fk_dataversefeatureditem_dataverse_id references dvobject,
    dvobject_id   bigint constraint fk_dataversefeatureditem_dvobject_id references dvobject
);

create index if not exists index_dataversefeatureditem_displayorder on dataversefeatureditem (displayorder);
-- It's unclear why EclipseLink generated this particular index and with this name... Just going along with it.
create index if not exists index_harvestingclient_harvesttype on dataversefeatureditem (id);
