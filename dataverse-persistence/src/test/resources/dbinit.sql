
-------------------- USERS --------------------

INSERT INTO authenticateduser (id, affiliation, createdtime, email, emailconfirmed, firstname, lastapiusetime, lastlogintime, lastname, "position", superuser, useridentifier)
    VALUES (2, 'Superuser Aff', '2019-05-31 09:32:32.922', 'superuser@mailinator.com', NULL, 'firstname', NULL, '2019-05-31 09:32:32.962', 'lastname', NULL, true, 'superuser');
INSERT INTO authenticateduserlookup (id, authenticationproviderid, persistentuserid, authenticateduser_id) VALUES (2, 'builtin', 'superuser', 2);

INSERT INTO authenticateduser (id, affiliation, createdtime, email, emailconfirmed, firstname, lastapiusetime, lastlogintime, lastname, "position", superuser, useridentifier)
    VALUES (3, 'some affiliation', '2019-10-28 13:29:06.000', 'filedownloader@mailinator.com', NULL, 'firstname', NULL, NULL, 'lastname', NULL, false, 'filedownloader');
INSERT INTO authenticateduserlookup (id, authenticationproviderid, persistentuserid, authenticateduser_id) VALUES (3, 'builtin', 'filedownloader', 3);

INSERT INTO authenticateduser (id, affiliation, createdtime, email, emailconfirmed, firstname, lastapiusetime, lastlogintime, lastname, "position", superuser, useridentifier)
    VALUES (4, 'some affiliation', '2019-10-25 13:22:51.000', 'groupmember@mailinator.com', NULL, 'firstname', NULL, NULL, 'lastname', NULL, false, 'rootGroupMember');
INSERT INTO authenticateduserlookup (id, authenticationproviderid, persistentuserid, authenticateduser_id) VALUES (4, 'builtin', 'rootGroupMember', 4);


INSERT INTO builtinuser (id, encryptedpassword, passwordencryptionversion, username) VALUES (2, '$2a$10$bdf0fR5BmHXhbqBKAk.11OkGSc99w2uvKU8xqKSrTpt33iycRoqfq', 1, 'superuser');
INSERT INTO builtinuser (id, encryptedpassword, passwordencryptionversion, username) VALUES (3, '$2a$10$bdf0fR5BmHXhbqBKAk.11OkGSc99w2uvKU8xqKSrTpt33iycRoqfq', 1, 'filedownloader');
INSERT INTO builtinuser (id, encryptedpassword, passwordencryptionversion, username) VALUES (4, '$2a$10$bdf0fR5BmHXhbqBKAk.11OkGSc99w2uvKU8xqKSrTpt33iycRoqfq', 1, 'rootGroupMember');



-------------------- DATAVERSES --------------------

--- ROOT ---
INSERT INTO dataversefacet (id, displayorder, datasetfieldtype_id, dataverse_id) VALUES (20, 1, 20, 1);
INSERT INTO dataversefacet (id, displayorder, datasetfieldtype_id, dataverse_id) VALUES (21, 0, 9, 1);
INSERT INTO dataversefacet (id, displayorder, datasetfieldtype_id, dataverse_id) VALUES (22, 2, 22, 1);
INSERT INTO dataversefacet (id, displayorder, datasetfieldtype_id, dataverse_id) VALUES (23, 4, 10, 1);

INSERT INTO dataversesubjects (dataverse_id, controlledvocabularyvalue_id) VALUES (1, 5);
INSERT INTO dataversesubjects (dataverse_id, controlledvocabularyvalue_id) VALUES (1, 4);
INSERT INTO dataversesubjects (dataverse_id, controlledvocabularyvalue_id) VALUES (1, 6);
INSERT INTO dataversesubjects (dataverse_id, controlledvocabularyvalue_id) VALUES (1, 3);
INSERT INTO dataversesubjects (dataverse_id, controlledvocabularyvalue_id) VALUES (1, 14);

INSERT INTO dataversetheme (id, backgroundcolor, linkcolor, linkurl, logo, logoalignment, logobackgroundcolor, logoformat, tagline, textcolor, dataverse_id)
    VALUES (1,'FFFFFF','428BCA','http://google.com','nonExistingLogo.png','CENTER','9c519c','SQUARE','sefgdfgsdfgsdfg','888888',1);


--- ROOT -> ownmetadatablocks ---
INSERT INTO dvobject (id, dtype, owner_id, previewimageavailable, protocol, authority, identifier, globalidcreatetime, identifierregistered, storageidentifier, releaseuser_id, publicationdate,
                      creator_id, createdate, modificationtime, permissionmodificationtime, indextime, permissionindextime)
    VALUES (19, 'Dataverse', 1, false, NULL, NULL, NULL, NULL, false, NULL, 1, '2019-06-06 08:27:43.217', 
            1, '2019-06-06 08:27:17.531', '2019-06-06 08:27:43.217', '2019-06-06 08:27:17.588', NULL, NULL);
INSERT INTO dataverse (id, alias, name, affiliation, dataversetype, description, defaultcontributorrole_id, defaulttemplate_id, facetroot, guestbookroot, metadatablockroot, permissionroot, templateroot, themeroot, allowmessagesbanners)
    VALUES (19, 'ownmetadatablocks', 'Own Metadatablock Dataverse', 'aff', 'ORGANIZATIONS_INSTITUTIONS', NULL, 6, NULL, false, false, true, true, false, true, false);
INSERT INTO dataverse_metadatablock (dataverse_id, metadatablocks_id) VALUES (19, 1);
INSERT INTO dataversefacet (id, displayorder, datasetfieldtype_id, dataverse_id) VALUES (19, 3, 58, 1);
INSERT INTO dataversecontact (id, contactemail, displayorder, dataverse_id) VALUES (2, 'ownmetadatablocks.dv.contact@mailinator.com', 0, 19);
INSERT INTO guestbook (id, createtime, emailrequired, enabled, institutionrequired, name, namerequired, positionrequired, dataverse_id)
    VALUES (2, '2019-06-06 08:27:43.217', false, false, false, 'Simple guestbook', true, false, 19);
INSERT INTO dataversetheme (id, backgroundcolor, linkcolor, linkurl, logo, logoalignment, logobackgroundcolor, logoformat, tagline, textcolor, dataverse_id)
    VALUES (2,'FFFFFF','428BCA','http://google.com','nonExistingLogo.png','CENTER','9c519c','SQUARE','sefgdfgsdfgsdfg','888888',19);

--- ROOT -> unreleased ---
INSERT INTO dvobject (id, dtype, owner_id, previewimageavailable, protocol, authority, identifier, globalidcreatetime, identifierregistered, storageidentifier, releaseuser_id, publicationdate,
                      creator_id, createdate, modificationtime, permissionmodificationtime, indextime, permissionindextime)
    VALUES (51, 'Dataverse', 1, false, NULL, NULL, NULL, NULL, false, NULL, 1, NULL, 
            1, '2019-08-19 13:14:48.434', '2019-08-19 13:15:02.415', '2019-08-19 13:14:48.799', NULL, NULL);
INSERT INTO dataverse (id, alias, name, affiliation, dataversetype, description, defaultcontributorrole_id, defaulttemplate_id, facetroot, guestbookroot, metadatablockroot, permissionroot, templateroot, themeroot, allowmessagesbanners)
    VALUES (51, 'unreleased', 'Unreleased Dataverse', 'ICM UW', 'JOURNALS', 'das', 6, NULL, false, false, false, true, false, true, false);
INSERT INTO dataversecontact (id, contactemail, displayorder, dataverse_id) VALUES (5, 'unreleased.dv.contact@mailinator.com', 0, 51);
INSERT INTO dataverserole (id, alias, description, name, permissionbits, owner_id) VALUES (13, 'unreleased_dv_test_role', 'Role description', 'Role name', 64, 51);


INSERT INTO dataversefeatureddataverse (id, displayorder, dataverse_id, featureddataverse_id) VALUES (3, 1, 1, 19);

--- ROOT -> simple Dataverse - without Data (no Datasets or child Dataverses) ---

INSERT INTO dvobject (id, dtype, owner_id, previewimageavailable, protocol, authority, identifier, globalidcreatetime, identifierregistered, storageidentifier, releaseuser_id, publicationdate,
                      creator_id, createdate, modificationtime, permissionmodificationtime, indextime, permissionindextime)
    VALUES (67, 'Dataverse', 1, false, NULL, NULL, NULL, NULL, false, NULL, 1, NULL,
            1, '2019-06-06 08:27:17.531', '2019-06-06 08:27:43.217', '2019-06-06 08:27:17.588', NULL, NULL);
INSERT INTO dataverse (id, alias, name, affiliation, dataversetype, description, defaultcontributorrole_id, defaulttemplate_id, facetroot, guestbookroot, metadatablockroot, permissionroot, templateroot, themeroot, allowmessagesbanners)
    VALUES (67, 'withoutData', 'withoutData', 'aff', 'ORGANIZATIONS_INSTITUTIONS', NULL, 6, NULL, false, false, true, true, false, true, false);


-------------------- DATASETS --------------------

INSERT INTO termsofuseandaccess (id, availabilitystatus, citationrequirements, conditions, confidentialitydeclaration, contactforaccess, dataaccessplace, depositorrequirements, disclaimer, fileaccessrequest, license, originalarchive, restrictions, sizeofcollection, specialpermissions, studycompletion, termsofaccess, termsofuse) VALUES (2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, 'CC0', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO termsofuseandaccess (id, availabilitystatus, citationrequirements, conditions, confidentialitydeclaration, contactforaccess, dataaccessplace, depositorrequirements, disclaimer, fileaccessrequest, license, originalarchive, restrictions, sizeofcollection, specialpermissions, studycompletion, termsofaccess, termsofuse) VALUES (36, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, 'CC0', NULL, NULL, NULL, NULL, NULL, NULL, NULL);

--- DV:ROOT -> Draft only dataset ---
INSERT INTO dvobject (id, dtype, owner_id, previewimageavailable, protocol, authority, identifier, globalidcreatetime, identifierregistered, storageidentifier, releaseuser_id, publicationdate,
                      creator_id, createdate, modificationtime, permissionmodificationtime, indextime, permissionindextime)
    VALUES (66, 'Dataset', 1, false, 'doi', '10.18150', 'FK2/QTVQKL', NULL, false, 'file://10.18150/FK2/QTVQKL', NULL, NULL, 
            1, '2019-09-26 11:43:58.194', '2019-09-26 11:43:58.194', '2019-09-26 11:43:58.194', NULL, NULL);
INSERT INTO dataset (id, fileaccessrequest, harvestidentifier, usegenericthumbnail, citationdatedatasetfieldtype_id, harvestingclient_id, guestbook_id, thumbnailfile_id)
    VALUES (66, NULL, NULL, false, NULL, NULL, NULL, NULL);
INSERT INTO datasetversion (id, version, dataset_id, versionstate, versionnumber, minorversionnumber, unf, termsofuseandaccess_id, archivenote, versionnote, deaccessionlink, createtime, lastupdatetime, releasetime, archivetime, archivalcopylocation)
    VALUES (41, 1, 66, 'DRAFT', 1, 0, NULL, NULL, NULL, NULL, NULL, '2019-09-26 11:43:58.204', '2019-09-26 11:43:58.204', NULL, NULL, NULL);
INSERT INTO datasetversionuser (id, lastupdatedate, authenticateduser_id, datasetversion_id) VALUES (44, '2019-09-26 11:43:58.194', 1, 41);

INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (668, NULL, 41, 1, 0, 'Draft only dataset');

INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (655, NULL, 41, 29, 0, NULL);
INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (670, 655, NULL, 31, 0, NULL);

INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (656, NULL, 41, 8, 0, NULL);
INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (660, 656, NULL, 11, 0, NULL);
INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (663, 656, NULL, 9, 0, 'Some author name');
INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (665, 656, NULL, 10, 0, 'author affiliation');

INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (657, NULL, 41, 17, 0, NULL);
INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (664, 657, NULL, 18, 0, 'draft only description');

INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (658, NULL, 41, 13, 0, NULL);
INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (666, 658, NULL, 14, 0, 'Some contact name');
INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (661, 658, NULL, 15, 0, 'contact affiliation');
INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (667, 658, NULL, 16, 0, 'fk2.qtvqkl.contact@mailinator.com');

INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (659, NULL, 41, 20, 0, NULL);
INSERT INTO datasetfield_controlledvocabularyvalue (datasetfield_id, controlledvocabularyvalues_id) VALUES (659, 4);

INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (662, NULL, 41, 58, 0, '2019-09-26');
INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (669, NULL, 41, 57, 0, 'Some depositor name');


--- DV:unreleased -> Draft with files ---
INSERT INTO dvobject (id, dtype, owner_id, previewimageavailable, protocol, authority, identifier, globalidcreatetime, identifierregistered, storageidentifier, releaseuser_id, publicationdate,
                      creator_id, createdate, modificationtime, permissionmodificationtime, indextime, permissionindextime)
    VALUES (52, 'Dataset', 51, false, 'doi', '10.18150', 'FK2/MLXK1N', NULL, false, 'file://10.18150/FK2/MLXK1N', NULL, NULL,
            2, '2019-08-22 08:22:33.069', '2019-09-27 12:00:43.188', '2019-08-22 08:22:33.069', NULL, NULL);
INSERT INTO dataset (id, fileaccessrequest, harvestidentifier, usegenericthumbnail, citationdatedatasetfieldtype_id, harvestingclient_id, guestbook_id, thumbnailfile_id)
    VALUES (52, NULL, NULL, false, NULL, NULL, NULL, NULL);
INSERT INTO datafilecategory (id, name, dataset_id) VALUES (11, 'Code', 52);
INSERT INTO datafilecategory (id, name, dataset_id) VALUES (12, 'CustomCategory', 52);
INSERT INTO datafilecategory (id, name, dataset_id) VALUES (13, 'UnassignedCategory', 52);
INSERT INTO datasetversion (id, version, dataset_id, versionstate, versionnumber, minorversionnumber, unf, termsofuseandaccess_id, archivenote, versionnote, deaccessionlink, createtime, lastupdatetime, releasetime, archivetime, archivalcopylocation)
    VALUES (36, 11, 52, 'DRAFT', NULL, NULL, NULL, 36, NULL, NULL, NULL, '2019-08-22 08:22:33.1', '2019-09-27 12:00:43.188', NULL, NULL, NULL);
INSERT INTO datasetversionuser (id, lastupdatedate, authenticateduser_id, datasetversion_id) VALUES (38, '2019-08-22 08:23:02.738', 2, 36);
INSERT INTO datasetversionuser (id, lastupdatedate, authenticateduser_id, datasetversion_id) VALUES (39, '2019-09-27 12:00:43.188', 1, 36);

INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (571, NULL, 36, 1, 0, 'Draft with files');

INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (564, NULL, 36, 8, 0, NULL);
INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (568, 564, NULL, 9, 0, 'Some author name');

INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (565, NULL, 36, 13, 0, NULL);
INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (574, 565, NULL, 14, 0, 'Some contact name');
INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (569, 565, NULL, 16, 0, 'fk2.mlxk1n.contact@mailinator.com');

INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (566, NULL, 36, 17, 0, NULL);
INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (572, 566, NULL, 18, 0, 'draft with files description');

INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (570, NULL, 36, 20, 0, NULL);
INSERT INTO datasetfield_controlledvocabularyvalue (datasetfield_id, controlledvocabularyvalues_id) VALUES (570, 3);

INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (573, NULL, 36, 57, 0, 'Some depositor name');
INSERT INTO datasetfield (id, datasetfieldparent_id, datasetversion_id, datasetfieldtype_id, displayorder, fieldvalue) VALUES (575, NULL, 36, 58, 0, '2019-08-22');



--- DV:ownmetadatablocks -> Dataset with Versions (last version is draft) ---
INSERT INTO dvobject (id, dtype, owner_id, previewimageavailable, protocol, authority, identifier, globalidcreatetime, identifierregistered, storageidentifier, releaseuser_id, publicationdate,
                      creator_id, createdate, modificationtime, permissionmodificationtime, indextime, permissionindextime)
    VALUES (56, 'Dataset', 19, false, 'doi', '10.18150', 'FK2/MLDB99', NULL, false, 'file://10.18150/FK2/MLDB99', 2, '2019-08-22 08:22:33.1', 
            2, '2019-08-22 08:22:33.069', '2019-09-27 12:00:43.188', '2019-08-22 08:22:33.069', NULL, NULL);
INSERT INTO dataset (id, fileaccessrequest, harvestidentifier, usegenericthumbnail, citationdatedatasetfieldtype_id, harvestingclient_id, guestbook_id, thumbnailfile_id)
    VALUES (56, NULL, NULL, false, NULL, NULL, NULL, NULL);
INSERT INTO datasetversion (id, version, dataset_id, versionstate, versionnumber, minorversionnumber, unf, termsofuseandaccess_id, archivenote, versionnote, deaccessionlink, createtime, lastupdatetime, releasetime, archivetime, archivalcopylocation)
    VALUES (42, 11, 56, 'ARCHIVED', 1, 0, NULL, 36, 'https://www.google.com/', NULL, NULL, '2019-08-22 08:22:33.1', '2019-09-27 12:00:43.188', '2019-09-27 12:00:43.188', NULL, NULL);
INSERT INTO datasetversion (id, version, dataset_id, versionstate, versionnumber, minorversionnumber, unf, termsofuseandaccess_id, archivenote, versionnote, deaccessionlink, createtime, lastupdatetime, releasetime, archivetime, archivalcopylocation)
    VALUES (43, 11, 56, 'RELEASED', 2, 0, NULL, 36, NULL, NULL, NULL, '2019-08-22 08:22:33.1', '2019-09-27 12:00:43.188', '2019-09-27 12:00:43.188', NULL, NULL);
INSERT INTO datasetversion (id, version, dataset_id, versionstate, versionnumber, minorversionnumber, unf, termsofuseandaccess_id, archivenote, versionnote, deaccessionlink, createtime, lastupdatetime, releasetime, archivetime, archivalcopylocation)
    VALUES (44, 11, 56, 'DRAFT', NULL, NULL, NULL, 36, NULL, NULL, NULL, '2019-08-22 08:22:33.1', '2019-09-27 12:00:43.188', NULL, NULL, NULL);
INSERT INTO datasetversionuser (id, lastupdatedate, authenticateduser_id, datasetversion_id) VALUES (40, '2019-08-22 08:23:02.738', 2, 42);
INSERT INTO datasetversionuser (id, lastupdatedate, authenticateduser_id, datasetversion_id) VALUES (41, '2019-08-22 08:23:02.738', 2, 43);
INSERT INTO datasetversionuser (id, lastupdatedate, authenticateduser_id, datasetversion_id) VALUES (42, '2019-08-22 08:23:02.738', 2, 43);

--- DV:ownmetadatablocks -> Dataset with versions (last version is released) ---
INSERT INTO dvobject (id, dtype, owner_id, previewimageavailable, protocol, authority, identifier, globalidcreatetime, identifierregistered, storageidentifier, releaseuser_id, publicationdate,
                      creator_id, createdate, modificationtime, permissionmodificationtime, indextime, permissionindextime)
    VALUES (57, 'Dataset', 19, false, 'doi', '10.18150', 'FK2/BWM3WL', NULL, false, 'file://10.18150/FK2/BWM3WL', 2, '2020-01-03 12:22:33', 
            2, '2020-01-03 12:22:33', '2020-01-06 12:00:43', '2020-01-06 12:00:43', NULL, NULL);
INSERT INTO dataset (id, fileaccessrequest, harvestidentifier, usegenericthumbnail, citationdatedatasetfieldtype_id, harvestingclient_id, guestbook_id, thumbnailfile_id)
    VALUES (57, NULL, NULL, false, NULL, NULL, NULL, NULL);
INSERT INTO datasetversion (id, version, dataset_id, versionstate, versionnumber, minorversionnumber, unf, termsofuseandaccess_id, archivenote, versionnote, deaccessionlink, createtime, lastupdatetime, releasetime, archivetime, archivalcopylocation)
    VALUES (50, 1, 57, 'RELEASED', 1, 0, NULL, 36, NULL, NULL, NULL, '2020-01-03 12:22:33', '2020-01-04 01:48:00', '2019-01-04 01:48:00', NULL, NULL);
INSERT INTO datasetversion (id, version, dataset_id, versionstate, versionnumber, minorversionnumber, unf, termsofuseandaccess_id, archivenote, versionnote, deaccessionlink, createtime, lastupdatetime, releasetime, archivetime, archivalcopylocation)
    VALUES (51, 1, 57, 'RELEASED', 2, 0, NULL, 36, NULL, NULL, NULL, '2020-01-06 09:10:00', '2020-01-06 12:00:43', '2020-01-06 12:00:43', NULL, NULL);


-------------------- DATAFILES --------------------

--- DS:Draft with files -> testfile6.zip
INSERT INTO dvobject (id, dtype, owner_id, previewimageavailable, protocol, authority, identifier, globalidcreatetime, identifierregistered, storageidentifier, releaseuser_id, publicationdate,
                      creator_id, createdate, modificationtime, permissionmodificationtime, indextime, permissionindextime)
    VALUES (53, 'DataFile', 52, false, NULL, NULL, NULL, NULL, false, '16d24989113-5834f8e1e68e', NULL, NULL, 
            1, '2019-09-12 08:30:53.446', '2019-09-27 12:00:43.188', '2019-09-12 08:30:41.424', NULL, NULL);
INSERT INTO datafile (id, checksumtype, checksumvalue, contenttype, filesize, ingeststatus, previousdatafileid, prov_entityname, restricted, rootdatafileid)
    VALUES (53, 'MD5', 'd9881598b92d3f87fd8a7c7eb99f84b7', 'application/zip', 19, 'A', NULL, NULL, NULL, -1);
INSERT INTO filetermsofuse (id, allrightsreserved, restrictcustomtext, restricttype, license_id) VALUES (117, false, NULL, NULL, 1);
INSERT INTO filemetadata (id, description, directorylabel, label, prov_freeform, restricted, version, datafile_id, datasetversion_id, displayorder, termsofuse_id) VALUES (110, '', NULL, 'testfile6.zip', NULL, NULL, 5, 53, 36, 0, 117);
INSERT INTO filemetadata_datafilecategory (filecategories_id, filemetadatas_id) VALUES (11, 110);
INSERT INTO filemetadata_datafilecategory (filecategories_id, filemetadatas_id) VALUES (12, 110);

--- DS:Draft with files -> testfile1.zip
INSERT INTO dvobject (id, dtype, owner_id, previewimageavailable, protocol, authority, identifier, globalidcreatetime, identifierregistered, storageidentifier, releaseuser_id, publicationdate,
                      creator_id, createdate, modificationtime, permissionmodificationtime, indextime, permissionindextime)
    VALUES (55, 'DataFile', 52, false, NULL, NULL, NULL, NULL, false, '16d24989319-2c86e28809de', NULL, NULL, 
            1, '2019-09-12 08:30:53.446', '2019-09-27 12:00:43.188', '2019-09-12 08:30:41.942', NULL, NULL);
INSERT INTO datafile (id, checksumtype, checksumvalue, contenttype, filesize, ingeststatus, previousdatafileid, prov_entityname, restricted, rootdatafileid)
    VALUES (55, 'MD5', '7ed0097d7e9ee73cf0952a1f0a07c07e', 'application/zip', 3, 'A', NULL, NULL, NULL, -1);
INSERT INTO filetermsofuse (id, allrightsreserved, restrictcustomtext, restricttype, license_id) VALUES (119, false, NULL, NULL, 1);
INSERT INTO filemetadata (id, description, directorylabel, label, prov_freeform, restricted, version, datafile_id, datasetversion_id, displayorder, termsofuse_id) VALUES (112, '', NULL, 'testfile1.zip', NULL, NULL, 5, 55, 36, 1, 119);
INSERT INTO filemetadata_datafilecategory (filecategories_id, filemetadatas_id) VALUES (11, 112);


-------------------- GROUPS --------------------

INSERT INTO explicitgroup (id, description, displayname, groupalias, groupaliasinowner, owner_id)
    VALUES (1, NULL, 'Group in root dataverse', '1-rootgroup', 'rootgroup', 1);

INSERT INTO explicitgroup_containedroleassignees (explicitgroup_id, containedroleassignees) VALUES (1, '@rootGroupMember');

INSERT INTO explicitgroup_authenticateduser (explicitgroup_id, containedauthenticatedusers_id) VALUES (1, 2);
INSERT INTO explicitgroup_authenticateduser (explicitgroup_id, containedauthenticatedusers_id) VALUES (1, 3);

-------------------- ROLE ASSIGNMENTS --------------------

INSERT INTO roleassignment (id, assigneeidentifier, privateurltoken, definitionpoint_id, role_id) VALUES (5, '@dataverseAdmin', NULL, 1, 1);
INSERT INTO roleassignment (id, assigneeidentifier, privateurltoken, definitionpoint_id, role_id) VALUES (7, '@dataverseAdmin', NULL, 19, 1);
INSERT INTO roleassignment (id, assigneeidentifier, privateurltoken, definitionpoint_id, role_id) VALUES (29, '@dataverseAdmin', NULL, 51, 1);

INSERT INTO roleassignment (id, assigneeidentifier, privateurltoken, definitionpoint_id, role_id) VALUES (30, '@filedownloader', NULL, 53, 2);
INSERT INTO roleassignment (id, assigneeidentifier, privateurltoken, definitionpoint_id, role_id) VALUES (31, '@filedownloader', NULL, 55, 2);

INSERT INTO roleassignment (id, assigneeidentifier, privateurltoken, definitionpoint_id, role_id) VALUES (32, '&explicit/1-rootgroup', NULL, 1, 7);
INSERT INTO roleassignment (id, assigneeidentifier, privateurltoken, definitionpoint_id, role_id) VALUES (33, '@superuser', NULL, 1, 6);

-------------------- TEMPLATES --------------------
INSERT INTO template (id, createtime, name, usagecount, dataverse_id, termsofuseandaccess_id)
 VALUES (1, '2019-08-22 08:23:02.738', 'testTemplate', 0, 1, NULL);

-------------------- Fix sequences --------------------

SELECT setval('authenticateduser_id_seq', COALESCE((SELECT MAX(id)+1 FROM authenticateduser), 1), false);
SELECT setval('authenticateduserlookup_id_seq', COALESCE((SELECT MAX(id)+1 FROM authenticateduserlookup), 1), false);
SELECT setval('builtinuser_id_seq', COALESCE((SELECT MAX(id)+1 FROM builtinuser), 1), false);
SELECT setval('dataversefacet_id_seq', COALESCE((SELECT MAX(id)+1 FROM dataversefacet), 1), false);
SELECT setval('dataversetextmessage_id_seq', COALESCE((SELECT MAX(id)+1 FROM dataversetextmessage), 1), false);
SELECT setval('dataverselocalizedmessage_id_seq', COALESCE((SELECT MAX(id)+1 FROM dataverselocalizedmessage), 1), false);
SELECT setval('dataverserole_id_seq', COALESCE((SELECT MAX(id)+1 FROM dataverserole), 1), false);
SELECT setval('dataversecontact_id_seq', COALESCE((SELECT MAX(id)+1 FROM dataversecontact), 1), false);
SELECT setval('dataverserole_id_seq', COALESCE((SELECT MAX(id)+1 FROM dataverserole), 1), false);
SELECT setval('dataversefeatureddataverse_id_seq', COALESCE((SELECT MAX(id)+1 FROM dataversefeatureddataverse), 1), false);
SELECT setval('datafilecategory_id_seq', COALESCE((SELECT MAX(id)+1 FROM datafilecategory), 1), false);
SELECT setval('datasetversion_id_seq', COALESCE((SELECT MAX(id)+1 FROM datasetversion), 1), false);
SELECT setval('datasetversionuser_id_seq', COALESCE((SELECT MAX(id)+1 FROM datasetversionuser), 1), false);
SELECT setval('dvobject_id_seq', COALESCE((SELECT MAX(id)+1 FROM dvobject), 1), false);
SELECT setval('termsofuse_id_seq', COALESCE((SELECT MAX(id)+1 FROM filetermsofuse), 1), false);
SELECT setval('filemetadata_id_seq', COALESCE((SELECT MAX(id)+1 FROM filemetadata), 1), false);
SELECT setval('termsofuseandaccess_id_seq', COALESCE((SELECT MAX(id)+1 FROM termsofuseandaccess), 1), false);
SELECT setval('template_id_seq', COALESCE((SELECT MAX(id)+1 FROM template), 1), false);
SELECT setval('datasetfield_id_seq', COALESCE((SELECT MAX(id)+1 FROM datasetfield), 1), false);
SELECT setval('explicitgroup_id_seq', COALESCE((SELECT MAX(id)+1 FROM explicitgroup), 1), false);
SELECT setval('guestbook_id_seq', COALESCE((SELECT MAX(id)+1 FROM guestbook), 1), false);
SELECT setval('customquestion_id_seq', COALESCE((SELECT MAX(id)+1 FROM customquestion), 1), false);
SELECT setval('customquestionvalue_id_seq', COALESCE((SELECT MAX(id)+1 FROM customquestionvalue), 1), false);
SELECT setval('ingestreport_id_seq', COALESCE((SELECT MAX(id)+1 FROM ingestreport), 1), false);
SELECT setval('roleassignment_id_seq', COALESCE((SELECT MAX(id)+1 FROM roleassignment), 1), false);
