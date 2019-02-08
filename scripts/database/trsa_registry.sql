-- Drop table

DROP TABLE public.trsa_registry

CREATE TABLE public.trsa_registry (
	id serial NOT NULL,
	installation varchar(255) NOT NULL,
	email varchar(255) NOT NULL,
	dataverseurl varchar(255) NOT NULL,
	apitoken varchar(255) NOT NULL,
	datastoragelocation varchar(255) NOT NULL,
	dataaccessinfo varchar(255) NOT NULL,
	notaryserviceurl varchar(255) NOT NULL,
	safeserviceurl varchar(255) NOT NULL,
	registertime timestamp NULL,
	expiretime timestamp NULL,
	disabled bool NULL,
	CONSTRAINT trsa_registry_pkey null
);

-- Permissions

ALTER TABLE public.trsa_registry OWNER TO dvnapp;
GRANT ALL ON TABLE public.trsa_registry TO dvnapp;
