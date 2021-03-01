CREATE UNIQUE INDEX IF NOT EXISTS index_authenticateduser_lower_useridentifier ON authenticateduser (lower(useridentifier));
