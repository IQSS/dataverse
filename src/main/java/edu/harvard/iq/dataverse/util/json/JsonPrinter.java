package edu.harvard.iq.dataverse.util.json;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetDistributor;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.DataverseTheme;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.api.Util;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddressRange;
import edu.harvard.iq.dataverse.authorization.groups.impl.shib.ShibGroup;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderRow;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.util.DatasetFieldWalker;
import edu.harvard.iq.dataverse.util.StringUtil;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import java.util.ArrayList;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import static java.util.stream.Collectors.toList;
import javax.json.JsonArray;
import javax.json.JsonObject;

/**
 * Convert objects to Json.
 *
 * @author michael
 */
public class JsonPrinter {

    public static final BriefJsonPrinter brief = new BriefJsonPrinter();

    public static JsonArrayBuilder asJsonArray(Collection<String> strings) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        for (String s : strings) {
            arr.add(s);
        }
        return arr;
    }

    public static JsonObjectBuilder json(User u) {
        RoleAssigneeDisplayInfo displayInfo = u.getDisplayInfo();
        return jsonObjectBuilder()
                .add("identifier", u.getIdentifier())
                .add("displayInfo", jsonObjectBuilder()
                        .add("Title", displayInfo.getTitle())
                        .add("email", displayInfo.getEmailAddress()));
    }

    /**
     * @todo Rename this to just "json" to match the other methods once "json(
     * Dataverse dv )" is reviewed since in calls the "json( User u )" version
     * and we want to keep it that way rather than calling this method.
     */
    public static JsonObjectBuilder jsonForAuthUser(AuthenticatedUser authenticatedUser) {
        return jsonObjectBuilder()
                .add("id", authenticatedUser.getId())
                .add("identifier", authenticatedUser.getIdentifier())
                .add("displayName", authenticatedUser.getDisplayInfo().getTitle())
                .add("firstName", authenticatedUser.getFirstName())
                .add("lastName", authenticatedUser.getLastName())
                .add("email", authenticatedUser.getEmail())
                .add("superuser", authenticatedUser.isSuperuser())
                .add("affiliation", authenticatedUser.getAffiliation())
                .add("position", authenticatedUser.getPosition())
                .add("persistentUserId", authenticatedUser.getAuthenticatedUserLookup().getPersistentUserId())
                .add("emailLastConfirmed", authenticatedUser.getEmailConfirmed())
                .add("authenticationProviderId", authenticatedUser.getAuthenticatedUserLookup().getAuthenticationProviderId());
    }
    
    public static JsonObjectBuilder json( RoleAssignment ra ) {
		return jsonObjectBuilder()
				.add("id", ra.getId())
				.add("assignee", ra.getAssigneeIdentifier() )
				.add("roleId", ra.getRole().getId() )
				.add("_roleAlias", ra.getRole().getAlias())
				.add("privateUrlToken", ra.getPrivateUrlToken())
				.add("definitionPointId", ra.getDefinitionPoint().getId() );
	}
	
	public static JsonArrayBuilder json( Set<Permission> permissions ) {
		JsonArrayBuilder bld = Json.createArrayBuilder();
		for ( Permission p : permissions ) {
			bld.add( p.name() );
		}
		return bld;
	}
    
    public static JsonObjectBuilder json( RoleAssigneeDisplayInfo d ) {
        return jsonObjectBuilder()
                .add("title", d.getTitle())
                .add("email", d.getEmailAddress())
                .add("affiliation", d.getAffiliation());
    }

    public static JsonObjectBuilder json(IpGroup grp) {
         // collect single addresses
        List<String> singles = grp.getRanges().stream().filter( IpAddressRange::isSingleAddress )
                                .map( IpAddressRange::getBottom )
                                .map( IpAddress::toString ).collect(toList());
        // collect "real" ranges
        List<List<String>> ranges = grp.getRanges().stream().filter( rng -> !rng.isSingleAddress() )
                                .map( rng -> Arrays.asList(rng.getBottom().toString(), rng.getTop().toString()) )
                                .collect(toList());

        JsonObjectBuilder bld = jsonObjectBuilder()
                .add("alias", grp.getPersistedGroupAlias() )
                .add("identifier", grp.getIdentifier())
                .add("id", grp.getId() )
                .add("name", grp.getDisplayName() )
                .add("description", grp.getDescription() );
       
        if ( ! singles.isEmpty() ) {
            bld.add("addresses", asJsonArray(singles) );
        }
        
        if ( ! ranges.isEmpty() ) {
            JsonArrayBuilder rangesBld = Json.createArrayBuilder();
            ranges.forEach( r -> rangesBld.add( Json.createArrayBuilder().add(r.get(0)).add(r.get(1))) );
            bld.add("ranges", rangesBld );
        }
        
        return bld;
    }

    public static JsonObjectBuilder json(ShibGroup grp) {
        return jsonObjectBuilder()
                .add("name", grp.getName())
                .add("attribute", grp.getAttribute())
                .add("pattern", grp.getPattern())
                .add("id", grp.getId());
    }

    public static JsonArrayBuilder rolesToJson(List<DataverseRole> role) {
        JsonArrayBuilder bld = Json.createArrayBuilder();
        for (DataverseRole r : role) {
            bld.add(json(r));
        }
        return bld;
    }

    public static JsonObjectBuilder json(DataverseRole role) {
        JsonObjectBuilder bld = jsonObjectBuilder()
                .add("alias", role.getAlias())
                .add("name", role.getName())
                .add("permissions", json(role.permissions()))
                .add("description", role.getDescription());
        if (role.getId() != null) {
            bld.add("id", role.getId());
        }
        if (role.getOwner() != null && role.getOwner().getId() != null) {
            bld.add("ownerId", role.getOwner().getId());
        }

        return bld;
    }

    public static JsonObjectBuilder json(Dataverse dv) {
        JsonObjectBuilder bld = jsonObjectBuilder()
                .add("id", dv.getId())
                .add("alias", dv.getAlias())
                .add("name", dv.getName())
                .add("affiliation", dv.getAffiliation())
                .add("dataverseContacts", json(dv.getDataverseContacts()))
                .add("permissionRoot", dv.isPermissionRoot())
                .add("description", dv.getDescription())
                .add("dataverseType", dv.getDataverseType().name());
        if (dv.getOwner() != null) {
            bld.add("ownerId", dv.getOwner().getId());
        }
        if (dv.getCreateDate() != null) {
            bld.add("creationDate", Util.getDateTimeFormat().format(dv.getCreateDate()));
        }
        if (dv.getCreator() != null) {
            bld.add("creator", json(dv.getCreator()));
        }
        if (dv.getDataverseTheme() != null) {
            bld.add("theme", json(dv.getDataverseTheme()));
        }

        return bld;
    }

    public static JsonArrayBuilder json(List<DataverseContact> dataverseContacts) {
        return dataverseContacts.stream()
                .map( dc -> jsonObjectBuilder()
                        .add("displayOrder", dc.getDisplayOrder())
                        .add("contactEmail", dc.getContactEmail())
                ).collect( toJsonArray() );
    }
    
    public static JsonObjectBuilder json( DataverseTheme theme ) {
        final NullSafeJsonBuilder baseObject = jsonObjectBuilder()
                .add("id", theme.getId() )
                .add("logo", theme.getLogo())
                .add("tagline", theme.getTagline())
                .add("linkUrl", theme.getLinkUrl())
                .add("linkColor", theme.getLinkColor())
                .add("textColor", theme.getTextColor())
                .add("backgroundColor", theme.getBackgroundColor());
        if ( theme.getLogoAlignment() != null ) {
            baseObject.add("logoBackgroundColor", theme.getLogoBackgroundColor());
        }
        return baseObject;
    }

    public static JsonObjectBuilder json(BuiltinUser user) {
        return (user == null)
                ? null
                : jsonObjectBuilder()
                .add("id", user.getId())
                .add("firstName", user.getFirstName())
                .add("lastName", user.getLastName())
                .add("userName", user.getUserName())
                .add("affiliation", user.getAffiliation())
                .add("position", user.getPosition())
                .add("email", user.getEmail());
    }

    public static JsonObjectBuilder json(Dataset ds) {
        return jsonObjectBuilder()
                .add("id", ds.getId())
                .add("identifier", ds.getIdentifier())
                .add("persistentUrl", ds.getPersistentURL())
                .add("protocol", ds.getProtocol())
                .add("authority", ds.getAuthority())
                .add("publisher", getRootDataverseNameforCitation(ds))
                .add("publicationDate", ds.getPublicationDateFormattedYYYYMMDD());
    }

    public static JsonObjectBuilder json(DatasetVersion dsv) {
        JsonObjectBuilder bld = jsonObjectBuilder()
                .add("id", dsv.getId())
                .add("versionNumber", dsv.getVersionNumber())
                .add("versionMinorNumber", dsv.getMinorVersionNumber())
                .add("versionState", dsv.getVersionState().name())
                .add("versionNote", dsv.getVersionNote())
                .add("archiveNote", dsv.getArchiveNote())
                .add("deaccessionLink", dsv.getDeaccessionLink())
                .add("distributionDate", dsv.getDistributionDate())
                .add("productionDate", dsv.getProductionDate())
                .add("UNF", dsv.getUNF())
                .add("archiveTime", format(dsv.getArchiveTime()))
                .add("lastUpdateTime", format(dsv.getLastUpdateTime()))
                .add("releaseTime", format(dsv.getReleaseTime()))
                .add("createTime", format(dsv.getCreateTime()))
                .add("license", dsv.getTermsOfUseAndAccess().getLicense() != null ? dsv.getTermsOfUseAndAccess().getLicense().toString() : null)
                .add("termsOfUse", getLicenseInfo(dsv))
                .add("confidentialityDeclaration", dsv.getTermsOfUseAndAccess().getConfidentialityDeclaration() != null ? dsv.getTermsOfUseAndAccess().getConfidentialityDeclaration() : null)
                .add("availabilityStatus", dsv.getTermsOfUseAndAccess().getAvailabilityStatus() != null ? dsv.getTermsOfUseAndAccess().getAvailabilityStatus() : null)
                .add("specialPermissions", dsv.getTermsOfUseAndAccess().getSpecialPermissions() != null ? dsv.getTermsOfUseAndAccess().getSpecialPermissions() : null)
                .add("restrictions", dsv.getTermsOfUseAndAccess().getRestrictions() != null ? dsv.getTermsOfUseAndAccess().getRestrictions() : null)
                .add("citationRequirements", dsv.getTermsOfUseAndAccess().getCitationRequirements() != null ? dsv.getTermsOfUseAndAccess().getCitationRequirements() : null)
                .add("depositorRequirements", dsv.getTermsOfUseAndAccess().getDepositorRequirements() != null ? dsv.getTermsOfUseAndAccess().getDepositorRequirements() : null)
                .add("conditions", dsv.getTermsOfUseAndAccess().getConditions() != null ? dsv.getTermsOfUseAndAccess().getConditions() : null)
                .add("disclaimer", dsv.getTermsOfUseAndAccess().getDisclaimer() != null ? dsv.getTermsOfUseAndAccess().getDisclaimer() : null)
                .add("termsOfAccess", dsv.getTermsOfUseAndAccess().getTermsOfAccess() != null ? dsv.getTermsOfUseAndAccess().getTermsOfAccess() : null)
                .add("dataAccessPlace", dsv.getTermsOfUseAndAccess().getDataAccessPlace() != null ? dsv.getTermsOfUseAndAccess().getDataAccessPlace() : null)
                .add("originalArchive", dsv.getTermsOfUseAndAccess().getOriginalArchive() != null ? dsv.getTermsOfUseAndAccess().getOriginalArchive() : null)
                .add("availabilityStatus", dsv.getTermsOfUseAndAccess().getAvailabilityStatus() != null ? dsv.getTermsOfUseAndAccess().getAvailabilityStatus() : null)
                .add("contactForAccess", dsv.getTermsOfUseAndAccess().getContactForAccess() != null ? dsv.getTermsOfUseAndAccess().getContactForAccess() : null)
                .add("sizeOfCollection", dsv.getTermsOfUseAndAccess().getSizeOfCollection() != null ? dsv.getTermsOfUseAndAccess().getSizeOfCollection() : null)
                .add("studyCompletion", dsv.getTermsOfUseAndAccess().getStudyCompletion() != null ? dsv.getTermsOfUseAndAccess().getStudyCompletion() : null);

        bld.add("metadataBlocks", jsonByBlocks(dsv.getDatasetFields()));

        bld.add("files", jsonFileMetadatas(dsv.getFileMetadatas()));

        return bld;
    }
    
    private static String getRootDataverseNameforCitation(Dataset dataset) {
        Dataverse root = dataset.getOwner();
        while (root.getOwner() != null) {
            root = root.getOwner();
        }
        String rootDataverseName = root.getName();
        if (!StringUtil.isEmpty(rootDataverseName)) {
            return rootDataverseName + " Dataverse";
        } else {
            return "";
        }
    }
    
    private static String getLicenseInfo(DatasetVersion dsv) {
        if (dsv.getTermsOfUseAndAccess().getLicense() != null && dsv.getTermsOfUseAndAccess().getLicense().equals(TermsOfUseAndAccess.License.CC0)) {
            return "CC0 Waiver";
        }
        return dsv.getTermsOfUseAndAccess().getTermsOfUse();
    }

    /**
     * Export formats such as DDI require the citation to be included. See
     * https://github.com/IQSS/dataverse/issues/2579 for more on DDI export.
     *
     * @todo Instead of having this separate method, should "citation" be added
     * to the regular `json` method for DatasetVersion? Will anything break?
     * Unit tests for that method could not be found.
     */
    public static JsonObjectBuilder jsonWithCitation(DatasetVersion dsv) {
        JsonObjectBuilder dsvWithCitation = json(dsv);
        dsvWithCitation.add("citation", dsv.getCitation());
        return dsvWithCitation;
    }

    /**
     * Export formats such as DDI require the persistent identifier components
     * such as "protocol", "authority" and "identifier" to be included so we
     * create a JSON object we can convert to a DatasetDTO which can include a
     * DatasetVersionDTO, which has all the metadata fields we need to export.
     * See https://github.com/IQSS/dataverse/issues/2579 for more on DDI export.
     *
     * @todo Instead of having this separate method, should "datasetVersion" be
     * added to the regular `json` method for Dataset? Will anything break? Unit
     * tests for that method could not be found. If we keep this method as-is
     * should the method be renamed?
     */
    public static JsonObjectBuilder jsonAsDatasetDto(DatasetVersion dsv) {
        JsonObjectBuilder datasetDtoAsJson = json(dsv.getDataset());
        datasetDtoAsJson.add("datasetVersion", jsonWithCitation(dsv));
        return datasetDtoAsJson;
    }

    public static JsonArrayBuilder jsonFileMetadatas(Collection<FileMetadata> fmds) {
        JsonArrayBuilder filesArr = Json.createArrayBuilder();
        for (FileMetadata fmd : fmds) {
            filesArr.add(json(fmd));
        }
        return filesArr;
    }

    public static JsonObjectBuilder json(DatasetDistributor dist) {
        return jsonObjectBuilder()
                .add("displayOrder", dist.getDisplayOrder())
                .add("version", dist.getVersion())
                .add("abbreviation", json(dist.getAbbreviation()))
                .add("affiliation", json(dist.getAffiliation()))
                .add("logo", json(dist.getLogo()))
                .add("name", json(dist.getName()))
                .add("url", json(dist.getUrl()));
    }

    public static JsonObjectBuilder jsonByBlocks(List<DatasetField> fields) {
        JsonObjectBuilder blocksBld = jsonObjectBuilder();

        for (Map.Entry<MetadataBlock, List<DatasetField>> blockAndFields : DatasetField.groupByBlock(fields).entrySet()) {
            MetadataBlock block = blockAndFields.getKey();
            blocksBld.add(block.getName(), json(block, blockAndFields.getValue()));
        }
        return blocksBld;
    }

    /**
     * Create a JSON object for the block and its fields. The fields are assumed
     * to belong to the block - there's no checking of that in the method.
     *
     * @param block
     * @param fields
     * @return JSON Object builder with the block and fields information.
     */
    public static JsonObjectBuilder json(MetadataBlock block, List<DatasetField> fields) {
        JsonObjectBuilder blockBld = jsonObjectBuilder();

        blockBld.add("displayName", block.getDisplayName());
        final JsonArrayBuilder fieldsArray = Json.createArrayBuilder();

        DatasetFieldWalker.walk(fields, new DatasetFieldsToJson(fieldsArray));

        blockBld.add("fields", fieldsArray);
        return blockBld;
    }

    public static String typeClassString(DatasetFieldType typ) {
        if (typ.isControlledVocabulary()) {
            return "controlledVocabulary";
        }
        if (typ.isCompound()) {
            return "compound";
        }
        return "primitive";
    }

    public static JsonObject json(DatasetField dfv) {
        if (dfv.isEmpty()) {
            return null;
        } else {
            JsonArrayBuilder fieldArray = Json.createArrayBuilder();
            DatasetFieldWalker.walk(dfv, new DatasetFieldsToJson(fieldArray));
            JsonArray out = fieldArray.build();
            return out.getJsonObject(0);
        }
    }

    public static JsonObjectBuilder json(MetadataBlock blk) {
        JsonObjectBuilder bld = jsonObjectBuilder();
        bld.add("id", blk.getId());
        bld.add("name", blk.getName());
        bld.add("displayName", blk.getDisplayName());

        JsonObjectBuilder fieldsBld = jsonObjectBuilder();
        for (DatasetFieldType df : new TreeSet<>(blk.getDatasetFieldTypes())) {
            fieldsBld.add(df.getName(), json(df));
        }

        bld.add("fields", fieldsBld);

        return bld;
    }

    public static JsonObjectBuilder json(DatasetFieldType fld) {
        JsonObjectBuilder fieldsBld = jsonObjectBuilder();
        fieldsBld.add("name", fld.getName());
        fieldsBld.add("displayName", fld.getDisplayName());
        fieldsBld.add("title", fld.getTitle());
        fieldsBld.add("type", fld.getFieldType().toString());
        fieldsBld.add("watermark", fld.getWatermark());
        fieldsBld.add("description", fld.getDescription());
        if (!fld.getChildDatasetFieldTypes().isEmpty()) {
            JsonObjectBuilder subFieldsBld = jsonObjectBuilder();
            for (DatasetFieldType subFld : fld.getChildDatasetFieldTypes()) {
                subFieldsBld.add(subFld.getName(), json(subFld));
            }
            fieldsBld.add("childFields", subFieldsBld);
        }

        return fieldsBld;
    }

    public static JsonObjectBuilder json(FileMetadata fmd) {
        return jsonObjectBuilder()
                // deprecated: .add("category", fmd.getCategory())
                // TODO: uh, figure out what to do here... it's deprecated 
                // in a sense that there's no longer the category field in the 
                // fileMetadata object; but there are now multiple, oneToMany file 
                // categories - and we probably need to export them too!) -- L.A. 4.5
                .add("description", fmd.getDescription())
                .add("label", fmd.getLabel()) // "label" is the filename
                .add("directoryLabel", fmd.getDirectoryLabel())
                .add("version", fmd.getVersion())
                .add("datasetVersionId", fmd.getDatasetVersion().getId())
                .add("dataFile", json(fmd.getDataFile(), fmd));
    }

    public static JsonObjectBuilder json(DataFile df) {
        return json(df, null);
    }
    
    public static JsonObjectBuilder json(DataFile df, FileMetadata fileMetadata) {
        // File names are no longer stored in the DataFile entity; 
        // (they are instead in the FileMetadata (as "labels") - this way 
        // the filename can change between versions... 
        // It does appear that for some historical purpose we still need the
        // filename in the file DTO (?)... We rely on it to be there for the 
        // DDI export, for example. So we need to make sure this is is the 
        // *correct* file name - i.e., that it comes from the right version. 
        // (TODO...? L.A. 4.5, Aug 7 2016)
        String fileName = null;
        
        if (fileMetadata != null) {
            fileName = fileMetadata.getLabel();
        } else if (df.getFileMetadata() != null) {
            // Note that this may not necessarily grab the file metadata from the 
            // version *you want*! (L.A.)
            fileName = df.getFileMetadata().getLabel();
        }
        
        return jsonObjectBuilder()
                .add("id", df.getId())
                .add("filename", fileName)
                .add("contentType", df.getContentType())
                .add("storageIdentifier", df.getStorageIdentifier())
                .add("originalFileFormat", df.getOriginalFileFormat())
                .add("originalFormatLabel", df.getOriginalFormatLabel())
                .add("UNF", df.getUnf())
                /**
                 * @todo Should we deprecate "md5" now that it's under
                 * "checksum" (which may also be a SHA-1 rather than an MD5)?
                 */
                .add("md5", getMd5IfItExists(df.getChecksumType(), df.getChecksumValue()))
                .add("checksum", getChecksumTypeAndValue(df.getChecksumType(), df.getChecksumValue()))
                .add("description", df.getDescription());
    }

    public static String format(Date d) {
        return (d == null) ? null : Util.getDateTimeFormat().format(d);
    }

    private static class DatasetFieldsToJson implements DatasetFieldWalker.Listener {

        Deque<JsonObjectBuilder> objectStack = new LinkedList<>();
        Deque<JsonArrayBuilder> valueArrStack = new LinkedList<>();
        JsonObjectBuilder result = null;

        DatasetFieldsToJson(JsonArrayBuilder result) {
            valueArrStack.push(result);
        }

        @Override
        public void startField(DatasetField f) {
            objectStack.push(jsonObjectBuilder());
            // Invariant: all values are multiple. Diffrentiation between multiple and single is done at endField.
            valueArrStack.push(Json.createArrayBuilder());

            DatasetFieldType typ = f.getDatasetFieldType();
            objectStack.peek().add("typeName", typ.getName());
            objectStack.peek().add("multiple", typ.isAllowMultiples());
            objectStack.peek().add("typeClass", typeClassString(typ));
        }

        @Override
        public void endField(DatasetField f) {
            JsonObjectBuilder jsonField = objectStack.pop();
            JsonArray jsonValues = valueArrStack.pop().build();
            if (!jsonValues.isEmpty()) {
                jsonField.add("value",
                        f.getDatasetFieldType().isAllowMultiples() ? jsonValues
                                : jsonValues.get(0));
                valueArrStack.peek().add(jsonField);
            }
        }

        @Override
        public void primitiveValue(DatasetFieldValue dsfv) {
            if (dsfv.getValue() != null) {
                valueArrStack.peek().add(dsfv.getValue());
            }
        }

        @Override
        public void controledVocabularyValue(ControlledVocabularyValue cvv) {
            valueArrStack.peek().add(cvv.getStrValue());
        }

        @Override
        public void startCompoundValue(DatasetFieldCompoundValue dsfcv) {
            valueArrStack.push(Json.createArrayBuilder());
        }

        @Override
        public void endCompoundValue(DatasetFieldCompoundValue dsfcv) {
            JsonArray jsonValues = valueArrStack.pop().build();
            if (!jsonValues.isEmpty()) {
                JsonObjectBuilder jsonField = jsonObjectBuilder();
                for (JsonObject jobj : jsonValues.getValuesAs(JsonObject.class)) {
                    jsonField.add(jobj.getString("typeName"), jobj);
                }
                valueArrStack.peek().add(jsonField);
            }
        }
    }

    public static JsonObjectBuilder json(AuthenticationProviderRow aRow) {
        return jsonObjectBuilder()
                        .add("id", aRow.getId())
                        .add("factoryAlias", aRow.getFactoryAlias() )
                        .add("title", aRow.getTitle())
                        .add("subtitle",aRow.getSubtitle())
                        .add("factoryData", aRow.getFactoryData())
                        .add("enabled", aRow.isEnabled())
                ;
    }

    public static JsonObjectBuilder json(PrivateUrl privateUrl) {
        return jsonObjectBuilder()
                // We provide the token here as a convenience even though it is also in the role assignment.
                .add("token", privateUrl.getToken())
                .add("link", privateUrl.getLink())
                .add("roleAssignment", json(privateUrl.getRoleAssignment()));
    }

    public static JsonObjectBuilder json( ExplicitGroup eg ) {
        JsonArrayBuilder ras = Json.createArrayBuilder();
            for (String u : eg.getContainedRoleAssgineeIdentifiers()) {
                ras.add(u);
            }
            return jsonObjectBuilder()
                    .add("identifier", eg.getIdentifier())
                    .add("groupAliasInOwner", eg.getGroupAliasInOwner())
                    .add("owner", eg.getOwner().getId())
                    .add("description", eg.getDescription())
                    .add("displayName", eg.getDisplayName())
                    .add("containedRoleAssignees", ras);
    }
    
    public static JsonObjectBuilder json( DataverseFacet aFacet ) {
        return jsonObjectBuilder()
                    .add("id", String.valueOf(aFacet.getId())) // TODO should just be id I think
                    .add("name", aFacet.getDatasetFieldType().getDisplayName());
    }
        
    public static Collector<String, JsonArrayBuilder, JsonArrayBuilder> stringsToJsonArray() {
        return new Collector<String, JsonArrayBuilder, JsonArrayBuilder>() {

            @Override
            public Supplier<JsonArrayBuilder> supplier() {
                return () -> Json.createArrayBuilder();
            }

            @Override
            public BiConsumer<JsonArrayBuilder, String> accumulator() {
                return (JsonArrayBuilder b, String s) -> b.add(s);
            }

            @Override
            public BinaryOperator<JsonArrayBuilder> combiner() {
                return (jab1, jab2) -> {
                    JsonArrayBuilder retVal = Json.createArrayBuilder();
                    jab1.build().forEach(retVal::add);
                    jab2.build().forEach(retVal::add);
                    return retVal;
                };
            }

            @Override
            public Function<JsonArrayBuilder, JsonArrayBuilder> finisher() {
                return Function.identity();
            }

            @Override
            public Set<Collector.Characteristics> characteristics() {
                return EnumSet.of(Collector.Characteristics.IDENTITY_FINISH);
            }
        };
    }

    public static Collector<JsonObjectBuilder, ArrayList<JsonObjectBuilder>, JsonArrayBuilder> toJsonArray() {
        return new Collector<JsonObjectBuilder, ArrayList<JsonObjectBuilder>, JsonArrayBuilder>() {

            @Override
            public Supplier<ArrayList<JsonObjectBuilder>> supplier() {
                return () -> new ArrayList<>();
            }

            @Override
            public BiConsumer<ArrayList<JsonObjectBuilder>, JsonObjectBuilder> accumulator() {
                return (t, u) ->t.add(u);
            }

            @Override
            public BinaryOperator<ArrayList<JsonObjectBuilder>> combiner() {
                return (jab1, jab2) -> {
                    jab1.addAll(jab2);
                    return jab1;
                };
            }

            @Override
            public Function<ArrayList<JsonObjectBuilder>, JsonArrayBuilder> finisher() {
                return (l) -> {
                  JsonArrayBuilder bld = Json.createArrayBuilder();
                  l.forEach( bld::add );
                  return bld;
                };
            }

            @Override
            public Set<Collector.Characteristics> characteristics() {
                return Collections.emptySet();
            }
        };
    }

    public static String getMd5IfItExists(DataFile.ChecksumType checksumType, String checksumValue) {
        if (DataFile.ChecksumType.MD5.equals(checksumType)) {
            return checksumValue;
        } else {
            return null;
        }
    }

    public static JsonObjectBuilder getChecksumTypeAndValue(DataFile.ChecksumType checksumType, String checksumValue) {
        if (checksumType != null) {
            return Json.createObjectBuilder()
                    .add("type", checksumType.toString())
                    .add("value", checksumValue);
        } else {
            return null;
        }
    }

}
