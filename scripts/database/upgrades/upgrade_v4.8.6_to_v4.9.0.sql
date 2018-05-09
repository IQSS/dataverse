ALTER TABLE externaltool ADD COLUMN type character varying(255);
ALTER TABLE externaltool ALTER COLUMN type SET NOT NULL;
-- Previously, the only explore tool was TwoRavens. We now persist the name of the tool.
UPDATE guestbookresponse SET downloadtype = 'TwoRavens' WHERE downloadtype = 'Explore';
ALTER TABLE filemetadata ADD COLUMN prov_freeform text;
-- ALTER TABLE datafile ADD COLUMN prov_cplid int;
ALTER TABLE datafile ADD COLUMN prov_entityname text;


--MAD: Creates with wrong owner,
--MAD: Do I need to grant on creation?
CREATE TABLE METRIC (
    ID  SERIAL NOT NULL, --MAD: SYSTEM IS COMPLAINING ABOUT NO PRIMARY KEY
    metricName character varying(255) NOT NULL,
--    metricMonth integer NOT NULL,
--    metricYear integer NOT NULL,
    metricValue integer NOT NULL, --MAD: SHOULD THIS BE A LONG? HOW DOES THAT WORK?
    lastCalledDate timestamp without time zone NOT NULL, --MAD: Will use for non-month metrics
    CONSTRAINT metric_pkey PRIMARY KEY (id),
    CONSTRAINT metric_name_key UNIQUE (metricName)
);