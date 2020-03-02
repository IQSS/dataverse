create table consent
(
    id serial
        constraint consent_pkey
            primary key,
    name varchar(255)
        constraint consent_name_key
            unique,
    displayorder integer not null default 0,
    hidden boolean not null default false,
    required boolean not null default false
);

create table consentaction
(
    id serial
        constraint consentaction_pkey
            primary key,
    actionoptions json,
    consentactiontype text not null,
    consent_id bigint
        constraint fk_consentaction_consent_id
            references consent
);

create table consentdetails
(
    id serial
        constraint consentdetails_pkey
            primary key,
    text text not null,
    language varchar(255) not null,
    consent_id bigint not null
        constraint fk_consentdetails_consent_id
            references consent
);

create table acceptedconsent
(
    id serial
        constraint acceptedconsent_pkey
            primary key,
    name varchar(255) not null,
    language varchar(255) not null,
    text text not null,
    required boolean not null,
    user_id bigint not null
        constraint fk_acceptedconsent_user_id
            references authenticateduser
);
