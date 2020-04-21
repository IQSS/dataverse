CREATE TABLE IF NOT EXISTS grantSuggestions (
    id SERIAL NOT NULL,
    grantAgency varchar(255) NOT NULL,
    grantAgencyAcronym varchar(255) NOT NULL,
    fundingProgram varchar(255) NOT NULL,
    foreignName varchar(255) NOT NULL,
    foreignNameLocale varchar(255) NOT NULL
);