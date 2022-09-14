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
-- Name: actionlogrecord; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE actionlogrecord (
    id character varying(36) NOT NULL,
    actionresult character varying(255),
    actionsubtype character varying(255),
    actiontype character varying(255),
    endtime timestamp without time zone,
    info character varying(1024),
    starttime timestamp without time zone,
    useridentifier character varying(255)
);


ALTER TABLE public.actionlogrecord OWNER TO dataverse_app;

--
-- Name: apitoken; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE apitoken (
    id integer NOT NULL,
    createtime timestamp without time zone NOT NULL,
    disabled boolean NOT NULL,
    expiretime timestamp without time zone NOT NULL,
    tokenstring character varying(255) NOT NULL,
    authenticateduser_id bigint NOT NULL
);


ALTER TABLE public.apitoken OWNER TO dataverse_app;

--
-- Name: apitoken_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE apitoken_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.apitoken_id_seq OWNER TO dataverse_app;

--
-- Name: apitoken_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE apitoken_id_seq OWNED BY apitoken.id;


--
-- Name: apitoken_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('apitoken_id_seq', 1, true);


--
-- Name: authenticateduser; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE authenticateduser (
    id integer NOT NULL,
    affiliation character varying(255),
    email character varying(255) NOT NULL,
    firstname character varying(255),
    lastname character varying(255),
    modificationtime timestamp without time zone,
    name character varying(255),
    "position" character varying(255),
    superuser boolean,
    useridentifier character varying(255) NOT NULL
);


ALTER TABLE public.authenticateduser OWNER TO dataverse_app;

--
-- Name: authenticateduser_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE authenticateduser_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.authenticateduser_id_seq OWNER TO dataverse_app;

--
-- Name: authenticateduser_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE authenticateduser_id_seq OWNED BY authenticateduser.id;


--
-- Name: authenticateduser_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('authenticateduser_id_seq', 1, true);


--
-- Name: authenticateduserlookup; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE authenticateduserlookup (
    id integer NOT NULL,
    authenticationproviderid character varying(255),
    persistentuserid character varying(255),
    authenticateduser_id bigint NOT NULL
);


ALTER TABLE public.authenticateduserlookup OWNER TO dataverse_app;

--
-- Name: authenticateduserlookup_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE authenticateduserlookup_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.authenticateduserlookup_id_seq OWNER TO dataverse_app;

--
-- Name: authenticateduserlookup_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE authenticateduserlookup_id_seq OWNED BY authenticateduserlookup.id;


--
-- Name: authenticateduserlookup_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('authenticateduserlookup_id_seq', 1, true);


--
-- Name: authenticationproviderrow; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE authenticationproviderrow (
    id character varying(255) NOT NULL,
    enabled boolean,
    factoryalias character varying(255),
    factorydata text,
    subtitle character varying(255),
    title character varying(255)
);


ALTER TABLE public.authenticationproviderrow OWNER TO dataverse_app;

--
-- Name: builtinuser; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE builtinuser (
    id integer NOT NULL,
    affiliation character varying(255),
    email character varying(255) NOT NULL,
    encryptedpassword character varying(255),
    firstname character varying(255),
    lastname character varying(255),
    passwordencryptionversion integer,
    "position" character varying(255),
    username character varying(255) NOT NULL
);


ALTER TABLE public.builtinuser OWNER TO dataverse_app;

--
-- Name: builtinuser_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE builtinuser_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.builtinuser_id_seq OWNER TO dataverse_app;

--
-- Name: builtinuser_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE builtinuser_id_seq OWNED BY builtinuser.id;


--
-- Name: builtinuser_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('builtinuser_id_seq', 1, true);


--
-- Name: controlledvocabalternate; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE controlledvocabalternate (
    id integer NOT NULL,
    strvalue text,
    controlledvocabularyvalue_id bigint NOT NULL,
    datasetfieldtype_id bigint NOT NULL
);


ALTER TABLE public.controlledvocabalternate OWNER TO dataverse_app;

--
-- Name: controlledvocabalternate_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE controlledvocabalternate_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.controlledvocabalternate_id_seq OWNER TO dataverse_app;

--
-- Name: controlledvocabalternate_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE controlledvocabalternate_id_seq OWNED BY controlledvocabalternate.id;


--
-- Name: controlledvocabalternate_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('controlledvocabalternate_id_seq', 24, true);


--
-- Name: controlledvocabularyvalue; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE controlledvocabularyvalue (
    id integer NOT NULL,
    displayorder integer,
    identifier character varying(255),
    strvalue text,
    datasetfieldtype_id bigint
);


ALTER TABLE public.controlledvocabularyvalue OWNER TO dataverse_app;

--
-- Name: controlledvocabularyvalue_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE controlledvocabularyvalue_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.controlledvocabularyvalue_id_seq OWNER TO dataverse_app;

--
-- Name: controlledvocabularyvalue_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE controlledvocabularyvalue_id_seq OWNED BY controlledvocabularyvalue.id;


--
-- Name: controlledvocabularyvalue_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('controlledvocabularyvalue_id_seq', 824, true);


--
-- Name: customfieldmap; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE customfieldmap (
    id integer NOT NULL,
    sourcedatasetfield character varying(255),
    sourcetemplate character varying(255),
    targetdatasetfield character varying(255)
);


ALTER TABLE public.customfieldmap OWNER TO dataverse_app;

--
-- Name: customfieldmap_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE customfieldmap_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.customfieldmap_id_seq OWNER TO dataverse_app;

--
-- Name: customfieldmap_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE customfieldmap_id_seq OWNED BY customfieldmap.id;


--
-- Name: customfieldmap_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('customfieldmap_id_seq', 1, false);


--
-- Name: customquestion; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE customquestion (
    id integer NOT NULL,
    displayorder integer,
    hidden boolean,
    questionstring character varying(255) NOT NULL,
    questiontype character varying(255) NOT NULL,
    required boolean,
    guestbook_id bigint NOT NULL
);


ALTER TABLE public.customquestion OWNER TO dataverse_app;

--
-- Name: customquestion_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE customquestion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.customquestion_id_seq OWNER TO dataverse_app;

--
-- Name: customquestion_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE customquestion_id_seq OWNED BY customquestion.id;


--
-- Name: customquestion_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('customquestion_id_seq', 1, false);


--
-- Name: customquestionresponse; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE customquestionresponse (
    id integer NOT NULL,
    response character varying(255),
    customquestion_id bigint NOT NULL,
    guestbookresponse_id bigint NOT NULL
);


ALTER TABLE public.customquestionresponse OWNER TO dataverse_app;

--
-- Name: customquestionresponse_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE customquestionresponse_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.customquestionresponse_id_seq OWNER TO dataverse_app;

--
-- Name: customquestionresponse_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE customquestionresponse_id_seq OWNED BY customquestionresponse.id;


--
-- Name: customquestionresponse_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('customquestionresponse_id_seq', 1, false);


--
-- Name: customquestionvalue; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE customquestionvalue (
    id integer NOT NULL,
    displayorder integer,
    valuestring character varying(255) NOT NULL,
    customquestion_id bigint NOT NULL
);


ALTER TABLE public.customquestionvalue OWNER TO dataverse_app;

--
-- Name: customquestionvalue_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE customquestionvalue_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.customquestionvalue_id_seq OWNER TO dataverse_app;

--
-- Name: customquestionvalue_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE customquestionvalue_id_seq OWNED BY customquestionvalue.id;


--
-- Name: customquestionvalue_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('customquestionvalue_id_seq', 1, false);


--
-- Name: datafile; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE datafile (
    id bigint NOT NULL,
    contenttype character varying(255) NOT NULL,
    filesystemname character varying(255) NOT NULL,
    filesize bigint,
    ingeststatus character(1),
    md5 character varying(255) NOT NULL,
    name character varying(255),
    restricted boolean
);


ALTER TABLE public.datafile OWNER TO dataverse_app;

--
-- Name: datafilecategory; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE datafilecategory (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    dataset_id bigint NOT NULL
);


ALTER TABLE public.datafilecategory OWNER TO dataverse_app;

--
-- Name: datafilecategory_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE datafilecategory_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.datafilecategory_id_seq OWNER TO dataverse_app;

--
-- Name: datafilecategory_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE datafilecategory_id_seq OWNED BY datafilecategory.id;


--
-- Name: datafilecategory_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('datafilecategory_id_seq', 1, true);


--
-- Name: datafiletag; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE datafiletag (
    id integer NOT NULL,
    type integer NOT NULL,
    datafile_id bigint NOT NULL
);


ALTER TABLE public.datafiletag OWNER TO dataverse_app;

--
-- Name: datafiletag_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE datafiletag_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.datafiletag_id_seq OWNER TO dataverse_app;

--
-- Name: datafiletag_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE datafiletag_id_seq OWNED BY datafiletag.id;


--
-- Name: datafiletag_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('datafiletag_id_seq', 1, false);


--
-- Name: dataset; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE dataset (
    id bigint NOT NULL,
    authority character varying(255),
    doiseparator character varying(255),
    fileaccessrequest boolean,
    globalidcreatetime timestamp without time zone,
    identifier character varying(255) NOT NULL,
    protocol character varying(255),
    guestbook_id bigint,
    thumbnailfile_id bigint
);


ALTER TABLE public.dataset OWNER TO dataverse_app;

--
-- Name: datasetfield; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE datasetfield (
    id integer NOT NULL,
    datasetfieldtype_id bigint NOT NULL,
    datasetversion_id bigint,
    parentdatasetfieldcompoundvalue_id bigint,
    template_id bigint
);


ALTER TABLE public.datasetfield OWNER TO dataverse_app;

--
-- Name: datasetfield_controlledvocabularyvalue; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE datasetfield_controlledvocabularyvalue (
    datasetfield_id bigint NOT NULL,
    controlledvocabularyvalues_id bigint NOT NULL
);


ALTER TABLE public.datasetfield_controlledvocabularyvalue OWNER TO dataverse_app;

--
-- Name: datasetfield_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE datasetfield_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.datasetfield_id_seq OWNER TO dataverse_app;

--
-- Name: datasetfield_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE datasetfield_id_seq OWNED BY datasetfield.id;


--
-- Name: datasetfield_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('datasetfield_id_seq', 14, true);


--
-- Name: datasetfieldcompoundvalue; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE datasetfieldcompoundvalue (
    id integer NOT NULL,
    displayorder integer,
    parentdatasetfield_id bigint
);


ALTER TABLE public.datasetfieldcompoundvalue OWNER TO dataverse_app;

--
-- Name: datasetfieldcompoundvalue_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE datasetfieldcompoundvalue_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.datasetfieldcompoundvalue_id_seq OWNER TO dataverse_app;

--
-- Name: datasetfieldcompoundvalue_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE datasetfieldcompoundvalue_id_seq OWNED BY datasetfieldcompoundvalue.id;


--
-- Name: datasetfieldcompoundvalue_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('datasetfieldcompoundvalue_id_seq', 3, true);


--
-- Name: datasetfielddefaultvalue; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE datasetfielddefaultvalue (
    id integer NOT NULL,
    displayorder integer,
    strvalue text,
    datasetfield_id bigint NOT NULL,
    defaultvalueset_id bigint NOT NULL,
    parentdatasetfielddefaultvalue_id bigint
);


ALTER TABLE public.datasetfielddefaultvalue OWNER TO dataverse_app;

--
-- Name: datasetfielddefaultvalue_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE datasetfielddefaultvalue_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.datasetfielddefaultvalue_id_seq OWNER TO dataverse_app;

--
-- Name: datasetfielddefaultvalue_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE datasetfielddefaultvalue_id_seq OWNED BY datasetfielddefaultvalue.id;


--
-- Name: datasetfielddefaultvalue_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('datasetfielddefaultvalue_id_seq', 1, false);


--
-- Name: datasetfieldtype; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE datasetfieldtype (
    id integer NOT NULL,
    advancedsearchfieldtype boolean,
    allowcontrolledvocabulary boolean,
    allowmultiples boolean,
    description text,
    displayformat character varying(255),
    displayoncreate boolean,
    displayorder integer,
    facetable boolean,
    fieldtype character varying(255) NOT NULL,
    name text,
    required boolean,
    title text,
    watermark character varying(255),
    metadatablock_id bigint,
    parentdatasetfieldtype_id bigint
);


ALTER TABLE public.datasetfieldtype OWNER TO dataverse_app;

--
-- Name: datasetfieldtype_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE datasetfieldtype_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.datasetfieldtype_id_seq OWNER TO dataverse_app;

--
-- Name: datasetfieldtype_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE datasetfieldtype_id_seq OWNED BY datasetfieldtype.id;


--
-- Name: datasetfieldtype_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('datasetfieldtype_id_seq', 154, true);


--
-- Name: datasetfieldvalue; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE datasetfieldvalue (
    id integer NOT NULL,
    displayorder integer,
    value text,
    datasetfield_id bigint NOT NULL
);


ALTER TABLE public.datasetfieldvalue OWNER TO dataverse_app;

--
-- Name: datasetfieldvalue_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE datasetfieldvalue_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.datasetfieldvalue_id_seq OWNER TO dataverse_app;

--
-- Name: datasetfieldvalue_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE datasetfieldvalue_id_seq OWNED BY datasetfieldvalue.id;


--
-- Name: datasetfieldvalue_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('datasetfieldvalue_id_seq', 9, true);


--
-- Name: datasetlinkingdataverse; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE datasetlinkingdataverse (
    id integer NOT NULL,
    linkcreatetime timestamp without time zone NOT NULL,
    dataset_id bigint NOT NULL,
    linkingdataverse_id bigint NOT NULL
);


ALTER TABLE public.datasetlinkingdataverse OWNER TO dataverse_app;

--
-- Name: datasetlinkingdataverse_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE datasetlinkingdataverse_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.datasetlinkingdataverse_id_seq OWNER TO dataverse_app;

--
-- Name: datasetlinkingdataverse_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE datasetlinkingdataverse_id_seq OWNED BY datasetlinkingdataverse.id;


--
-- Name: datasetlinkingdataverse_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('datasetlinkingdataverse_id_seq', 1, false);


--
-- Name: datasetlock; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE datasetlock (
    id integer NOT NULL,
    info character varying(255),
    starttime timestamp without time zone,
    user_id bigint NOT NULL,
    dataset_id bigint NOT NULL
);


ALTER TABLE public.datasetlock OWNER TO dataverse_app;

--
-- Name: datasetlock_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE datasetlock_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.datasetlock_id_seq OWNER TO dataverse_app;

--
-- Name: datasetlock_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE datasetlock_id_seq OWNED BY datasetlock.id;


--
-- Name: datasetlock_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('datasetlock_id_seq', 1, false);


--
-- Name: datasetversion; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE datasetversion (
    id integer NOT NULL,
    unf character varying(255),
    archivenote character varying(1000),
    archivetime timestamp without time zone,
    availabilitystatus text,
    citationrequirements text,
    conditions text,
    confidentialitydeclaration text,
    contactforaccess text,
    createtime timestamp without time zone NOT NULL,
    dataaccessplace text,
    deaccessionlink character varying(255),
    depositorrequirements text,
    disclaimer text,
    fileaccessrequest boolean,
    inreview boolean,
    lastupdatetime timestamp without time zone NOT NULL,
    license character varying(255),
    minorversionnumber bigint,
    originalarchive text,
    releasetime timestamp without time zone,
    restrictions text,
    sizeofcollection text,
    specialpermissions text,
    studycompletion text,
    termsofaccess text,
    termsofuse text,
    version bigint,
    versionnote character varying(1000),
    versionnumber bigint,
    versionstate character varying(255),
    dataset_id bigint
);


ALTER TABLE public.datasetversion OWNER TO dataverse_app;

--
-- Name: datasetversion_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE datasetversion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.datasetversion_id_seq OWNER TO dataverse_app;

--
-- Name: datasetversion_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE datasetversion_id_seq OWNED BY datasetversion.id;


--
-- Name: datasetversion_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('datasetversion_id_seq', 1, true);


--
-- Name: datasetversionuser; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE datasetversionuser (
    id integer NOT NULL,
    lastupdatedate timestamp without time zone NOT NULL,
    authenticateduser_id bigint,
    datasetversion_id bigint
);


ALTER TABLE public.datasetversionuser OWNER TO dataverse_app;

--
-- Name: datasetversionuser_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE datasetversionuser_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.datasetversionuser_id_seq OWNER TO dataverse_app;

--
-- Name: datasetversionuser_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE datasetversionuser_id_seq OWNED BY datasetversionuser.id;


--
-- Name: datasetversionuser_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('datasetversionuser_id_seq', 1, true);


--
-- Name: datatable; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE datatable (
    id integer NOT NULL,
    casequantity bigint,
    originalfileformat character varying(255),
    originalformatversion character varying(255),
    recordspercase bigint,
    unf character varying(255) NOT NULL,
    varquantity bigint,
    datafile_id bigint NOT NULL
);


ALTER TABLE public.datatable OWNER TO dataverse_app;

--
-- Name: datatable_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE datatable_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.datatable_id_seq OWNER TO dataverse_app;

--
-- Name: datatable_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE datatable_id_seq OWNED BY datatable.id;


--
-- Name: datatable_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('datatable_id_seq', 1, false);


--
-- Name: datavariable; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE datavariable (
    id integer NOT NULL,
    fileendposition bigint,
    fileorder integer,
    filestartposition bigint,
    format character varying(255),
    formatcategory character varying(255),
    "interval" integer,
    label text,
    name character varying(255),
    numberofdecimalpoints bigint,
    orderedfactor boolean,
    recordsegmentnumber bigint,
    type integer,
    unf character varying(255),
    universe character varying(255),
    weighted boolean,
    datatable_id bigint NOT NULL
);


ALTER TABLE public.datavariable OWNER TO dataverse_app;

--
-- Name: datavariable_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE datavariable_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.datavariable_id_seq OWNER TO dataverse_app;

--
-- Name: datavariable_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE datavariable_id_seq OWNED BY datavariable.id;


--
-- Name: datavariable_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('datavariable_id_seq', 1, false);


--
-- Name: dataverse; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE dataverse (
    id bigint NOT NULL,
    affiliation character varying(255),
    alias character varying(255) NOT NULL,
    dataversetype character varying(255) NOT NULL,
    description text,
    facetroot boolean,
    guestbookroot boolean,
    metadatablockroot boolean,
    name character varying(255) NOT NULL,
    permissionroot boolean,
    templateroot boolean,
    themeroot boolean,
    defaultcontributorrole_id bigint NOT NULL,
    defaulttemplate_id bigint
);


ALTER TABLE public.dataverse OWNER TO dataverse_app;

--
-- Name: dataverse_metadatablock; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE dataverse_metadatablock (
    dataverse_id bigint NOT NULL,
    metadatablocks_id bigint NOT NULL
);


ALTER TABLE public.dataverse_metadatablock OWNER TO dataverse_app;

--
-- Name: dataversecontact; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE dataversecontact (
    id integer NOT NULL,
    contactemail character varying(255) NOT NULL,
    displayorder integer,
    dataverse_id bigint
);


ALTER TABLE public.dataversecontact OWNER TO dataverse_app;

--
-- Name: dataversecontact_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE dataversecontact_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dataversecontact_id_seq OWNER TO dataverse_app;

--
-- Name: dataversecontact_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE dataversecontact_id_seq OWNED BY dataversecontact.id;


--
-- Name: dataversecontact_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('dataversecontact_id_seq', 2, true);


--
-- Name: dataversefacet; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE dataversefacet (
    id integer NOT NULL,
    displayorder integer,
    datasetfieldtype_id bigint,
    dataverse_id bigint
);


ALTER TABLE public.dataversefacet OWNER TO dataverse_app;

--
-- Name: dataversefacet_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE dataversefacet_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dataversefacet_id_seq OWNER TO dataverse_app;

--
-- Name: dataversefacet_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE dataversefacet_id_seq OWNED BY dataversefacet.id;


--
-- Name: dataversefacet_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('dataversefacet_id_seq', 4, true);


--
-- Name: dataversefeatureddataverse; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE dataversefeatureddataverse (
    id integer NOT NULL,
    displayorder integer,
    dataverse_id bigint,
    featureddataverse_id bigint
);


ALTER TABLE public.dataversefeatureddataverse OWNER TO dataverse_app;

--
-- Name: dataversefeatureddataverse_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE dataversefeatureddataverse_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dataversefeatureddataverse_id_seq OWNER TO dataverse_app;

--
-- Name: dataversefeatureddataverse_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE dataversefeatureddataverse_id_seq OWNED BY dataversefeatureddataverse.id;


--
-- Name: dataversefeatureddataverse_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('dataversefeatureddataverse_id_seq', 1, false);


--
-- Name: dataversefieldtypeinputlevel; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE dataversefieldtypeinputlevel (
    id integer NOT NULL,
    include boolean,
    required boolean,
    datasetfieldtype_id bigint,
    dataverse_id bigint
);


ALTER TABLE public.dataversefieldtypeinputlevel OWNER TO dataverse_app;

--
-- Name: dataversefieldtypeinputlevel_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE dataversefieldtypeinputlevel_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dataversefieldtypeinputlevel_id_seq OWNER TO dataverse_app;

--
-- Name: dataversefieldtypeinputlevel_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE dataversefieldtypeinputlevel_id_seq OWNED BY dataversefieldtypeinputlevel.id;


--
-- Name: dataversefieldtypeinputlevel_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('dataversefieldtypeinputlevel_id_seq', 1, false);


--
-- Name: dataverselinkingdataverse; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE dataverselinkingdataverse (
    id integer NOT NULL,
    linkcreatetime timestamp without time zone,
    dataverse_id bigint NOT NULL,
    linkingdataverse_id bigint NOT NULL
);


ALTER TABLE public.dataverselinkingdataverse OWNER TO dataverse_app;

--
-- Name: dataverselinkingdataverse_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE dataverselinkingdataverse_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dataverselinkingdataverse_id_seq OWNER TO dataverse_app;

--
-- Name: dataverselinkingdataverse_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE dataverselinkingdataverse_id_seq OWNED BY dataverselinkingdataverse.id;


--
-- Name: dataverselinkingdataverse_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('dataverselinkingdataverse_id_seq', 1, false);


--
-- Name: dataverserole; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE dataverserole (
    id integer NOT NULL,
    alias character varying(255) NOT NULL,
    description character varying(255),
    name character varying(255) NOT NULL,
    permissionbits bigint,
    owner_id bigint
);


ALTER TABLE public.dataverserole OWNER TO dataverse_app;

--
-- Name: dataverserole_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE dataverserole_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dataverserole_id_seq OWNER TO dataverse_app;

--
-- Name: dataverserole_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE dataverserole_id_seq OWNED BY dataverserole.id;


--
-- Name: dataverserole_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('dataverserole_id_seq', 8, true);


--
-- Name: dataversesubjects; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE dataversesubjects (
    dataverse_id bigint NOT NULL,
    controlledvocabularyvalue_id bigint NOT NULL
);


ALTER TABLE public.dataversesubjects OWNER TO dataverse_app;

--
-- Name: dataversetheme; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE dataversetheme (
    id integer NOT NULL,
    backgroundcolor character varying(255),
    linkcolor character varying(255),
    linkurl character varying(255),
    logo character varying(255),
    logoalignment character varying(255),
    logobackgroundcolor character varying(255),
    logoformat character varying(255),
    tagline character varying(255),
    textcolor character varying(255),
    dataverse_id bigint
);


ALTER TABLE public.dataversetheme OWNER TO dataverse_app;

--
-- Name: dataversetheme_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE dataversetheme_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dataversetheme_id_seq OWNER TO dataverse_app;

--
-- Name: dataversetheme_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE dataversetheme_id_seq OWNED BY dataversetheme.id;


--
-- Name: dataversetheme_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('dataversetheme_id_seq', 1, false);


--
-- Name: defaultvalueset; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE defaultvalueset (
    id integer NOT NULL,
    name character varying(255) NOT NULL
);


ALTER TABLE public.defaultvalueset OWNER TO dataverse_app;

--
-- Name: defaultvalueset_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE defaultvalueset_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.defaultvalueset_id_seq OWNER TO dataverse_app;

--
-- Name: defaultvalueset_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE defaultvalueset_id_seq OWNED BY defaultvalueset.id;


--
-- Name: defaultvalueset_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('defaultvalueset_id_seq', 1, false);


--
-- Name: dvobject; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE dvobject (
    id integer NOT NULL,
    dtype character varying(31),
    createdate timestamp without time zone NOT NULL,
    indextime timestamp without time zone,
    modificationtime timestamp without time zone NOT NULL,
    permissionindextime timestamp without time zone,
    permissionmodificationtime timestamp without time zone,
    publicationdate timestamp without time zone,
    creator_id bigint,
    owner_id bigint,
    releaseuser_id bigint
);


ALTER TABLE public.dvobject OWNER TO dataverse_app;

--
-- Name: dvobject_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE dvobject_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.dvobject_id_seq OWNER TO dataverse_app;

--
-- Name: dvobject_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE dvobject_id_seq OWNED BY dvobject.id;


--
-- Name: dvobject_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('dvobject_id_seq', 4, true);


--
-- Name: explicitgroup; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE explicitgroup (
    id integer NOT NULL,
    description character varying(1024),
    displayname character varying(255),
    groupalias character varying(255),
    groupaliasinowner character varying(255),
    owner_id bigint
);


ALTER TABLE public.explicitgroup OWNER TO dataverse_app;

--
-- Name: explicitgroup_authenticateduser; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE explicitgroup_authenticateduser (
    explicitgroup_id bigint NOT NULL,
    containedauthenticatedusers_id bigint NOT NULL
);


ALTER TABLE public.explicitgroup_authenticateduser OWNER TO dataverse_app;

--
-- Name: explicitgroup_containedroleassignees; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE explicitgroup_containedroleassignees (
    explicitgroup_id bigint,
    containedroleassignees character varying(255)
);


ALTER TABLE public.explicitgroup_containedroleassignees OWNER TO dataverse_app;

--
-- Name: explicitgroup_explicitgroup; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE explicitgroup_explicitgroup (
    explicitgroup_id bigint NOT NULL,
    containedexplicitgroups_id bigint NOT NULL
);


ALTER TABLE public.explicitgroup_explicitgroup OWNER TO dataverse_app;

--
-- Name: explicitgroup_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE explicitgroup_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.explicitgroup_id_seq OWNER TO dataverse_app;

--
-- Name: explicitgroup_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE explicitgroup_id_seq OWNED BY explicitgroup.id;


--
-- Name: explicitgroup_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('explicitgroup_id_seq', 1, false);


--
-- Name: fileaccessrequests; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE fileaccessrequests (
    datafile_id bigint NOT NULL,
    authenticated_user_id bigint NOT NULL
);


ALTER TABLE public.fileaccessrequests OWNER TO dataverse_app;

--
-- Name: filemetadata; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE filemetadata (
    id integer NOT NULL,
    description text,
    label character varying(255) NOT NULL,
    restricted boolean,
    version bigint,
    datafile_id bigint NOT NULL,
    datasetversion_id bigint NOT NULL
);


ALTER TABLE public.filemetadata OWNER TO dataverse_app;

--
-- Name: filemetadata_datafilecategory; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE filemetadata_datafilecategory (
    filecategories_id bigint NOT NULL,
    filemetadatas_id bigint NOT NULL
);


ALTER TABLE public.filemetadata_datafilecategory OWNER TO dataverse_app;

--
-- Name: filemetadata_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE filemetadata_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.filemetadata_id_seq OWNER TO dataverse_app;

--
-- Name: filemetadata_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE filemetadata_id_seq OWNED BY filemetadata.id;


--
-- Name: filemetadata_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('filemetadata_id_seq', 1, true);


--
-- Name: foreignmetadatafieldmapping; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE foreignmetadatafieldmapping (
    id integer NOT NULL,
    datasetfieldname text,
    foreignfieldxpath text,
    isattribute boolean,
    foreignmetadataformatmapping_id bigint,
    parentfieldmapping_id bigint
);


ALTER TABLE public.foreignmetadatafieldmapping OWNER TO dataverse_app;

--
-- Name: foreignmetadatafieldmapping_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE foreignmetadatafieldmapping_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.foreignmetadatafieldmapping_id_seq OWNER TO dataverse_app;

--
-- Name: foreignmetadatafieldmapping_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE foreignmetadatafieldmapping_id_seq OWNED BY foreignmetadatafieldmapping.id;


--
-- Name: foreignmetadatafieldmapping_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('foreignmetadatafieldmapping_id_seq', 1, false);


--
-- Name: foreignmetadataformatmapping; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE foreignmetadataformatmapping (
    id integer NOT NULL,
    displayname character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    schemalocation character varying(255),
    startelement character varying(255)
);


ALTER TABLE public.foreignmetadataformatmapping OWNER TO dataverse_app;

--
-- Name: foreignmetadataformatmapping_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE foreignmetadataformatmapping_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.foreignmetadataformatmapping_id_seq OWNER TO dataverse_app;

--
-- Name: foreignmetadataformatmapping_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE foreignmetadataformatmapping_id_seq OWNED BY foreignmetadataformatmapping.id;


--
-- Name: foreignmetadataformatmapping_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('foreignmetadataformatmapping_id_seq', 1, false);


--
-- Name: guestbook; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE guestbook (
    id integer NOT NULL,
    createtime timestamp without time zone NOT NULL,
    emailrequired boolean,
    enabled boolean,
    institutionrequired boolean,
    name character varying(255),
    namerequired boolean,
    positionrequired boolean,
    dataverse_id bigint
);


ALTER TABLE public.guestbook OWNER TO dataverse_app;

--
-- Name: guestbook_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE guestbook_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.guestbook_id_seq OWNER TO dataverse_app;

--
-- Name: guestbook_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE guestbook_id_seq OWNED BY guestbook.id;


--
-- Name: guestbook_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('guestbook_id_seq', 1, false);


--
-- Name: guestbookresponse; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE guestbookresponse (
    id integer NOT NULL,
    downloadtype character varying(255),
    email character varying(255),
    institution character varying(255),
    name character varying(255),
    "position" character varying(255),
    responsetime timestamp without time zone,
    sessionid character varying(255),
    authenticateduser_id bigint,
    datafile_id bigint NOT NULL,
    dataset_id bigint NOT NULL,
    datasetversion_id bigint,
    guestbook_id bigint NOT NULL
);


ALTER TABLE public.guestbookresponse OWNER TO dataverse_app;

--
-- Name: guestbookresponse_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE guestbookresponse_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.guestbookresponse_id_seq OWNER TO dataverse_app;

--
-- Name: guestbookresponse_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE guestbookresponse_id_seq OWNED BY guestbookresponse.id;


--
-- Name: guestbookresponse_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('guestbookresponse_id_seq', 1, false);


--
-- Name: harvestingdataverseconfig; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE harvestingdataverseconfig (
    id bigint NOT NULL,
    archivedescription text,
    archiveurl character varying(255),
    harveststyle character varying(255),
    harvesttype character varying(255),
    harvestingset character varying(255),
    harvestingurl character varying(255),
    dataverse_id bigint
);


ALTER TABLE public.harvestingdataverseconfig OWNER TO dataverse_app;

--
-- Name: ingestreport; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE ingestreport (
    id integer NOT NULL,
    endtime timestamp without time zone,
    report character varying(255),
    starttime timestamp without time zone,
    status integer,
    type integer,
    datafile_id bigint NOT NULL
);


ALTER TABLE public.ingestreport OWNER TO dataverse_app;

--
-- Name: ingestreport_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE ingestreport_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ingestreport_id_seq OWNER TO dataverse_app;

--
-- Name: ingestreport_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE ingestreport_id_seq OWNED BY ingestreport.id;


--
-- Name: ingestreport_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('ingestreport_id_seq', 1, false);


--
-- Name: ingestrequest; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE ingestrequest (
    id integer NOT NULL,
    controlcard character varying(255),
    labelsfile character varying(255),
    textencoding character varying(255),
    datafile_id bigint
);


ALTER TABLE public.ingestrequest OWNER TO dataverse_app;

--
-- Name: ingestrequest_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE ingestrequest_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ingestrequest_id_seq OWNER TO dataverse_app;

--
-- Name: ingestrequest_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE ingestrequest_id_seq OWNED BY ingestrequest.id;


--
-- Name: ingestrequest_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('ingestrequest_id_seq', 1, false);


--
-- Name: ipv4range; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE ipv4range (
    id bigint NOT NULL,
    bottomaslong bigint,
    topaslong bigint,
    owner_id bigint
);


ALTER TABLE public.ipv4range OWNER TO dataverse_app;

--
-- Name: ipv6range; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE ipv6range (
    id bigint NOT NULL,
    bottoma bigint,
    bottomb bigint,
    bottomc bigint,
    bottomd bigint,
    topa bigint,
    topb bigint,
    topc bigint,
    topd bigint,
    owner_id bigint
);


ALTER TABLE public.ipv6range OWNER TO dataverse_app;

--
-- Name: maplayermetadata; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE maplayermetadata (
    id integer NOT NULL,
    embedmaplink character varying(255) NOT NULL,
    layerlink character varying(255) NOT NULL,
    layername character varying(255) NOT NULL,
    mapimagelink character varying(255),
    worldmapusername character varying(255) NOT NULL,
    dataset_id bigint NOT NULL,
    datafile_id bigint NOT NULL
);


ALTER TABLE public.maplayermetadata OWNER TO dataverse_app;

--
-- Name: maplayermetadata_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE maplayermetadata_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.maplayermetadata_id_seq OWNER TO dataverse_app;

--
-- Name: maplayermetadata_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE maplayermetadata_id_seq OWNED BY maplayermetadata.id;


--
-- Name: maplayermetadata_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('maplayermetadata_id_seq', 1, false);


--
-- Name: metadatablock; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE metadatablock (
    id integer NOT NULL,
    displayname character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    owner_id bigint
);


ALTER TABLE public.metadatablock OWNER TO dataverse_app;

--
-- Name: metadatablock_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE metadatablock_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.metadatablock_id_seq OWNER TO dataverse_app;

--
-- Name: metadatablock_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE metadatablock_id_seq OWNED BY metadatablock.id;


--
-- Name: metadatablock_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('metadatablock_id_seq', 6, true);


--
-- Name: passwordresetdata; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE passwordresetdata (
    id integer NOT NULL,
    created timestamp without time zone NOT NULL,
    expires timestamp without time zone NOT NULL,
    reason character varying(255),
    token character varying(255),
    builtinuser_id bigint NOT NULL
);


ALTER TABLE public.passwordresetdata OWNER TO dataverse_app;

--
-- Name: passwordresetdata_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE passwordresetdata_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.passwordresetdata_id_seq OWNER TO dataverse_app;

--
-- Name: passwordresetdata_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE passwordresetdata_id_seq OWNED BY passwordresetdata.id;


--
-- Name: passwordresetdata_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('passwordresetdata_id_seq', 1, false);


--
-- Name: persistedglobalgroup; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE persistedglobalgroup (
    id bigint NOT NULL,
    dtype character varying(31),
    description character varying(255),
    displayname character varying(255),
    persistedgroupalias character varying(255)
);


ALTER TABLE public.persistedglobalgroup OWNER TO dataverse_app;

--
-- Name: roleassignment; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE roleassignment (
    id integer NOT NULL,
    assigneeidentifier character varying(255) NOT NULL,
    definitionpoint_id bigint NOT NULL,
    role_id bigint NOT NULL
);


ALTER TABLE public.roleassignment OWNER TO dataverse_app;

--
-- Name: roleassignment_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE roleassignment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.roleassignment_id_seq OWNER TO dataverse_app;

--
-- Name: roleassignment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE roleassignment_id_seq OWNED BY roleassignment.id;


--
-- Name: roleassignment_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('roleassignment_id_seq', 3, true);


--
-- Name: savedsearch; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE savedsearch (
    id integer NOT NULL,
    query text,
    creator_id bigint NOT NULL,
    definitionpoint_id bigint NOT NULL
);


ALTER TABLE public.savedsearch OWNER TO dataverse_app;

--
-- Name: savedsearch_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE savedsearch_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.savedsearch_id_seq OWNER TO dataverse_app;

--
-- Name: savedsearch_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE savedsearch_id_seq OWNED BY savedsearch.id;


--
-- Name: savedsearch_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('savedsearch_id_seq', 1, false);


--
-- Name: savedsearchfilterquery; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE savedsearchfilterquery (
    id integer NOT NULL,
    filterquery text,
    savedsearch_id bigint NOT NULL
);


ALTER TABLE public.savedsearchfilterquery OWNER TO dataverse_app;

--
-- Name: savedsearchfilterquery_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE savedsearchfilterquery_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.savedsearchfilterquery_id_seq OWNER TO dataverse_app;

--
-- Name: savedsearchfilterquery_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE savedsearchfilterquery_id_seq OWNED BY savedsearchfilterquery.id;


--
-- Name: savedsearchfilterquery_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('savedsearchfilterquery_id_seq', 1, false);


--
-- Name: sequence; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE sequence (
    seq_name character varying(50) NOT NULL,
    seq_count numeric(38,0)
);


ALTER TABLE public.sequence OWNER TO dataverse_app;

--
-- Name: setting; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE setting (
    name character varying(255) NOT NULL,
    content text
);


ALTER TABLE public.setting OWNER TO dataverse_app;

--
-- Name: shibgroup; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE shibgroup (
    id integer NOT NULL,
    attribute character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    pattern character varying(255) NOT NULL
);


ALTER TABLE public.shibgroup OWNER TO dataverse_app;

--
-- Name: shibgroup_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE shibgroup_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.shibgroup_id_seq OWNER TO dataverse_app;

--
-- Name: shibgroup_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE shibgroup_id_seq OWNED BY shibgroup.id;


--
-- Name: shibgroup_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('shibgroup_id_seq', 1, false);


--
-- Name: summarystatistic; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE summarystatistic (
    id integer NOT NULL,
    type integer,
    value character varying(255),
    datavariable_id bigint NOT NULL
);


ALTER TABLE public.summarystatistic OWNER TO dataverse_app;

--
-- Name: summarystatistic_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE summarystatistic_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.summarystatistic_id_seq OWNER TO dataverse_app;

--
-- Name: summarystatistic_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE summarystatistic_id_seq OWNED BY summarystatistic.id;


--
-- Name: summarystatistic_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('summarystatistic_id_seq', 1, false);


--
-- Name: template; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE template (
    id integer NOT NULL,
    createtime timestamp without time zone NOT NULL,
    name character varying(255) NOT NULL,
    usagecount bigint,
    dataverse_id bigint
);


ALTER TABLE public.template OWNER TO dataverse_app;

--
-- Name: template_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE template_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.template_id_seq OWNER TO dataverse_app;

--
-- Name: template_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE template_id_seq OWNED BY template.id;


--
-- Name: template_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('template_id_seq', 1, false);


--
-- Name: usernotification; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE usernotification (
    id integer NOT NULL,
    emailed boolean,
    objectid bigint,
    readnotification boolean,
    senddate timestamp without time zone,
    type integer NOT NULL,
    user_id bigint NOT NULL
);


ALTER TABLE public.usernotification OWNER TO dataverse_app;

--
-- Name: usernotification_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE usernotification_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.usernotification_id_seq OWNER TO dataverse_app;

--
-- Name: usernotification_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE usernotification_id_seq OWNED BY usernotification.id;


--
-- Name: usernotification_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('usernotification_id_seq', 2, true);


--
-- Name: variablecategory; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
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


ALTER TABLE public.variablecategory OWNER TO dataverse_app;

--
-- Name: variablecategory_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE variablecategory_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.variablecategory_id_seq OWNER TO dataverse_app;

--
-- Name: variablecategory_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE variablecategory_id_seq OWNED BY variablecategory.id;


--
-- Name: variablecategory_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('variablecategory_id_seq', 1, false);


--
-- Name: variablerange; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE variablerange (
    id integer NOT NULL,
    beginvalue character varying(255),
    beginvaluetype integer,
    endvalue character varying(255),
    endvaluetype integer,
    datavariable_id bigint NOT NULL
);


ALTER TABLE public.variablerange OWNER TO dataverse_app;

--
-- Name: variablerange_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE variablerange_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.variablerange_id_seq OWNER TO dataverse_app;

--
-- Name: variablerange_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE variablerange_id_seq OWNED BY variablerange.id;


--
-- Name: variablerange_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('variablerange_id_seq', 1, false);


--
-- Name: variablerangeitem; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE variablerangeitem (
    id integer NOT NULL,
    value numeric(38,0),
    datavariable_id bigint NOT NULL
);


ALTER TABLE public.variablerangeitem OWNER TO dataverse_app;

--
-- Name: variablerangeitem_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE variablerangeitem_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.variablerangeitem_id_seq OWNER TO dataverse_app;

--
-- Name: variablerangeitem_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE variablerangeitem_id_seq OWNED BY variablerangeitem.id;


--
-- Name: variablerangeitem_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('variablerangeitem_id_seq', 1, false);


--
-- Name: worldmapauth_token; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE worldmapauth_token (
    id integer NOT NULL,
    created timestamp without time zone NOT NULL,
    hasexpired boolean NOT NULL,
    lastrefreshtime timestamp without time zone NOT NULL,
    modified timestamp without time zone NOT NULL,
    token character varying(255),
    application_id bigint NOT NULL,
    datafile_id bigint NOT NULL,
    dataverseuser_id bigint NOT NULL
);


ALTER TABLE public.worldmapauth_token OWNER TO dataverse_app;

--
-- Name: worldmapauth_token_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE worldmapauth_token_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.worldmapauth_token_id_seq OWNER TO dataverse_app;

--
-- Name: worldmapauth_token_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE worldmapauth_token_id_seq OWNED BY worldmapauth_token.id;


--
-- Name: worldmapauth_token_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('worldmapauth_token_id_seq', 1, false);


--
-- Name: worldmapauth_tokentype; Type: TABLE; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE TABLE worldmapauth_tokentype (
    id integer NOT NULL,
    contactemail character varying(255),
    created timestamp without time zone NOT NULL,
    hostname character varying(255),
    ipaddress character varying(255),
    mapitlink character varying(255) NOT NULL,
    md5 character varying(255) NOT NULL,
    modified timestamp without time zone NOT NULL,
    name character varying(255) NOT NULL,
    timelimitminutes integer DEFAULT 30,
    timelimitseconds bigint DEFAULT 1800
);


ALTER TABLE public.worldmapauth_tokentype OWNER TO dataverse_app;

--
-- Name: worldmapauth_tokentype_id_seq; Type: SEQUENCE; Schema: public; Owner: dataverse_app
--

CREATE SEQUENCE worldmapauth_tokentype_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.worldmapauth_tokentype_id_seq OWNER TO dataverse_app;

--
-- Name: worldmapauth_tokentype_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: dataverse_app
--

ALTER SEQUENCE worldmapauth_tokentype_id_seq OWNED BY worldmapauth_tokentype.id;


--
-- Name: worldmapauth_tokentype_id_seq; Type: SEQUENCE SET; Schema: public; Owner: dataverse_app
--

SELECT pg_catalog.setval('worldmapauth_tokentype_id_seq', 1, false);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY apitoken ALTER COLUMN id SET DEFAULT nextval('apitoken_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY authenticateduser ALTER COLUMN id SET DEFAULT nextval('authenticateduser_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY authenticateduserlookup ALTER COLUMN id SET DEFAULT nextval('authenticateduserlookup_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY builtinuser ALTER COLUMN id SET DEFAULT nextval('builtinuser_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY controlledvocabalternate ALTER COLUMN id SET DEFAULT nextval('controlledvocabalternate_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY controlledvocabularyvalue ALTER COLUMN id SET DEFAULT nextval('controlledvocabularyvalue_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY customfieldmap ALTER COLUMN id SET DEFAULT nextval('customfieldmap_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY customquestion ALTER COLUMN id SET DEFAULT nextval('customquestion_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY customquestionresponse ALTER COLUMN id SET DEFAULT nextval('customquestionresponse_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY customquestionvalue ALTER COLUMN id SET DEFAULT nextval('customquestionvalue_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datafilecategory ALTER COLUMN id SET DEFAULT nextval('datafilecategory_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datafiletag ALTER COLUMN id SET DEFAULT nextval('datafiletag_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfield ALTER COLUMN id SET DEFAULT nextval('datasetfield_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfieldcompoundvalue ALTER COLUMN id SET DEFAULT nextval('datasetfieldcompoundvalue_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfielddefaultvalue ALTER COLUMN id SET DEFAULT nextval('datasetfielddefaultvalue_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfieldtype ALTER COLUMN id SET DEFAULT nextval('datasetfieldtype_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfieldvalue ALTER COLUMN id SET DEFAULT nextval('datasetfieldvalue_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetlinkingdataverse ALTER COLUMN id SET DEFAULT nextval('datasetlinkingdataverse_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetlock ALTER COLUMN id SET DEFAULT nextval('datasetlock_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetversion ALTER COLUMN id SET DEFAULT nextval('datasetversion_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetversionuser ALTER COLUMN id SET DEFAULT nextval('datasetversionuser_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datatable ALTER COLUMN id SET DEFAULT nextval('datatable_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datavariable ALTER COLUMN id SET DEFAULT nextval('datavariable_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataversecontact ALTER COLUMN id SET DEFAULT nextval('dataversecontact_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataversefacet ALTER COLUMN id SET DEFAULT nextval('dataversefacet_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataversefeatureddataverse ALTER COLUMN id SET DEFAULT nextval('dataversefeatureddataverse_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataversefieldtypeinputlevel ALTER COLUMN id SET DEFAULT nextval('dataversefieldtypeinputlevel_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataverselinkingdataverse ALTER COLUMN id SET DEFAULT nextval('dataverselinkingdataverse_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataverserole ALTER COLUMN id SET DEFAULT nextval('dataverserole_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataversetheme ALTER COLUMN id SET DEFAULT nextval('dataversetheme_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY defaultvalueset ALTER COLUMN id SET DEFAULT nextval('defaultvalueset_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dvobject ALTER COLUMN id SET DEFAULT nextval('dvobject_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY explicitgroup ALTER COLUMN id SET DEFAULT nextval('explicitgroup_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY filemetadata ALTER COLUMN id SET DEFAULT nextval('filemetadata_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY foreignmetadatafieldmapping ALTER COLUMN id SET DEFAULT nextval('foreignmetadatafieldmapping_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY foreignmetadataformatmapping ALTER COLUMN id SET DEFAULT nextval('foreignmetadataformatmapping_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY guestbook ALTER COLUMN id SET DEFAULT nextval('guestbook_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY guestbookresponse ALTER COLUMN id SET DEFAULT nextval('guestbookresponse_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY ingestreport ALTER COLUMN id SET DEFAULT nextval('ingestreport_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY ingestrequest ALTER COLUMN id SET DEFAULT nextval('ingestrequest_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY maplayermetadata ALTER COLUMN id SET DEFAULT nextval('maplayermetadata_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY metadatablock ALTER COLUMN id SET DEFAULT nextval('metadatablock_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY passwordresetdata ALTER COLUMN id SET DEFAULT nextval('passwordresetdata_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY roleassignment ALTER COLUMN id SET DEFAULT nextval('roleassignment_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY savedsearch ALTER COLUMN id SET DEFAULT nextval('savedsearch_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY savedsearchfilterquery ALTER COLUMN id SET DEFAULT nextval('savedsearchfilterquery_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY shibgroup ALTER COLUMN id SET DEFAULT nextval('shibgroup_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY summarystatistic ALTER COLUMN id SET DEFAULT nextval('summarystatistic_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY template ALTER COLUMN id SET DEFAULT nextval('template_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY usernotification ALTER COLUMN id SET DEFAULT nextval('usernotification_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY variablecategory ALTER COLUMN id SET DEFAULT nextval('variablecategory_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY variablerange ALTER COLUMN id SET DEFAULT nextval('variablerange_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY variablerangeitem ALTER COLUMN id SET DEFAULT nextval('variablerangeitem_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY worldmapauth_token ALTER COLUMN id SET DEFAULT nextval('worldmapauth_token_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY worldmapauth_tokentype ALTER COLUMN id SET DEFAULT nextval('worldmapauth_tokentype_id_seq'::regclass);


--
-- Data for Name: actionlogrecord; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY actionlogrecord (id, actionresult, actionsubtype, actiontype, endtime, info, starttime, useridentifier) FROM stdin;
111734e5-cc21-4ef1-917c-d5100e596be5	OK	loadDatasetFields	Admin	2015-06-08 13:08:17.955	rep4508757747349037455tmp	2015-06-08 13:08:15.768	\N
7e484d19-611e-47c6-b0d2-f9f50b63f2f3	OK	loadDatasetFields	Admin	2015-06-08 13:08:19.44	rep937678722988769217tmp	2015-06-08 13:08:17.985	\N
d6dc80fd-2d43-416e-9df8-2d7b3d552c73	OK	loadDatasetFields	Admin	2015-06-08 13:08:19.58	rep3716520730701613426tmp	2015-06-08 13:08:19.465	\N
64431d29-3993-4750-aaae-349df637f7a4	OK	loadDatasetFields	Admin	2015-06-08 13:08:19.825	rep6974913189748432210tmp	2015-06-08 13:08:19.601	\N
ec39e535-02db-4ea3-b92c-24232dc58ce2	OK	loadDatasetFields	Admin	2015-06-08 13:08:21.104	rep851714502082007892tmp	2015-06-08 13:08:19.863	\N
fbea7dcb-4903-4066-8ac9-df6a2679a9ae	OK	loadDatasetFields	Admin	2015-06-08 13:08:21.268	rep342120996714352751tmp	2015-06-08 13:08:21.127	\N
c5dc0649-80a3-4fe0-953d-8d919558ddbf	OK	createBuiltInRole	Admin	2015-06-08 13:08:21.571	admin:A person who has all permissions for dataverses, datasets, and files.	2015-06-08 13:08:21.557	\N
3f8be9a1-9a63-4205-b083-e9037cd2313d	OK	createBuiltInRole	Admin	2015-06-08 13:08:21.602	fileDownloader:A person who can download a file.	2015-06-08 13:08:21.599	\N
1578195e-87b3-4482-a3ee-3496d92ef66a	OK	createBuiltInRole	Admin	2015-06-08 13:08:21.628	fullContributor:A person who can add subdataverses and datasets within a dataverse.	2015-06-08 13:08:21.625	\N
d9e83295-2c89-44cd-afbe-2f555e48e00e	OK	createBuiltInRole	Admin	2015-06-08 13:08:21.652	dvContributor:A person who can add subdataverses within a dataverse.	2015-06-08 13:08:21.65	\N
59661f33-746a-4d69-a412-92c9c4b1d66e	OK	createBuiltInRole	Admin	2015-06-08 13:08:21.675	dsContributor:A person who can add datasets within a dataverse.	2015-06-08 13:08:21.672	\N
c027269c-e06b-4685-97ce-a16ea73e7307	OK	createBuiltInRole	Admin	2015-06-08 13:08:21.7	editor:For datasets, a person who can edit License + Terms, and then submit them for review.	2015-06-08 13:08:21.698	\N
c6989a37-1b0f-4d10-aad6-6cdc3369d72b	OK	createBuiltInRole	Admin	2015-06-08 13:08:21.754	curator:For datasets, a person who can edit License + Terms, edit Permissions, and publish datasets.	2015-06-08 13:08:21.752	\N
ba926273-10b2-4c19-a945-f48e50d9a6f8	OK	createBuiltInRole	Admin	2015-06-08 13:08:21.778	member:A person who can view both unpublished dataverses and datasets.	2015-06-08 13:08:21.776	\N
d842e3ac-3982-4c20-ba5a-64486e08c0c1	OK	deregisterProvider	Auth	2015-06-08 13:08:21.823	builtin	2015-06-08 13:08:21.823	\N
b1cdc146-466e-4a12-bb00-2d46e318f0c2	OK	registerProvider	Auth	2015-06-08 13:08:21.827	builtin:Build-in Provider	2015-06-08 13:08:21.826	\N
a7a100fa-de33-4fb9-9892-f7452a8aaa5c	OK	deregisterProvider	Auth	2015-06-08 13:08:21.856	echo-simple	2015-06-08 13:08:21.856	\N
63ce8b53-9abe-4cde-b7bf-c2b3afc178c8	OK	registerProvider	Auth	2015-06-08 13:08:21.858	echo-simple:Echo provider	2015-06-08 13:08:21.858	\N
949b0e25-b0e6-40a5-905d-3a693d209f82	OK	deregisterProvider	Auth	2015-06-08 13:08:21.879	echo-dignified	2015-06-08 13:08:21.879	\N
0d8067b9-2bc0-4ec1-be8b-f00a2ec6dac8	OK	registerProvider	Auth	2015-06-08 13:08:21.881	echo-dignified:Dignified Echo provider	2015-06-08 13:08:21.881	\N
0b4e73b1-f5a1-4dcd-9b4a-00ada47cdc62	OK	set	Setting	2015-06-08 13:08:21.908	:AllowSignUp: yes	2015-06-08 13:08:21.908	\N
036e7053-7ca0-4500-9e74-0b2754cb7f4f	OK	set	Setting	2015-06-08 13:08:21.932	:SignUpUrl: /dataverseuser.xhtml?editMode=CREATE	2015-06-08 13:08:21.932	\N
206f3de4-c5be-4912-a17d-38647b22ccfd	OK	set	Setting	2015-06-08 13:08:21.953	:Protocol: doi	2015-06-08 13:08:21.953	\N
9280cf0a-fe45-4a99-8fc5-91c26ce88fac	OK	set	Setting	2015-06-08 13:08:21.977	:Authority: 10.5072/FK2	2015-06-08 13:08:21.977	\N
41ecc86a-d851-4738-8347-ebb3fd06da30	OK	set	Setting	2015-06-08 13:08:22.002	:DoiProvider: EZID	2015-06-08 13:08:22.001	\N
96eca709-071d-4ce8-8af4-43fca2b01595	OK	set	Setting	2015-06-08 13:08:22.023	:DoiSeparator: /	2015-06-08 13:08:22.023	\N
4a5b8a1a-af57-49f5-8c52-8e9331f85723	OK	set	Setting	2015-06-08 13:08:22.043	BuiltinUsers.KEY: burrito	2015-06-08 13:08:22.043	\N
8651ac19-16a7-4cf1-88d2-54d949d52b0b	OK	set	Setting	2015-06-08 13:08:22.064	:BlockedApiKey: empanada	2015-06-08 13:08:22.064	\N
68db6078-d857-4e3b-93a4-67286e18bcdc	OK	set	Setting	2015-06-08 13:08:22.083	:BlockedApiPolicy: localhost-only	2015-06-08 13:08:22.083	\N
908e955e-1b95-4811-b1a1-b5388382f192	OK	createUser	Auth	2015-06-08 13:08:22.253	@dataverseAdmin	2015-06-08 13:08:22.253	\N
7e06b039-4e9b-45ef-b332-6dbb63755761	OK	create	BuiltinUser	2015-06-08 13:08:22.276	builtinUser:dataverseAdmin authenticatedUser:@dataverseAdmin	2015-06-08 13:08:22.116	\N
dbe67569-f670-492f-a894-60a6c580ce6b	OK	toggleSuperuser	Admin	2015-06-08 13:08:22.302	dataverseAdmin	2015-06-08 13:08:22.296	\N
00a8631d-83fe-4e72-a0b3-642e7aa2a94a	OK	edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand	Command	2015-06-08 13:08:22.462	:<null> 	2015-06-08 13:08:22.367	@dataverseAdmin
3b5e97dd-502e-48ce-a5ff-0a424e9b5ae2	OK	edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseMetadataBlocksCommand.SetRoot	Command	2015-06-08 13:08:22.589	:[1 Root] 	2015-06-08 13:08:22.578	@dataverseAdmin
58a1dc66-778c-4522-ad5c-de1fa7c477cd	OK	edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseMetadataBlocksCommand.SetBlocks	Command	2015-06-08 13:08:22.591	:[1 Root] 	2015-06-08 13:08:22.516	@dataverseAdmin
0c31f247-fdfe-4a5e-a3e5-791ce25f8c2e	OK	edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand	Command	2015-06-08 13:08:22.666	:[1 Root] 	2015-06-08 13:08:22.629	@dataverseAdmin
14acfd86-aade-4ea5-aed9-a5d7f5d0ff4d	OK	updateUser	Auth	2015-06-08 13:21:29.017	@dataverseAdmin	2015-06-08 13:21:29.017	\N
4a9910ce-ab98-4918-9baa-96fe423e4195	OK	login	SessionManagement	2015-06-08 13:21:29.023	\N	2015-06-08 13:21:29.023	@dataverseAdmin
c5180d89-c5b9-47c9-a961-e3e4a3879d56	OK	edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand	Command	2015-06-08 13:29:07.634	:[1 Root] 	2015-06-08 13:29:07.303	@dataverseAdmin
b5706636-4797-4202-9cd0-ff2a8a079958	OK	edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand	Command	2015-06-08 13:29:18.388	:[1 Root] 	2015-06-08 13:29:18.363	@dataverseAdmin
49d75936-04bb-4237-823a-7535cdd76ec5	OK	edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand	Command	2015-06-08 13:30:10.011	:[2 testDV] 	2015-06-08 13:30:08.565	@dataverseAdmin
d1310999-acb9-4b09-831c-2b8ad4f2b00e	OK	registerProvider	Auth	2015-06-08 14:27:00.22	builtin:Build-in Provider	2015-06-08 14:27:00.214	\N
989f37fb-48b7-4fb5-ae0f-9302cd5e87d0	OK	registerProvider	Auth	2015-06-08 14:27:00.231	echo-simple:Echo provider	2015-06-08 14:27:00.231	\N
03541856-1c8e-4267-9461-ce1328fc29d4	OK	registerProvider	Auth	2015-06-08 14:27:00.233	echo-dignified:Dignified Echo provider	2015-06-08 14:27:00.233	\N
3d683e6c-2a75-441d-893a-cb302725ad7f	OK	updateUser	Auth	2015-06-08 14:27:07.812	@dataverseAdmin	2015-06-08 14:27:07.811	\N
30710fd9-5947-46b4-8829-5a8eccf9c58d	OK	login	SessionManagement	2015-06-08 14:27:07.824	\N	2015-06-08 14:27:07.823	@dataverseAdmin
e2a0c5d5-d91a-460b-88ed-89ceb6339c6a	OK	edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand	Command	2015-06-08 15:05:01.065	:[3 Sample Dataset] 	2015-06-08 15:05:00.469	@dataverseAdmin
42d5c863-48ca-4bee-b0ba-ad9f00a3487f	OK	registerProvider	Auth	2015-06-08 15:40:06.501	builtin:Build-in Provider	2015-06-08 15:40:06.5	\N
628a317e-2b61-406e-89ce-6c05452f2007	OK	registerProvider	Auth	2015-06-08 15:40:06.506	echo-simple:Echo provider	2015-06-08 15:40:06.506	\N
7025d857-4e0d-43e3-9c84-8e3112279a88	OK	registerProvider	Auth	2015-06-08 15:40:06.508	echo-dignified:Dignified Echo provider	2015-06-08 15:40:06.508	\N
3a7f405a-7223-48cb-9059-4aa757089367	OK	updateUser	Auth	2015-06-08 15:40:09.28	@dataverseAdmin	2015-06-08 15:40:09.279	\N
48c9ad6d-1ab2-4886-8d59-fc0a909edde8	OK	login	SessionManagement	2015-06-08 15:40:09.285	\N	2015-06-08 15:40:09.285	@dataverseAdmin
5b0570c8-a702-46a5-a346-50bf76ead788	OK	edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand	Command	2015-06-08 15:40:14.328	:[2 testDV] 	2015-06-08 15:40:14.147	@dataverseAdmin
b7f4217c-8c53-486f-b3d4-7e42536be1c6	OK	edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand	Command	2015-06-08 15:40:17.632	:[3 Sample Dataset] 	2015-06-08 15:40:14.334	@dataverseAdmin
\.


--
-- Data for Name: apitoken; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY apitoken (id, createtime, disabled, expiretime, tokenstring, authenticateduser_id) FROM stdin;
1	2015-06-08 13:08:22.264	f	2016-06-08 13:08:22.264	a65048f8-875c-4479-a91d-33cb8cd12821	1
\.


--
-- Data for Name: authenticateduser; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY authenticateduser (id, affiliation, email, firstname, lastname, modificationtime, name, "position", superuser, useridentifier) FROM stdin;
1	Dataverse.org	dataverse@mailinator.com	Dataverse	Admin	2015-06-08 15:40:09.283	\N	Admin	t	dataverseAdmin
\.


--
-- Data for Name: authenticateduserlookup; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY authenticateduserlookup (id, authenticationproviderid, persistentuserid, authenticateduser_id) FROM stdin;
1	builtin	dataverseAdmin	1
\.


--
-- Data for Name: authenticationproviderrow; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY authenticationproviderrow (id, enabled, factoryalias, factorydata, subtitle, title) FROM stdin;
builtin	t	BuiltinAuthenticationProvider		Datavers' Internal Authentication provider	Dataverse Local
echo-simple	t	Echo	,	Approves everyone, based on their credentials	Echo provider
echo-dignified	t	Echo	Sir,Esq.	Approves everyone, based on their credentials, and adds some flair	Dignified Echo provider
\.


--
-- Data for Name: builtinuser; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY builtinuser (id, affiliation, email, encryptedpassword, firstname, lastname, passwordencryptionversion, "position", username) FROM stdin;
1	Dataverse.org	dataverse@mailinator.com	$2a$10$NGp3jxhSh4IBfiGIb5CPsOUovwfZ2xT7sklweW.LInjKtAZcbWokO	Dataverse	Admin	1	Admin	dataverseAdmin
\.


--
-- Data for Name: controlledvocabalternate; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY controlledvocabalternate (id, strvalue, controlledvocabularyvalue_id, datasetfieldtype_id) FROM stdin;
1	arxiv	17	30
2	BOTSWANA	266	79
3	Brasil	268	79
4	Gambia, The	317	79
5	Germany (Federal Republic of)	319	79
6	GHANA	320	79
7	INDIA	339	79
8	Sumatra	340	79
9	Iran	341	79
10	Iran (Islamic Republic of)	341	79
11	IRAQ	342	79
12	Laos	358	79
13	LESOTHO	361	79
14	MOZAMBIQUE	388	79
15	NAMIBIA	390	79
16	SWAZILAND	450	79
17	Taiwan	454	79
18	Tanzania	456	79
19	UAE	470	79
20	USA	472	79
21	U.S.A	472	79
22	United States of America	472	79
23	U.S.A.	472	79
24	YEMEN	483	79
\.


--
-- Data for Name: controlledvocabularyvalue; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY controlledvocabularyvalue (id, displayorder, identifier, strvalue, datasetfieldtype_id) FROM stdin;
1	0	\N	N/A	\N
2	0	D01	Agricultural Sciences	19
3	1	D0	Arts and Humanities	19
4	2	D1	Astronomy and Astrophysics	19
5	3	D2	Business and Management	19
6	4	D3	Chemistry	19
7	5	D4	Earth and Environmental Sciences	19
8	6	D5	Engineering	19
9	7	D6	Medicine, Health and Life Sciences	19
10	8	D7	Computer and Information Science	19
11	9	D8	Law	19
12	10	D9	Mathematical Sciences	19
13	11	D10	Physics	19
14	12	D11	Social Sciences	19
15	13	D12	Other	19
16	0		ark	30
17	1		arXiv	30
18	2		bibcode	30
19	3		doi	30
20	4		ean13	30
21	5		eissn	30
22	6		handle	30
23	7		isbn	30
24	8		issn	30
25	9		istc	30
26	10		lissn	30
27	11		lsid	30
28	12		pmid	30
29	13		purl	30
30	14		upc	30
31	15		url	30
32	16		urn	30
33	0		Data Collector	44
34	1		Data Curator	44
35	2		Data Manager	44
36	3		Editor	44
37	4		Funder	44
38	5		Hosting Institution	44
39	6		Project Leader	44
40	7		Project Manager	44
41	8		Project Member	44
42	9		Related Person	44
43	10		Researcher	44
44	11		Research Group	44
45	12		Rights Holder	44
46	13		Sponsor	44
47	14		Supervisor	44
48	15		Work Package Leader	44
49	16		Other	44
50	0		ORCID	10
51	1		ISNI	10
52	2		LCNA	10
53	0		Abkhaz	34
54	1		Afar	34
55	2		Afrikaans	34
56	3		Akan	34
57	4		Albanian	34
58	5		Amharic	34
59	6		Arabic	34
60	7		Aragonese	34
61	8		Armenian	34
62	9		Assamese	34
63	10		Avaric	34
64	11		Avestan	34
65	12		Aymara	34
66	13		Azerbaijani	34
67	14		Bambara	34
68	15		Bashkir	34
69	16		Basque	34
70	17		Belarusian	34
71	18		Bengali, Bangla	34
72	19		Bihari	34
73	20		Bislama	34
74	21		Bosnian	34
75	22		Breton	34
76	23		Bulgarian	34
77	24		Burmese	34
78	25		Catalan,Valencian	34
79	26		Chamorro	34
80	27		Chechen	34
81	28		Chichewa, Chewa, Nyanja	34
82	29		Chinese	34
83	30		Chuvash	34
84	31		Cornish	34
85	32		Corsican	34
86	33		Cree	34
87	34		Croatian	34
88	35		Czech	34
89	36		Danish	34
90	37		Divehi, Dhivehi, Maldivian	34
91	38		Dutch	34
92	39		Dzongkha	34
93	40		English	34
94	41		Esperanto	34
95	42		Estonian	34
96	43		Ewe	34
97	44		Faroese	34
98	45		Fijian	34
99	46		Finnish	34
100	47		French	34
101	48		Fula, Fulah, Pulaar, Pular	34
102	49		Galician	34
103	50		Georgian	34
104	51		German	34
105	52		Greek (modern)	34
106	53		Guaraní	34
107	54		Gujarati	34
108	55		Haitian, Haitian Creole	34
109	56		Hausa	34
110	57		Hebrew (modern)	34
111	58		Herero	34
112	59		Hindi	34
113	60		Hiri Motu	34
114	61		Hungarian	34
115	62		Interlingua	34
116	63		Indonesian	34
117	64		Interlingue	34
118	65		Irish	34
119	66		Igbo	34
120	67		Inupiaq	34
121	68		Ido	34
122	69		Icelandic	34
123	70		Italian	34
124	71		Inuktitut	34
125	72		Japanese	34
126	73		Javanese	34
127	74		Kalaallisut, Greenlandic	34
128	75		Kannada	34
129	76		Kanuri	34
130	77		Kashmiri	34
131	78		Kazakh	34
132	79		Khmer	34
133	80		Kikuyu, Gikuyu	34
134	81		Kinyarwanda	34
135	82		Kyrgyz	34
136	83		Komi	34
137	84		Kongo	34
138	85		Korean	34
139	86		Kurdish	34
140	87		Kwanyama, Kuanyama	34
141	88		Latin	34
142	89		Luxembourgish, Letzeburgesch	34
143	90		Ganda	34
144	91		Limburgish, Limburgan, Limburger	34
145	92		Lingala	34
146	93		Lao	34
147	94		Lithuanian	34
148	95		Luba-Katanga	34
149	96		Latvian	34
150	97		Manx	34
151	98		Macedonian	34
152	99		Malagasy	34
153	100		Malay	34
154	101		Malayalam	34
155	102		Maltese	34
156	103		Māori	34
157	104		Marathi (Marāṭhī)	34
158	105		Marshallese	34
159	106		Mongolian	34
160	107		Nauru	34
161	108		Navajo, Navaho	34
162	109		Northern Ndebele	34
163	110		Nepali	34
164	111		Ndonga	34
165	112		Norwegian Bokmål	34
166	113		Norwegian Nynorsk	34
167	114		Norwegian	34
168	115		Nuosu	34
169	116		Southern Ndebele	34
170	117		Occitan	34
171	118		Ojibwe, Ojibwa	34
172	119		Old Church Slavonic,Church Slavonic,Old Bulgarian	34
173	120		Oromo	34
174	121		Oriya	34
175	122		Ossetian, Ossetic	34
176	123		Panjabi, Punjabi	34
177	124		Pāli	34
178	125		Persian (Farsi)	34
179	126		Polish	34
180	127		Pashto, Pushto	34
181	128		Portuguese	34
182	129		Quechua	34
183	130		Romansh	34
184	131		Kirundi	34
185	132		Romanian	34
186	133		Russian	34
187	134		Sanskrit (Saṁskṛta)	34
188	135		Sardinian	34
189	136		Sindhi	34
190	137		Northern Sami	34
191	138		Samoan	34
192	139		Sango	34
193	140		Serbian	34
194	141		Scottish Gaelic, Gaelic	34
195	142		Shona	34
196	143		Sinhala, Sinhalese	34
197	144		Slovak	34
198	145		Slovene	34
199	146		Somali	34
200	147		Southern Sotho	34
201	148		Spanish, Castilian	34
202	149		Sundanese	34
203	150		Swahili	34
204	151		Swati	34
205	152		Swedish	34
206	153		Tamil	34
207	154		Telugu	34
208	155		Tajik	34
209	156		Thai	34
210	157		Tigrinya	34
211	158		Tibetan Standard, Tibetan, Central	34
212	159		Turkmen	34
213	160		Tagalog	34
214	161		Tswana	34
215	162		Tonga (Tonga Islands)	34
216	163		Turkish	34
217	164		Tsonga	34
218	165		Tatar	34
219	166		Twi	34
220	167		Tahitian	34
221	168		Uyghur, Uighur	34
222	169		Ukrainian	34
223	170		Urdu	34
224	171		Uzbek	34
225	172		Venda	34
226	173		Vietnamese	34
227	174		Volapük	34
228	175		Walloon	34
229	176		Welsh	34
230	177		Wolof	34
231	178		Western Frisian	34
232	179		Xhosa	34
233	180		Yiddish	34
234	181		Yoruba	34
235	182		Zhuang, Chuang	34
236	183		Zulu	34
237	184		Not applicable	34
238	0		Afghanistan	79
239	1		Albania	79
240	2		Algeria	79
241	3		American Samoa	79
242	4		Andorra	79
243	5		Angola	79
244	6		Anguilla	79
245	7		Antarctica	79
246	8		Antigua and Barbuda	79
247	9		Argentina	79
248	10		Armenia	79
249	11		Aruba	79
250	12		Australia	79
251	13		Austria	79
252	14		Azerbaijan	79
253	15		Bahamas	79
254	16		Bahrain	79
255	17		Bangladesh	79
256	18		Barbados	79
257	19		Belarus	79
258	20		Belgium	79
259	21		Belize	79
260	22		Benin	79
261	23		Bermuda	79
262	24		Bhutan	79
263	25		Bolivia, Plurinational State of	79
264	26		Bonaire, Sint Eustatius and Saba	79
265	27		Bosnia and Herzegovina	79
266	28		Botswana	79
267	29		Bouvet Island	79
268	30		Brazil	79
269	31		British Indian Ocean Territory	79
270	32		Brunei Darussalam	79
271	33		Bulgaria	79
272	34		Burkina Faso	79
273	35		Burundi	79
274	36		Cambodia	79
275	37		Cameroon	79
276	38		Canada	79
277	39		Cape Verde	79
278	40		Cayman Islands	79
279	41		Central African Republic	79
280	42		Chad	79
281	43		Chile	79
282	44		China	79
283	45		Christmas Island	79
284	46		Cocos (Keeling) Islands	79
285	47		Colombia	79
286	48		Comoros	79
287	49		Congo	79
288	50		Congo, the Democratic Republic of the	79
289	51		Cook Islands	79
290	52		Costa Rica	79
291	53		Croatia	79
292	54		Cuba	79
293	55		Curaçao	79
294	56		Cyprus	79
295	57		Czech Republic	79
296	58		Côte d'Ivoire	79
297	59		Denmark	79
298	60		Djibouti	79
299	61		Dominica	79
300	62		Dominican Republic	79
301	63		Ecuador	79
302	64		Egypt	79
303	65		El Salvador	79
304	66		Equatorial Guinea	79
305	67		Eritrea	79
306	68		Estonia	79
307	69		Ethiopia	79
308	70		Falkland Islands (Malvinas)	79
309	71		Faroe Islands	79
310	72		Fiji	79
311	73		Finland	79
312	74		France	79
313	75		French Guiana	79
314	76		French Polynesia	79
315	77		French Southern Territories	79
316	78		Gabon	79
317	79		Gambia	79
318	80		Georgia	79
319	81		Germany	79
320	82		Ghana	79
321	83		Gibraltar	79
322	84		Greece	79
323	85		Greenland	79
324	86		Grenada	79
325	87		Guadeloupe	79
326	88		Guam	79
327	89		Guatemala	79
328	90		Guernsey	79
329	91		Guinea	79
330	92		Guinea-Bissau	79
331	93		Guyana	79
332	94		Haiti	79
333	95		Heard Island and Mcdonald Islands	79
334	96		Holy See (Vatican City State)	79
335	97		Honduras	79
336	98		Hong Kong	79
337	99		Hungary	79
338	100		Iceland	79
339	101		India	79
340	102		Indonesia	79
341	103		Iran, Islamic Republic of	79
342	104		Iraq	79
343	105		Ireland	79
344	106		Isle of Man	79
345	107		Israel	79
346	108		Italy	79
347	109		Jamaica	79
348	110		Japan	79
349	111		Jersey	79
350	112		Jordan	79
351	113		Kazakhstan	79
352	114		Kenya	79
353	115		Kiribati	79
354	116		Korea, Democratic People's Republic of	79
355	117		Korea, Republic of	79
356	118		Kuwait	79
357	119		Kyrgyzstan	79
358	120		Lao People's Democratic Republic	79
359	121		Latvia	79
360	122		Lebanon	79
361	123		Lesotho	79
362	124		Liberia	79
363	125		Libya	79
364	126		Liechtenstein	79
365	127		Lithuania	79
366	128		Luxembourg	79
367	129		Macao	79
368	130		Macedonia, the Former Yugoslav Republic of	79
369	131		Madagascar	79
370	132		Malawi	79
371	133		Malaysia	79
372	134		Maldives	79
373	135		Mali	79
374	136		Malta	79
375	137		Marshall Islands	79
376	138		Martinique	79
377	139		Mauritania	79
378	140		Mauritius	79
379	141		Mayotte	79
380	142		Mexico	79
381	143		Micronesia, Federated States of	79
382	144		Moldova, Republic of	79
383	145		Monaco	79
384	146		Mongolia	79
385	147		Montenegro	79
386	148		Montserrat	79
387	149		Morocco	79
388	150		Mozambique	79
389	151		Myanmar	79
390	152		Namibia	79
391	153		Nauru	79
392	154		Nepal	79
393	155		Netherlands	79
394	156		New Caledonia	79
395	157		New Zealand	79
396	158		Nicaragua	79
397	159		Niger	79
398	160		Nigeria	79
399	161		Niue	79
400	162		Norfolk Island	79
401	163		Northern Mariana Islands	79
402	164		Norway	79
403	165		Oman	79
404	166		Pakistan	79
405	167		Palau	79
406	168		Palestine, State of	79
407	169		Panama	79
408	170		Papua New Guinea	79
409	171		Paraguay	79
410	172		Peru	79
411	173		Philippines	79
412	174		Pitcairn	79
413	175		Poland	79
414	176		Portugal	79
415	177		Puerto Rico	79
416	178		Qatar	79
417	179		Romania	79
418	180		Russian Federation	79
419	181		Rwanda	79
420	182		Réunion	79
421	183		Saint Barthélemy	79
422	184		Saint Helena, Ascension and Tristan da Cunha	79
423	185		Saint Kitts and Nevis	79
424	186		Saint Lucia	79
425	187		Saint Martin (French part)	79
426	188		Saint Pierre and Miquelon	79
427	189		Saint Vincent and the Grenadines	79
428	190		Samoa	79
429	191		San Marino	79
430	192		Sao Tome and Principe	79
431	193		Saudi Arabia	79
432	194		Senegal	79
433	195		Serbia	79
434	196		Seychelles	79
435	197		Sierra Leone	79
436	198		Singapore	79
437	199		Sint Maarten (Dutch part)	79
438	200		Slovakia	79
439	201		Slovenia	79
440	202		Solomon Islands	79
441	203		Somalia	79
442	204		South Africa	79
443	205		South Georgia and the South Sandwich Islands	79
444	206		South Sudan	79
445	207		Spain	79
446	208		Sri Lanka	79
447	209		Sudan	79
448	210		Suriname	79
449	211		Svalbard and Jan Mayen	79
450	212		Swaziland	79
451	213		Sweden	79
452	214		Switzerland	79
453	215		Syrian Arab Republic	79
454	216		Taiwan, Province of China	79
455	217		Tajikistan	79
456	218		Tanzania, United Republic of	79
457	219		Thailand	79
458	220		Timor-Leste	79
459	221		Togo	79
460	222		Tokelau	79
461	223		Tonga	79
462	224		Trinidad and Tobago	79
463	225		Tunisia	79
464	226		Turkey	79
465	227		Turkmenistan	79
466	228		Turks and Caicos Islands	79
467	229		Tuvalu	79
468	230		Uganda	79
469	231		Ukraine	79
470	232		United Arab Emirates	79
471	233		United Kingdom	79
472	234		United States	79
473	235		United States Minor Outlying Islands	79
474	236		Uruguay	79
475	237		Uzbekistan	79
476	238		Vanuatu	79
477	239		Venezuela, Bolivarian Republic of	79
478	240		Viet Nam	79
479	241		Virgin Islands, British	79
480	242		Virgin Islands, U.S.	79
481	243		Wallis and Futuna	79
482	244		Western Sahara	79
483	245		Yemen	79
484	246		Zambia	79
485	247		Zimbabwe	79
486	248		Åland Islands	79
487	0		Image	115
488	1		Mosaic	115
489	2		EventList	115
490	3		Spectrum	115
491	4		Cube	115
492	5		Table	115
493	6		Catalog	115
494	7		LightCurve	115
495	8		Simulation	115
496	9		Figure	115
497	10		Artwork	115
498	11		Animation	115
499	12		PrettyPicture	115
500	13		Documentation	115
501	14		Other	115
502	15		Library	115
503	16		Press Release	115
504	17		Facsimile	115
505	18		Historical	115
506	19		Observation	115
507	20		Object	115
508	21		Value	115
509	22		ValuePair	115
510	23		Survey	115
511	0	EFO_0001427	Case Control	141
512	1	EFO_0001428	Cross Sectional	141
513	2	OCRE100078	Cohort Study	141
514	3	NCI_C48202	Nested Case Control Design	141
515	4	OTHER_DESIGN	Not Specified	141
516	5	OBI_0500006	Parallel Group Design	141
517	6	OBI_0001033	Perturbation Design	141
518	7	MESH_D016449	Randomized Controlled Trial	141
519	8	TECH_DESIGN	Technological Design	141
520	0	EFO_0000246	Age	142
521	1	BIOMARKERS	Biomarkers	142
522	2	CELL_SURFACE_M	Cell Surface Markers	142
523	3	EFO_0000324;EFO_0000322	Cell Type/Cell Line	142
524	4	EFO_0000399	Developmental Stage	142
525	5	OBI_0001293	Disease State	142
526	6	IDO_0000469	Drug Susceptibility	142
527	7	FBcv_0010001	Extract Molecule	142
528	8	OBI_0001404	Genetic Characteristics	142
529	9	OBI_0000690	Immunoprecipitation Antibody	142
530	10	OBI_0100026	Organism	142
531	11	OTHER_FACTOR	Other	142
532	12	PASSAGES_FACTOR	Passages	142
533	13	OBI_0000050	Platform	142
534	14	EFO_0000695	Sex	142
535	15	EFO_0005135	Strain	142
536	16	EFO_0000724	Time Point	142
537	17	BTO_0001384	Tissue Type	142
538	18	EFO_0000369	Treatment Compound	142
539	19	EFO_0000727	Treatment Type	142
540	0	ERO_0001899	cell counting	145
541	1	CHMO_0001085	cell sorting	145
542	2	OBI_0000520	clinical chemistry analysis	145
543	3	OBI_0000537	copy number variation profiling	145
544	4	OBI_0000634	DNA methylation profiling	145
545	5	OBI_0000748	DNA methylation profiling (Bisulfite-Seq)	145
546	6	_OBI_0000634	DNA methylation profiling (MeDIP-Seq)	145
547	7	_IDO_0000469	drug susceptibility	145
548	8	ENV_GENE_SURVEY	environmental gene survey	145
549	9	ERO_0001183	genome sequencing	145
550	10	OBI_0000630	hematology	145
551	11	OBI_0600020	histology	145
552	12	OBI_0002017	Histone Modification (ChIP-Seq)	145
553	13	SO_0001786	loss of heterozygosity profiling	145
554	14	OBI_0000366	metabolite profiling	145
555	15	METAGENOME_SEQ	metagenome sequencing	145
556	16	OBI_0000615	protein expression profiling	145
557	17	ERO_0000346	protein identification	145
558	18	PROTEIN_DNA_BINDING	protein-DNA binding site identification	145
559	19	OBI_0000288	protein-protein interaction detection	145
560	20	PROTEIN_RNA_BINDING	protein-RNA binding (RIP-Seq)	145
561	21	OBI_0000435	SNP analysis	145
562	22	TARGETED_SEQ	targeted sequencing	145
563	23	OBI_0002018	transcription factor binding (ChIP-Seq)	145
564	24	OBI_0000291	transcription factor binding site identification	145
565	25	OBI_0000424	transcription profiling	145
566	26	EFO_0001032	transcription profiling	145
567	27	TRANSCRIPTION_PROF	transcription profiling (Microarray)	145
568	28	OBI_0001271	transcription profiling (RNA-Seq)	145
569	29	TRAP_TRANS_PROF	TRAP translational profiling	145
570	30	OTHER_MEASUREMENT	Other	145
571	0	NCBITaxon_3702	Arabidopsis thaliana	143
572	1	NCBITaxon_9913	Bos taurus	143
573	2	NCBITaxon_6239	Caenorhabditis elegans	143
574	3	NCBITaxon_3055	Chlamydomonas reinhardtii	143
575	4	NCBITaxon_7955	Danio rerio (zebrafish)	143
576	5	NCBITaxon_44689	Dictyostelium discoideum	143
577	6	NCBITaxon_7227	Drosophila melanogaster	143
578	7	NCBITaxon_562	Escherichia coli	143
579	8	NCBITaxon_11103	Hepatitis C virus	143
580	9	NCBITaxon_9606	Homo sapiens	143
581	10	NCBITaxon_10090	Mus musculus	143
582	11	NCBITaxon_33894	Mycobacterium africanum	143
583	12	NCBITaxon_78331	Mycobacterium canetti	143
584	13	NCBITaxon_1773	Mycobacterium tuberculosis	143
585	14	NCBITaxon_2104	Mycoplasma pneumoniae	143
586	15	NCBITaxon_4530	Oryza sativa	143
587	16	NCBITaxon_5833	Plasmodium falciparum	143
588	17	NCBITaxon_4754	Pneumocystis carinii	143
589	18	NCBITaxon_10116	Rattus norvegicus	143
590	19	NCBITaxon_4932	Saccharomyces cerevisiae (brewer's yeast)	143
591	20	NCBITaxon_4896	Schizosaccharomyces pombe	143
592	21	NCBITaxon_31033	Takifugu rubripes	143
593	22	NCBITaxon_8355	Xenopus laevis	143
594	23	NCBITaxon_4577	Zea mays	143
595	24	OTHER_TAXONOMY	Other	143
596	0	CULTURE_DRUG_TEST_SINGLE	culture based drug susceptibility testing, single concentration	147
597	1	CULTURE_DRUG_TEST_TWO	culture based drug susceptibility testing, two concentrations	147
598	2	CULTURE_DRUG_TEST_THREE	culture based drug susceptibility testing, three or more concentrations (minimium inhibitory concentration measurement)	147
599	3	OBI_0400148	DNA microarray	147
600	4	OBI_0000916	flow cytometry	147
601	5	OBI_0600053	gel electrophoresis	147
602	6	OBI_0000470	mass spectrometry	147
603	7	OBI_0000623	NMR spectroscopy	147
604	8	OBI_0000626	nucleotide sequencing	147
605	9	OBI_0400149	protein microarray	147
606	10	OBI_0000893	real time PCR	147
607	11	NO_TECHNOLOGY	no technology required	147
608	12	OTHER_TECHNOLOGY	Other	147
609	0	210_MS_GC	210-MS GC Ion Trap (Varian)	148
610	1	220_MS_GC	220-MS GC Ion Trap (Varian)	148
611	2	225_MS_GC	225-MS GC Ion Trap (Varian)	148
612	3	240_MS_GC	240-MS GC Ion Trap (Varian)	148
613	4	300_MS_GCMS	300-MS quadrupole GC/MS (Varian)	148
614	5	320_MS_LCMS	320-MS LC/MS (Varian)	148
615	6	325_MS_LCMS	325-MS LC/MS (Varian)	148
616	7	500_MS_GCMS	320-MS GC/MS (Varian)	148
617	8	500_MS_LCMS	500-MS LC/MS (Varian)	148
618	9	800D	800D (Jeol)	148
619	10	910_MS_TQFT	910-MS TQ-FT (Varian)	148
620	11	920_MS_TQFT	920-MS TQ-FT (Varian)	148
621	12	3100_MASS_D	3100 Mass Detector (Waters)	148
622	13	6110_QUAD_LCMS	6110 Quadrupole LC/MS (Agilent)	148
623	14	6120_QUAD_LCMS	6120 Quadrupole LC/MS (Agilent)	148
624	15	6130_QUAD_LCMS	6130 Quadrupole LC/MS (Agilent)	148
625	16	6140_QUAD_LCMS	6140 Quadrupole LC/MS (Agilent)	148
626	17	6310_ION_LCMS	6310 Ion Trap LC/MS (Agilent)	148
627	18	6320_ION_LCMS	6320 Ion Trap LC/MS (Agilent)	148
628	19	6330_ION_LCMS	6330 Ion Trap LC/MS (Agilent)	148
629	20	6340_ION_LCMS	6340 Ion Trap LC/MS (Agilent)	148
630	21	6410_TRIPLE_LCMS	6410 Triple Quadrupole LC/MS (Agilent)	148
631	22	6430_TRIPLE_LCMS	6430 Triple Quadrupole LC/MS (Agilent)	148
632	23	6460_TRIPLE_LCMS	6460 Triple Quadrupole LC/MS (Agilent)	148
633	24	6490_TRIPLE_LCMS	6490 Triple Quadrupole LC/MS (Agilent)	148
634	25	6530_Q_TOF_LCMS	6530 Q-TOF LC/MS (Agilent)	148
635	26	6540_Q_TOF_LCMS	6540 Q-TOF LC/MS (Agilent)	148
636	27	6210_Q_TOF_LCMS	6210 TOF LC/MS (Agilent)	148
637	28	6220_Q_TOF_LCMS	6220 TOF LC/MS (Agilent)	148
638	29	6230_Q_TOF_LCMS	6230 TOF LC/MS (Agilent)	148
639	30	700B_TRIPLE_GCMS	7000B Triple Quadrupole GC/MS (Agilent)	148
640	31	ACCUTO_DART	AccuTO DART (Jeol)	148
641	32	ACCUTOF_GC	AccuTOF GC (Jeol)	148
642	33	ACCUTOF_LC	AccuTOF LC (Jeol)	148
643	34	ACQUITY_SQD	ACQUITY SQD (Waters)	148
644	35	ACQUITY_TQD	ACQUITY TQD (Waters)	148
645	36	AGILENT	Agilent	148
646	37	AGILENT_ 5975E_GCMSD	Agilent 5975E GC/MSD (Agilent)	148
647	38	AGILENT_5975T_LTM_GCMSD	Agilent 5975T LTM GC/MSD (Agilent)	148
648	39	5975C_GCMSD	5975C Series GC/MSD (Agilent)	148
649	40	AFFYMETRIX	Affymetrix	148
650	41	AMAZON_ETD_ESI	amaZon ETD ESI Ion Trap (Bruker)	148
651	42	AMAZON_X_ESI	amaZon X ESI Ion Trap (Bruker)	148
652	43	APEX_ULTRA_QQ_FTMS	apex-ultra hybrid Qq-FTMS (Bruker)	148
653	44	API_2000	API 2000 (AB Sciex)	148
654	45	API_3200	API 3200 (AB Sciex)	148
655	46	API_3200_QTRAP	API 3200 QTRAP (AB Sciex)	148
656	47	API_4000	API 4000 (AB Sciex)	148
657	48	API_4000_QTRAP	API 4000 QTRAP (AB Sciex)	148
658	49	API_5000	API 5000 (AB Sciex)	148
659	50	API_5500	API 5500 (AB Sciex)	148
660	51	API_5500_QTRAP	API 5500 QTRAP (AB Sciex)	148
661	52	APPLIED_BIOSYSTEMS	Applied Biosystems Group (ABI)	148
662	53	AQI_BIOSCIENCES	AQI Biosciences	148
663	54	ATMOS_GC	Atmospheric Pressure GC (Waters)	148
664	55	AUTOFLEX_III_MALDI_TOF_MS	autoflex III MALDI-TOF MS (Bruker)	148
665	56	AUTOFLEX_SPEED	autoflex speed(Bruker)	148
666	57	AUTOSPEC_PREMIER	AutoSpec Premier (Waters)	148
667	58	AXIMA_MEGA_TOF	AXIMA Mega TOF (Shimadzu)	148
668	59	AXIMA_PERF_MALDI_TOF	AXIMA Performance MALDI TOF/TOF (Shimadzu)	148
669	60	A_10_ANALYZER	A-10 Analyzer (Apogee)	148
670	61	A_40_MINIFCM	A-40-MiniFCM (Apogee)	148
671	62	BACTIFLOW	Bactiflow (Chemunex SA)	148
672	63	BASE4INNOVATION	Base4innovation	148
673	64	BD_BACTEC_MGIT_320	BD BACTEC MGIT 320	148
674	65	BD_BACTEC_MGIT_960	BD BACTEC MGIT 960	148
675	66	BD_RADIO_BACTEC_460TB	BD Radiometric BACTEC 460TB	148
676	67	BIONANOMATRIX	BioNanomatrix	148
677	68	CELL_LAB_QUANTA_SC	Cell Lab Quanta SC (Becman Coulter)	148
678	69	CLARUS_560_D_GCMS	Clarus 560 D GC/MS (PerkinElmer)	148
679	70	CLARUS_560_S_GCMS	Clarus 560 S GC/MS (PerkinElmer)	148
680	71	CLARUS_600_GCMS	Clarus 600 GC/MS (PerkinElmer)	148
681	72	COMPLETE_GENOMICS	Complete Genomics	148
682	73	CYAN	Cyan (Dako Cytomation)	148
683	74	CYFLOW_ML	CyFlow ML (Partec)	148
684	75	CYFLOW_SL	Cyow SL (Partec)	148
685	76	CYFLOW_SL3	CyFlow SL3 (Partec)	148
686	77	CYTOBUOY	CytoBuoy (Cyto Buoy Inc)	148
687	78	CYTOSENCE	CytoSence (Cyto Buoy Inc)	148
688	79	CYTOSUB	CytoSub (Cyto Buoy Inc)	148
689	80	DANAHER	Danaher	148
690	81	DFS	DFS (Thermo Scientific)	148
691	82	EXACTIVE	Exactive(Thermo Scientific)	148
692	83	FACS_CANTO	FACS Canto (Becton Dickinson)	148
693	84	FACS_CANTO2	FACS Canto2 (Becton Dickinson)	148
694	85	FACS_SCAN	FACS Scan (Becton Dickinson)	148
695	86	FC_500	FC 500 (Becman Coulter)	148
696	87	GCMATE_II	GCmate II GC/MS (Jeol)	148
697	88	GCMS_QP2010_PLUS	GCMS-QP2010 Plus (Shimadzu)	148
698	89	GCMS_QP2010S_PLUS	GCMS-QP2010S Plus (Shimadzu)	148
699	90	GCT_PREMIER	GCT Premier (Waters)	148
700	91	GENEQ	GENEQ	148
701	92	GENOME_CORP	Genome Corp.	148
702	93	GENOVOXX	GenoVoxx	148
703	94	GNUBIO	GnuBio	148
704	95	GUAVA_EASYCYTE_MINI	Guava EasyCyte Mini (Millipore)	148
705	96	GUAVA_EASYCYTE_PLUS	Guava EasyCyte Plus (Millipore)	148
706	97	GUAVA_PERSONAL_CELL	Guava Personal Cell Analysis (Millipore)	148
707	98	GUAVA_PERSONAL_CELL_96	Guava Personal Cell Analysis-96 (Millipore)	148
708	99	HELICOS_BIO	Helicos BioSciences	148
709	100	ILLUMINA	Illumina	148
710	101	INDIRECT_LJ_MEDIUM	Indirect proportion method on LJ medium	148
711	102	INDIRECT_AGAR_7H9	Indirect proportion method on Middlebrook Agar 7H9	148
712	103	INDIRECT_AGAR_7H10	Indirect proportion method on Middlebrook Agar 7H10	148
713	104	INDIRECT_AGAR_7H11	Indirect proportion method on Middlebrook Agar 7H11	148
714	105	INFLUX_ANALYZER	inFlux Analyzer (Cytopeia)	148
715	106	INTELLIGENT_BIOSYSTEMS	Intelligent Bio-Systems	148
716	107	ITQ_700	ITQ 700 (Thermo Scientific)	148
717	108	ITQ_900	ITQ 900 (Thermo Scientific)	148
718	109	ITQ_1100	ITQ 1100 (Thermo Scientific)	148
719	110	JMS_53000_SPIRAL	JMS-53000 SpiralTOF (Jeol)	148
720	111	LASERGEN	LaserGen	148
721	112	LCMS_2020	LCMS-2020 (Shimadzu)	148
722	113	LCMS_2010EV	LCMS-2010EV (Shimadzu)	148
723	114	LCMS_IT_TOF	LCMS-IT-TOF (Shimadzu)	148
724	115	LI_COR	Li-Cor	148
725	116	LIFE_TECH	Life Tech	148
726	117	LIGHTSPEED_GENOMICS	LightSpeed Genomics	148
727	118	LCT_PREMIER_XE	LCT Premier XE (Waters)	148
728	119	LCQ_DECA_XP_MAX	LCQ Deca XP MAX (Thermo Scientific)	148
729	120	LCQ_FLEET	LCQ Fleet (Thermo Scientific)	148
730	121	LXQ_THERMO	LXQ (Thermo Scientific)	148
731	122	LTQ_CLASSIC	LTQ Classic (Thermo Scientific)	148
732	123	LTQ_XL	LTQ XL (Thermo Scientific)	148
733	124	LTQ_VELOS	LTQ Velos (Thermo Scientific)	148
734	125	LTQ_ORBITRAP_CLASSIC	LTQ Orbitrap Classic (Thermo Scientific)	148
735	126	LTQ_ORBITRAP_XL	LTQ Orbitrap XL (Thermo Scientific)	148
736	127	LTQ_ORBITRAP_DISCOVERY	LTQ Orbitrap Discovery (Thermo Scientific)	148
737	128	LTQ_ORBITRAP_VELOS	LTQ Orbitrap Velos (Thermo Scientific)	148
738	129	LUMINEX_100	Luminex 100 (Luminex)	148
739	130	LUMINEX_200	Luminex 200 (Luminex)	148
740	131	MACS_QUANT	MACS Quant (Miltenyi)	148
741	132	MALDI_SYNAPT_G2_HDMS	MALDI SYNAPT G2 HDMS (Waters)	148
742	133	MALDI_SYNAPT_G2_MS	MALDI SYNAPT G2 MS (Waters)	148
743	134	MALDI_SYNAPT_HDMS	MALDI SYNAPT HDMS (Waters)	148
744	135	MALDI_SYNAPT_MS	MALDI SYNAPT MS (Waters)	148
745	136	MALDI_MICROMX	MALDI micro MX (Waters)	148
746	137	MAXIS	maXis (Bruker)	148
747	138	MAXISG4	maXis G4 (Bruker)	148
748	139	MICROFLEX_LT_MALDI_TOF_MS	microflex LT MALDI-TOF MS (Bruker)	148
749	140	MICROFLEX_LRF_MALDI_TOF_MS	microflex LRF MALDI-TOF MS (Bruker)	148
750	141	MICROFLEX_III_TOF_MS	microflex III MALDI-TOF MS (Bruker)	148
751	142	MICROTOF_II_ESI_TOF	micrOTOF II ESI TOF (Bruker)	148
752	143	MICROTOF_Q_II_ESI_QQ_TOF	micrOTOF-Q II ESI-Qq-TOF (Bruker)	148
753	144	MICROPLATE_ALAMAR_BLUE_COLORIMETRIC	microplate Alamar Blue (resazurin) colorimetric method	148
754	145	MSTATION	Mstation (Jeol)	148
755	146	MSQ_PLUS	MSQ Plus (Thermo Scientific)	148
756	147	NABSYS	NABsys	148
757	148	NANOPHOTONICS_BIOSCIENCES	Nanophotonics Biosciences	148
758	149	NETWORK_BIOSYSTEMS	Network Biosystems	148
759	150	NIMBLEGEN	Nimblegen	148
760	151	OXFORD_NANOPORE_TECHNOLOGIES	Oxford Nanopore Technologies	148
761	152	PACIFIC_BIOSCIENCES	Pacific Biosciences	148
762	153	POPULATION_GENETICS_TECHNOLOGIES	Population Genetics Technologies	148
763	154	Q1000GC_ULTRAQUAD	Q1000GC UltraQuad (Jeol)	148
764	155	QUATTRO_MICRO_API	Quattro micro API (Waters)	148
765	156	QUATTRO_MICRO_GC	Quattro micro GC (Waters)	148
766	157	QUATTRO_PREMIER_XE	Quattro Premier XE (Waters)	148
767	158	QSTAR	QSTAR (AB Sciex)	148
768	159	REVEO	Reveo	148
769	160	ROCHE	Roche	148
770	161	SEIRAD	Seirad	148
771	162	SOLARIX_HYBRID_QQ_FTMS	solariX hybrid Qq-FTMS (Bruker)	148
772	163	SOMACOUNT	Somacount (Bently Instruments)	148
773	164	SOMASCOPE	SomaScope (Bently Instruments)	148
774	165	SYNAPT_G2_HDMS	SYNAPT G2 HDMS (Waters)	148
775	166	SYNAPT_G2_MS	SYNAPT G2 MS (Waters)	148
776	167	SYNAPT_HDMS	SYNAPT HDMS (Waters)	148
777	168	SYNAPT_MS	SYNAPT MS (Waters)	148
778	169	TRIPLETOF_5600	TripleTOF 5600 (AB Sciex)	148
779	170	TSQ_QUANTUM_ULTRA	TSQ Quantum Ultra (Thermo Scientific)	148
780	171	TSQ_QUANTUM_ACCESS	TSQ Quantum Access (Thermo Scientific)	148
781	172	TSQ_QUANTUM_ACCESS_MAX	TSQ Quantum Access MAX (Thermo Scientific)	148
782	173	TSQ_QUANTUM_DISCOVERY_MAX	TSQ Quantum Discovery MAX (Thermo Scientific)	148
783	174	TSQ_QUANTUM_GC	TSQ Quantum GC (Thermo Scientific)	148
784	175	TSQ_QUANTUM_XLS	TSQ Quantum XLS (Thermo Scientific)	148
785	176	TSQ_VANTAGE	TSQ Vantage (Thermo Scientific)	148
786	177	ULTRAFLEXTREME_MALDI_TOF_MS	ultrafleXtreme MALDI-TOF MS (Bruker)	148
787	178	VISIGEN_BIO	VisiGen Biotechnologies	148
788	179	XEVO_G2_QTOF	Xevo G2 QTOF (Waters)	148
789	180	XEVO_QTOF_MS	Xevo QTof MS (Waters)	148
790	181	XEVO_TQ_MS	Xevo TQ MS (Waters)	148
791	182	XEVO_TQ_S	Xevo TQ-S (Waters)	148
792	183	OTHER_PLATFORM	Other	148
793	0		abstract	154
794	1		addendum	154
795	2		announcement	154
796	3		article-commentary	154
797	4		book review	154
798	5		books received	154
799	6		brief report	154
800	7		calendar	154
801	8		case report	154
802	9		collection	154
803	10		correction	154
804	11		data paper	154
805	12		discussion	154
806	13		dissertation	154
807	14		editorial	154
808	15		in brief	154
809	16		introduction	154
810	17		letter	154
811	18		meeting report	154
812	19		news	154
813	20		obituary	154
814	21		oration	154
815	22		partial retraction	154
816	23		product review	154
817	24		rapid communication	154
818	25		reply	154
819	26		reprint	154
820	27		research article	154
821	28		retraction	154
822	29		review article	154
823	30		translation	154
824	31		other	154
\.


--
-- Data for Name: customfieldmap; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY customfieldmap (id, sourcedatasetfield, sourcetemplate, targetdatasetfield) FROM stdin;
\.


--
-- Data for Name: customquestion; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY customquestion (id, displayorder, hidden, questionstring, questiontype, required, guestbook_id) FROM stdin;
\.


--
-- Data for Name: customquestionresponse; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY customquestionresponse (id, response, customquestion_id, guestbookresponse_id) FROM stdin;
\.


--
-- Data for Name: customquestionvalue; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY customquestionvalue (id, displayorder, valuestring, customquestion_id) FROM stdin;
\.


--
-- Data for Name: datafile; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY datafile (id, contenttype, filesystemname, filesize, ingeststatus, md5, name, restricted) FROM stdin;
4	application/vnd.google-earth.kmz	14dd48f37d9-68789d517db2	0	A	cfaad1e9562443bb07119fcdbe11ccd2	\N	f
\.


--
-- Data for Name: datafilecategory; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY datafilecategory (id, name, dataset_id) FROM stdin;
1	Code	3
\.


--
-- Data for Name: datafiletag; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY datafiletag (id, type, datafile_id) FROM stdin;
\.


--
-- Data for Name: dataset; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY dataset (id, authority, doiseparator, fileaccessrequest, globalidcreatetime, identifier, protocol, guestbook_id, thumbnailfile_id) FROM stdin;
3	10.5072/FK2	/	f	2015-06-08 13:30:09.023	A0Y3TZ	doi	\N	\N
\.


--
-- Data for Name: datasetfield; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY datasetfield (id, datasetfieldtype_id, datasetversion_id, parentdatasetfieldcompoundvalue_id, template_id) FROM stdin;
1	16	1	\N	\N
2	12	1	\N	\N
3	7	1	\N	\N
4	1	1	\N	\N
5	14	\N	2	\N
6	8	\N	3	\N
7	19	1	\N	\N
8	17	\N	1	\N
9	57	1	\N	\N
10	10	\N	3	\N
11	13	\N	2	\N
12	15	\N	2	\N
13	9	\N	3	\N
14	56	1	\N	\N
\.


--
-- Data for Name: datasetfield_controlledvocabularyvalue; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY datasetfield_controlledvocabularyvalue (datasetfield_id, controlledvocabularyvalues_id) FROM stdin;
7	3
\.


--
-- Data for Name: datasetfieldcompoundvalue; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY datasetfieldcompoundvalue (id, displayorder, parentdatasetfield_id) FROM stdin;
1	0	1
2	0	2
3	0	3
\.


--
-- Data for Name: datasetfielddefaultvalue; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY datasetfielddefaultvalue (id, displayorder, strvalue, datasetfield_id, defaultvalueset_id, parentdatasetfielddefaultvalue_id) FROM stdin;
\.


--
-- Data for Name: datasetfieldtype; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY datasetfieldtype (id, advancedsearchfieldtype, allowcontrolledvocabulary, allowmultiples, description, displayformat, displayoncreate, displayorder, facetable, fieldtype, name, required, title, watermark, metadatablock_id, parentdatasetfieldtype_id) FROM stdin;
1	t	f	f	Full title by which the Dataset is known.		t	0	f	TEXT	title	t	Title	Enter title...	1	\N
2	f	f	f	A secondary title used to amplify or state certain limitations on the main title.		f	1	f	TEXT	subtitle	f	Subtitle		1	\N
3	f	f	f	A title by which the work is commonly referred, or an abbreviation of the title.		f	2	f	TEXT	alternativeTitle	f	Alternative Title		1	\N
4	f	f	t	Another unique identifier that identifies this Dataset (e.g., producer's or another repository's number).	:	f	3	f	NONE	otherId	f	Other ID		1	\N
5	f	f	f	Name of agency which generated this identifier.	#VALUE	f	4	f	TEXT	otherIdAgency	f	Agency		1	4
6	f	f	f	Other identifier that corresponds to this Dataset.	#VALUE	f	5	f	TEXT	otherIdValue	f	Identifier		1	4
7	f	f	t	The person(s), corporate body(ies), or agency(ies) responsible for creating the work.		t	6	f	NONE	author	f	Author		1	\N
8	t	f	f	The author's Family Name, Given Name or the name of the organization responsible for this Dataset.	#VALUE	t	7	t	TEXT	authorName	t	Name	FamilyName, GivenName or Organization	1	7
9	t	f	f	The organization with which the author is affiliated.	(#VALUE)	t	8	t	TEXT	authorAffiliation	f	Affiliation		1	7
10	f	t	f	Name of the identifier scheme (ORCID, ISNI).	- #VALUE:	t	9	f	TEXT	authorIdentifierScheme	f	Identifier Scheme		1	7
11	f	f	f	Uniquely identifies an individual author or organization, according to various schemes.	#VALUE	t	10	f	TEXT	authorIdentifier	f	Identifier		1	7
12	f	f	t	The contact(s) for this Dataset.		t	11	f	NONE	datasetContact	f	Contact		1	\N
13	f	f	f	The contact's Family Name, Given Name or the name of the organization.	#VALUE	t	12	f	TEXT	datasetContactName	f	Name	FamilyName, GivenName or Organization	1	12
14	f	f	f	The organization with which the contact is affiliated.	(#VALUE)	t	13	f	TEXT	datasetContactAffiliation	f	Affiliation		1	12
15	f	f	f	The e-mail address(es) of the contact(s) for the Dataset. This will not be displayed.	#EMAIL	t	14	f	EMAIL	datasetContactEmail	t	E-mail		1	12
16	f	f	t	A summary describing the purpose, nature, and scope of the Dataset.		t	15	f	NONE	dsDescription	f	Description		1	\N
17	t	f	f	A summary describing the purpose, nature, and scope of the Dataset.	#VALUE	t	16	f	TEXTBOX	dsDescriptionValue	t	Text		1	16
18	f	f	f	In cases where a Dataset contains more than one description (for example, one might be supplied by the data producer and another prepared by the data repository where the data are deposited), the date attribute is used to distinguish between the two descriptions. The date attribute follows the ISO convention of YYYY-MM-DD.	(#VALUE)	t	17	f	DATE	dsDescriptionDate	f	Date	YYYY-MM-DD	1	16
19	t	t	t	Domain-specific Subject Categories that are topically relevant to the Dataset.		t	18	t	TEXT	subject	t	Subject		1	\N
20	f	f	t	Key terms that describe important aspects of the Dataset.		t	19	f	NONE	keyword	f	Keyword		1	\N
21	t	f	f	Key terms that describe important aspects of the Dataset. Can be used for building keyword indexes and for classification and retrieval purposes. A controlled vocabulary can be employed. The vocab attribute is provided for specification of the controlled vocabulary in use, such as LCSH, MeSH, or others. The vocabURI attribute specifies the location for the full controlled vocabulary.	#VALUE	t	20	t	TEXT	keywordValue	f	Term		1	20
22	f	f	f	For the specification of the keyword controlled vocabulary in use, such as LCSH, MeSH, or others.	(#VALUE)	t	21	f	TEXT	keywordVocabulary	f	Vocabulary		1	20
23	f	f	f	Keyword vocabulary URL points to the web presence that describes the keyword vocabulary, if appropriate. Enter an absolute URL where the keyword vocabulary web site is found, such as http://www.my.org.	<a href="#VALUE">#VALUE</a>	t	22	f	URL	keywordVocabularyURI	f	Vocabulary URL	Enter full URL, starting with http://	1	20
24	f	f	t	The classification field indicates the broad important topic(s) and subjects that the data cover. Library of Congress subject terms may be used here.  		f	23	f	NONE	topicClassification	f	Topic Classification		1	\N
25	t	f	f	Topic or Subject term that is relevant to this Dataset.	#VALUE	f	24	t	TEXT	topicClassValue	f	Term		1	24
26	f	f	f	Provided for specification of the controlled vocabulary in use, e.g., LCSH, MeSH, etc.	(#VALUE)	f	25	f	TEXT	topicClassVocab	f	Vocabulary		1	24
27	f	f	f	Specifies the URL location for the full controlled vocabulary.	<a href="#VALUE">#VALUE</a>	f	26	f	URL	topicClassVocabURI	f	Vocabulary URL	Enter full URL, starting with http://	1	24
28	f	f	t	Publications that use the data from this Dataset.		f	27	f	NONE	publication	f	Related Publication		1	\N
29	t	f	f	The full bibliographic citation for this related publication.	#VALUE	f	28	f	TEXTBOX	publicationCitation	f	Citation		1	28
30	t	t	f	The type of digital identifier used for this publication (e.g., Digital Object Identifier (DOI)).	#VALUE: 	f	29	f	TEXT	publicationIDType	f	ID Type		1	28
31	t	f	f	The identifier for the selected ID type.	#VALUE	f	30	f	TEXT	publicationIDNumber	f	ID Number		1	28
32	f	f	f	Link to the publication web page (e.g., journal article page, archive record page, or other).	<a href="#VALUE">#VALUE</a>	f	31	f	URL	publicationURL	f	URL	Enter full URL, starting with http://	1	28
33	f	f	f	Additional important information about the Dataset.		t	32	f	TEXTBOX	notesText	f	Notes		1	\N
34	t	t	t	Language of the Dataset		f	33	t	TEXT	language	f	Language		1	\N
35	f	f	t	Person or organization with the financial or administrative responsibility over this Dataset		f	34	f	NONE	producer	f	Producer		1	\N
36	t	f	f	Producer name	#VALUE	f	35	t	TEXT	producerName	f	Name	FamilyName, GivenName or Organization	1	35
37	f	f	f	The organization with which the producer is affiliated.	(#VALUE)	f	36	f	TEXT	producerAffiliation	f	Affiliation		1	35
38	f	f	f	The abbreviation by which the producer is commonly known. (ex. IQSS, ICPSR)	(#VALUE)	f	37	f	TEXT	producerAbbreviation	f	Abbreviation		1	35
39	f	f	f	Producer URL points to the producer's web presence, if appropriate. Enter an absolute URL where the producer's web site is found, such as http://www.my.org.  	<a href="#VALUE">#VALUE</a>	f	38	f	URL	producerURL	f	URL	Enter full URL, starting with http://	1	35
40	f	f	f	URL for the producer's logo, which points to this  producer's web-accessible logo image. Enter an absolute URL where the producer's logo image is found, such as http://www.my.org/images/logo.gif.	<img src="#VALUE" alt="#NAME" class="metadata-logo"/><br/>	f	39	f	URL	producerLogoURL	f	Logo URL	Enter full URL for image, starting with http://	1	35
41	t	f	f	Date when the data collection or other materials were produced (not distributed, published or archived).		f	40	t	DATE	productionDate	f	Production Date	YYYY-MM-DD	1	\N
42	f	f	f	The location where the data collection and any other related materials were produced.		f	41	f	TEXT	productionPlace	f	Production Place		1	\N
43	f	f	t	The organization or person responsible for either collecting, managing, or otherwise contributing in some form to the development of the resource.	:	f	42	f	NONE	contributor	f	Contributor		1	\N
44	t	t	f	The type of contributor of the  resource.  	#VALUE 	f	43	t	TEXT	contributorType	f	Type		1	43
45	t	f	f	The Family Name, Given Name or organization name of the contributor.	#VALUE	f	44	t	TEXT	contributorName	f	Name	FamilyName, GivenName or Organization	1	43
46	f	f	t	Grant Information	:	f	45	f	NONE	grantNumber	f	Grant Information		1	\N
47	f	f	f	Grant Number Agency	#VALUE	f	46	f	TEXT	grantNumberAgency	f	Grant Agency		1	46
48	f	f	f	The grant or contract number of the project that  sponsored the effort.	#VALUE	f	47	f	TEXT	grantNumberValue	f	Grant Number		1	46
49	f	f	t	The organization designated by the author or producer to generate copies of the particular work including any necessary editions or revisions.		f	48	f	NONE	distributor	f	Distributor		1	\N
50	t	f	f	Distributor name	#VALUE	f	49	t	TEXT	distributorName	f	Name	FamilyName, GivenName or Organization	1	49
51	f	f	f	The organization with which the distributor contact is affiliated.	(#VALUE)	f	50	f	TEXT	distributorAffiliation	f	Affiliation		1	49
52	f	f	f	The abbreviation by which this distributor is commonly known (e.g., IQSS, ICPSR).	(#VALUE)	f	51	f	TEXT	distributorAbbreviation	f	Abbreviation		1	49
53	f	f	f	Distributor URL points to the distributor's web presence, if appropriate. Enter an absolute URL where the distributor's web site is found, such as http://www.my.org.	<a href="#VALUE">#VALUE</a>	f	52	f	URL	distributorURL	f	URL	Enter full URL, starting with http://	1	49
54	f	f	f	URL of the distributor's logo, which points to this  distributor's web-accessible logo image. Enter an absolute URL where the distributor's logo image is found, such as http://www.my.org/images/logo.gif.	<img src="#VALUE" alt="#NAME" class="metadata-logo"/><br/>	f	53	f	URL	distributorLogoURL	f	Logo URL	Enter full URL for image, starting with http://	1	49
55	t	f	f	Date that the work was made available for distribution/presentation.		f	54	t	DATE	distributionDate	f	Distribution Date	YYYY-MM-DD	1	\N
56	f	f	f	The person (Family Name, Given Name) or the name of the organization that deposited this Dataset to the repository.		f	55	f	TEXT	depositor	f	Depositor		1	\N
57	f	f	f	Date that the Dataset was deposited into the repository.		f	56	t	DATE	dateOfDeposit	f	Deposit Date	YYYY-MM-DD	1	\N
58	f	f	t	Time period to which the data refer. This item reflects the time period covered by the data, not the dates of coding or making documents machine-readable or the dates the data were collected. Also known as span.	;	f	57	f	NONE	timePeriodCovered	f	Time Period Covered		1	\N
59	t	f	f	Start date which reflects the time period covered by the data, not the dates of coding or making documents machine-readable or the dates the data were collected.	#NAME: #VALUE 	f	58	t	DATE	timePeriodCoveredStart	f	Start	YYYY-MM-DD	1	58
60	t	f	f	End date which reflects the time period covered by the data, not the dates of coding or making documents machine-readable or the dates the data were collected.	#NAME: #VALUE 	f	59	t	DATE	timePeriodCoveredEnd	f	End	YYYY-MM-DD	1	58
61	f	f	t	Contains the date(s) when the data were collected.	;	f	60	f	NONE	dateOfCollection	f	Date of Collection		1	\N
62	f	f	f	Date when the data collection started.	#NAME: #VALUE 	f	61	f	DATE	dateOfCollectionStart	f	Start	YYYY-MM-DD	1	61
63	f	f	f	Date when the data collection ended.	#NAME: #VALUE 	f	62	f	DATE	dateOfCollectionEnd	f	End	YYYY-MM-DD	1	61
64	t	f	t	Type of data included in the file: survey data, census/enumeration data, aggregate data, clinical data, event/transaction data, program source code, machine-readable text, administrative records data, experimental data, psychological test, textual data, coded textual, coded documents, time budget diaries, observation data/ratings, process-produced data, or other.		f	63	t	TEXT	kindOfData	f	Kind of Data		1	\N
65	f	f	f	Information about the Dataset series.	:	f	64	f	NONE	series	f	Series		1	\N
66	t	f	f	Name of the dataset series to which the Dataset belongs.	#VALUE	f	65	t	TEXT	seriesName	f	Name		1	65
67	f	f	f	History of the series and summary of those features that apply to the series as a whole.	#VALUE	f	66	f	TEXTBOX	seriesInformation	f	Information		1	65
68	f	f	t	Information about the software used to generate the Dataset.	,	f	67	f	NONE	software	f	Software		1	\N
69	f	t	f	Name of software used to generate the Dataset.	#VALUE	f	68	f	TEXT	softwareName	f	Name		1	68
70	f	f	f	Version of the software used to generate the Dataset.	#NAME: #VALUE	f	69	f	TEXT	softwareVersion	f	Version		1	68
71	f	f	t	Any material related to this Dataset.		f	70	f	TEXTBOX	relatedMaterial	f	Related Material		1	\N
72	f	f	t	Any Datasets that are related to this Dataset, such as previous research on this subject.		f	71	f	TEXTBOX	relatedDatasets	f	Related Datasets		1	\N
73	f	f	t	Any references that would serve as background or supporting material to this Dataset.		f	72	f	TEXT	otherReferences	f	Other References		1	\N
74	f	f	t	List of books, articles, serials, or machine-readable data files that served as the sources of the data collection.		f	73	f	TEXTBOX	dataSources	f	Data Sources		1	\N
75	f	f	f	For historical materials, information about the origin of the sources and the rules followed in establishing the sources should be specified.		f	74	f	TEXTBOX	originOfSources	f	Origin of Sources		1	\N
76	f	f	f	Assessment of characteristics and source material.		f	75	f	TEXTBOX	characteristicOfSources	f	Characteristic of Sources Noted		1	\N
77	f	f	f	Level of documentation of the original sources.		f	76	f	TEXTBOX	accessToSources	f	Documentation and Access to Sources		1	\N
78	f	f	t	Information on the geographic coverage of the data. Includes the total geographic scope of the data.		f	0	f	NONE	geographicCoverage	f	Geographic Coverage		2	\N
79	t	t	f	The country or nation that the Dataset is about.		f	1	t	TEXT	country	f	Country / Nation		2	78
80	t	f	f	The state or province that the Dataset is about. Use GeoNames for correct spelling and avoid abbreviations.		f	2	t	TEXT	state	f	State / Province		2	78
81	t	f	f	The name of the city that the Dataset is about. Use GeoNames for correct spelling and avoid abbreviations.		f	3	t	TEXT	city	f	City		2	78
82	f	f	f	Other information on the geographic coverage of the data.		f	4	f	TEXT	otherGeographicCoverage	f	Other		2	78
83	t	f	t	Lowest level of geographic aggregation covered by the Dataset, e.g., village, county, region.		f	5	t	TEXT	geographicUnit	f	Geographic Unit		2	\N
84	f	f	t	The fundamental geometric description for any Dataset that models geography is the geographic bounding box. It describes the minimum box, defined by west and east longitudes and north and south latitudes, which includes the largest geographic extent of the  Dataset's geographic coverage. This element is used in the first pass of a coordinate-based search. Inclusion of this element in the codebook is recommended, but is required if the bound polygon box is included. 		f	6	f	NONE	geographicBoundingBox	f	Geographic Bounding Box		2	\N
85	f	f	f	Westernmost coordinate delimiting the geographic extent of the Dataset. A valid range of values,  expressed in decimal degrees, is -180,0 <= West  Bounding Longitude Value <= 180,0.		f	7	f	TEXT	westLongitude	f	West Longitude		2	84
86	f	f	f	Easternmost coordinate delimiting the geographic extent of the Dataset. A valid range of values,  expressed in decimal degrees, is -180,0 <= East Bounding Longitude Value <= 180,0.		f	8	f	TEXT	eastLongitude	f	East Longitude		2	84
87	f	f	f	Northernmost coordinate delimiting the geographic extent of the Dataset. A valid range of values,  expressed in decimal degrees, is -90,0 <= North Bounding Latitude Value <= 90,0.		f	9	f	TEXT	northLongitude	f	North Latitude		2	84
88	f	f	f	Southernmost coordinate delimiting the geographic extent of the Dataset. A valid range of values,  expressed in decimal degrees, is -90,0 <= South Bounding Latitude Value <= 90,0.		f	10	f	TEXT	southLongitude	f	South Latitude		2	84
89	t	f	t	Basic unit of analysis or observation that this Dataset describes, such as individuals, families/households, groups, institutions/organizations, administrative units, and more. For information about the DDI's controlled vocabulary for this element, please refer to the DDI web page at http://www.ddialliance.org/Specification/DDI-CV/.		f	0	t	TEXTBOX	unitOfAnalysis	f	Unit of Analysis		3	\N
90	t	f	t	Description of the population covered by the data in the file; the group of people or other elements that are the object of the study and to which the study results refer. Age, nationality, and residence commonly help to  delineate a given universe, but any number of other factors may be used, such as age limits, sex, marital status, race, ethnic group, nationality, income, veteran status, criminal convictions, and more. The universe may consist of elements other than persons, such as housing units, court cases, deaths, countries, and so on. In general, it should be possible to tell from the description of the universe whether a given individual or element is a member of the population under study. Also known as the universe of interest, population of interest, and target population.		f	1	t	TEXTBOX	universe	f	Universe		3	\N
91	t	f	f	The time method or time dimension of the data collection, such as panel, cross-sectional, trend, time- series, or other.		f	2	t	TEXT	timeMethod	f	Time Method		3	\N
92	f	f	f	Individual, agency or organization responsible for  administering the questionnaire or interview or compiling the data.		f	3	f	TEXT	dataCollector	f	Data Collector	FamilyName, GivenName or Organization	3	\N
93	f	f	f	Type of training provided to the data collector		f	4	f	TEXT	collectorTraining	f	Collector Training		3	\N
94	t	f	f	If the data collected includes more than one point in time, indicate the frequency with which the data was collected; that is, monthly, quarterly, or other.		f	5	t	TEXT	frequencyOfDataCollection	f	Frequency		3	\N
95	f	f	f	Type of sample and sample design used to select the survey respondents to represent the population. May include reference to the target sample size and the sampling fraction.		f	6	f	TEXTBOX	samplingProcedure	f	Sampling Procedure		3	\N
96	f	f	f	Specific information regarding the target sample size, actual  sample size, and the formula used to determine this.		f	7	f	NONE	targetSampleSize	f	Target Sample Size		3	\N
97	f	f	f	Actual sample size.		f	8	f	INT	targetSampleActualSize	f	Actual	Enter an integer...	3	96
98	f	f	f	Formula used to determine target sample size.		f	9	f	TEXT	targetSampleSizeFormula	f	Formula		3	96
99	f	f	f	Show correspondence as well as discrepancies between the sampled units (obtained) and available statistics for the population (age, sex-ratio, marital status, etc.) as a whole.		f	10	f	TEXT	deviationsFromSampleDesign	f	Major Deviations for Sample Design		3	\N
100	f	f	f	Method used to collect the data; instrumentation characteristics (e.g., telephone interview, mail questionnaire, or other).		f	11	f	TEXTBOX	collectionMode	f	Collection Mode		3	\N
101	f	f	f	Type of data collection instrument used. Structured indicates an instrument in which all respondents are asked the same questions/tests, possibly with precoded answers. If a small portion of such a questionnaire includes open-ended questions, provide appropriate comments. Semi-structured indicates that the research instrument contains mainly open-ended questions. Unstructured indicates that in-depth interviews were conducted.		f	12	f	TEXT	researchInstrument	f	Type of Research Instrument		3	\N
102	f	f	f	Description of noteworthy aspects of the data collection situation. Includes information on factors such as cooperativeness of respondents, duration of interviews, number of call backs, or similar.		f	13	f	TEXTBOX	dataCollectionSituation	f	Characteristics of Data Collection Situation		3	\N
103	f	f	f	Summary of actions taken to minimize data loss. Include information on actions such as follow-up visits, supervisory checks, historical matching, estimation, and so on.		f	14	f	TEXT	actionsToMinimizeLoss	f	Actions to Minimize Losses		3	\N
104	f	f	f	Control OperationsMethods to facilitate data control performed by the primary investigator or by the data archive.		f	15	f	TEXT	controlOperations	f	Control Operations		3	\N
105	f	f	f	The use of sampling procedures might make it necessary to apply weights to produce accurate statistical results. Describes the criteria for using weights in analysis of a collection. If a weighting formula or coefficient was developed, the formula is provided, its elements are defined, and it is indicated how the formula was applied to the data.		f	16	f	TEXTBOX	weighting	f	Weighting		3	\N
106	f	f	f	Methods used to clean the data collection, such as consistency checking, wildcode checking, or other.		f	17	f	TEXT	cleaningOperations	f	Cleaning Operations		3	\N
107	f	f	f	Note element used for any information annotating or clarifying the methodology and processing of the study. 		f	18	f	TEXT	datasetLevelErrorNotes	f	Study Level Error Notes		3	\N
108	t	f	f	Percentage of sample members who provided information.		f	19	t	TEXTBOX	responseRate	f	Response Rate		3	\N
109	f	f	f	Measure of how precisely one can estimate a population value from a given sample.		f	20	f	TEXT	samplingErrorEstimates	f	Estimates of Sampling Error		3	\N
110	f	f	f	Other issues pertaining to the data appraisal. Describe issues such as response variance, nonresponse rate  and testing for bias, interviewer and response bias, confidence levels, question bias, or similar.		f	21	f	TEXT	otherDataAppraisal	f	Other Forms of Data Appraisal		3	\N
111	f	f	f	General notes about this Dataset.		f	22	f	NONE	socialScienceNotes	f	Notes		3	\N
112	f	f	f	Type of note.		f	23	f	TEXT	socialScienceNotesType	f	Type		3	111
113	f	f	f	Note subject.		f	24	f	TEXT	socialScienceNotesSubject	f	Subject		3	111
114	f	f	f	Text for this note.		f	25	f	TEXTBOX	socialScienceNotesText	f	Text		3	111
115	t	t	t	The nature or genre of the content of the files in the dataset.		f	0	t	TEXT	astroType	f	Type		4	\N
116	t	t	t	The observatory or facility where the data was obtained. 		f	1	t	TEXT	astroFacility	f	Facility		4	\N
117	t	t	t	The instrument used to collect the data.		f	2	t	TEXT	astroInstrument	f	Instrument		4	\N
118	t	f	t	Astronomical Objects represented in the data (Given as SIMBAD recognizable names preferred).		f	3	t	TEXT	astroObject	f	Object		4	\N
119	t	f	f	The spatial (angular) resolution that is typical of the observations, in decimal degrees.		f	4	t	TEXT	resolution.Spatial	f	Spatial Resolution		4	\N
120	t	f	f	The spectral resolution that is typical of the observations, given as the ratio λ/Δλ.		f	5	t	TEXT	resolution.Spectral	f	Spectral Resolution		4	\N
121	f	f	f	The temporal resolution that is typical of the observations, given in seconds.		f	6	f	TEXT	resolution.Temporal	f	Time Resolution		4	\N
122	t	t	t	Conventional bandpass name		f	7	t	TEXT	coverage.Spectral.Bandpass	f	Bandpass		4	\N
123	t	f	t	The central wavelength of the spectral bandpass, in meters.		f	8	t	FLOAT	coverage.Spectral.CentralWavelength	f	Central Wavelength (m)	Enter a floating-point number.	4	\N
124	f	f	t	The minimum and maximum wavelength of the spectral bandpass.		f	9	f	NONE	coverage.Spectral.Wavelength	f	Wavelength Range	Enter a floating-point number.	4	\N
125	t	f	f	The minimum wavelength of the spectral bandpass, in meters.		f	10	t	FLOAT	coverage.Spectral.MinimumWavelength	f	Minimum (m)	Enter a floating-point number.	4	124
126	t	f	f	The maximum wavelength of the spectral bandpass, in meters.		f	11	t	FLOAT	coverage.Spectral.MaximumWavelength	f	Maximum (m)	Enter a floating-point number.	4	124
127	f	f	t	 Time period covered by the data.		f	12	f	NONE	coverage.Temporal	f	Dataset Date Range		4	\N
128	t	f	f	Dataset Start Date		f	13	t	DATE	coverage.Temporal.StartTime	f	Start	YYYY-MM-DD	4	127
129	t	f	f	Dataset End Date		f	14	t	DATE	coverage.Temporal.StopTime	f	End	YYYY-MM-DD	4	127
130	f	f	t	The sky coverage of the data object.		f	15	f	TEXT	coverage.Spatial	f	Sky Coverage		4	\N
131	f	f	f	The (typical) depth coverage, or sensitivity, of the data object in Jy.		f	16	f	FLOAT	coverage.Depth	f	Depth Coverage	Enter a floating-point number.	4	\N
132	f	f	f	The (typical) density of objects, catalog entries, telescope pointings, etc., on the sky, in number per square degree.		f	17	f	FLOAT	coverage.ObjectDensity	f	Object Density	Enter a floating-point number.	4	\N
133	f	f	f	The total number of objects, catalog entries, etc., in the data object.		f	18	f	INT	coverage.ObjectCount	f	Object Count	Enter an integer.	4	\N
134	f	f	f	The fraction of the sky represented in the observations, ranging from 0 to 1.		f	19	f	FLOAT	coverage.SkyFraction	f	Fraction of Sky	Enter a floating-point number.	4	\N
135	f	f	f	The polarization coverage		f	20	f	TEXT	coverage.Polarization	f	Polarization		4	\N
136	f	f	f	RedshiftType string C "Redshift"; or "Optical" or "Radio" definitions of Doppler velocity used in the data object.		f	21	f	TEXT	redshiftType	f	RedshiftType		4	\N
137	f	f	f	The resolution in redshift (unitless) or Doppler velocity (km/s) in the data object.		f	22	f	FLOAT	resolution.Redshift	f	Redshift Resolution	Enter a floating-point number.	4	\N
138	f	f	t	The value of the redshift (unitless) or Doppler velocity (km/s in the data object.		f	23	f	FLOAT	coverage.RedshiftValue	f	Redshift Value	Enter a floating-point number.	4	\N
139	f	f	f	The minimum value of the redshift (unitless) or Doppler velocity (km/s in the data object.		f	24	f	FLOAT	coverage.Redshift.MinimumValue	f	Minimum	Enter a floating-point number.	4	138
140	f	f	f	The maximum value of the redshift (unitless) or Doppler velocity (km/s in the data object.		f	25	f	FLOAT	coverage.Redshift.MaximumValue	f	Maximum	Enter a floating-point number.	4	138
141	t	t	t	Design types that are based on the overall experimental design.		f	0	t	TEXT	studyDesignType	f	Design Type		5	\N
142	t	t	t	Factors used in the Dataset. 		f	1	t	TEXT	studyFactorType	f	Factor Type		5	\N
143	t	t	t	The taxonomic name of the organism used in the Dataset or from which the  starting biological material derives.		f	2	t	TEXT	studyAssayOrganism	f	Organism		5	\N
144	t	f	t	If Other was selected in Organism, list any other organisms that were used in this Dataset. Terms from the NCBI Taxonomy are recommended.		f	3	t	TEXT	studyAssayOtherOrganism	f	Other Organism		5	\N
145	t	t	t	A term to qualify the endpoint, or what is being measured (e.g. gene expression profiling; protein identification). 		f	4	t	TEXT	studyAssayMeasurementType	f	Measurement Type		5	\N
146	t	f	t	If Other was selected in Measurement Type, list any other measurement types that were used. Terms from NCBO Bioportal are recommended.		f	5	t	TEXT	studyAssayOtherMeasurmentType	f	Other Measurement Type		5	\N
147	t	t	t	A term to identify the technology used to perform the measurement (e.g. DNA microarray; mass spectrometry).		f	6	t	TEXT	studyAssayTechnologyType	f	Technology Type		5	\N
148	t	t	t	The manufacturer and name of the technology platform used in the assay (e.g. Bruker AVANCE).		f	7	t	TEXT	studyAssayPlatform	f	Technology Platform		5	\N
149	t	t	t	The name of the cell line from which the source or sample derives.		f	8	t	TEXT	studyAssayCellType	f	Cell Type		5	\N
150	f	f	t	Indicates the volume, issue and date of a journal, which this Dataset is associated with.		f	0	f	NONE	journalVolumeIssue	f	Journal		6	\N
151	t	f	f	The journal volume which this Dataset is associated with (e.g., Volume 4).		f	1	t	TEXT	journalVolume	f	Volume		6	150
152	t	f	f	The journal issue number which this Dataset is associated with (e.g., Number 2, Autumn).		f	2	t	TEXT	journalIssue	f	Issue		6	150
153	t	f	f	The publication date for this journal volume/issue, which this Dataset is associated with (e.g., 1999).		f	3	t	DATE	journalPubDate	f	Publication Date	YYYY or YYYY-MM or YYYY-MM-DD	6	150
154	t	t	f	Indicates what kind of article this is, for example, a research article, a commentary, a book or product review, a case report, a calendar, etc (based on JATS). 		f	4	t	TEXT	journalArticleType	f	Type of Article		6	\N
\.


--
-- Data for Name: datasetfieldvalue; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY datasetfieldvalue (id, displayorder, value, datasetfield_id) FROM stdin;
1	0	We need to add files to this Dataset.	8
2	0	Sample Dataset	4
3	0	Dataverse.org	13
4	0	Admin, Dataverse	6
5	0	Admin, Dataverse	11
6	0	2015-06-08	9
7	0	dataverse@mailinator.com	12
8	0	Admin, Dataverse	14
9	0	Dataverse.org	5
\.


--
-- Data for Name: datasetlinkingdataverse; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY datasetlinkingdataverse (id, linkcreatetime, dataset_id, linkingdataverse_id) FROM stdin;
\.


--
-- Data for Name: datasetlock; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY datasetlock (id, info, starttime, user_id, dataset_id) FROM stdin;
\.


--
-- Data for Name: datasetversion; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY datasetversion (id, unf, archivenote, archivetime, availabilitystatus, citationrequirements, conditions, confidentialitydeclaration, contactforaccess, createtime, dataaccessplace, deaccessionlink, depositorrequirements, disclaimer, fileaccessrequest, inreview, lastupdatetime, license, minorversionnumber, originalarchive, releasetime, restrictions, sizeofcollection, specialpermissions, studycompletion, termsofaccess, termsofuse, version, versionnote, versionnumber, versionstate, dataset_id) FROM stdin;
1	\N	\N	\N	\N	\N	\N	\N	\N	2015-06-08 13:30:09.023	\N	\N	\N	\N	f	f	2015-06-08 15:40:14.341	CC0	0	\N	2015-06-08 15:40:14.341	\N	\N	\N	\N	\N	\N	2	\N	1	RELEASED	3
\.


--
-- Data for Name: datasetversionuser; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY datasetversionuser (id, lastupdatedate, authenticateduser_id, datasetversion_id) FROM stdin;
1	2015-06-08 15:40:14.341	1	1
\.


--
-- Data for Name: datatable; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY datatable (id, casequantity, originalfileformat, originalformatversion, recordspercase, unf, varquantity, datafile_id) FROM stdin;
\.


--
-- Data for Name: datavariable; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY datavariable (id, fileendposition, fileorder, filestartposition, format, formatcategory, "interval", label, name, numberofdecimalpoints, orderedfactor, recordsegmentnumber, type, unf, universe, weighted, datatable_id) FROM stdin;
\.


--
-- Data for Name: dataverse; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY dataverse (id, affiliation, alias, dataversetype, description, facetroot, guestbookroot, metadatablockroot, name, permissionroot, templateroot, themeroot, defaultcontributorrole_id, defaulttemplate_id) FROM stdin;
1	\N	root	UNCATEGORIZED	The root dataverse.	t	f	t	Root	t	f	t	6	\N
2	Dataverse.org	test-dv	RESEARCHERS	\N	f	f	f	testDV	t	f	t	6	\N
\.


--
-- Data for Name: dataverse_metadatablock; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY dataverse_metadatablock (dataverse_id, metadatablocks_id) FROM stdin;
1	1
\.


--
-- Data for Name: dataversecontact; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY dataversecontact (id, contactemail, displayorder, dataverse_id) FROM stdin;
1	root@mailinator.com	0	1
2	dataverse@mailinator.com	0	2
\.


--
-- Data for Name: dataversefacet; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY dataversefacet (id, displayorder, datasetfieldtype_id, dataverse_id) FROM stdin;
1	3	57	1
2	2	21	1
3	0	8	1
4	1	19	1
\.


--
-- Data for Name: dataversefeatureddataverse; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY dataversefeatureddataverse (id, displayorder, dataverse_id, featureddataverse_id) FROM stdin;
\.


--
-- Data for Name: dataversefieldtypeinputlevel; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY dataversefieldtypeinputlevel (id, include, required, datasetfieldtype_id, dataverse_id) FROM stdin;
\.


--
-- Data for Name: dataverselinkingdataverse; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY dataverselinkingdataverse (id, linkcreatetime, dataverse_id, linkingdataverse_id) FROM stdin;
\.


--
-- Data for Name: dataverserole; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY dataverserole (id, alias, description, name, permissionbits, owner_id) FROM stdin;
1	admin	A person who has all permissions for dataverses, datasets, and files.	Admin	8191	\N
2	fileDownloader	A person who can download a file.	File Downloader	16	\N
3	fullContributor	A person who can add subdataverses and datasets within a dataverse.	Dataverse + Dataset Creator	3	\N
4	dvContributor	A person who can add subdataverses within a dataverse.	Dataverse Creator	1	\N
5	dsContributor	A person who can add datasets within a dataverse.	Dataset Creator	2	\N
6	editor	For datasets, a person who can edit License + Terms, and then submit them for review.	Contributor	4184	\N
7	curator	For datasets, a person who can edit License + Terms, edit Permissions, and publish datasets.	Curator	5471	\N
8	member	A person who can view both unpublished dataverses and datasets.	Member	12	\N
\.


--
-- Data for Name: dataversesubjects; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY dataversesubjects (dataverse_id, controlledvocabularyvalue_id) FROM stdin;
2	3
1	3
\.


--
-- Data for Name: dataversetheme; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY dataversetheme (id, backgroundcolor, linkcolor, linkurl, logo, logoalignment, logobackgroundcolor, logoformat, tagline, textcolor, dataverse_id) FROM stdin;
\.


--
-- Data for Name: defaultvalueset; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY defaultvalueset (id, name) FROM stdin;
\.


--
-- Data for Name: dvobject; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY dvobject (id, dtype, createdate, indextime, modificationtime, permissionindextime, permissionmodificationtime, publicationdate, creator_id, owner_id, releaseuser_id) FROM stdin;
1	Dataverse	2015-06-08 13:08:22.373	\N	2015-06-08 13:29:18.365	2015-06-08 13:29:18.388	2015-06-08 13:08:22.45	2015-06-08 13:29:18.365	1	\N	1
4	DataFile	2015-06-08 15:05:00.586	\N	2015-06-08 15:05:00.586	2015-06-08 15:40:14.657	2015-06-08 15:04:25.299	2015-06-08 15:40:14.341	1	3	\N
3	Dataset	2015-06-08 13:30:09.023	2015-06-08 15:40:14.504	2015-06-08 15:40:14.341	2015-06-08 15:40:14.691	2015-06-08 13:30:09.845	2015-06-08 15:40:14.341	1	2	1
2	Dataverse	2015-06-08 13:29:07.308	2015-06-08 15:40:14.739	2015-06-08 15:40:14.152	2015-06-08 15:40:14.768	2015-06-08 13:29:07.485	2015-06-08 15:40:14.152	1	1	1
\.


--
-- Data for Name: explicitgroup; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY explicitgroup (id, description, displayname, groupalias, groupaliasinowner, owner_id) FROM stdin;
\.


--
-- Data for Name: explicitgroup_authenticateduser; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY explicitgroup_authenticateduser (explicitgroup_id, containedauthenticatedusers_id) FROM stdin;
\.


--
-- Data for Name: explicitgroup_containedroleassignees; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY explicitgroup_containedroleassignees (explicitgroup_id, containedroleassignees) FROM stdin;
\.


--
-- Data for Name: explicitgroup_explicitgroup; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY explicitgroup_explicitgroup (explicitgroup_id, containedexplicitgroups_id) FROM stdin;
\.


--
-- Data for Name: fileaccessrequests; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY fileaccessrequests (datafile_id, authenticated_user_id) FROM stdin;
\.


--
-- Data for Name: filemetadata; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY filemetadata (id, description, label, restricted, version, datafile_id, datasetversion_id) FROM stdin;
1	This is a description of the file.	2001, Palestinian Proposal at the Taba Conference.kmz	f	1	4	1
\.


--
-- Data for Name: filemetadata_datafilecategory; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY filemetadata_datafilecategory (filecategories_id, filemetadatas_id) FROM stdin;
1	1
\.


--
-- Data for Name: foreignmetadatafieldmapping; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY foreignmetadatafieldmapping (id, datasetfieldname, foreignfieldxpath, isattribute, foreignmetadataformatmapping_id, parentfieldmapping_id) FROM stdin;
\.


--
-- Data for Name: foreignmetadataformatmapping; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY foreignmetadataformatmapping (id, displayname, name, schemalocation, startelement) FROM stdin;
\.


--
-- Data for Name: guestbook; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY guestbook (id, createtime, emailrequired, enabled, institutionrequired, name, namerequired, positionrequired, dataverse_id) FROM stdin;
\.


--
-- Data for Name: guestbookresponse; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY guestbookresponse (id, downloadtype, email, institution, name, "position", responsetime, sessionid, authenticateduser_id, datafile_id, dataset_id, datasetversion_id, guestbook_id) FROM stdin;
\.


--
-- Data for Name: harvestingdataverseconfig; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY harvestingdataverseconfig (id, archivedescription, archiveurl, harveststyle, harvesttype, harvestingset, harvestingurl, dataverse_id) FROM stdin;
\.


--
-- Data for Name: ingestreport; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY ingestreport (id, endtime, report, starttime, status, type, datafile_id) FROM stdin;
\.


--
-- Data for Name: ingestrequest; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY ingestrequest (id, controlcard, labelsfile, textencoding, datafile_id) FROM stdin;
\.


--
-- Data for Name: ipv4range; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY ipv4range (id, bottomaslong, topaslong, owner_id) FROM stdin;
\.


--
-- Data for Name: ipv6range; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY ipv6range (id, bottoma, bottomb, bottomc, bottomd, topa, topb, topc, topd, owner_id) FROM stdin;
\.


--
-- Data for Name: maplayermetadata; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY maplayermetadata (id, embedmaplink, layerlink, layername, mapimagelink, worldmapusername, dataset_id, datafile_id) FROM stdin;
\.


--
-- Data for Name: metadatablock; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY metadatablock (id, displayname, name, owner_id) FROM stdin;
1	Citation Metadata	citation	\N
2	Geospatial Metadata	geospatial	\N
3	Social Science and Humanities Metadata	socialscience	\N
4	Astronomy and Astrophysics Metadata	astrophysics	\N
5	Life Sciences Metadata	biomedical	\N
6	Journal Metadata	journal	\N
\.


--
-- Data for Name: passwordresetdata; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY passwordresetdata (id, created, expires, reason, token, builtinuser_id) FROM stdin;
\.


--
-- Data for Name: persistedglobalgroup; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY persistedglobalgroup (id, dtype, description, displayname, persistedgroupalias) FROM stdin;
\.


--
-- Data for Name: roleassignment; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY roleassignment (id, assigneeidentifier, definitionpoint_id, role_id) FROM stdin;
1	@dataverseAdmin	1	1
2	@dataverseAdmin	2	1
3	@dataverseAdmin	3	6
\.


--
-- Data for Name: savedsearch; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY savedsearch (id, query, creator_id, definitionpoint_id) FROM stdin;
\.


--
-- Data for Name: savedsearchfilterquery; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY savedsearchfilterquery (id, filterquery, savedsearch_id) FROM stdin;
\.


--
-- Data for Name: sequence; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY sequence (seq_name, seq_count) FROM stdin;
SEQ_GEN	0
\.


--
-- Data for Name: setting; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY setting (name, content) FROM stdin;
:AllowSignUp	yes
:SignUpUrl	/dataverseuser.xhtml?editMode=CREATE
:Protocol	doi
:Authority	10.5072/FK2
:DoiProvider	EZID
:DoiSeparator	/
BuiltinUsers.KEY	burrito
:BlockedApiKey	empanada
:BlockedApiPolicy	localhost-only
\.


--
-- Data for Name: shibgroup; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY shibgroup (id, attribute, name, pattern) FROM stdin;
\.


--
-- Data for Name: summarystatistic; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY summarystatistic (id, type, value, datavariable_id) FROM stdin;
\.


--
-- Data for Name: template; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY template (id, createtime, name, usagecount, dataverse_id) FROM stdin;
\.


--
-- Data for Name: usernotification; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY usernotification (id, emailed, objectid, readnotification, senddate, type, user_id) FROM stdin;
1	f	2	f	2015-06-08 13:29:07.308	0	1
2	f	1	f	2015-06-08 13:30:09.023	1	1
\.


--
-- Data for Name: variablecategory; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY variablecategory (id, catorder, frequency, label, missing, value, datavariable_id) FROM stdin;
\.


--
-- Data for Name: variablerange; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY variablerange (id, beginvalue, beginvaluetype, endvalue, endvaluetype, datavariable_id) FROM stdin;
\.


--
-- Data for Name: variablerangeitem; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY variablerangeitem (id, value, datavariable_id) FROM stdin;
\.


--
-- Data for Name: worldmapauth_token; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY worldmapauth_token (id, created, hasexpired, lastrefreshtime, modified, token, application_id, datafile_id, dataverseuser_id) FROM stdin;
\.


--
-- Data for Name: worldmapauth_tokentype; Type: TABLE DATA; Schema: public; Owner: dataverse_app
--

COPY worldmapauth_tokentype (id, contactemail, created, hostname, ipaddress, mapitlink, md5, modified, name, timelimitminutes, timelimitseconds) FROM stdin;
\.


--
-- Name: actionlogrecord_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY actionlogrecord
    ADD CONSTRAINT actionlogrecord_pkey PRIMARY KEY (id);


--
-- Name: apitoken_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY apitoken
    ADD CONSTRAINT apitoken_pkey PRIMARY KEY (id);


--
-- Name: apitoken_tokenstring_key; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY apitoken
    ADD CONSTRAINT apitoken_tokenstring_key UNIQUE (tokenstring);


--
-- Name: authenticateduser_email_key; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY authenticateduser
    ADD CONSTRAINT authenticateduser_email_key UNIQUE (email);


--
-- Name: authenticateduser_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY authenticateduser
    ADD CONSTRAINT authenticateduser_pkey PRIMARY KEY (id);


--
-- Name: authenticateduser_useridentifier_key; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY authenticateduser
    ADD CONSTRAINT authenticateduser_useridentifier_key UNIQUE (useridentifier);


--
-- Name: authenticateduserlookup_authenticateduser_id_key; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY authenticateduserlookup
    ADD CONSTRAINT authenticateduserlookup_authenticateduser_id_key UNIQUE (authenticateduser_id);


--
-- Name: authenticateduserlookup_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY authenticateduserlookup
    ADD CONSTRAINT authenticateduserlookup_pkey PRIMARY KEY (id);


--
-- Name: authenticationproviderrow_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY authenticationproviderrow
    ADD CONSTRAINT authenticationproviderrow_pkey PRIMARY KEY (id);


--
-- Name: builtinuser_email_key; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY builtinuser
    ADD CONSTRAINT builtinuser_email_key UNIQUE (email);


--
-- Name: builtinuser_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY builtinuser
    ADD CONSTRAINT builtinuser_pkey PRIMARY KEY (id);


--
-- Name: builtinuser_username_key; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY builtinuser
    ADD CONSTRAINT builtinuser_username_key UNIQUE (username);


--
-- Name: controlledvocabalternate_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY controlledvocabalternate
    ADD CONSTRAINT controlledvocabalternate_pkey PRIMARY KEY (id);


--
-- Name: controlledvocabularyvalue_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY controlledvocabularyvalue
    ADD CONSTRAINT controlledvocabularyvalue_pkey PRIMARY KEY (id);


--
-- Name: customfieldmap_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY customfieldmap
    ADD CONSTRAINT customfieldmap_pkey PRIMARY KEY (id);


--
-- Name: customquestion_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY customquestion
    ADD CONSTRAINT customquestion_pkey PRIMARY KEY (id);


--
-- Name: customquestionresponse_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY customquestionresponse
    ADD CONSTRAINT customquestionresponse_pkey PRIMARY KEY (id);


--
-- Name: customquestionvalue_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY customquestionvalue
    ADD CONSTRAINT customquestionvalue_pkey PRIMARY KEY (id);


--
-- Name: datafile_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY datafile
    ADD CONSTRAINT datafile_pkey PRIMARY KEY (id);


--
-- Name: datafilecategory_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY datafilecategory
    ADD CONSTRAINT datafilecategory_pkey PRIMARY KEY (id);


--
-- Name: datafiletag_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY datafiletag
    ADD CONSTRAINT datafiletag_pkey PRIMARY KEY (id);


--
-- Name: dataset_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY dataset
    ADD CONSTRAINT dataset_pkey PRIMARY KEY (id);


--
-- Name: datasetfield_controlledvocabularyvalue_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY datasetfield_controlledvocabularyvalue
    ADD CONSTRAINT datasetfield_controlledvocabularyvalue_pkey PRIMARY KEY (datasetfield_id, controlledvocabularyvalues_id);


--
-- Name: datasetfield_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY datasetfield
    ADD CONSTRAINT datasetfield_pkey PRIMARY KEY (id);


--
-- Name: datasetfieldcompoundvalue_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY datasetfieldcompoundvalue
    ADD CONSTRAINT datasetfieldcompoundvalue_pkey PRIMARY KEY (id);


--
-- Name: datasetfielddefaultvalue_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY datasetfielddefaultvalue
    ADD CONSTRAINT datasetfielddefaultvalue_pkey PRIMARY KEY (id);


--
-- Name: datasetfieldtype_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY datasetfieldtype
    ADD CONSTRAINT datasetfieldtype_pkey PRIMARY KEY (id);


--
-- Name: datasetfieldvalue_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY datasetfieldvalue
    ADD CONSTRAINT datasetfieldvalue_pkey PRIMARY KEY (id);


--
-- Name: datasetlinkingdataverse_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY datasetlinkingdataverse
    ADD CONSTRAINT datasetlinkingdataverse_pkey PRIMARY KEY (id);


--
-- Name: datasetlock_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY datasetlock
    ADD CONSTRAINT datasetlock_pkey PRIMARY KEY (id);


--
-- Name: datasetversion_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY datasetversion
    ADD CONSTRAINT datasetversion_pkey PRIMARY KEY (id);


--
-- Name: datasetversionuser_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY datasetversionuser
    ADD CONSTRAINT datasetversionuser_pkey PRIMARY KEY (id);


--
-- Name: datatable_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY datatable
    ADD CONSTRAINT datatable_pkey PRIMARY KEY (id);


--
-- Name: datavariable_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY datavariable
    ADD CONSTRAINT datavariable_pkey PRIMARY KEY (id);


--
-- Name: dataverse_alias_key; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY dataverse
    ADD CONSTRAINT dataverse_alias_key UNIQUE (alias);


--
-- Name: dataverse_metadatablock_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY dataverse_metadatablock
    ADD CONSTRAINT dataverse_metadatablock_pkey PRIMARY KEY (dataverse_id, metadatablocks_id);


--
-- Name: dataverse_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY dataverse
    ADD CONSTRAINT dataverse_pkey PRIMARY KEY (id);


--
-- Name: dataversecontact_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY dataversecontact
    ADD CONSTRAINT dataversecontact_pkey PRIMARY KEY (id);


--
-- Name: dataversefacet_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY dataversefacet
    ADD CONSTRAINT dataversefacet_pkey PRIMARY KEY (id);


--
-- Name: dataversefeatureddataverse_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY dataversefeatureddataverse
    ADD CONSTRAINT dataversefeatureddataverse_pkey PRIMARY KEY (id);


--
-- Name: dataversefieldtypeinputlevel_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY dataversefieldtypeinputlevel
    ADD CONSTRAINT dataversefieldtypeinputlevel_pkey PRIMARY KEY (id);


--
-- Name: dataverselinkingdataverse_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY dataverselinkingdataverse
    ADD CONSTRAINT dataverselinkingdataverse_pkey PRIMARY KEY (id);


--
-- Name: dataverserole_alias_key; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY dataverserole
    ADD CONSTRAINT dataverserole_alias_key UNIQUE (alias);


--
-- Name: dataverserole_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY dataverserole
    ADD CONSTRAINT dataverserole_pkey PRIMARY KEY (id);


--
-- Name: dataversesubjects_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY dataversesubjects
    ADD CONSTRAINT dataversesubjects_pkey PRIMARY KEY (dataverse_id, controlledvocabularyvalue_id);


--
-- Name: dataversetheme_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY dataversetheme
    ADD CONSTRAINT dataversetheme_pkey PRIMARY KEY (id);


--
-- Name: defaultvalueset_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY defaultvalueset
    ADD CONSTRAINT defaultvalueset_pkey PRIMARY KEY (id);


--
-- Name: dvobject_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY dvobject
    ADD CONSTRAINT dvobject_pkey PRIMARY KEY (id);


--
-- Name: explicitgroup_authenticateduser_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY explicitgroup_authenticateduser
    ADD CONSTRAINT explicitgroup_authenticateduser_pkey PRIMARY KEY (explicitgroup_id, containedauthenticatedusers_id);


--
-- Name: explicitgroup_explicitgroup_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY explicitgroup_explicitgroup
    ADD CONSTRAINT explicitgroup_explicitgroup_pkey PRIMARY KEY (explicitgroup_id, containedexplicitgroups_id);


--
-- Name: explicitgroup_groupalias_key; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY explicitgroup
    ADD CONSTRAINT explicitgroup_groupalias_key UNIQUE (groupalias);


--
-- Name: explicitgroup_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY explicitgroup
    ADD CONSTRAINT explicitgroup_pkey PRIMARY KEY (id);


--
-- Name: fileaccessrequests_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY fileaccessrequests
    ADD CONSTRAINT fileaccessrequests_pkey PRIMARY KEY (datafile_id, authenticated_user_id);


--
-- Name: filemetadata_datafilecategory_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY filemetadata_datafilecategory
    ADD CONSTRAINT filemetadata_datafilecategory_pkey PRIMARY KEY (filecategories_id, filemetadatas_id);


--
-- Name: filemetadata_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY filemetadata
    ADD CONSTRAINT filemetadata_pkey PRIMARY KEY (id);


--
-- Name: foreignmetadatafieldmapping_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY foreignmetadatafieldmapping
    ADD CONSTRAINT foreignmetadatafieldmapping_pkey PRIMARY KEY (id);


--
-- Name: foreignmetadataformatmapping_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY foreignmetadataformatmapping
    ADD CONSTRAINT foreignmetadataformatmapping_pkey PRIMARY KEY (id);


--
-- Name: guestbook_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY guestbook
    ADD CONSTRAINT guestbook_pkey PRIMARY KEY (id);


--
-- Name: guestbookresponse_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY guestbookresponse
    ADD CONSTRAINT guestbookresponse_pkey PRIMARY KEY (id);


--
-- Name: harvestingdataverseconfig_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY harvestingdataverseconfig
    ADD CONSTRAINT harvestingdataverseconfig_pkey PRIMARY KEY (id);


--
-- Name: ingestreport_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY ingestreport
    ADD CONSTRAINT ingestreport_pkey PRIMARY KEY (id);


--
-- Name: ingestrequest_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY ingestrequest
    ADD CONSTRAINT ingestrequest_pkey PRIMARY KEY (id);


--
-- Name: ipv4range_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY ipv4range
    ADD CONSTRAINT ipv4range_pkey PRIMARY KEY (id);


--
-- Name: ipv6range_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY ipv6range
    ADD CONSTRAINT ipv6range_pkey PRIMARY KEY (id);


--
-- Name: maplayermetadata_datafile_id_key; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY maplayermetadata
    ADD CONSTRAINT maplayermetadata_datafile_id_key UNIQUE (datafile_id);


--
-- Name: maplayermetadata_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY maplayermetadata
    ADD CONSTRAINT maplayermetadata_pkey PRIMARY KEY (id);


--
-- Name: metadatablock_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY metadatablock
    ADD CONSTRAINT metadatablock_pkey PRIMARY KEY (id);


--
-- Name: passwordresetdata_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY passwordresetdata
    ADD CONSTRAINT passwordresetdata_pkey PRIMARY KEY (id);


--
-- Name: persistedglobalgroup_persistedgroupalias_key; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY persistedglobalgroup
    ADD CONSTRAINT persistedglobalgroup_persistedgroupalias_key UNIQUE (persistedgroupalias);


--
-- Name: persistedglobalgroup_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY persistedglobalgroup
    ADD CONSTRAINT persistedglobalgroup_pkey PRIMARY KEY (id);


--
-- Name: roleassignment_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY roleassignment
    ADD CONSTRAINT roleassignment_pkey PRIMARY KEY (id);


--
-- Name: savedsearch_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY savedsearch
    ADD CONSTRAINT savedsearch_pkey PRIMARY KEY (id);


--
-- Name: savedsearchfilterquery_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY savedsearchfilterquery
    ADD CONSTRAINT savedsearchfilterquery_pkey PRIMARY KEY (id);


--
-- Name: sequence_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY sequence
    ADD CONSTRAINT sequence_pkey PRIMARY KEY (seq_name);


--
-- Name: setting_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY setting
    ADD CONSTRAINT setting_pkey PRIMARY KEY (name);


--
-- Name: shibgroup_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY shibgroup
    ADD CONSTRAINT shibgroup_pkey PRIMARY KEY (id);


--
-- Name: summarystatistic_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY summarystatistic
    ADD CONSTRAINT summarystatistic_pkey PRIMARY KEY (id);


--
-- Name: template_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY template
    ADD CONSTRAINT template_pkey PRIMARY KEY (id);


--
-- Name: unq_authenticateduserlookup_0; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY authenticateduserlookup
    ADD CONSTRAINT unq_authenticateduserlookup_0 UNIQUE (persistentuserid, authenticationproviderid);


--
-- Name: unq_dataset_0; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY dataset
    ADD CONSTRAINT unq_dataset_0 UNIQUE (authority, protocol, identifier, doiseparator);


--
-- Name: unq_dataversefieldtypeinputlevel_0; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY dataversefieldtypeinputlevel
    ADD CONSTRAINT unq_dataversefieldtypeinputlevel_0 UNIQUE (dataverse_id, datasetfieldtype_id);


--
-- Name: unq_foreignmetadatafieldmapping_0; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY foreignmetadatafieldmapping
    ADD CONSTRAINT unq_foreignmetadatafieldmapping_0 UNIQUE (foreignmetadataformatmapping_id, foreignfieldxpath);


--
-- Name: unq_roleassignment_0; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY roleassignment
    ADD CONSTRAINT unq_roleassignment_0 UNIQUE (assigneeidentifier, role_id, definitionpoint_id);


--
-- Name: usernotification_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY usernotification
    ADD CONSTRAINT usernotification_pkey PRIMARY KEY (id);


--
-- Name: variablecategory_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY variablecategory
    ADD CONSTRAINT variablecategory_pkey PRIMARY KEY (id);


--
-- Name: variablerange_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY variablerange
    ADD CONSTRAINT variablerange_pkey PRIMARY KEY (id);


--
-- Name: variablerangeitem_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY variablerangeitem
    ADD CONSTRAINT variablerangeitem_pkey PRIMARY KEY (id);


--
-- Name: worldmapauth_token_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY worldmapauth_token
    ADD CONSTRAINT worldmapauth_token_pkey PRIMARY KEY (id);


--
-- Name: worldmapauth_tokentype_pkey; Type: CONSTRAINT; Schema: public; Owner: dataverse_app; Tablespace: 
--

ALTER TABLE ONLY worldmapauth_tokentype
    ADD CONSTRAINT worldmapauth_tokentype_pkey PRIMARY KEY (id);


--
-- Name: application_name; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE UNIQUE INDEX application_name ON worldmapauth_tokentype USING btree (name);


--
-- Name: index_actionlogrecord_actiontype; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_actionlogrecord_actiontype ON actionlogrecord USING btree (actiontype);


--
-- Name: index_actionlogrecord_starttime; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_actionlogrecord_starttime ON actionlogrecord USING btree (starttime);


--
-- Name: index_actionlogrecord_useridentifier; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_actionlogrecord_useridentifier ON actionlogrecord USING btree (useridentifier);


--
-- Name: index_apitoken_authenticateduser_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_apitoken_authenticateduser_id ON apitoken USING btree (authenticateduser_id);


--
-- Name: index_authenticationproviderrow_enabled; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_authenticationproviderrow_enabled ON authenticationproviderrow USING btree (enabled);


--
-- Name: index_builtinuser_lastname; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_builtinuser_lastname ON builtinuser USING btree (lastname);


--
-- Name: index_controlledvocabalternate_controlledvocabularyvalue_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_controlledvocabalternate_controlledvocabularyvalue_id ON controlledvocabalternate USING btree (controlledvocabularyvalue_id);


--
-- Name: index_controlledvocabalternate_datasetfieldtype_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_controlledvocabalternate_datasetfieldtype_id ON controlledvocabalternate USING btree (datasetfieldtype_id);


--
-- Name: index_controlledvocabularyvalue_datasetfieldtype_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_controlledvocabularyvalue_datasetfieldtype_id ON controlledvocabularyvalue USING btree (datasetfieldtype_id);


--
-- Name: index_controlledvocabularyvalue_displayorder; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_controlledvocabularyvalue_displayorder ON controlledvocabularyvalue USING btree (displayorder);


--
-- Name: index_customfieldmap_sourcedatasetfield; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_customfieldmap_sourcedatasetfield ON customfieldmap USING btree (sourcedatasetfield);


--
-- Name: index_customfieldmap_sourcetemplate; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_customfieldmap_sourcetemplate ON customfieldmap USING btree (sourcetemplate);


--
-- Name: index_customquestion_guestbook_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_customquestion_guestbook_id ON customquestion USING btree (guestbook_id);


--
-- Name: index_customquestionresponse_guestbookresponse_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_customquestionresponse_guestbookresponse_id ON customquestionresponse USING btree (guestbookresponse_id);


--
-- Name: index_datafile_contenttype; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datafile_contenttype ON datafile USING btree (contenttype);


--
-- Name: index_datafile_ingeststatus; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datafile_ingeststatus ON datafile USING btree (ingeststatus);


--
-- Name: index_datafile_md5; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datafile_md5 ON datafile USING btree (md5);


--
-- Name: index_datafile_restricted; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datafile_restricted ON datafile USING btree (restricted);


--
-- Name: index_datafilecategory_dataset_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datafilecategory_dataset_id ON datafilecategory USING btree (dataset_id);


--
-- Name: index_datafiletag_datafile_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datafiletag_datafile_id ON datafiletag USING btree (datafile_id);


--
-- Name: index_dataset_guestbook_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataset_guestbook_id ON dataset USING btree (guestbook_id);


--
-- Name: index_dataset_thumbnailfile_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataset_thumbnailfile_id ON dataset USING btree (thumbnailfile_id);


--
-- Name: index_datasetfield_controlledvocabularyvalue_controlledvocabula; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetfield_controlledvocabularyvalue_controlledvocabula ON datasetfield_controlledvocabularyvalue USING btree (controlledvocabularyvalues_id);


--
-- Name: index_datasetfield_controlledvocabularyvalue_datasetfield_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetfield_controlledvocabularyvalue_datasetfield_id ON datasetfield_controlledvocabularyvalue USING btree (datasetfield_id);


--
-- Name: index_datasetfield_datasetfieldtype_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetfield_datasetfieldtype_id ON datasetfield USING btree (datasetfieldtype_id);


--
-- Name: index_datasetfield_datasetversion_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetfield_datasetversion_id ON datasetfield USING btree (datasetversion_id);


--
-- Name: index_datasetfield_parentdatasetfieldcompoundvalue_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetfield_parentdatasetfieldcompoundvalue_id ON datasetfield USING btree (parentdatasetfieldcompoundvalue_id);


--
-- Name: index_datasetfield_template_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetfield_template_id ON datasetfield USING btree (template_id);


--
-- Name: index_datasetfieldcompoundvalue_parentdatasetfield_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetfieldcompoundvalue_parentdatasetfield_id ON datasetfieldcompoundvalue USING btree (parentdatasetfield_id);


--
-- Name: index_datasetfielddefaultvalue_datasetfield_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetfielddefaultvalue_datasetfield_id ON datasetfielddefaultvalue USING btree (datasetfield_id);


--
-- Name: index_datasetfielddefaultvalue_defaultvalueset_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetfielddefaultvalue_defaultvalueset_id ON datasetfielddefaultvalue USING btree (defaultvalueset_id);


--
-- Name: index_datasetfielddefaultvalue_displayorder; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetfielddefaultvalue_displayorder ON datasetfielddefaultvalue USING btree (displayorder);


--
-- Name: index_datasetfielddefaultvalue_parentdatasetfielddefaultvalue_i; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetfielddefaultvalue_parentdatasetfielddefaultvalue_i ON datasetfielddefaultvalue USING btree (parentdatasetfielddefaultvalue_id);


--
-- Name: index_datasetfieldtype_metadatablock_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetfieldtype_metadatablock_id ON datasetfieldtype USING btree (metadatablock_id);


--
-- Name: index_datasetfieldtype_parentdatasetfieldtype_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetfieldtype_parentdatasetfieldtype_id ON datasetfieldtype USING btree (parentdatasetfieldtype_id);


--
-- Name: index_datasetfieldvalue_datasetfield_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetfieldvalue_datasetfield_id ON datasetfieldvalue USING btree (datasetfield_id);


--
-- Name: index_datasetlinkingdataverse_dataset_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetlinkingdataverse_dataset_id ON datasetlinkingdataverse USING btree (dataset_id);


--
-- Name: index_datasetlinkingdataverse_linkingdataverse_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetlinkingdataverse_linkingdataverse_id ON datasetlinkingdataverse USING btree (linkingdataverse_id);


--
-- Name: index_datasetlock_dataset_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetlock_dataset_id ON datasetlock USING btree (dataset_id);


--
-- Name: index_datasetlock_user_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetlock_user_id ON datasetlock USING btree (user_id);


--
-- Name: index_datasetversion_dataset_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetversion_dataset_id ON datasetversion USING btree (dataset_id);


--
-- Name: index_datasetversionuser_authenticateduser_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetversionuser_authenticateduser_id ON datasetversionuser USING btree (authenticateduser_id);


--
-- Name: index_datasetversionuser_datasetversion_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datasetversionuser_datasetversion_id ON datasetversionuser USING btree (datasetversion_id);


--
-- Name: index_datatable_datafile_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datatable_datafile_id ON datatable USING btree (datafile_id);


--
-- Name: index_datavariable_datatable_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_datavariable_datatable_id ON datavariable USING btree (datatable_id);


--
-- Name: index_dataverse_affiliation; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataverse_affiliation ON dataverse USING btree (affiliation);


--
-- Name: index_dataverse_alias; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataverse_alias ON dataverse USING btree (alias);


--
-- Name: index_dataverse_dataversetype; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataverse_dataversetype ON dataverse USING btree (dataversetype);


--
-- Name: index_dataverse_defaultcontributorrole_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataverse_defaultcontributorrole_id ON dataverse USING btree (defaultcontributorrole_id);


--
-- Name: index_dataverse_defaulttemplate_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataverse_defaulttemplate_id ON dataverse USING btree (defaulttemplate_id);


--
-- Name: index_dataverse_facetroot; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataverse_facetroot ON dataverse USING btree (facetroot);


--
-- Name: index_dataverse_guestbookroot; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataverse_guestbookroot ON dataverse USING btree (guestbookroot);


--
-- Name: index_dataverse_metadatablockroot; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataverse_metadatablockroot ON dataverse USING btree (metadatablockroot);


--
-- Name: index_dataverse_permissionroot; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataverse_permissionroot ON dataverse USING btree (permissionroot);


--
-- Name: index_dataverse_templateroot; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataverse_templateroot ON dataverse USING btree (templateroot);


--
-- Name: index_dataverse_themeroot; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataverse_themeroot ON dataverse USING btree (themeroot);


--
-- Name: index_dataversecontact_contactemail; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataversecontact_contactemail ON dataversecontact USING btree (contactemail);


--
-- Name: index_dataversecontact_dataverse_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataversecontact_dataverse_id ON dataversecontact USING btree (dataverse_id);


--
-- Name: index_dataversecontact_displayorder; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataversecontact_displayorder ON dataversecontact USING btree (displayorder);


--
-- Name: index_dataversefacet_datasetfieldtype_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataversefacet_datasetfieldtype_id ON dataversefacet USING btree (datasetfieldtype_id);


--
-- Name: index_dataversefacet_dataverse_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataversefacet_dataverse_id ON dataversefacet USING btree (dataverse_id);


--
-- Name: index_dataversefacet_displayorder; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataversefacet_displayorder ON dataversefacet USING btree (displayorder);


--
-- Name: index_dataversefeatureddataverse_dataverse_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataversefeatureddataverse_dataverse_id ON dataversefeatureddataverse USING btree (dataverse_id);


--
-- Name: index_dataversefeatureddataverse_displayorder; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataversefeatureddataverse_displayorder ON dataversefeatureddataverse USING btree (displayorder);


--
-- Name: index_dataversefeatureddataverse_featureddataverse_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataversefeatureddataverse_featureddataverse_id ON dataversefeatureddataverse USING btree (featureddataverse_id);


--
-- Name: index_dataversefieldtypeinputlevel_datasetfieldtype_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataversefieldtypeinputlevel_datasetfieldtype_id ON dataversefieldtypeinputlevel USING btree (datasetfieldtype_id);


--
-- Name: index_dataversefieldtypeinputlevel_dataverse_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataversefieldtypeinputlevel_dataverse_id ON dataversefieldtypeinputlevel USING btree (dataverse_id);


--
-- Name: index_dataversefieldtypeinputlevel_required; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataversefieldtypeinputlevel_required ON dataversefieldtypeinputlevel USING btree (required);


--
-- Name: index_dataverselinkingdataverse_dataverse_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataverselinkingdataverse_dataverse_id ON dataverselinkingdataverse USING btree (dataverse_id);


--
-- Name: index_dataverselinkingdataverse_linkingdataverse_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataverselinkingdataverse_linkingdataverse_id ON dataverselinkingdataverse USING btree (linkingdataverse_id);


--
-- Name: index_dataverserole_alias; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataverserole_alias ON dataverserole USING btree (alias);


--
-- Name: index_dataverserole_name; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataverserole_name ON dataverserole USING btree (name);


--
-- Name: index_dataverserole_owner_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataverserole_owner_id ON dataverserole USING btree (owner_id);


--
-- Name: index_dataversetheme_dataverse_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dataversetheme_dataverse_id ON dataversetheme USING btree (dataverse_id);


--
-- Name: index_dvobject_creator_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dvobject_creator_id ON dvobject USING btree (creator_id);


--
-- Name: index_dvobject_dtype; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dvobject_dtype ON dvobject USING btree (dtype);


--
-- Name: index_dvobject_owner_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dvobject_owner_id ON dvobject USING btree (owner_id);


--
-- Name: index_dvobject_releaseuser_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_dvobject_releaseuser_id ON dvobject USING btree (releaseuser_id);


--
-- Name: index_explicitgroup_groupaliasinowner; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_explicitgroup_groupaliasinowner ON explicitgroup USING btree (groupaliasinowner);


--
-- Name: index_explicitgroup_owner_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_explicitgroup_owner_id ON explicitgroup USING btree (owner_id);


--
-- Name: index_filemetadata_datafile_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_filemetadata_datafile_id ON filemetadata USING btree (datafile_id);


--
-- Name: index_filemetadata_datafilecategory_filecategories_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_filemetadata_datafilecategory_filecategories_id ON filemetadata_datafilecategory USING btree (filecategories_id);


--
-- Name: index_filemetadata_datafilecategory_filemetadatas_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_filemetadata_datafilecategory_filemetadatas_id ON filemetadata_datafilecategory USING btree (filemetadatas_id);


--
-- Name: index_filemetadata_datasetversion_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_filemetadata_datasetversion_id ON filemetadata USING btree (datasetversion_id);


--
-- Name: index_foreignmetadatafieldmapping_foreignfieldxpath; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_foreignmetadatafieldmapping_foreignfieldxpath ON foreignmetadatafieldmapping USING btree (foreignfieldxpath);


--
-- Name: index_foreignmetadatafieldmapping_foreignmetadataformatmapping_; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_foreignmetadatafieldmapping_foreignmetadataformatmapping_ ON foreignmetadatafieldmapping USING btree (foreignmetadataformatmapping_id);


--
-- Name: index_foreignmetadatafieldmapping_parentfieldmapping_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_foreignmetadatafieldmapping_parentfieldmapping_id ON foreignmetadatafieldmapping USING btree (parentfieldmapping_id);


--
-- Name: index_foreignmetadataformatmapping_name; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_foreignmetadataformatmapping_name ON foreignmetadataformatmapping USING btree (name);


--
-- Name: index_guestbookresponse_datafile_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_guestbookresponse_datafile_id ON guestbookresponse USING btree (datafile_id);


--
-- Name: index_guestbookresponse_dataset_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_guestbookresponse_dataset_id ON guestbookresponse USING btree (dataset_id);


--
-- Name: index_guestbookresponse_guestbook_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_guestbookresponse_guestbook_id ON guestbookresponse USING btree (guestbook_id);


--
-- Name: index_harvestingdataverseconfig_dataverse_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_harvestingdataverseconfig_dataverse_id ON harvestingdataverseconfig USING btree (dataverse_id);


--
-- Name: index_harvestingdataverseconfig_harvestingurl; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_harvestingdataverseconfig_harvestingurl ON harvestingdataverseconfig USING btree (harvestingurl);


--
-- Name: index_harvestingdataverseconfig_harveststyle; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_harvestingdataverseconfig_harveststyle ON harvestingdataverseconfig USING btree (harveststyle);


--
-- Name: index_harvestingdataverseconfig_harvesttype; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_harvestingdataverseconfig_harvesttype ON harvestingdataverseconfig USING btree (harvesttype);


--
-- Name: index_ingestreport_datafile_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_ingestreport_datafile_id ON ingestreport USING btree (datafile_id);


--
-- Name: index_ingestrequest_datafile_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_ingestrequest_datafile_id ON ingestrequest USING btree (datafile_id);


--
-- Name: index_ipv4range_owner_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_ipv4range_owner_id ON ipv4range USING btree (owner_id);


--
-- Name: index_ipv6range_owner_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_ipv6range_owner_id ON ipv6range USING btree (owner_id);


--
-- Name: index_maplayermetadata_dataset_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_maplayermetadata_dataset_id ON maplayermetadata USING btree (dataset_id);


--
-- Name: index_metadatablock_name; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_metadatablock_name ON metadatablock USING btree (name);


--
-- Name: index_metadatablock_owner_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_metadatablock_owner_id ON metadatablock USING btree (owner_id);


--
-- Name: index_passwordresetdata_builtinuser_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_passwordresetdata_builtinuser_id ON passwordresetdata USING btree (builtinuser_id);


--
-- Name: index_passwordresetdata_token; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_passwordresetdata_token ON passwordresetdata USING btree (token);


--
-- Name: index_persistedglobalgroup_dtype; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_persistedglobalgroup_dtype ON persistedglobalgroup USING btree (dtype);


--
-- Name: index_roleassignment_assigneeidentifier; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_roleassignment_assigneeidentifier ON roleassignment USING btree (assigneeidentifier);


--
-- Name: index_roleassignment_definitionpoint_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_roleassignment_definitionpoint_id ON roleassignment USING btree (definitionpoint_id);


--
-- Name: index_roleassignment_role_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_roleassignment_role_id ON roleassignment USING btree (role_id);


--
-- Name: index_savedsearch_creator_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_savedsearch_creator_id ON savedsearch USING btree (creator_id);


--
-- Name: index_savedsearch_definitionpoint_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_savedsearch_definitionpoint_id ON savedsearch USING btree (definitionpoint_id);


--
-- Name: index_savedsearchfilterquery_savedsearch_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_savedsearchfilterquery_savedsearch_id ON savedsearchfilterquery USING btree (savedsearch_id);


--
-- Name: index_summarystatistic_datavariable_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_summarystatistic_datavariable_id ON summarystatistic USING btree (datavariable_id);


--
-- Name: index_template_dataverse_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_template_dataverse_id ON template USING btree (dataverse_id);


--
-- Name: index_usernotification_user_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_usernotification_user_id ON usernotification USING btree (user_id);


--
-- Name: index_variablecategory_datavariable_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_variablecategory_datavariable_id ON variablecategory USING btree (datavariable_id);


--
-- Name: index_variablerange_datavariable_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_variablerange_datavariable_id ON variablerange USING btree (datavariable_id);


--
-- Name: index_variablerangeitem_datavariable_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_variablerangeitem_datavariable_id ON variablerangeitem USING btree (datavariable_id);


--
-- Name: index_worldmapauth_token_application_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_worldmapauth_token_application_id ON worldmapauth_token USING btree (application_id);


--
-- Name: index_worldmapauth_token_datafile_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_worldmapauth_token_datafile_id ON worldmapauth_token USING btree (datafile_id);


--
-- Name: index_worldmapauth_token_dataverseuser_id; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE INDEX index_worldmapauth_token_dataverseuser_id ON worldmapauth_token USING btree (dataverseuser_id);


--
-- Name: token_value; Type: INDEX; Schema: public; Owner: dataverse_app; Tablespace: 
--

CREATE UNIQUE INDEX token_value ON worldmapauth_token USING btree (token);


--
-- Name: dtasetfieldcontrolledvocabularyvaluecntrolledvocabularyvaluesid; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfield_controlledvocabularyvalue
    ADD CONSTRAINT dtasetfieldcontrolledvocabularyvaluecntrolledvocabularyvaluesid FOREIGN KEY (controlledvocabularyvalues_id) REFERENCES controlledvocabularyvalue(id);


--
-- Name: explicitgroup_authenticateduser_containedauthenticatedusers_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY explicitgroup_authenticateduser
    ADD CONSTRAINT explicitgroup_authenticateduser_containedauthenticatedusers_id FOREIGN KEY (containedauthenticatedusers_id) REFERENCES authenticateduser(id);


--
-- Name: fk_apitoken_authenticateduser_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY apitoken
    ADD CONSTRAINT fk_apitoken_authenticateduser_id FOREIGN KEY (authenticateduser_id) REFERENCES authenticateduser(id);


--
-- Name: fk_authenticateduserlookup_authenticateduser_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY authenticateduserlookup
    ADD CONSTRAINT fk_authenticateduserlookup_authenticateduser_id FOREIGN KEY (authenticateduser_id) REFERENCES authenticateduser(id);


--
-- Name: fk_controlledvocabalternate_controlledvocabularyvalue_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY controlledvocabalternate
    ADD CONSTRAINT fk_controlledvocabalternate_controlledvocabularyvalue_id FOREIGN KEY (controlledvocabularyvalue_id) REFERENCES controlledvocabularyvalue(id);


--
-- Name: fk_controlledvocabalternate_datasetfieldtype_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY controlledvocabalternate
    ADD CONSTRAINT fk_controlledvocabalternate_datasetfieldtype_id FOREIGN KEY (datasetfieldtype_id) REFERENCES datasetfieldtype(id);


--
-- Name: fk_controlledvocabularyvalue_datasetfieldtype_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY controlledvocabularyvalue
    ADD CONSTRAINT fk_controlledvocabularyvalue_datasetfieldtype_id FOREIGN KEY (datasetfieldtype_id) REFERENCES datasetfieldtype(id);


--
-- Name: fk_customquestion_guestbook_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY customquestion
    ADD CONSTRAINT fk_customquestion_guestbook_id FOREIGN KEY (guestbook_id) REFERENCES guestbook(id);


--
-- Name: fk_customquestionresponse_customquestion_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY customquestionresponse
    ADD CONSTRAINT fk_customquestionresponse_customquestion_id FOREIGN KEY (customquestion_id) REFERENCES customquestion(id);


--
-- Name: fk_customquestionresponse_guestbookresponse_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY customquestionresponse
    ADD CONSTRAINT fk_customquestionresponse_guestbookresponse_id FOREIGN KEY (guestbookresponse_id) REFERENCES guestbookresponse(id);


--
-- Name: fk_customquestionvalue_customquestion_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY customquestionvalue
    ADD CONSTRAINT fk_customquestionvalue_customquestion_id FOREIGN KEY (customquestion_id) REFERENCES customquestion(id);


--
-- Name: fk_datafile_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datafile
    ADD CONSTRAINT fk_datafile_id FOREIGN KEY (id) REFERENCES dvobject(id);


--
-- Name: fk_datafilecategory_dataset_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datafilecategory
    ADD CONSTRAINT fk_datafilecategory_dataset_id FOREIGN KEY (dataset_id) REFERENCES dvobject(id);


--
-- Name: fk_datafiletag_datafile_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datafiletag
    ADD CONSTRAINT fk_datafiletag_datafile_id FOREIGN KEY (datafile_id) REFERENCES dvobject(id);


--
-- Name: fk_dataset_guestbook_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataset
    ADD CONSTRAINT fk_dataset_guestbook_id FOREIGN KEY (guestbook_id) REFERENCES guestbook(id);


--
-- Name: fk_dataset_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataset
    ADD CONSTRAINT fk_dataset_id FOREIGN KEY (id) REFERENCES dvobject(id);


--
-- Name: fk_dataset_thumbnailfile_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataset
    ADD CONSTRAINT fk_dataset_thumbnailfile_id FOREIGN KEY (thumbnailfile_id) REFERENCES dvobject(id);


--
-- Name: fk_datasetfield_controlledvocabularyvalue_datasetfield_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfield_controlledvocabularyvalue
    ADD CONSTRAINT fk_datasetfield_controlledvocabularyvalue_datasetfield_id FOREIGN KEY (datasetfield_id) REFERENCES datasetfield(id);


--
-- Name: fk_datasetfield_datasetfieldtype_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfield
    ADD CONSTRAINT fk_datasetfield_datasetfieldtype_id FOREIGN KEY (datasetfieldtype_id) REFERENCES datasetfieldtype(id);


--
-- Name: fk_datasetfield_datasetversion_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfield
    ADD CONSTRAINT fk_datasetfield_datasetversion_id FOREIGN KEY (datasetversion_id) REFERENCES datasetversion(id);


--
-- Name: fk_datasetfield_parentdatasetfieldcompoundvalue_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfield
    ADD CONSTRAINT fk_datasetfield_parentdatasetfieldcompoundvalue_id FOREIGN KEY (parentdatasetfieldcompoundvalue_id) REFERENCES datasetfieldcompoundvalue(id);


--
-- Name: fk_datasetfield_template_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfield
    ADD CONSTRAINT fk_datasetfield_template_id FOREIGN KEY (template_id) REFERENCES template(id);


--
-- Name: fk_datasetfieldcompoundvalue_parentdatasetfield_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfieldcompoundvalue
    ADD CONSTRAINT fk_datasetfieldcompoundvalue_parentdatasetfield_id FOREIGN KEY (parentdatasetfield_id) REFERENCES datasetfield(id);


--
-- Name: fk_datasetfielddefaultvalue_datasetfield_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfielddefaultvalue
    ADD CONSTRAINT fk_datasetfielddefaultvalue_datasetfield_id FOREIGN KEY (datasetfield_id) REFERENCES datasetfieldtype(id);


--
-- Name: fk_datasetfielddefaultvalue_defaultvalueset_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfielddefaultvalue
    ADD CONSTRAINT fk_datasetfielddefaultvalue_defaultvalueset_id FOREIGN KEY (defaultvalueset_id) REFERENCES defaultvalueset(id);


--
-- Name: fk_datasetfielddefaultvalue_parentdatasetfielddefaultvalue_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfielddefaultvalue
    ADD CONSTRAINT fk_datasetfielddefaultvalue_parentdatasetfielddefaultvalue_id FOREIGN KEY (parentdatasetfielddefaultvalue_id) REFERENCES datasetfielddefaultvalue(id);


--
-- Name: fk_datasetfieldtype_metadatablock_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfieldtype
    ADD CONSTRAINT fk_datasetfieldtype_metadatablock_id FOREIGN KEY (metadatablock_id) REFERENCES metadatablock(id);


--
-- Name: fk_datasetfieldtype_parentdatasetfieldtype_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfieldtype
    ADD CONSTRAINT fk_datasetfieldtype_parentdatasetfieldtype_id FOREIGN KEY (parentdatasetfieldtype_id) REFERENCES datasetfieldtype(id);


--
-- Name: fk_datasetfieldvalue_datasetfield_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetfieldvalue
    ADD CONSTRAINT fk_datasetfieldvalue_datasetfield_id FOREIGN KEY (datasetfield_id) REFERENCES datasetfield(id);


--
-- Name: fk_datasetlinkingdataverse_dataset_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetlinkingdataverse
    ADD CONSTRAINT fk_datasetlinkingdataverse_dataset_id FOREIGN KEY (dataset_id) REFERENCES dvobject(id);


--
-- Name: fk_datasetlinkingdataverse_linkingdataverse_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetlinkingdataverse
    ADD CONSTRAINT fk_datasetlinkingdataverse_linkingdataverse_id FOREIGN KEY (linkingdataverse_id) REFERENCES dvobject(id);


--
-- Name: fk_datasetlock_dataset_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetlock
    ADD CONSTRAINT fk_datasetlock_dataset_id FOREIGN KEY (dataset_id) REFERENCES dvobject(id);


--
-- Name: fk_datasetlock_user_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetlock
    ADD CONSTRAINT fk_datasetlock_user_id FOREIGN KEY (user_id) REFERENCES authenticateduser(id);


--
-- Name: fk_datasetversion_dataset_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetversion
    ADD CONSTRAINT fk_datasetversion_dataset_id FOREIGN KEY (dataset_id) REFERENCES dvobject(id);


--
-- Name: fk_datasetversionuser_authenticateduser_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetversionuser
    ADD CONSTRAINT fk_datasetversionuser_authenticateduser_id FOREIGN KEY (authenticateduser_id) REFERENCES authenticateduser(id);


--
-- Name: fk_datasetversionuser_datasetversion_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datasetversionuser
    ADD CONSTRAINT fk_datasetversionuser_datasetversion_id FOREIGN KEY (datasetversion_id) REFERENCES datasetversion(id);


--
-- Name: fk_datatable_datafile_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datatable
    ADD CONSTRAINT fk_datatable_datafile_id FOREIGN KEY (datafile_id) REFERENCES dvobject(id);


--
-- Name: fk_datavariable_datatable_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY datavariable
    ADD CONSTRAINT fk_datavariable_datatable_id FOREIGN KEY (datatable_id) REFERENCES datatable(id);


--
-- Name: fk_dataverse_defaultcontributorrole_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataverse
    ADD CONSTRAINT fk_dataverse_defaultcontributorrole_id FOREIGN KEY (defaultcontributorrole_id) REFERENCES dataverserole(id);


--
-- Name: fk_dataverse_defaulttemplate_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataverse
    ADD CONSTRAINT fk_dataverse_defaulttemplate_id FOREIGN KEY (defaulttemplate_id) REFERENCES template(id);


--
-- Name: fk_dataverse_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataverse
    ADD CONSTRAINT fk_dataverse_id FOREIGN KEY (id) REFERENCES dvobject(id);


--
-- Name: fk_dataverse_metadatablock_dataverse_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataverse_metadatablock
    ADD CONSTRAINT fk_dataverse_metadatablock_dataverse_id FOREIGN KEY (dataverse_id) REFERENCES dvobject(id);


--
-- Name: fk_dataverse_metadatablock_metadatablocks_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataverse_metadatablock
    ADD CONSTRAINT fk_dataverse_metadatablock_metadatablocks_id FOREIGN KEY (metadatablocks_id) REFERENCES metadatablock(id);


--
-- Name: fk_dataversecontact_dataverse_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataversecontact
    ADD CONSTRAINT fk_dataversecontact_dataverse_id FOREIGN KEY (dataverse_id) REFERENCES dvobject(id);


--
-- Name: fk_dataversefacet_datasetfieldtype_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataversefacet
    ADD CONSTRAINT fk_dataversefacet_datasetfieldtype_id FOREIGN KEY (datasetfieldtype_id) REFERENCES datasetfieldtype(id);


--
-- Name: fk_dataversefacet_dataverse_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataversefacet
    ADD CONSTRAINT fk_dataversefacet_dataverse_id FOREIGN KEY (dataverse_id) REFERENCES dvobject(id);


--
-- Name: fk_dataversefeatureddataverse_dataverse_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataversefeatureddataverse
    ADD CONSTRAINT fk_dataversefeatureddataverse_dataverse_id FOREIGN KEY (dataverse_id) REFERENCES dvobject(id);


--
-- Name: fk_dataversefeatureddataverse_featureddataverse_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataversefeatureddataverse
    ADD CONSTRAINT fk_dataversefeatureddataverse_featureddataverse_id FOREIGN KEY (featureddataverse_id) REFERENCES dvobject(id);


--
-- Name: fk_dataversefieldtypeinputlevel_datasetfieldtype_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataversefieldtypeinputlevel
    ADD CONSTRAINT fk_dataversefieldtypeinputlevel_datasetfieldtype_id FOREIGN KEY (datasetfieldtype_id) REFERENCES datasetfieldtype(id);


--
-- Name: fk_dataversefieldtypeinputlevel_dataverse_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataversefieldtypeinputlevel
    ADD CONSTRAINT fk_dataversefieldtypeinputlevel_dataverse_id FOREIGN KEY (dataverse_id) REFERENCES dvobject(id);


--
-- Name: fk_dataverselinkingdataverse_dataverse_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataverselinkingdataverse
    ADD CONSTRAINT fk_dataverselinkingdataverse_dataverse_id FOREIGN KEY (dataverse_id) REFERENCES dvobject(id);


--
-- Name: fk_dataverselinkingdataverse_linkingdataverse_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataverselinkingdataverse
    ADD CONSTRAINT fk_dataverselinkingdataverse_linkingdataverse_id FOREIGN KEY (linkingdataverse_id) REFERENCES dvobject(id);


--
-- Name: fk_dataverserole_owner_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataverserole
    ADD CONSTRAINT fk_dataverserole_owner_id FOREIGN KEY (owner_id) REFERENCES dvobject(id);


--
-- Name: fk_dataversesubjects_controlledvocabularyvalue_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataversesubjects
    ADD CONSTRAINT fk_dataversesubjects_controlledvocabularyvalue_id FOREIGN KEY (controlledvocabularyvalue_id) REFERENCES controlledvocabularyvalue(id);


--
-- Name: fk_dataversesubjects_dataverse_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataversesubjects
    ADD CONSTRAINT fk_dataversesubjects_dataverse_id FOREIGN KEY (dataverse_id) REFERENCES dvobject(id);


--
-- Name: fk_dataversetheme_dataverse_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dataversetheme
    ADD CONSTRAINT fk_dataversetheme_dataverse_id FOREIGN KEY (dataverse_id) REFERENCES dvobject(id);


--
-- Name: fk_dvobject_creator_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dvobject
    ADD CONSTRAINT fk_dvobject_creator_id FOREIGN KEY (creator_id) REFERENCES authenticateduser(id);


--
-- Name: fk_dvobject_owner_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dvobject
    ADD CONSTRAINT fk_dvobject_owner_id FOREIGN KEY (owner_id) REFERENCES dvobject(id);


--
-- Name: fk_dvobject_releaseuser_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY dvobject
    ADD CONSTRAINT fk_dvobject_releaseuser_id FOREIGN KEY (releaseuser_id) REFERENCES authenticateduser(id);


--
-- Name: fk_explicitgroup_authenticateduser_explicitgroup_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY explicitgroup_authenticateduser
    ADD CONSTRAINT fk_explicitgroup_authenticateduser_explicitgroup_id FOREIGN KEY (explicitgroup_id) REFERENCES explicitgroup(id);


--
-- Name: fk_explicitgroup_containedroleassignees_explicitgroup_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY explicitgroup_containedroleassignees
    ADD CONSTRAINT fk_explicitgroup_containedroleassignees_explicitgroup_id FOREIGN KEY (explicitgroup_id) REFERENCES explicitgroup(id);


--
-- Name: fk_explicitgroup_explicitgroup_containedexplicitgroups_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY explicitgroup_explicitgroup
    ADD CONSTRAINT fk_explicitgroup_explicitgroup_containedexplicitgroups_id FOREIGN KEY (containedexplicitgroups_id) REFERENCES explicitgroup(id);


--
-- Name: fk_explicitgroup_explicitgroup_explicitgroup_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY explicitgroup_explicitgroup
    ADD CONSTRAINT fk_explicitgroup_explicitgroup_explicitgroup_id FOREIGN KEY (explicitgroup_id) REFERENCES explicitgroup(id);


--
-- Name: fk_explicitgroup_owner_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY explicitgroup
    ADD CONSTRAINT fk_explicitgroup_owner_id FOREIGN KEY (owner_id) REFERENCES dvobject(id);


--
-- Name: fk_fileaccessrequests_authenticated_user_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY fileaccessrequests
    ADD CONSTRAINT fk_fileaccessrequests_authenticated_user_id FOREIGN KEY (authenticated_user_id) REFERENCES authenticateduser(id);


--
-- Name: fk_fileaccessrequests_datafile_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY fileaccessrequests
    ADD CONSTRAINT fk_fileaccessrequests_datafile_id FOREIGN KEY (datafile_id) REFERENCES dvobject(id);


--
-- Name: fk_filemetadata_datafile_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY filemetadata
    ADD CONSTRAINT fk_filemetadata_datafile_id FOREIGN KEY (datafile_id) REFERENCES dvobject(id);


--
-- Name: fk_filemetadata_datafilecategory_filecategories_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY filemetadata_datafilecategory
    ADD CONSTRAINT fk_filemetadata_datafilecategory_filecategories_id FOREIGN KEY (filecategories_id) REFERENCES datafilecategory(id);


--
-- Name: fk_filemetadata_datafilecategory_filemetadatas_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY filemetadata_datafilecategory
    ADD CONSTRAINT fk_filemetadata_datafilecategory_filemetadatas_id FOREIGN KEY (filemetadatas_id) REFERENCES filemetadata(id);


--
-- Name: fk_filemetadata_datasetversion_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY filemetadata
    ADD CONSTRAINT fk_filemetadata_datasetversion_id FOREIGN KEY (datasetversion_id) REFERENCES datasetversion(id);


--
-- Name: fk_foreignmetadatafieldmapping_foreignmetadataformatmapping_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY foreignmetadatafieldmapping
    ADD CONSTRAINT fk_foreignmetadatafieldmapping_foreignmetadataformatmapping_id FOREIGN KEY (foreignmetadataformatmapping_id) REFERENCES foreignmetadataformatmapping(id);


--
-- Name: fk_foreignmetadatafieldmapping_parentfieldmapping_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY foreignmetadatafieldmapping
    ADD CONSTRAINT fk_foreignmetadatafieldmapping_parentfieldmapping_id FOREIGN KEY (parentfieldmapping_id) REFERENCES foreignmetadatafieldmapping(id);


--
-- Name: fk_guestbook_dataverse_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY guestbook
    ADD CONSTRAINT fk_guestbook_dataverse_id FOREIGN KEY (dataverse_id) REFERENCES dvobject(id);


--
-- Name: fk_guestbookresponse_authenticateduser_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY guestbookresponse
    ADD CONSTRAINT fk_guestbookresponse_authenticateduser_id FOREIGN KEY (authenticateduser_id) REFERENCES authenticateduser(id);


--
-- Name: fk_guestbookresponse_datafile_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY guestbookresponse
    ADD CONSTRAINT fk_guestbookresponse_datafile_id FOREIGN KEY (datafile_id) REFERENCES dvobject(id);


--
-- Name: fk_guestbookresponse_dataset_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY guestbookresponse
    ADD CONSTRAINT fk_guestbookresponse_dataset_id FOREIGN KEY (dataset_id) REFERENCES dvobject(id);


--
-- Name: fk_guestbookresponse_datasetversion_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY guestbookresponse
    ADD CONSTRAINT fk_guestbookresponse_datasetversion_id FOREIGN KEY (datasetversion_id) REFERENCES datasetversion(id);


--
-- Name: fk_guestbookresponse_guestbook_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY guestbookresponse
    ADD CONSTRAINT fk_guestbookresponse_guestbook_id FOREIGN KEY (guestbook_id) REFERENCES guestbook(id);


--
-- Name: fk_harvestingdataverseconfig_dataverse_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY harvestingdataverseconfig
    ADD CONSTRAINT fk_harvestingdataverseconfig_dataverse_id FOREIGN KEY (dataverse_id) REFERENCES dvobject(id);


--
-- Name: fk_ingestreport_datafile_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY ingestreport
    ADD CONSTRAINT fk_ingestreport_datafile_id FOREIGN KEY (datafile_id) REFERENCES dvobject(id);


--
-- Name: fk_ingestrequest_datafile_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY ingestrequest
    ADD CONSTRAINT fk_ingestrequest_datafile_id FOREIGN KEY (datafile_id) REFERENCES dvobject(id);


--
-- Name: fk_ipv4range_owner_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY ipv4range
    ADD CONSTRAINT fk_ipv4range_owner_id FOREIGN KEY (owner_id) REFERENCES persistedglobalgroup(id);


--
-- Name: fk_ipv6range_owner_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY ipv6range
    ADD CONSTRAINT fk_ipv6range_owner_id FOREIGN KEY (owner_id) REFERENCES persistedglobalgroup(id);


--
-- Name: fk_maplayermetadata_datafile_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY maplayermetadata
    ADD CONSTRAINT fk_maplayermetadata_datafile_id FOREIGN KEY (datafile_id) REFERENCES dvobject(id);


--
-- Name: fk_maplayermetadata_dataset_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY maplayermetadata
    ADD CONSTRAINT fk_maplayermetadata_dataset_id FOREIGN KEY (dataset_id) REFERENCES dvobject(id);


--
-- Name: fk_metadatablock_owner_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY metadatablock
    ADD CONSTRAINT fk_metadatablock_owner_id FOREIGN KEY (owner_id) REFERENCES dvobject(id);


--
-- Name: fk_passwordresetdata_builtinuser_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY passwordresetdata
    ADD CONSTRAINT fk_passwordresetdata_builtinuser_id FOREIGN KEY (builtinuser_id) REFERENCES builtinuser(id);


--
-- Name: fk_roleassignment_definitionpoint_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY roleassignment
    ADD CONSTRAINT fk_roleassignment_definitionpoint_id FOREIGN KEY (definitionpoint_id) REFERENCES dvobject(id);


--
-- Name: fk_roleassignment_role_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY roleassignment
    ADD CONSTRAINT fk_roleassignment_role_id FOREIGN KEY (role_id) REFERENCES dataverserole(id);


--
-- Name: fk_savedsearch_creator_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY savedsearch
    ADD CONSTRAINT fk_savedsearch_creator_id FOREIGN KEY (creator_id) REFERENCES authenticateduser(id);


--
-- Name: fk_savedsearch_definitionpoint_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY savedsearch
    ADD CONSTRAINT fk_savedsearch_definitionpoint_id FOREIGN KEY (definitionpoint_id) REFERENCES dvobject(id);


--
-- Name: fk_savedsearchfilterquery_savedsearch_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY savedsearchfilterquery
    ADD CONSTRAINT fk_savedsearchfilterquery_savedsearch_id FOREIGN KEY (savedsearch_id) REFERENCES savedsearch(id);


--
-- Name: fk_summarystatistic_datavariable_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY summarystatistic
    ADD CONSTRAINT fk_summarystatistic_datavariable_id FOREIGN KEY (datavariable_id) REFERENCES datavariable(id);


--
-- Name: fk_template_dataverse_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY template
    ADD CONSTRAINT fk_template_dataverse_id FOREIGN KEY (dataverse_id) REFERENCES dvobject(id);


--
-- Name: fk_usernotification_user_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY usernotification
    ADD CONSTRAINT fk_usernotification_user_id FOREIGN KEY (user_id) REFERENCES authenticateduser(id);


--
-- Name: fk_variablecategory_datavariable_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY variablecategory
    ADD CONSTRAINT fk_variablecategory_datavariable_id FOREIGN KEY (datavariable_id) REFERENCES datavariable(id);


--
-- Name: fk_variablerange_datavariable_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY variablerange
    ADD CONSTRAINT fk_variablerange_datavariable_id FOREIGN KEY (datavariable_id) REFERENCES datavariable(id);


--
-- Name: fk_variablerangeitem_datavariable_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY variablerangeitem
    ADD CONSTRAINT fk_variablerangeitem_datavariable_id FOREIGN KEY (datavariable_id) REFERENCES datavariable(id);


--
-- Name: fk_worldmapauth_token_application_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY worldmapauth_token
    ADD CONSTRAINT fk_worldmapauth_token_application_id FOREIGN KEY (application_id) REFERENCES worldmapauth_tokentype(id);


--
-- Name: fk_worldmapauth_token_datafile_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY worldmapauth_token
    ADD CONSTRAINT fk_worldmapauth_token_datafile_id FOREIGN KEY (datafile_id) REFERENCES dvobject(id);


--
-- Name: fk_worldmapauth_token_dataverseuser_id; Type: FK CONSTRAINT; Schema: public; Owner: dataverse_app
--

ALTER TABLE ONLY worldmapauth_token
    ADD CONSTRAINT fk_worldmapauth_token_dataverseuser_id FOREIGN KEY (dataverseuser_id) REFERENCES authenticateduser(id);


--
-- Name: public; Type: ACL; Schema: -; Owner: michael
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM michael;
GRANT ALL ON SCHEMA public TO michael;
GRANT ALL ON SCHEMA public TO dataverse_app;


--
-- PostgreSQL database dump complete
--

