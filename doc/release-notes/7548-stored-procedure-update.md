### Upgrade Notes

**If your installation relies on the database-side stored procedure for generating sequential numeric identifiers:**

*(Note: You can skip the following paragraph if your installation uses the default-style, randomly-generated six alphanumeric 
character-long identifiers for your datasets!)*

The underlying database framework has been modified in this release, to make it easier for installations 
to create custom procedures for generating identifier strings that suit their needs. Your current configuration will 
be automatically updated by the database upgrade (Flyway) script incorporated in the release. No manual configuration 
changes should be necessary. However, after the upgrade, we recommend that you confirm that your installation can still 
create new datasets, and that they are still assigned sequential numeric identifiers. In the unlikely chance that this 
is no longer working, please re-create the stored procedure following the steps described in the documentation for the 
`:IdentifierGenerationStyle` setting in the *Configuration* section of the Installation Guide for this release (v5.6). 
(Running the script supplied there will NOT overwrite the position on the sequence you are currently using!)
