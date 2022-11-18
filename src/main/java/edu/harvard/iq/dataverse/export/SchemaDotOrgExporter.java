package edu.harvard.iq.dataverse.export;

import com.google.auto.service.AutoService;
import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.util.*;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import javax.json.*;
import javax.ws.rs.core.MediaType;

/**
 * Schema.org JSON-LD is used by Google Dataset Search and other services to
 * make datasets more discoverable. It is embedded in the HTML of dataset pages
 * and available as an export format.
 * <p>
 * Do not make any backward incompatible changes unless it's absolutely
 * necessary and list them in the API Guide. The existing list is in the
 * "Native API" section.
 * <p>
 * {@link SchemaDotOrgExporterTest} has most of the tests. See
 * https://schema.org/docs/gs.html#schemaorg_expected for some discussion on
 * what a flexible format Schema.org JSON-LD. Use of tools such as
 * https://search.google.com/structured-data/testing-tool and
 * https://webmaster.yandex.com/tools/microtest/ and
 * http://linter.structured-data.org to make sure Dataverse continues to emit
 * valid output is encouraged but you will find that these tools (and the
 * underlying spec) can be extremely accommodating to fairly radical
 * restructuring of the JSON output. Strings can become objects or arrays, for
 * example, and Honey Badger don't care. Because we expect API users will make
 * use of the JSON output, you should not change it or you will break their
 * code.
 * <p>
 * Copying and pasting output into
 * https://search.google.com/structured-data/testing-tool to make sure it's
 * still valid can get tedious but we are not aware of a better way. We looked
 * at https://github.com/jessedc/ajv-cli (doesn't support JSON-LD, always
 * reports "valid"), https://github.com/jsonld-java/jsonld-java and
 * https://github.com/jsonld-java/jsonld-java-tools (unclear if they support
 * validation), https://github.com/structured-data/linter (couldn't get it
 * installed), https://github.com/json-ld/json-ld.org (couldn't get the test
 * suite to detect changes) , https://tech.yandex.com/validator/ (requires API
 * key),
 * https://packagist.org/packages/padosoft/laravel-google-structured-data-testing-tool
 * (may be promising). We use https://github.com/everit-org/json-schema in our
 * app already to validate JSON Schema but JSON-LD is a different animal.
 * https://schema.org/Dataset.jsonld appears to be the way to download just the
 * "Dataset" definition ( https://schema.org/Dataset ) from schema.org but the
 * official way to download these definitions is from
 * https://schema.org/docs/developers.html#defs . Despite all this
 * experimentation (some of these tools were found at
 * https://medium.com/@vilcins/structured-data-markup-validation-and-testing-tools-1968bd5dea37
 * ), the accepted answer at
 * https://webmasters.stackexchange.com/questions/56577/any-way-to-validate-schema-org-json-ld-before-publishing
 * is to just copy and paste your output into one of the online tools so for
 * now, just do that.
 * <p>
 * Google provides a Schema.org JSON-LD example at
 * https://developers.google.com/search/docs/data-types/dataset but we've also
 * looked at examples from
 * https://zenodo.org/record/1419226/export/schemaorg_jsonld#.W9NJjicpDUI ,
 * https://www.icpsr.umich.edu/icpsrweb/ICPSR/studies/23980/export , and
 * https://doi.pangaea.de/10.1594/PANGAEA.884619
 */
@AutoService(Exporter.class)
public class SchemaDotOrgExporter implements Exporter {

    private static final Logger logger = Logger.getLogger(SchemaDotOrgExporter.class.getCanonicalName());

    public static final String NAME = "schema.org";

    /**
     * Generates a schema.org representation for a given DatasetVersion
     * @param version the DatasetVersion to create the schema.org representation for.
     * @param json not used
     * @param outputStream where the schema.org representation is written to
     * @throws ExportException
     */
    @Override
    public void exportDataset(DatasetVersion version, JsonObject json, OutputStream outputStream) throws ExportException {
        String jsonLdAsString = getJsonLd(version);
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonLdAsString));) {
            JsonObject jsonLdJsonObject = jsonReader.readObject();
            try {
                outputStream.write(jsonLdJsonObject.toString().getBytes("UTF8"));
            } catch (IOException ex) {
                logger.info("IOException calling outputStream.write: " + ex);
            }
            try {
                outputStream.flush();
            } catch (IOException ex) {
                logger.info("IOException calling outputStream.flush: " + ex);
            }
        }
    }


    // The export subsystem assumes there is only
    // one metadata export in a given format per dataset (it uses the current
    // released (published) version. This JSON fragment is generated for a
    // specific released version - and we can have multiple released versions.
    // So something will need to be modified to accommodate this. -- L.A.
    /**
     * We call the export format "Schema.org JSON-LD" and extensive Javadoc can
     * be found in {@link SchemaDotOrgExporter}.
     */
    public String getJsonLd(DatasetVersion version) {
        String jsonLd;

        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("@context", "http://schema.org");
        job.add("@type", "Dataset");
        // Note that whenever you use "@id" you should also use "identifier" and vice versa.
        job.add("@id", version.getDataset().getPersistentURL());
        job.add("identifier", version.getDataset().getPersistentURL());
        job.add("name", version.getTitle());
        JsonArrayBuilder authors = Json.createArrayBuilder();
        for (DatasetAuthor datasetAuthor : version.getDatasetAuthors()) {
            JsonObjectBuilder author = Json.createObjectBuilder();
            String name = datasetAuthor.getName().getDisplayValue();
            DatasetField authorAffiliation = datasetAuthor.getAffiliation();
            String affiliation = null;
            if (authorAffiliation != null) {
                affiliation = datasetAuthor.getAffiliation().getDisplayValue();
            }
            // We are aware of "givenName" and "familyName" but instead of a person it might be an organization such as "Gallup Organization".
            //author.add("@type", "Person");
            author.add("name", name);
            // We are aware that the following error is thrown by https://search.google.com/structured-data/testing-tool
            // "The property affiliation is not recognized by Google for an object of type Thing."
            // Someone at Google has said this is ok.
            // This logic could be moved into the `if (authorAffiliation != null)` block above.
            if (!StringUtil.isEmpty(affiliation)) {
                author.add("affiliation", affiliation);
            }
            String identifierAsUrl = datasetAuthor.getIdentifierAsUrl();
            if (identifierAsUrl != null) {
                // It would be valid to provide an array of identifiers for authors but we have decided to only provide one.
                author.add("@id", identifierAsUrl);
                author.add("identifier", identifierAsUrl);
            }
            authors.add(author);
        }
        JsonArray authorsArray = authors.build();
        /**
         * "creator" is being added along side "author" (below) as an
         * experiment. We think Google Dataset Search might like "creator"
         * better".
         */
        job.add("creator", authorsArray);
        /**
         * "author" is still here for backward compatibility. Depending on how
         * the "creator" experiment above goes, we may deprecate it in the
         * future.
         */
        job.add("author", authorsArray);
        /**
         * We are aware that there is a "datePublished" field but it means "Date
         * of first broadcast/publication." This only makes sense for a 1.0
         * version.
         *
         * TODO: Should we remove the comment above about a 1.0 version? We
         * included this "datePublished" field in Dataverse 4.8.4.
         */
        String datePublished = version.getDataset().getPublicationDateFormattedYYYYMMDD();
        if (datePublished != null) {
            job.add("datePublished", datePublished);
        }

        /**
         * "dateModified" is more appropriate for a version: "The date on which
         * the CreativeWork was most recently modified or when the item's entry
         * was modified within a DataFeed."
         */
        job.add("dateModified", version.getPublicationDateAsString());
        job.add("version", version.getVersionNumber().toString());

        JsonArrayBuilder descriptionsArray = Json.createArrayBuilder();
        List<String> descriptions = version.getDescriptionsPlainText();
        for (String description : descriptions) {
            descriptionsArray.add(description);
        }
        /**
         * In Dataverse 4.8.4 "description" was a single string but now it's an
         * array.
         */
        job.add("description", descriptionsArray);

        /**
         * "keywords" - contains subject(s), datasetkeyword(s) and topicclassification(s)
         * metadata fields for the version. -- L.A.
         * (see #2243 for details/discussion/feedback from Google)
         */
        JsonArrayBuilder keywords = Json.createArrayBuilder();

        for (String subject : version.getDatasetSubjects()) {
            keywords.add(subject);
        }

        for (String topic : version.getTopicClassifications()) {
            keywords.add(topic);
        }

        for (String keyword : version.getKeywords()) {
            keywords.add(keyword);
        }

        job.add("keywords", keywords);

        /**
         * citation: (multiple) related publication citation and URLs, if
         * present.
         *
         * In Dataverse 4.8.4 "citation" was an array of strings but now it's an
         * array of objects.
         */
        List<DatasetRelPublication> relatedPublications = version.getRelatedPublications();
        if (!relatedPublications.isEmpty()) {
            JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
            for (DatasetRelPublication relatedPub : relatedPublications) {
                boolean addToArray = false;
                String pubCitation = relatedPub.getText();
                String pubUrl = relatedPub.getUrl();
                if (pubCitation != null || pubUrl != null) {
                    addToArray = true;
                }
                JsonObjectBuilder citationEntry = Json.createObjectBuilder();
                citationEntry.add("@type", "CreativeWork");
                if (pubCitation != null) {
                    citationEntry.add("text", pubCitation);
                }
                if (pubUrl != null) {
                    citationEntry.add("@id", pubUrl);
                    citationEntry.add("identifier", pubUrl);
                }
                if (addToArray) {
                    jsonArrayBuilder.add(citationEntry);
                }
            }
            JsonArray jsonArray = jsonArrayBuilder.build();
            if (!jsonArray.isEmpty()) {
                job.add("citation", jsonArray);
            }
        }

        /**
         * temporalCoverage:
         * (if available)
         */

        List<String> timePeriodsCovered = version.getTimePeriodsCovered();
        if (timePeriodsCovered.size() > 0) {
            JsonArrayBuilder temporalCoverage = Json.createArrayBuilder();
            for (String timePeriod : timePeriodsCovered) {
                temporalCoverage.add(timePeriod);
            }
            job.add("temporalCoverage", temporalCoverage);
        }

        /**
         * https://schema.org/version/3.4/ says, "Note that schema.org release
         * numbers are not generally included when you use schema.org. In
         * contexts (e.g. related standards work) when a particular release
         * needs to be cited, this document provides the appropriate URL."
         *
         * For the reason above we decided to take out schemaVersion but we're
         * leaving this Javadoc in here to remind us that we made this decision.
         * We used to include "https://schema.org/version/3.3" in the output for
         * "schemaVersion".
         */
        TermsOfUseAndAccess terms = version.getTermsOfUseAndAccess();
        if (terms != null) {
            job.add("license", DatasetUtil.getLicenseURI(version));
        }

        job.add("includedInDataCatalog", Json.createObjectBuilder()
                .add("@type", "DataCatalog")
                .add("name", BrandingUtil.getRootDataverseCollectionName())
                .add("url", SystemConfig.getDataverseSiteUrlStatic())
        );

        String installationBrandName = BrandingUtil.getInstallationBrandName();
        /**
         * Both "publisher" and "provider" are included but they have the same
         * values. Some services seem to prefer one over the other.
         */
        job.add("publisher", Json.createObjectBuilder()
                .add("@type", "Organization")
                .add("name", installationBrandName)
        );
        job.add("provider", Json.createObjectBuilder()
                .add("@type", "Organization")
                .add("name", installationBrandName)
        );

        List<String> funderNames = version.getFunders();
        if (!funderNames.isEmpty()) {
            JsonArrayBuilder funderArray = Json.createArrayBuilder();
            for (String funderName : funderNames) {
                JsonObjectBuilder funder = NullSafeJsonBuilder.jsonObjectBuilder();
                funder.add("@type", "Organization");
                funder.add("name", funderName);
                funderArray.add(funder);
            }
            job.add("funder", funderArray);
        }

        boolean commaSeparated = true;
        List<String> spatialCoverages = version.getSpatialCoverages(commaSeparated);
        if (!spatialCoverages.isEmpty()) {
            JsonArrayBuilder spatialArray = Json.createArrayBuilder();
            for (String spatialCoverage : spatialCoverages) {
                spatialArray.add(spatialCoverage);
            }
            job.add("spatialCoverage", spatialArray);
        }

        List<FileMetadata> fileMetadatasSorted = version.getFileMetadatasSorted();
        if (fileMetadatasSorted != null && !fileMetadatasSorted.isEmpty()) {
            JsonArrayBuilder fileArray = Json.createArrayBuilder();
            String dataverseSiteUrl = SystemConfig.getDataverseSiteUrlStatic();
            for (FileMetadata fileMetadata : fileMetadatasSorted) {
                JsonObjectBuilder fileObject = NullSafeJsonBuilder.jsonObjectBuilder();
                String filePidUrlAsString = null;
                URL filePidUrl = fileMetadata.getDataFile().getGlobalId().toURL();
                if (filePidUrl != null) {
                    filePidUrlAsString = filePidUrl.toString();
                }
                fileObject.add("@type", "DataDownload");
                fileObject.add("name", fileMetadata.getLabel());
                fileObject.add("fileFormat", fileMetadata.getDataFile().getContentType());
                fileObject.add("contentSize", fileMetadata.getDataFile().getFilesize());
                fileObject.add("description", fileMetadata.getDescription());
                fileObject.add("@id", filePidUrlAsString);
                fileObject.add("identifier", filePidUrlAsString);
                String hideFilesBoolean = System.getProperty(SystemConfig.FILES_HIDE_SCHEMA_DOT_ORG_DOWNLOAD_URLS);
                if (hideFilesBoolean != null && hideFilesBoolean.equals("true")) {
                    // no-op
                } else {
                    if (FileUtil.isPubliclyDownloadable(fileMetadata)) {
                        String nullDownloadType = null;
                        fileObject.add("contentUrl", dataverseSiteUrl + FileUtil.getFileDownloadUrlPath(nullDownloadType, fileMetadata.getDataFile().getId(), false, fileMetadata.getId()));
                    }
                }
                fileArray.add(fileObject);
            }
            job.add("distribution", fileArray);
        }
        jsonLd = job.build().toString();

        //Most fields above should be stripped/sanitized but, since this is output in the dataset page as header metadata, do a final sanitize step to make sure
        jsonLd = MarkupChecker.stripAllTags(jsonLd);

        return jsonLd;
    }

    @Override
    public String getProviderName() {
        return NAME;
    }

    @Override
    public String getDisplayName() {
        return BundleUtil.getStringFromBundle("dataset.exportBtn.itemLabel.schemaDotOrg");
    }

    @Override
    public Boolean isXMLFormat() {
        return false;
    }

    @Override
    public Boolean isHarvestable() {
        // Defer harvesting because the current effort was estimated as a "2": https://github.com/IQSS/dataverse/issues/3700
        return false;
    }

    @Override
    public Boolean isAvailableToUsers() {
        return true;
    }

    @Override
    public String getXMLNameSpace() throws ExportException {
        throw new ExportException(SchemaDotOrgExporter.class.getSimpleName() + ": not an XML format.");
    }

    @Override
    public String getXMLSchemaLocation() throws ExportException {
        throw new ExportException(SchemaDotOrgExporter.class.getSimpleName() + ": not an XML format.");
    }

    @Override
    public String getXMLSchemaVersion() throws ExportException {
        throw new ExportException(SchemaDotOrgExporter.class.getSimpleName() + ": not an XML format.");
    }

    @Override
    public void setParam(String name, Object value) {
        // this exporter doesn't need/doesn't currently take any parameters
    }
    

    @Override
    public String getMediaType() {
        return MediaType.APPLICATION_JSON;
    }

}
