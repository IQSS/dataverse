
UPDATE datasetfieldtype SET
    inputrenderertype='SUGGESTION_TEXT',
    inputrendereroptions='{"suggestionSourceClass":"RorSuggestionHandler", "suggestionDisplayType": "TWO_COLUMNS"}'
WHERE name='authorAffiliationIdentifier';

UPDATE datasetfieldtype SET
    inputrenderertype='SUGGESTION_TEXT',
    inputrendereroptions='{"suggestionSourceClass":"RorSuggestionHandler", "suggestionDisplayType": "TWO_COLUMNS"}'
WHERE name='grantNumberAgencyIdentifier';

UPDATE datasetfieldtype SET
    inputrenderertype='SUGGESTION_TEXT',
    inputrendereroptions='{"suggestionSourceClass":"GrantAgencySuggestionHandler"}'
WHERE name='grantNumberAgency';

UPDATE datasetfieldtype SET
    inputrenderertype='SUGGESTION_TEXT',
    inputrendereroptions='{"suggestionFilteredBy":["grantNumberAgency:suggestionName"], "suggestionSourceClass":"GrantAgencyAcronymSuggestionHandler"}'
WHERE name='grantNumberAgencyShortName';

UPDATE datasetfieldtype SET
    inputrenderertype='SUGGESTION_TEXT',
    inputrendereroptions='{"suggestionFilteredBy":["grantNumberAgency:suggestionName"], "suggestionSourceClass":"GrantProgramSuggestionHandler"}'
WHERE name='grantNumberProgram';

