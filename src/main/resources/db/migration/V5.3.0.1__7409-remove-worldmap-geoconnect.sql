-- psql:src/main/resources/db/migration/V5.3.0.1__7409-remove-worldmap-geoconnect.sql:5: ERROR:  cannot drop table worldmapauth_tokentype because other objects depend on it
-- DETAIL:  constraint fk_worldmapauth_token_application_id on table worldmapauth_token depends on table worldmapauth_tokentype
-- HINT:  Use DROP ... CASCADE to drop the dependent objects too.
drop table if exists worldmapauth_tokentype cascade;
drop table if exists worldmapauth_token;
drop table if exists maplayermetadata;
