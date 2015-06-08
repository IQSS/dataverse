/* ----------------------------------------
    Description: These SQL statements in this file relate to the following tickets
    
    (1) "Index Check" - https://github.com/IQSS/dataverse/issues/1880
        Summary: Add indices to existing columns.

*/ ----------------------------------------
/* ----------------------------------------
   actionlogrecord indices (ActionLogRecord.java)
*/ ----------------------------------------
CREATE INDEX index_actionlogrecord_useridentifier ON actionlogrecord (useridentifier);
CREATE INDEX index_actionlogrecord_actiontype ON actionlogrecord (actiontype);
CREATE INDEX index_actionlogrecord_starttime ON actionlogrecord (starttime);
/* ----------------------------------------
    authenticationproviderrow index (AuthenticationProviderRow.java)
*/ ----------------------------------------
CREATE INDEX index_authenticationproviderrow_enabled ON authenticationproviderrow (enabled);
/* ----------------------------------------
    builtinuser index (BuiltInUser.java)
*/ ----------------------------------------
CREATE INDEX index_builtinuser_lastname ON builtinuser (lastname);
/* ----------------------------------------
   controlledvocabalternate indices (ControlledVocabAlternate.java)
*/ ----------------------------------------
CREATE INDEX index_controlledvocabalternate_controlledvocabularyvalue_id ON controlledvocabalternate (controlledvocabularyvalue_id);
CREATE INDEX index_controlledvocabalternate_datasetfieldtype_id ON controlledvocabalternate (datasetfieldtype_id);
/* ----------------------------------------
   controlledvocabularyvalue indices (ControlledVocabularyValue.java)
*/ ----------------------------------------
CREATE INDEX index_controlledvocabularyvalue_datasetfieldtype_id ON controlledvocabularyvalue (datasetfieldtype_id);
CREATE INDEX index_controlledvocabularyvalue_displayorder ON controlledvocabularyvalue (displayorder);
/* ----------------------------------------
   customfieldmap indices (CustomFieldMap.java)
*/ ----------------------------------------
CREATE INDEX index_customfieldmap_sourcedatasetfield ON customfieldmap (sourcedatasetfield);
CREATE INDEX index_customfieldmap_sourcetemplate ON customfieldmap (sourcetemplate);
/* ----------------------------------------
   datafile indices (DataFile.java)
*/ ----------------------------------------
CREATE INDEX index_datafile_ingeststatus ON datafile (ingeststatus);
CREATE INDEX index_datafile_md5 ON datafile (md5);
CREATE INDEX index_datafile_contenttype ON datafile (contenttype);
CREATE INDEX index_datafile_restricted ON datafile (restricted);
/* ----------------------------------------
   datasetfielddefaultvalue indices (DatasetFieldDefaultValue.java)
*/ ----------------------------------------
CREATE INDEX index_datasetfielddefaultvalue_datasetfield_id ON datasetfielddefaultvalue (datasetfield_id);
CREATE INDEX index_datasetfielddefaultvalue_defaultvalueset_id ON datasetfielddefaultvalue (defaultvalueset_id);
CREATE INDEX index_datasetfielddefaultvalue_parentdatasetfielddefaultvalue_id ON datasetfielddefaultvalue (parentdatasetfielddefaultvalue_id);
CREATE INDEX index_datasetfielddefaultvalue_displayorder ON datasetfielddefaultvalue (displayorder);
/* ----------------------------------------
   datasetlock indices (DatasetLock.java)
*/ ----------------------------------------
CREATE INDEX index_datasetlock_user_id ON datasetlock (user_id);
CREATE INDEX index_datasetlock_dataset_id ON datasetlock (dataset_id);
/* ----------------------------------------
   datasetversionuser indices (DatasetVersionUser.java)
*/ ----------------------------------------
CREATE INDEX index_datasetversionuser_authenticateduser_id ON datasetversionuser (authenticateduser_id);
CREATE INDEX index_datasetversionuser_datasetversion_id ON datasetversionuser (datasetversion_id);
/* ----------------------------------------
   dataverse indices (Dataverse.java)
*/ ----------------------------------------
CREATE INDEX index_dataverse_fk_dataverse_id ON dataverse (fk_dataverse_id);
CREATE INDEX index_dataverse_defaultcontributorrole_id ON dataverse (defaultcontributorrole_id);
CREATE INDEX index_dataverse_defaulttemplate_id ON dataverse (defaulttemplate_id);
CREATE INDEX index_dataverse_alias ON dataverse (alias);
CREATE INDEX index_dataverse_affiliation ON dataverse (affiliation);
CREATE INDEX index_dataverse_dataversetype ON dataverse (dataversetype);
CREATE INDEX index_dataverse_facetroot ON dataverse (facetroot);
CREATE INDEX index_dataverse_guestbookroot ON dataverse (guestbookroot);
CREATE INDEX index_dataverse_metadatablockroot ON dataverse (metadatablockroot);
CREATE INDEX index_dataverse_templateroot ON dataverse (templateroot);
CREATE INDEX index_dataverse_permissionroot ON dataverse (permissionroot);
CREATE INDEX index_dataverse_themeroot ON dataverse (themeroot);
/* ----------------------------------------
   dataversecontact indices (DataverseContact.java)
*/ ----------------------------------------
CREATE INDEX index_dataversecontact_dataverse_id ON dataversecontact (dataverse_id);
CREATE INDEX index_dataversecontact_contactemail ON dataversecontact (contactemail);
CREATE INDEX index_dataversecontact_displayorder ON dataversecontact (displayorder);
/* ----------------------------------------
   dataversefacet indices (DataverseFacet.java)
*/ ----------------------------------------
CREATE INDEX index_dataversefacet_dataverse_id ON dataversefacet (dataverse_id);
CREATE INDEX index_dataversefacet_datasetfieldtype_id ON dataversefacet (datasetfieldtype_id);
CREATE INDEX index_dataversefacet_displayorder ON dataversefacet (displayorder);
/* ----------------------------------------
   dataversefeatureddataverse indices (DataverseFeaturedDataverse.java)
*/ ----------------------------------------
CREATE INDEX index_dataversefeatureddataverse_dataverse_id ON dataversefeatureddataverse (dataverse_id);
CREATE INDEX index_dataversefeatureddataverse_featureddataverse_id ON dataversefeatureddataverse (featureddataverse_id);
CREATE INDEX index_dataversefeatureddataverse_displayorder ON dataversefeatureddataverse (displayorder);
/* ----------------------------------------
   dataversefieldtypeinputlevel indices (DataverseFieldTypeInputLevel.java)
*/ ----------------------------------------
CREATE INDEX index_dataversefieldtypeinputlevel_dataverse_id ON dataversefieldtypeinputlevel (dataverse_id);
CREATE INDEX index_dataversefieldtypeinputlevel_datasetfieldtype_id ON dataversefieldtypeinputlevel (datasetfieldtype_id);
CREATE INDEX index_dataversefieldtypeinputlevel_required ON dataversefieldtypeinputlevel (required);
/* ----------------------------------------
   dataverserole indices (DataverseRole.java)
*/ ----------------------------------------
CREATE INDEX index_dataverserole_owner_id ON dataverserole (owner_id);
CREATE INDEX index_dataverserole_name ON dataverserole (name);
CREATE INDEX index_dataverserole_alias ON dataverserole (alias);
/* ----------------------------------------
   dvobject indices (DvObject.java)
*/ ----------------------------------------
CREATE INDEX index_dvobject_dtype ON dvobject (dtype);
/*  Should already exist:
CREATE INDEX index_dvobject_owner_id ON dvobject (owner_id);
CREATE INDEX index_dvobject_creator_id ON dvobject (creator_id);
CREATE INDEX index_dvobject_releaseuser_id ON dvobject (releaseuser_id);
*/
/* ----------------------------------------
   explicitgroup indices (ExplicitGroup.java)
*/ ----------------------------------------
CREATE INDEX index_explicitgroup_owner_id ON explicitgroup (owner_id);
CREATE INDEX index_explicitgroup_groupalias ON explicitgroup (groupalias);
CREATE INDEX index_explicitgroup_groupaliasinowner ON explicitgroup (groupaliasinowner);
/* ----------------------------------------
   foreignmetadatafieldmapping indices (ForeignMetadataFieldMapping.java)
*/ ----------------------------------------
CREATE INDEX index_foreignmetadatafieldmapping_foreignmetadataformatmapping_id ON foreignmetadatafieldmapping (foreignmetadataformatmapping_id);
CREATE INDEX index_foreignmetadatafieldmapping_foreignfieldxpath ON foreignmetadatafieldmapping (foreignfieldxpath);
CREATE INDEX index_foreignmetadatafieldmapping_parentfieldmapping_id ON foreignmetadatafieldmapping (parentfieldmapping_id);
/* ----------------------------------------
    foreignmetadataformatmapping index (ForeignMetadataFormatMapping.java)
*/ ----------------------------------------
CREATE INDEX index_foreignmetadataformatmapping_name ON foreignmetadataformatmapping (name);
/* ----------------------------------------
   harvestingdataverseconfig indices (HarvestingDataverseConfig.java)
*/ ----------------------------------------
CREATE INDEX index_harvestingdataverseconfig_dataverse_id ON harvestingdataverseconfig (dataverse_id);
CREATE INDEX index_harvestingdataverseconfig_harvesttype ON harvestingdataverseconfig (harvesttype);
CREATE INDEX index_harvestingdataverseconfig_harveststyle ON harvestingdataverseconfig (harveststyle);
CREATE INDEX index_harvestingdataverseconfig_harvestingurl ON harvestingdataverseconfig (harvestingurl);
/* ----------------------------------------
    ipv4range index (IPv4Range.java)
*/ ----------------------------------------
CREATE INDEX index_ipv4range_owner_id ON ipv4range (owner_id);
/* ----------------------------------------
    ipv6range index (IPv6Range.java)
*/ ----------------------------------------
CREATE INDEX index_ipv6range_owner_id ON ipv6range (owner_id);
/* ----------------------------------------
   maplayermetadata indices (MapLayerMetadata.java)
*/ ----------------------------------------
CREATE INDEX index_maplayermetadata_dataset_id ON maplayermetadata (dataset_id);
CREATE INDEX index_maplayermetadata_datafile_id ON maplayermetadata (datafile_id);
/* ----------------------------------------
   metadatablock indices (MetadataBlock.java)
*/ ----------------------------------------
CREATE INDEX index_metadatablock_name ON metadatablock (name);
CREATE INDEX index_metadatablock_owner_id ON metadatablock (owner_id);
/* ----------------------------------------
   passwordresetdata indices (PasswordResetData.java)
*/ ----------------------------------------
CREATE INDEX index_passwordresetdata_token ON passwordresetdata (token);
CREATE INDEX index_passwordresetdata_builtinuser_id ON passwordresetdata (builtinuser_id);
/* ----------------------------------------
   persistedglobalgroup indices (PersistedGlobalGroup.java)
*/ ----------------------------------------
CREATE INDEX index_persistedglobalgroup_persistedgroupalias ON persistedglobalgroup (persistedgroupalias);
CREATE INDEX index_persistedglobalgroup_dtype ON persistedglobalgroup (dtype);
/* ----------------------------------------
   roleassignment indices (RoleAssignment.java)
*/ ----------------------------------------
CREATE INDEX index_roleassignment_assigneeidentifier ON roleassignment (assigneeidentifier);
CREATE INDEX index_roleassignment_definitionpoint_id ON roleassignment (definitionpoint_id);
CREATE INDEX index_roleassignment_role_id ON roleassignment (role_id);
/* ----------------------------------------
   savedsearch indices (SavedSearch.java)
*/ ----------------------------------------
CREATE INDEX index_savedsearch_definitionpoint_id ON savedsearch (definitionpoint_id);
CREATE INDEX index_savedsearch_creator_id ON savedsearch (creator_id);
/* ----------------------------------------
    savedsearchfilterquery index (SavedSearchFilterQuery.java)
*/ ----------------------------------------
CREATE INDEX index_savedsearchfilterquery_savedsearch_id ON savedsearchfilterquery (savedsearch_id);
/* ----------------------------------------
    template index (Template.java)
*/ ----------------------------------------
CREATE INDEX index_template_dataverse_id ON template (dataverse_id);
/* ----------------------------------------
   worldmapauth_token indices (WorldMapToken.java)
*/ ----------------------------------------
CREATE INDEX index_worldmapauth_token_application_id ON worldmapauth_token (application_id);
CREATE INDEX index_worldmapauth_token_datafile_id ON worldmapauth_token (datafile_id);
CREATE INDEX index_worldmapauth_token_dataverseuser_id ON worldmapauth_token (dataverseuser_id);
/*------------------------------------------
 Add Compound Unique Constraint to dataversefieldtypeinputlevel
    combining dataverse_id and datasetfieldtype_id
*/------------------------------------------
ALTER TABLE dataversefieldtypeinputlevel
  ADD CONSTRAINT unq_dataversefieldtypeinputlevel_add  UNIQUE (dataverse_id, datasetfieldtype_id);