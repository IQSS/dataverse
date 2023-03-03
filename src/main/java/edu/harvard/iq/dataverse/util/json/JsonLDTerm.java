package edu.harvard.iq.dataverse.util.json;

public class JsonLDTerm {

    JsonLDNamespace namespace = null;

    String term = null;

    String url = null;

    public static JsonLDTerm termsOfUse = JsonLDTerm.DVCore("termsOfUse");
    public static JsonLDTerm confidentialityDeclaration = JsonLDTerm.DVCore("confidentialityDeclaration");
    public static JsonLDTerm specialPermissions = JsonLDTerm.DVCore("specialPermissions");
    public static JsonLDTerm restrictions = JsonLDTerm.DVCore("restrictions");
    public static JsonLDTerm citationRequirements = JsonLDTerm.DVCore("citationRequirements");
    public static JsonLDTerm depositorRequirements = JsonLDTerm.DVCore("depositorRequirements");
    public static JsonLDTerm conditions = JsonLDTerm.DVCore("conditions");
    public static JsonLDTerm disclaimer = JsonLDTerm.DVCore("disclaimer");

    public static JsonLDTerm fileTermsOfAccess = JsonLDTerm.DVCore("fileTermsOfAccess");

    public static JsonLDTerm termsOfAccess = JsonLDTerm.DVCore("termsOfAccess");
    public static JsonLDTerm fileRequestAccess = JsonLDTerm.DVCore("fileRequestAccess");
    public static JsonLDTerm dataAccessPlace = JsonLDTerm.DVCore("dataAccessPlace");
    public static JsonLDTerm originalArchive = JsonLDTerm.DVCore("originalArchive");
    public static JsonLDTerm availabilityStatus = JsonLDTerm.DVCore("availabilityStatus");
    public static JsonLDTerm contactForAccess = JsonLDTerm.DVCore("contactForAccess");
    public static JsonLDTerm sizeOfCollection = JsonLDTerm.DVCore("sizeOfCollection");
    public static JsonLDTerm studyCompletion = JsonLDTerm.DVCore("studyCompletion");

    public static JsonLDTerm restricted = JsonLDTerm.DVCore("restricted");
    public static JsonLDTerm directoryLabel = JsonLDTerm.DVCore("directoryLabel");
    public static JsonLDTerm datasetVersionId = JsonLDTerm.DVCore("datasetVersionId");
    public static JsonLDTerm categories = JsonLDTerm.DVCore("categories");
    public static JsonLDTerm filesize = JsonLDTerm.DVCore("filesize");
    public static JsonLDTerm storageIdentifier = JsonLDTerm.DVCore("storageIdentifier");
    @Deprecated
    public static JsonLDTerm originalFileFormat = JsonLDTerm.DVCore("originalFileFormat");
    @Deprecated
    public static JsonLDTerm originalFormatLabel = JsonLDTerm.DVCore("originalFormatLabel");
    public static JsonLDTerm currentIngestedName= JsonLDTerm.DVCore("currentIngestedName");
    public static JsonLDTerm UNF = JsonLDTerm.DVCore("UNF");
    public static JsonLDTerm rootDataFileId = JsonLDTerm.DVCore("rootDataFileId");
    public static JsonLDTerm previousDataFileId = JsonLDTerm.DVCore("previousDataFileId");
    public static JsonLDTerm checksum = JsonLDTerm.DVCore("checksum");
    public static JsonLDTerm tabularTags = JsonLDTerm.DVCore("tabularTags");

    public static JsonLDTerm totalSize = JsonLDTerm.DVCore("totalSize");
    public static JsonLDTerm fileCount = JsonLDTerm.DVCore("fileCount");
    public static JsonLDTerm maxFileSize = JsonLDTerm.DVCore("maxFileSize");

    public JsonLDTerm(JsonLDNamespace namespace, String term) {
        this.namespace = namespace;
        this.term = term;
    }

    public JsonLDTerm(String term, String url) {
        this.term = term;
        this.url = url;
    }

    public static JsonLDTerm DVCore(String term) {
        return new JsonLDTerm(JsonLDNamespace.dvcore, term);
    }

    public static JsonLDTerm ore(String term) {
        return new JsonLDTerm(JsonLDNamespace.ore, term);
    }

    public static JsonLDTerm schemaOrg(String term) {
        return new JsonLDTerm(JsonLDNamespace.schema, term);
    }

    public static JsonLDTerm dcTerms(String term) {
        return new JsonLDTerm(JsonLDNamespace.dcterms, term);
    }

    public String getLabel() {
        if (namespace == null) {
            return term;
        } else {
            return namespace.getPrefix() + ":" + term;
        }
    }

    public String getUrl() {
        if (namespace == null) {
            return url;
        } else {
            return namespace.getUrl() + term;
        }
    }

    public boolean inNamespace() {
        return (namespace != null);
    }
    
    public JsonLDNamespace getNamespace() {
        return namespace;
    }

}
