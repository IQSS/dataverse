### Multiple License Support

Users can now select from a set of configured licenses in addition to or instead of the current Creative Commons CC0 choice or provide custom terms of use (if configured) for their datasets. Administrators can configure their Dataverse instance via API to allow any desired license as a choice and can enable/disable the option to allow custom terms. Administrators can also mark licenses as 'inactive' to disallow future use while keeping that license for existing datasets. By default, only the CC0 license will be preinstalled. Examples in the Guides show how to add additional licenses and specific examples are given for several Creative Commons licenses. **Note: Datasets in existing installations will automatically be updated to conform to new requirements that custom terms cannot be used with a standard license and that custom terms cannot be empty. Administrators may wish to manually update datasets with these conditions if they do not like the automated migration choices. See the Notes for Dataverse Installation Administrators and Additional Release Steps sections for further information.**


## Major Use Cases and Infrastructure Enhancements


- When creating/updating datasets, users can select from a set of standard licenses configured by the administrator or provide custom terms (if the installation is configured to allow them).

## Notes for Dataverse Installation Administrators

### Updating for multiple license support

As part of installing/upgrading an existing installation, administrators may wish to add additional license choices and/or configure Dataverse to allow custom terms. Adding additional licenses is managed via API. Licenses are described via a JSON structure providing a name, URL, short description, and optional icon URL. Additionally licenses may be marked as active (selectable for new/updated datasets) or inactive (only allowed on existing datasets) and one license can be marked as the default. Custom Terms are allowed by default (backward compatible with the current option to select 'No' to using CC0) and can be disabled by setting `:AllowCustomTermsOfUse` to false.

Further administrators should review the following automated migration of existing licenses and terms into the new license framework and, if desired, should manually find and update any datasets for which the automated update is problematic. 
To understand the migration process, it is useful to understand how the multiple license feature works in this release:

'Custom Terms', aka a custom license, are defined through entries in the following fields of the dataset "Terms" tab:
- Terms of Use
- Confidentiality Declaration 
- Special Permissions 
- Restrictions 
- Citation Requirements 
- Depositor Requirements 
- Conditions 
- Disclaimer

'Custom Terms' require,at a minimum, a non-blank entry in the "Terms of Use" field. Entries in other fields are optional.

Since these fields are intended for terms/conditions that would potentially conflict with or modify the terms in a standard license, they are no longer shown when a standard license is selected.

In earlier Dataverse releases, it was possible to select the CC0 license and have entries in the fields above. It was also possible to say 'No' to using CC0 and leave all of these terms fields blank. 

The automated process will update existing datasets as follows.

- 'CC0 Waiver' and no entries in the fields above -> CC0 License (no change)
- No CC0 Waiver and an entry in the Terms of Use field and possibly others fields listed above -> 'Custom Terms' with the same entries in these fields (no change)

- CC0 Waiver and an entry in some of the fields listed -> 'Custom Terms' with the following text preprended in the "Terms of Use" field: "This dataset is made available under a Creative Commons CC0 license with the following additional/modified terms and conditions:"
- No CC0 Waiver and an entry in a field(s) other than the Terms of Use field -> 'Custom Terms' with the following "Terms of Use" added: "This dataset is made available with limited information on how it can be used. You may wish to communicate with the Contact(s) specified before use."
- No CC0 Waiver and no entry in any of the listed fields -> 'Custom Terms' with the following "Terms of Use" added: "This dataset is made available without information on how it can be used. You should communicate with the Contact(s) specified before use."

Administrators who have datasets where CC0 has been selected along with additional terms, or datasets where the Terms of Use field is empty, may wish to modify those datasets prior to upgrading to avoid the automated changes above. The Additonal Release Steps provides information on how to find and modify any such datasets.

## New JVM Options and DB Settings

- `:AllowCustomTermsOfUse` (default: true) allow users to provide Custom Terms instead of choosing one of the configured standard licenses.

See the [Database Settings](https://guides.dataverse.org/en/5.9/installation/config.html) section of the Guides for more information.

## Additional Release Steps

In most Dataverse installations, one would expect the vast majority of Datasets to either use the CC0 Waiver or have non-empty Terms of Use. As noted above, these will be migrated without any issue. Administrators may however wish to find and manually update datasets that specified a CC0 license but also had terms (no longer allowed) or had no license and no terms of use (also no longer allowed) rather than accept the default migrations for these datasets listed above.

### To find Datasets with a CC0 license and non-empty terms:

    select CONCAT('doi:', dvo.authority, '/', dvo.identifier), v.alias as dataverse_alias, case when versionstate='RELEASED' then concat(dv.versionnumber, '.', dv.minorversionnumber) else versionstate END  as version, dv.id as datasetversion_id, t.id as termsofuseandaccess_id, t.termsofuse, t.confidentialitydeclaration, t.specialpermissions, t.restrictions, t.citationrequirements, t.depositorrequirements, t.conditions, t.disclaimer from dvobject dvo, termsofuseandaccess t, datasetversion dv, dataverse v where dv.dataset_id=dvo.id and dv.termsofuseandaccess_id=t.id and dvo.owner_id=v.id and t.license='CC0' and not (t.termsofuse is null and t.confidentialitydeclaration is null and t.specialpermissions is null and t.restrictions is null and citationrequirements is null and t.depositorrequirements is null and t.conditions is null and t.disclaimer is null);

The datasetdoi column will let you find/view the affected dataset in the Dataverse web interface. The version column will indicate which version(s) are relevant. The dataverse_alias will tell you which Dataverse collection the dataset is in (and may be useful if you want to adjust all datasets in a given collection). The termsofuseandaccess_id column indicates which specific entry in that table is associated with the dataset/version. The remaining columns show the values of any terms fields.

There are two choices to migrate such datasets:

 - Set all terms fields to null:


    update termsofuseandaccess set termsofuse=null, confidentialitydeclaration=null, t.specialpermissions=null, t.restrictions=null, citationrequirements=null, depositorrequirements=null, conditions=null, disclaimer=null where id=<id>;

or to change several at once:

    update termsofuseandaccess set termsofuse=null, confidentialitydeclaration=null, t.specialpermissions=null, t.restrictions=null, citationrequirements=null, depositorrequirements=null, conditions=null, disclaimer=null where id in (<comma separated list of termsanduseofaccess_ids>);

 - Alternately, change the Dataset version(s) to not use the CCO waiver and modify the Terms of Use (and/or other fields) as you wish to indicate that the CC0 waiver was previously selected:


    update termsofuseandaccess set license='NONE', termsofuse=concat('New text. ', termsofuse) where id=<id>;
    
or 
    
    update termsofuseandaccess set license='NONE', termsofuse=concat('New text. ', termsofuse) where id in (<comma separated list of termsanduseofaccess_ids>);

### To find datasets without CC0 and having an empty Terms of Use field:

      select CONCAT('doi:', dvo.authority, '/', dvo.identifier), v.alias as dataverse_alias, case when versionstate='RELEASED' then concat(dv.versionnumber, '.', dv.minorversionnumber) else versionstate END  as version, dv.id as datasetversion_id, t.id as termsofuseandaccess_id, t.termsofuse, t.confidentialitydeclaration, t.specialpermissions, t.restrictions, t.citationrequirements, t.depositorrequirements, t.conditions, t.disclaimer from dvobject dvo, termsofuseandaccess t, datasetversion dv, dataverse v where dv.dataset_id=dvo.id and dv.termsofuseandaccess_id=t.id and dvo.owner_id=v.id and t.license='NONE' and t.termsofuse is null;

These datasets could be updated to use CC0:

    update termsofuseandaccess set license='CC0', confidentialitydeclaration=null, t.specialpermissions=null, t.restrictions=null, citationrequirements=null, depositorrequirements=null, conditions=null, disclaimer=null where id=<id>;
    
or Terms of Use could be added:

    update termsofuseandaccess set termsofuse='New text. ' where id=<id>;

In both cases, the same where id in (`<comma separated list of termsanduseofaccess_ids>`); ending could be used to change multiple datasets/versions at once.

### Standardizing Custom Licenses:

If many datasets use the same set of Custom Terms, it may make sense to create and register a standard license including those terms. Doing this would include:
- Creating and posting an external document that includes the custom terms, i.e. an HTML document with sections corresponding to the terms fields that are used.
- Defining a name, short description, URL (where it is posted), and optionally an icon URL for this license
- Using the Dataverse API to register the new license as one of the options available in your installation
- Using the API to make sure the license is active and deciding whether the license should also be the default
- Once the license is registered with Dataverse, making an SQL update to change datasets/versions using that license to reference it instead of having their own copy of those custom terms.

The benefits of this approach are:
- usability: the license can be selected for new datasets without allowing custom terms and without users having to cut/paste terms or collection administrators having to configure templates with those terms
- efficiency: custom terms are stored per dataset whereas licenses are registered once and all uses of it refer to the same object and external URL
- security: with the license terms maintained external to Dataverse, users cannot edit specific terms and curators do not need to check for edits

Once a standardized version of you Custom Terms are registered as a license, an SQL update like the following can be used to have datasets use it:

    UPDATE termsofuseandaccess
        SET license_id = (SELECT license.id FROM license WHERE license.name = '<Your License Name>'), termsofuse=null, confidentialitydeclaration=null, t.specialpermissions=null, t.restrictions=null, citationrequirements=null, depositorrequirements=null, conditions=null, disclaimer=null 
        WHERE termsofuseandaccess.termsofuse LIKE '%<Unique phrase in your Terms of Use>%';

## Backward Incompatibilities

With the change to support multiple licenses, which can include cases where CC0 is not an option, and the decision to prohibit two previously possible cases (no license and no entry in the Terms of Use field, a standard license and entries in Terms of Use, Special Permissions and related fields), this release contains changes to the display, API payloads, and export metadata that are not backward compatible. These include:
- Use of "CC0 1.0", the short name specified by Creative Commons, for what Dataverse has called the "CC0 Waiver" by default - in the display, API payloads, and export formats including a license name (note that installation admins can alter the license name in the database to maintain the original "CC0 Waiver" text)
- Schema.org metadata in page headers and the Schema.org json-ld metadata export now reference the license via URL (which should avoid the current warning from Google about an invalid license object in the page metadata)
- Metadata exports and import methods (including Sword) use either the license name (e.g. in the JSON export) or URL (e.g. in the OAI_ORE export) rather than a hardcoded value of "CC0" or "CC0 Waiver" currently (if the CC0 license is available, it's default name would be "CC0 1.0")
- API calls (e.g. for import, migrate) that specify both a license and custom terms will be considered an error, as would having no license and an empty/blank value for Terms of Use

Also note that, since CC0 Waiver is no longer a hardcoded option, text strings reference it have been edited or removed from Bundle.properties. This means that the ability to provide translations of the CC0 license name/description has been removed. The initial release of multiple license functionality doesn't include an alternative mechanism to provide translations of license names/descriptions, so this is a regression in capability. (The instructions and help information about license and terms remains internationalizable, it is only the name/description of the licenses themselves that cannot yet be translated).