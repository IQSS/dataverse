-- Alter the database to allow permissions.

ALTER TABLE dataverse ADD COLUMN permissionroot boolean;

CREATE TABLE dataverse_dataverserole (
    dataverse_id bigint NOT NULL,
    roles_id bigint NOT NULL
);


ALTER TABLE public.dataverse_dataverserole OWNER TO dvnapp;

CREATE TABLE dataverserole (
    id integer NOT NULL,
    alias character varying(255),
    description character varying(255),
    name character varying(255),
    permissionbits bigint,
    owner_id bigint NOT NULL
);
ALTER TABLE public.dataverserole OWNER TO dvnapp;

CREATE SEQUENCE dataverserole_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER TABLE public.dataverserole_id_seq OWNER TO dvnapp;
ALTER SEQUENCE dataverserole_id_seq OWNED BY dataverserole.id;

CREATE TABLE dataverserole_userdataverseassignedrole (
    dataverserole_id bigint NOT NULL,
    assignedroles_id bigint NOT NULL
);
ALTER TABLE public.dataverserole_userdataverseassignedrole OWNER TO dvnapp;


CREATE TABLE dataverseuser_userdataverseassignedrole (
    dataverseuser_id bigint NOT NULL,
    assignedroles_id bigint NOT NULL
);
ALTER TABLE public.dataverseuser_userdataverseassignedrole OWNER TO dvnapp;


CREATE TABLE userdataverseassignedrole (
    id integer NOT NULL,
    role_id bigint,
    user_id bigint
);
ALTER TABLE public.userdataverseassignedrole OWNER TO dvnapp;

CREATE SEQUENCE userdataverseassignedrole_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER TABLE public.userdataverseassignedrole_id_seq OWNER TO dvnapp;

ALTER SEQUENCE userdataverseassignedrole_id_seq OWNED BY userdataverseassignedrole.id;

ALTER TABLE ONLY dataverserole ALTER COLUMN id SET DEFAULT nextval('dataverserole_id_seq'::regclass);
ALTER TABLE ONLY userdataverseassignedrole ALTER COLUMN id SET DEFAULT nextval('userdataverseassignedrole_id_seq'::regclass);
ALTER TABLE ONLY dataverse_dataverserole
    ADD CONSTRAINT dataverse_dataverserole_pkey PRIMARY KEY (dataverse_id, roles_id);
ALTER TABLE ONLY dataverserole
    ADD CONSTRAINT dataverserole_pkey PRIMARY KEY (id);
ALTER TABLE ONLY dataverserole_userdataverseassignedrole
    ADD CONSTRAINT dataverserole_userdataverseassignedrole_pkey PRIMARY KEY (dataverserole_id, assignedroles_id);
ALTER TABLE ONLY dataverseuser_userdataverseassignedrole
    ADD CONSTRAINT dataverseuser_userdataverseassignedrole_pkey PRIMARY KEY (dataverseuser_id, assignedroles_id);
ALTER TABLE ONLY userdataverseassignedrole
    ADD CONSTRAINT unq_userdataverseassignedrole_0 UNIQUE (user_id, role_id);
ALTER TABLE ONLY userdataverseassignedrole
    ADD CONSTRAINT userdataverseassignedrole_pkey PRIMARY KEY (id);
ALTER TABLE ONLY dataverse_dataverserole
    ADD CONSTRAINT fk_dataverse_dataverserole_dataverse_id FOREIGN KEY (dataverse_id) REFERENCES dataverse(id);
ALTER TABLE ONLY dataverse_dataverserole
    ADD CONSTRAINT fk_dataverse_dataverserole_roles_id FOREIGN KEY (roles_id) REFERENCES dataverserole(id);
ALTER TABLE ONLY dataverserole
    ADD CONSTRAINT fk_dataverserole_owner_id FOREIGN KEY (owner_id) REFERENCES dataverse(id);
ALTER TABLE ONLY dataverserole_userdataverseassignedrole
    ADD CONSTRAINT fk_dataverserole_userdataverseassignedrole_assignedroles_id FOREIGN KEY (assignedroles_id) REFERENCES userdataverseassignedrole(id);
ALTER TABLE ONLY dataverserole_userdataverseassignedrole
    ADD CONSTRAINT fk_dataverserole_userdataverseassignedrole_dataverserole_id FOREIGN KEY (dataverserole_id) REFERENCES dataverserole(id);
ALTER TABLE ONLY dataverseuser_userdataverseassignedrole
    ADD CONSTRAINT fk_dataverseuser_userdataverseassignedrole_assignedroles_id FOREIGN KEY (assignedroles_id) REFERENCES userdataverseassignedrole(id);
ALTER TABLE ONLY dataverseuser_userdataverseassignedrole
    ADD CONSTRAINT fk_dataverseuser_userdataverseassignedrole_dataverseuser_id FOREIGN KEY (dataverseuser_id) REFERENCES dataverseuser(id);
ALTER TABLE ONLY userdataverseassignedrole
    ADD CONSTRAINT fk_userdataverseassignedrole_role_id FOREIGN KEY (role_id) REFERENCES dataverserole(id);
ALTER TABLE ONLY userdataverseassignedrole
    ADD CONSTRAINT fk_userdataverseassignedrole_user_id FOREIGN KEY (user_id) REFERENCES dataverseuser(id);
