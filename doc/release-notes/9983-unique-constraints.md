This release adds two missing database constraints that will assure that the externalvocabularyvalue table only has one entry for each uri and that the oaiset table only has one set for each spec. (In the very unlikely case that your existing database has duplicate entries now, install would fail. This can be checked by running

SELECT uri, count(*) FROM externalvocabularyvaluet group by uri;

and

SELECT spec, count(*) FROM oaiset group by spec;

and then removing any duplicate rows (where count>1).




TODO: Whoever puts the release notes together should make sure there is the standard note about reloading metadata blocks for the citation, astrophysics, and biomedical blocks (plus any others from other PRs) after upgrading.