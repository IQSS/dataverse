package edu.harvard.iq.dataverse.util.json;

public class JsonLDTerm {

    JsonLDNamespace namespace = null;
    String term = null;
    String url = null;

    public static JsonLDTerm restricted = JsonLDTerm.DVCore("restricted");
    public static JsonLDTerm directoryLabel = JsonLDTerm.DVCore("directoryLabel");
    public static JsonLDTerm datasetVersionId = JsonLDTerm.DVCore("datasetVersionId");
    public static JsonLDTerm categories = JsonLDTerm.DVCore("categories");
    public static JsonLDTerm filesize = JsonLDTerm.DVCore("filesize");
    public static JsonLDTerm storageIdentifier = JsonLDTerm.DVCore("storageIdentifier");
    public static JsonLDTerm originalFileFormat = JsonLDTerm.DVCore("originalFileFormat");
    public static JsonLDTerm originalFormatLabel = JsonLDTerm.DVCore("originalFormatLabel");
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
