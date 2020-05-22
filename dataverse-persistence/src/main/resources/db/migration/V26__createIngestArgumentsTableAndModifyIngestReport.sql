CREATE TABLE ingestreport_reportarguments
(
    ingestreport_id bigint
        constraint fk_ingestreport_reportarguments_ingestreport_id
            references ingestreport,
    reportarguments varchar,
    reportarguments_order integer
);

ALTER TABLE ingestreport RENAME COLUMN report TO errorkey;

UPDATE ingestreport SET errorkey = 'UNKNOWN_ERROR';
