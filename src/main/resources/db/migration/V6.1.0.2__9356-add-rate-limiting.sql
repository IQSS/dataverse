ALTER TABLE authenticateduser ADD COLUMN IF NOT EXISTS ratelimittier int DEFAULT 1;
UPDATE authenticateduser set ratelimittier = 1 WHERE ratelimittier = 0;