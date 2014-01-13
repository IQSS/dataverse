--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: datafile; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE datafile (
    id integer NOT NULL,
    contenttype character varying(255),
    name character varying(255),
    dataset_id bigint
);


ALTER TABLE public.datafile OWNER TO dvnapp;

--
-- Name: datafile_id_seq; Type: SEQUENCE; Schema: public; Owner: dvnapp
--

CREATE SEQUENCE datafile_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.datafile_id_seq OWNER TO dvnapp;

--
-- Name: datafile_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dvnapp
--

ALTER SEQUENCE datafile_id_seq OWNED BY datafile.id;


--
-- Name: dataset; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE dataset (
    id integer NOT NULL,
    author character varying(255),
    citationdate character varying(255),
    description character varying(255),
    distributor character varying(255),
    geographiccoverage character varying(255),
    keyword character varying(255),
    title character varying(255),
    topicclassification character varying(255),
    topicclassificationurl character varying(255),
    owner_id bigint NOT NULL
);


ALTER TABLE public.dataset OWNER TO dvnapp;

--
-- Name: dataset_id_seq; Type: SEQUENCE; Schema: public; Owner: dvnapp
--

CREATE SEQUENCE dataset_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dataset_id_seq OWNER TO dvnapp;

--
-- Name: dataset_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dvnapp
--

ALTER SEQUENCE dataset_id_seq OWNED BY dataset.id;


--
-- Name: datatable; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE datatable (
    id integer NOT NULL,
    casequantity bigint,
    recordspercase bigint,
    unf character varying(255),
    varquantity bigint,
    datafile_id bigint NOT NULL
);


ALTER TABLE public.datatable OWNER TO dvnapp;

--
-- Name: datatable_id_seq; Type: SEQUENCE; Schema: public; Owner: dvnapp
--

CREATE SEQUENCE datatable_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.datatable_id_seq OWNER TO dvnapp;

--
-- Name: datatable_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dvnapp
--

ALTER SEQUENCE datatable_id_seq OWNED BY datatable.id;


--
-- Name: datavariable; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE datavariable (
    id integer NOT NULL,
    fileendposition bigint,
    fileorder integer,
    filestartposition bigint,
    formatcategory character varying(255),
    formatschema character varying(255),
    formatschemaname character varying(255),
    label character varying(255),
    name character varying(255),
    numberofdecimalpoints bigint,
    orderedfactor boolean,
    recordsegmentnumber bigint,
    unf character varying(255),
    universe character varying(255),
    weighted boolean,
    datatable_id bigint NOT NULL,
    variableformattype_id bigint NOT NULL,
    variableintervaltype_id bigint
);


ALTER TABLE public.datavariable OWNER TO dvnapp;

--
-- Name: datavariable_id_seq; Type: SEQUENCE; Schema: public; Owner: dvnapp
--

CREATE SEQUENCE datavariable_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.datavariable_id_seq OWNER TO dvnapp;

--
-- Name: datavariable_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dvnapp
--

ALTER SEQUENCE datavariable_id_seq OWNED BY datavariable.id;


--
-- Name: dataverse; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE dataverse (
    id integer NOT NULL,
    affiliation character varying(255),
    alias character varying(255),
    contactemail character varying(255),
    description character varying(255),
    name character varying(255),
    permissionroot boolean,
    owner_id bigint
);


ALTER TABLE public.dataverse OWNER TO dvnapp;

--
-- Name: dataverse_dataverserole; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE dataverse_dataverserole (
    dataverse_id bigint NOT NULL,
    roles_id bigint NOT NULL
);


ALTER TABLE public.dataverse_dataverserole OWNER TO dvnapp;

--
-- Name: dataverse_id_seq; Type: SEQUENCE; Schema: public; Owner: dvnapp
--

CREATE SEQUENCE dataverse_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dataverse_id_seq OWNER TO dvnapp;

--
-- Name: dataverse_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dvnapp
--

ALTER SEQUENCE dataverse_id_seq OWNED BY dataverse.id;


--
-- Name: dataverserole; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE dataverserole (
    id integer NOT NULL,
    alias character varying(255),
    description character varying(255),
    name character varying(255),
    permissionbits bigint,
    owner_id bigint NOT NULL
);


ALTER TABLE public.dataverserole OWNER TO dvnapp;

--
-- Name: dataverserole_id_seq; Type: SEQUENCE; Schema: public; Owner: dvnapp
--

CREATE SEQUENCE dataverserole_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dataverserole_id_seq OWNER TO dvnapp;

--
-- Name: dataverserole_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dvnapp
--

ALTER SEQUENCE dataverserole_id_seq OWNED BY dataverserole.id;


--
-- Name: dataverserole_userdataverseassignedrole; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE dataverserole_userdataverseassignedrole (
    dataverserole_id bigint NOT NULL,
    assignedroles_id bigint NOT NULL
);


ALTER TABLE public.dataverserole_userdataverseassignedrole OWNER TO dvnapp;

--
-- Name: dataverseuser; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE dataverseuser (
    id integer NOT NULL,
    email character varying(255),
    encryptedpassword character varying(255),
    firstname character varying(255),
    institution character varying(255),
    lastname character varying(255),
    phone character varying(255),
    "position" character varying(255),
    username character varying(255)
);


ALTER TABLE public.dataverseuser OWNER TO dvnapp;

--
-- Name: dataverseuser_id_seq; Type: SEQUENCE; Schema: public; Owner: dvnapp
--

CREATE SEQUENCE dataverseuser_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dataverseuser_id_seq OWNER TO dvnapp;

--
-- Name: dataverseuser_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dvnapp
--

ALTER SEQUENCE dataverseuser_id_seq OWNED BY dataverseuser.id;


--
-- Name: dataverseuser_userdataverseassignedrole; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE dataverseuser_userdataverseassignedrole (
    dataverseuser_id bigint NOT NULL,
    assignedroles_id bigint NOT NULL
);


ALTER TABLE public.dataverseuser_userdataverseassignedrole OWNER TO dvnapp;

--
-- Name: summarystatistic; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE summarystatistic (
    id integer NOT NULL,
    value character varying(255),
    datavariable_id bigint NOT NULL,
    type_id bigint NOT NULL
);


ALTER TABLE public.summarystatistic OWNER TO dvnapp;

--
-- Name: summarystatistic_id_seq; Type: SEQUENCE; Schema: public; Owner: dvnapp
--

CREATE SEQUENCE summarystatistic_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.summarystatistic_id_seq OWNER TO dvnapp;

--
-- Name: summarystatistic_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dvnapp
--

ALTER SEQUENCE summarystatistic_id_seq OWNED BY summarystatistic.id;


--
-- Name: summarystatistictype; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE summarystatistictype (
    id integer NOT NULL,
    name character varying(255)
);


ALTER TABLE public.summarystatistictype OWNER TO dvnapp;

--
-- Name: summarystatistictype_id_seq; Type: SEQUENCE; Schema: public; Owner: dvnapp
--

CREATE SEQUENCE summarystatistictype_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.summarystatistictype_id_seq OWNER TO dvnapp;

--
-- Name: summarystatistictype_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dvnapp
--

ALTER SEQUENCE summarystatistictype_id_seq OWNED BY summarystatistictype.id;


--
-- Name: userdataverseassignedrole; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE userdataverseassignedrole (
    id integer NOT NULL,
    role_id bigint,
    user_id bigint
);


ALTER TABLE public.userdataverseassignedrole OWNER TO dvnapp;

--
-- Name: userdataverseassignedrole_id_seq; Type: SEQUENCE; Schema: public; Owner: dvnapp
--

CREATE SEQUENCE userdataverseassignedrole_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.userdataverseassignedrole_id_seq OWNER TO dvnapp;

--
-- Name: userdataverseassignedrole_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dvnapp
--

ALTER SEQUENCE userdataverseassignedrole_id_seq OWNED BY userdataverseassignedrole.id;


--
-- Name: variablecategory; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE variablecategory (
    id integer NOT NULL,
    catorder integer,
    frequency double precision,
    label character varying(255),
    missing boolean,
    value character varying(255),
    datavariable_id bigint NOT NULL
);


ALTER TABLE public.variablecategory OWNER TO dvnapp;

--
-- Name: variablecategory_id_seq; Type: SEQUENCE; Schema: public; Owner: dvnapp
--

CREATE SEQUENCE variablecategory_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.variablecategory_id_seq OWNER TO dvnapp;

--
-- Name: variablecategory_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dvnapp
--

ALTER SEQUENCE variablecategory_id_seq OWNED BY variablecategory.id;


--
-- Name: variableformattype; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE variableformattype (
    id integer NOT NULL,
    name character varying(255)
);


ALTER TABLE public.variableformattype OWNER TO dvnapp;

--
-- Name: variableformattype_id_seq; Type: SEQUENCE; Schema: public; Owner: dvnapp
--

CREATE SEQUENCE variableformattype_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.variableformattype_id_seq OWNER TO dvnapp;

--
-- Name: variableformattype_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dvnapp
--

ALTER SEQUENCE variableformattype_id_seq OWNED BY variableformattype.id;


--
-- Name: variableintervaltype; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE variableintervaltype (
    id integer NOT NULL,
    name character varying(255)
);


ALTER TABLE public.variableintervaltype OWNER TO dvnapp;

--
-- Name: variableintervaltype_id_seq; Type: SEQUENCE; Schema: public; Owner: dvnapp
--

CREATE SEQUENCE variableintervaltype_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.variableintervaltype_id_seq OWNER TO dvnapp;

--
-- Name: variableintervaltype_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dvnapp
--

ALTER SEQUENCE variableintervaltype_id_seq OWNED BY variableintervaltype.id;


--
-- Name: variablerange; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE variablerange (
    id integer NOT NULL,
    beginvalue character varying(255),
    endvalue character varying(255),
    beginvaluetype_id bigint,
    datavariable_id bigint NOT NULL,
    endvaluetype_id bigint
);


ALTER TABLE public.variablerange OWNER TO dvnapp;

--
-- Name: variablerange_id_seq; Type: SEQUENCE; Schema: public; Owner: dvnapp
--

CREATE SEQUENCE variablerange_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.variablerange_id_seq OWNER TO dvnapp;

--
-- Name: variablerange_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dvnapp
--

ALTER SEQUENCE variablerange_id_seq OWNED BY variablerange.id;


--
-- Name: variablerangeitem; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE variablerangeitem (
    id integer NOT NULL,
    value numeric(38,0),
    datavariable_id bigint NOT NULL
);


ALTER TABLE public.variablerangeitem OWNER TO dvnapp;

--
-- Name: variablerangeitem_id_seq; Type: SEQUENCE; Schema: public; Owner: dvnapp
--

CREATE SEQUENCE variablerangeitem_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.variablerangeitem_id_seq OWNER TO dvnapp;

--
-- Name: variablerangeitem_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dvnapp
--

ALTER SEQUENCE variablerangeitem_id_seq OWNED BY variablerangeitem.id;


--
-- Name: variablerangetype; Type: TABLE; Schema: public; Owner: dvnapp; Tablespace: 
--

CREATE TABLE variablerangetype (
    id integer NOT NULL,
    name character varying(255)
);


ALTER TABLE public.variablerangetype OWNER TO dvnapp;

--
-- Name: variablerangetype_id_seq; Type: SEQUENCE; Schema: public; Owner: dvnapp
--

CREATE SEQUENCE variablerangetype_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.variablerangetype_id_seq OWNER TO dvnapp;

--
-- Name: variablerangetype_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dvnapp
--

ALTER SEQUENCE variablerangetype_id_seq OWNED BY variablerangetype.id;


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY datafile ALTER COLUMN id SET DEFAULT nextval('datafile_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY dataset ALTER COLUMN id SET DEFAULT nextval('dataset_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY datatable ALTER COLUMN id SET DEFAULT nextval('datatable_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY datavariable ALTER COLUMN id SET DEFAULT nextval('datavariable_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY dataverse ALTER COLUMN id SET DEFAULT nextval('dataverse_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY dataverserole ALTER COLUMN id SET DEFAULT nextval('dataverserole_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY dataverseuser ALTER COLUMN id SET DEFAULT nextval('dataverseuser_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY summarystatistic ALTER COLUMN id SET DEFAULT nextval('summarystatistic_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY summarystatistictype ALTER COLUMN id SET DEFAULT nextval('summarystatistictype_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY userdataverseassignedrole ALTER COLUMN id SET DEFAULT nextval('userdataverseassignedrole_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY variablecategory ALTER COLUMN id SET DEFAULT nextval('variablecategory_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY variableformattype ALTER COLUMN id SET DEFAULT nextval('variableformattype_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY variableintervaltype ALTER COLUMN id SET DEFAULT nextval('variableintervaltype_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY variablerange ALTER COLUMN id SET DEFAULT nextval('variablerange_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY variablerangeitem ALTER COLUMN id SET DEFAULT nextval('variablerangeitem_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY variablerangetype ALTER COLUMN id SET DEFAULT nextval('variablerangetype_id_seq'::regclass);


--
-- Name: datafile_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY datafile
    ADD CONSTRAINT datafile_pkey PRIMARY KEY (id);


--
-- Name: dataset_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY dataset
    ADD CONSTRAINT dataset_pkey PRIMARY KEY (id);


--
-- Name: datatable_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY datatable
    ADD CONSTRAINT datatable_pkey PRIMARY KEY (id);


--
-- Name: datavariable_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY datavariable
    ADD CONSTRAINT datavariable_pkey PRIMARY KEY (id);


--
-- Name: dataverse_dataverserole_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY dataverse_dataverserole
    ADD CONSTRAINT dataverse_dataverserole_pkey PRIMARY KEY (dataverse_id, roles_id);


--
-- Name: dataverse_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY dataverse
    ADD CONSTRAINT dataverse_pkey PRIMARY KEY (id);


--
-- Name: dataverserole_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY dataverserole
    ADD CONSTRAINT dataverserole_pkey PRIMARY KEY (id);


--
-- Name: dataverserole_userdataverseassignedrole_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY dataverserole_userdataverseassignedrole
    ADD CONSTRAINT dataverserole_userdataverseassignedrole_pkey PRIMARY KEY (dataverserole_id, assignedroles_id);


--
-- Name: dataverseuser_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY dataverseuser
    ADD CONSTRAINT dataverseuser_pkey PRIMARY KEY (id);


--
-- Name: dataverseuser_userdataverseassignedrole_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY dataverseuser_userdataverseassignedrole
    ADD CONSTRAINT dataverseuser_userdataverseassignedrole_pkey PRIMARY KEY (dataverseuser_id, assignedroles_id);


--
-- Name: summarystatistic_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY summarystatistic
    ADD CONSTRAINT summarystatistic_pkey PRIMARY KEY (id);


--
-- Name: summarystatistictype_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY summarystatistictype
    ADD CONSTRAINT summarystatistictype_pkey PRIMARY KEY (id);


--
-- Name: unq_userdataverseassignedrole_0; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY userdataverseassignedrole
    ADD CONSTRAINT unq_userdataverseassignedrole_0 UNIQUE (user_id, role_id);


--
-- Name: userdataverseassignedrole_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY userdataverseassignedrole
    ADD CONSTRAINT userdataverseassignedrole_pkey PRIMARY KEY (id);


--
-- Name: variablecategory_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY variablecategory
    ADD CONSTRAINT variablecategory_pkey PRIMARY KEY (id);


--
-- Name: variableformattype_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY variableformattype
    ADD CONSTRAINT variableformattype_pkey PRIMARY KEY (id);


--
-- Name: variableintervaltype_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY variableintervaltype
    ADD CONSTRAINT variableintervaltype_pkey PRIMARY KEY (id);


--
-- Name: variablerange_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY variablerange
    ADD CONSTRAINT variablerange_pkey PRIMARY KEY (id);


--
-- Name: variablerangeitem_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY variablerangeitem
    ADD CONSTRAINT variablerangeitem_pkey PRIMARY KEY (id);


--
-- Name: variablerangetype_pkey; Type: CONSTRAINT; Schema: public; Owner: dvnapp; Tablespace: 
--

ALTER TABLE ONLY variablerangetype
    ADD CONSTRAINT variablerangetype_pkey PRIMARY KEY (id);


--
-- Name: fk_datafile_dataset_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY datafile
    ADD CONSTRAINT fk_datafile_dataset_id FOREIGN KEY (dataset_id) REFERENCES dataset(id);


--
-- Name: fk_dataset_owner_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY dataset
    ADD CONSTRAINT fk_dataset_owner_id FOREIGN KEY (owner_id) REFERENCES dataverse(id);


--
-- Name: fk_datatable_datafile_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY datatable
    ADD CONSTRAINT fk_datatable_datafile_id FOREIGN KEY (datafile_id) REFERENCES datafile(id);


--
-- Name: fk_datavariable_datatable_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY datavariable
    ADD CONSTRAINT fk_datavariable_datatable_id FOREIGN KEY (datatable_id) REFERENCES datatable(id);


--
-- Name: fk_datavariable_variableformattype_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY datavariable
    ADD CONSTRAINT fk_datavariable_variableformattype_id FOREIGN KEY (variableformattype_id) REFERENCES variableformattype(id);


--
-- Name: fk_datavariable_variableintervaltype_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY datavariable
    ADD CONSTRAINT fk_datavariable_variableintervaltype_id FOREIGN KEY (variableintervaltype_id) REFERENCES variableintervaltype(id);


--
-- Name: fk_dataverse_dataverserole_dataverse_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY dataverse_dataverserole
    ADD CONSTRAINT fk_dataverse_dataverserole_dataverse_id FOREIGN KEY (dataverse_id) REFERENCES dataverse(id);


--
-- Name: fk_dataverse_dataverserole_roles_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY dataverse_dataverserole
    ADD CONSTRAINT fk_dataverse_dataverserole_roles_id FOREIGN KEY (roles_id) REFERENCES dataverserole(id);


--
-- Name: fk_dataverse_owner_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY dataverse
    ADD CONSTRAINT fk_dataverse_owner_id FOREIGN KEY (owner_id) REFERENCES dataverse(id);


--
-- Name: fk_dataverserole_owner_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY dataverserole
    ADD CONSTRAINT fk_dataverserole_owner_id FOREIGN KEY (owner_id) REFERENCES dataverse(id);


--
-- Name: fk_dataverserole_userdataverseassignedrole_assignedroles_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY dataverserole_userdataverseassignedrole
    ADD CONSTRAINT fk_dataverserole_userdataverseassignedrole_assignedroles_id FOREIGN KEY (assignedroles_id) REFERENCES userdataverseassignedrole(id);


--
-- Name: fk_dataverserole_userdataverseassignedrole_dataverserole_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY dataverserole_userdataverseassignedrole
    ADD CONSTRAINT fk_dataverserole_userdataverseassignedrole_dataverserole_id FOREIGN KEY (dataverserole_id) REFERENCES dataverserole(id);


--
-- Name: fk_dataverseuser_userdataverseassignedrole_assignedroles_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY dataverseuser_userdataverseassignedrole
    ADD CONSTRAINT fk_dataverseuser_userdataverseassignedrole_assignedroles_id FOREIGN KEY (assignedroles_id) REFERENCES userdataverseassignedrole(id);


--
-- Name: fk_dataverseuser_userdataverseassignedrole_dataverseuser_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY dataverseuser_userdataverseassignedrole
    ADD CONSTRAINT fk_dataverseuser_userdataverseassignedrole_dataverseuser_id FOREIGN KEY (dataverseuser_id) REFERENCES dataverseuser(id);


--
-- Name: fk_summarystatistic_datavariable_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY summarystatistic
    ADD CONSTRAINT fk_summarystatistic_datavariable_id FOREIGN KEY (datavariable_id) REFERENCES datavariable(id);


--
-- Name: fk_summarystatistic_type_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY summarystatistic
    ADD CONSTRAINT fk_summarystatistic_type_id FOREIGN KEY (type_id) REFERENCES summarystatistictype(id);


--
-- Name: fk_userdataverseassignedrole_role_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY userdataverseassignedrole
    ADD CONSTRAINT fk_userdataverseassignedrole_role_id FOREIGN KEY (role_id) REFERENCES dataverserole(id);


--
-- Name: fk_userdataverseassignedrole_user_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY userdataverseassignedrole
    ADD CONSTRAINT fk_userdataverseassignedrole_user_id FOREIGN KEY (user_id) REFERENCES dataverseuser(id);


--
-- Name: fk_variablecategory_datavariable_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY variablecategory
    ADD CONSTRAINT fk_variablecategory_datavariable_id FOREIGN KEY (datavariable_id) REFERENCES datavariable(id);


--
-- Name: fk_variablerange_beginvaluetype_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY variablerange
    ADD CONSTRAINT fk_variablerange_beginvaluetype_id FOREIGN KEY (beginvaluetype_id) REFERENCES variablerangetype(id);


--
-- Name: fk_variablerange_datavariable_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY variablerange
    ADD CONSTRAINT fk_variablerange_datavariable_id FOREIGN KEY (datavariable_id) REFERENCES datavariable(id);


--
-- Name: fk_variablerange_endvaluetype_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY variablerange
    ADD CONSTRAINT fk_variablerange_endvaluetype_id FOREIGN KEY (endvaluetype_id) REFERENCES variablerangetype(id);


--
-- Name: fk_variablerangeitem_datavariable_id; Type: FK CONSTRAINT; Schema: public; Owner: dvnapp
--

ALTER TABLE ONLY variablerangeitem
    ADD CONSTRAINT fk_variablerangeitem_datavariable_id FOREIGN KEY (datavariable_id) REFERENCES datavariable(id);


--
-- Name: public; Type: ACL; Schema: -; Owner: michael
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM michael;
GRANT ALL ON SCHEMA public TO michael;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

