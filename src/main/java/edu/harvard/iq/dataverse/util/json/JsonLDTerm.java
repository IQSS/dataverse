package edu.harvard.iq.dataverse.util.json;

import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonValue;

import edu.harvard.iq.dataverse.util.SystemConfig;

public class JsonLDTerm {

    JsonLDNamespace namespace = null;

    String term = null;

    String url = null;
    private static Map<String, JsonLDTerm> translations = new HashMap<String, JsonLDTerm>();

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
    public static JsonLDTerm originalFileFormat = JsonLDTerm.DVCore("originalFileFormat");
    public static JsonLDTerm originalFormatLabel = JsonLDTerm.DVCore("originalFormatLabel");
    public static JsonLDTerm UNF = JsonLDTerm.DVCore("UNF");
    public static JsonLDTerm rootDataFileId = JsonLDTerm.DVCore("rootDataFileId");
    public static JsonLDTerm previousDataFileId = JsonLDTerm.DVCore("previousDataFileId");
    public static JsonLDTerm checksum = JsonLDTerm.DVCore("checksum");
    public static JsonLDTerm tabularTags = JsonLDTerm.DVCore("tabularTags");

    public static JsonLDTerm contact = JsonLDTerm.DVCore("contact");
    public static JsonLDTerm affiliation = JsonLDTerm.DVCore("affiliation");
    public static JsonLDTerm email = JsonLDTerm.DVCore("email");
    public static JsonLDTerm description = JsonLDTerm.DVCore("description");
    public static JsonLDTerm text = JsonLDTerm.DVCore("text");
    public static JsonLDTerm totalSize = JsonLDTerm.DVCore("totalSize");
    public static JsonLDTerm fileCount = JsonLDTerm.DVCore("fileCount");
    public static JsonLDTerm maxFileSize = JsonLDTerm.DVCore("maxFileSize");

    static {
        /*
         * Translations are intended to support project-specific translations for the
         * following use case(s): * A project wishes to use a different vocabulary for
         * export for terms that are not defined in tsv files, e.g. mapping 'DVCore'
         * entries to schema.org or Dublin core
         * 
         * In both cases, the translations map requires the url of the original term and
         * a JSONLDTerm that should be used as a replacement. 
         * Example:
         * 
         * translations.put(JsonLDTerm.contact.getUrl(), JsonLDTerm.dcTerms("subject"));
         * 
         * ToDo - add an api or file to read translations from so they can be set dynamically.
         * 
         * Note: Terms in tsv files can be mapped to external vocabularies directly in the tsv file.
         */

    }

    public JsonLDTerm(JsonLDNamespace namespace, String term) {
        JsonLDTerm translated = translations.get(namespace.getUrl() + term);
        if (translated != null) {
            this.namespace = translated.namespace;
            this.term = translated.term;
            this.url = translated.url;
        } else {
            this.namespace = namespace;
            this.term = term;
        }
    }

    public JsonLDTerm(String term, String url) {
        JsonLDTerm translated = translations.get(url);
        if (translated != null) {
            this.namespace = translated.namespace;
            this.term = translated.term;
            this.url = translated.url;
        }
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

}
