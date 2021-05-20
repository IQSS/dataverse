-- 7325 add TRSA DB customizations to vanilla Dataverse database
-- (do we want this in FlyWay?) should it be stand-alone SQL?

CREATE TABLE public.trsa (
        id bigserial NOT NULL,
        dataaccessinfo varchar(255) NOT NULL,
        datafileserverurl varchar(255) NOT NULL,
        disabled bool NULL,
        email varchar(255) NOT NULL,
        expiretime timestamp NULL,
        installation varchar(255) NOT NULL,
        notaryserviceurl varchar(255) NOT NULL,
        registertime timestamp NULL,
        trsaurl varchar(255) NULL,
        CONSTRAINT trsa_pkey PRIMARY KEY (id)
);

ALTER TABLE public.dataset ADD trsa_id int8 NULL DEFAULT NULL;
ALTER TABLE public.dataset ADD CONSTRAINT fk_dataset_trsa_id FOREIGN KEY (trsa_id) REFERENCES public.trsa(id);

ALTER TABLE public.datafile ADD notaryservicebound bool NOT NULL DEFAULT FALSE;
