CREATE TABLE datafilestatus (
    id integer NOT NULL,
    datasetidentifier character varying(255),
    storageidentifier character varying(255),
    status character varying(255),
    createdate timestamp without time zone,
    lastbackuptime timestamp without time zone,
    lastbackupmethod character varying(16)
);

ALTER TABLE datafilestatus OWNER TO dvnapp;

CREATE SEQUENCE datafilestatus_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE datafilestatus_id_seq OWNER TO dvnapp;

ALTER SEQUENCE datafilestatus_id_seq OWNED BY datafilestatus.id;

ALTER TABLE ONLY datafilestatus
    ADD CONSTRAINT datafilestatus_pkey PRIMARY KEY (id);

ALTER TABLE ONLY datafilestatus ALTER COLUMN id SET DEFAULT nextval('datafilestatus_id_seq'::regclass);

ALTER TABLE ONLY datafilestatus
    ADD CONSTRAINT datafilestatus_storageidentifier_key UNIQUE (storageidentifier);