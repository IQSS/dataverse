create table variablemetadata
(
    id                   serial not null,
    interviewinstruction text,
    isweightvar          boolean,
    label                text,
    literalquestion      text,
    notes                text,
    universe             varchar(255),
    weighted             boolean,
    datavariable_id      bigint not null,
    filemetadata_id      bigint not null,
    weightvariable_id    bigint,
    constraint variablemetadata_pkey
        primary key (id),
    constraint unq_variablemetadata_0
        unique (datavariable_id, filemetadata_id),
    constraint fk_variablemetadata_datavariable_id
        foreign key (datavariable_id) references datavariable,
    constraint fk_variablemetadata_filemetadata_id
        foreign key (filemetadata_id) references filemetadata,
    constraint fk_variablemetadata_weightvariable_id
        foreign key (weightvariable_id) references datavariable
);

create index index_variablemetadata_datavariable_id
    on variablemetadata (datavariable_id);

create index index_variablemetadata_filemetadata_id
    on variablemetadata (filemetadata_id);

create index index_variablemetadata_datavariable_id_filemetadata_id
    on variablemetadata (datavariable_id, filemetadata_id);

create table categorymetadata
(
    id                  serial not null,
    wfreq               double precision,
    category_id         bigint not null,
    variablemetadata_id bigint not null,
    constraint categorymetadata_pkey
        primary key (id),
    constraint fk_categorymetadata_variablemetadata_id
        foreign key (variablemetadata_id) references variablemetadata,
    constraint fk_categorymetadata_category_id
        foreign key (category_id) references variablecategory
);

create index index_categorymetadata_category_id
    on categorymetadata (category_id);

create index index_categorymetadata_variablemetadata_id
    on categorymetadata (variablemetadata_id);

create table vargroup
(
    id              serial not null,
    label           text,
    filemetadata_id bigint not null,
    constraint vargroup_pkey
        primary key (id),
    constraint fk_vargroup_filemetadata_id
        foreign key (filemetadata_id) references filemetadata
);

create index index_vargroup_filemetadata_id
    on vargroup (filemetadata_id);

ALTER TABLE datavariable
    add column vargroup_id bigint,

    add constraint fk_datavariable_vargroup_id
        foreign key (vargroup_id) references vargroup (id);
