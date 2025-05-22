-- This is a workaround for the missing externalvocabularyvalue table
create table if not exists externalvocabularyvalue
(
    id             serial primary key,
    lastupdatedate timestamp,
    uri            text constraint externalvocabularvalue_uri_key unique,
    value          text
);

create index if not exists index_externalvocabularyvalue_uri on externalvocabularyvalue (uri);


