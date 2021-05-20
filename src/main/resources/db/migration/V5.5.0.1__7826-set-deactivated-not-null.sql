/* 
Add not null constraint to deactivated column of authenticated user
 */

ALTER TABLE authenticateduser
ALTER COLUMN deactivated SET NOT NULL;

