-- This is a workaround for the missing dataset type tables in migration V6.3.0.3

create table if not exists datasettype
(
    id   serial primary key,
    name varchar(255) not null constraint unq_datasettype_0 unique
);

create table if not exists datasettype_licenses
(
    datasettype_id bigint not null
        constraint fk_datasettype_license_datasettype_id
            references datasettype,
    licenses_id    bigint not null
        constraint fk_datasettype_license_licenses_id
            references license,
    primary key (datasettype_id, licenses_id)
);

create table if not exists datasettype_metadatablocks
(
    datasettype_id    bigint not null
        constraint fk_datasettype_metadatablock_datasettype_id
            references datasettype,
    metadatablocks_id bigint not null
        constraint fk_datasettype_metadatablock_metadatablocks_id
            references public.metadatablock,
    primary key (datasettype_id, metadatablocks_id)
);