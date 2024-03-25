CREATE TABLE IF NOT EXISTS makedatacountprocessstate (
    id  SERIAL NOT NULL,
    yearMonth VARCHAR(16) NOT NULL UNIQUE,
    state ENUM('new', 'done', 'skip', 'processing', 'failed') NOT NULL,
    state_change_time TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    PRIMARY KEY (ID)
    );

CREATE INDEX IF NOT EXISTS INDEX_makedatacountprocessstate_yearMonth ON makedatacountprocessstate (yearMonth);

