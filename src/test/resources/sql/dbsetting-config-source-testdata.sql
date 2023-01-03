CREATE TABLE Setting (
    id            char(5) CONSTRAINT firstkey PRIMARY KEY,
    name          varchar(40) NOT NULL,
    content       varchar(40) NOT NULL,
    lang          varchar(40) DEFAULT NULL
);

INSERT INTO Setting (id, name, content)
    VALUES
        (1, 'SystemEmail', 'foobar@example.org')
    ON CONFLICT DO NOTHING;