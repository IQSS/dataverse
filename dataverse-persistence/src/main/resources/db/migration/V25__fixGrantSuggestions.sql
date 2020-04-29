ALTER TABLE IF EXISTS grantsuggestions RENAME TO grantsuggestion;
ALTER TABLE IF EXISTS grantsuggestion RENAME COLUMN grantAgency TO grantagency;
ALTER TABLE IF EXISTS grantsuggestion RENAME COLUMN grantAgencyAcronym TO grantagencyacronym;
ALTER TABLE IF EXISTS grantsuggestion RENAME COLUMN fundingProgram TO fundingprogram;
ALTER TABLE IF EXISTS grantsuggestion RENAME COLUMN foreignName TO suggestionname;
ALTER TABLE IF EXISTS grantsuggestion RENAME COLUMN foreignNameLocale TO suggestionnamelocale;
ALTER TABLE IF EXISTS grantsuggestion ALTER COLUMN fundingprogram DROP NOT NULL;