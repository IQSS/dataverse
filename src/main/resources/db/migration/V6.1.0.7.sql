ALTER TABLE authenticateduser ADD COLUMN IF NOT EXISTS ratelimittier int DEFAULT 1;
