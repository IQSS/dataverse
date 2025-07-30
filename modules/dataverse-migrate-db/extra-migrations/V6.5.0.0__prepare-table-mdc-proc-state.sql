-- This is a workaround for the missing MakeDataCount tables in migration V6.5.0.10

CREATE TABLE IF NOT EXISTS MakeDataCountProcessState
(
    id                   SERIAL PRIMARY KEY,
    yearMonth            VARCHAR(255) NOT NULL,
    state                INTEGER NOT NULL,
    stateChangeTimestamp TIMESTAMP WITHOUT TIME ZONE,
    server               VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS index_makedatacountprocessstate_yearmonth ON MakeDataCountProcessState (yearMonth);
