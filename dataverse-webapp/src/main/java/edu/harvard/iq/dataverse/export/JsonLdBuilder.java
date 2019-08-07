package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.common.BrandingUtil;
import edu.harvard.iq.dataverse.common.NullSafeJsonBuilder;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetAuthor;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRelPublication;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.util.FileUtil;
import org.apache.commons.lang3.StringUtils;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import java.net.URL;
import java.util.List;

public class JsonLdBuilder {
    // TODO: Consider moving this comment into the Exporter code.
    // The export subsystem assumes there is only
    // one metadata export in a given format per dataset (it uses the current 
    // released (published) version. This JSON fragment is generated for a 
    // specific released version - and we can have multiple released versions. 
    // So something will need to be modified to accommodate this. -- L.A.  

    /**
     * We call the export format "Schema.org JSON-LD" and extensive Javadoc can
     * be found in {@link SchemaDotOrgExporter}.
     */
    public static String buildJsonLd(DatasetVersion datasetVersion,  String dataverseSiteUrl, String hideSchemaDotOrgDownloadUrls) {
        // We show published datasets only for "datePublished" field below.
        if (!datasetVersion.isPublished()) {
            return "";
        }
        
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("@context", "http://schema.org");
        job.add("@type", "Dataset");
        // Note that whenever you use "@id" you should also use "identifier" and vice versa.
        job.add("@id", datasetVersion.getDataset().getPersistentURL());
        job.add("identifier", datasetVersion.getDataset().getPersistentURL());
        job.add("name", datasetVersion.getTitle());
        JsonArrayBuilder authors = Json.createArrayBuilder();
        for (DatasetAuthor datasetAuthor : datasetVersion.getDatasetAuthors()) {
            JsonObjectBuilder author = Json.createObjectBuilder();
            String name = datasetAuthor.getName().getValue();
            DatasetField authorAffiliation = datasetAuthor.getAffiliation();
            String affiliation = null;
            if (authorAffiliation != null) {
                affiliation = datasetAuthor.getAffiliation().getValue();
            }
            // We are aware of "givenName" and "familyName" but instead of a person it might be an organization such as "Gallup Organization".
            //author.add("@type", "Person");
            author.add("name", name);
            // We are aware that the following error is thrown by https://search.google.com/structured-data/testing-tool
            // "The property affiliation is not recognized by Google for an object of type Thing."
            // Someone at Google has said this is ok.
            // This logic could be moved into the `if (authorAffiliation != null)` block above.
            if (!StringUtils.isBlank(affiliation)) {
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
        String datePublished = datasetVersion.getDataset().getPublicationDateFormattedYYYYMMDD();
        if (datePublished != null) {
            job.add("datePublished", datePublished);
        }

        /**
         * "dateModified" is more appropriate for a version: "The date on which
         * the CreativeWork was most recently modified or when the item's entry
         * was modified within a DataFeed."
         */
        job.add("dateModified", datasetVersion.getPublicationDateAsString());
        job.add("version", datasetVersion.getVersionNumber().toString());

        JsonArrayBuilder descriptionsArray = Json.createArrayBuilder();
        List<String> descriptions = datasetVersion.getDescriptionsPlainText();
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

        for (String subject : datasetVersion.getDatasetSubjects()) {
            keywords.add(subject);
        }

        for (String topic : datasetVersion.getTopicClassifications()) {
            keywords.add(topic);
        }

        for (String keyword : datasetVersion.getKeywords()) {
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
        List<DatasetRelPublication> relatedPublications = datasetVersion.getRelatedPublications();
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

        List<String> timePeriodsCovered = datasetVersion.getTimePeriodsCovered();
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
        TermsOfUseAndAccess terms = datasetVersion.getTermsOfUseAndAccess();
        if (terms != null) {
            JsonObjectBuilder license = Json.createObjectBuilder().add("@type", "Dataset");

            if (TermsOfUseAndAccess.License.CC0.equals(terms.getLicense())) {
                license.add("text", "CC0").add("url", "https://creativecommons.org/publicdomain/zero/1.0/");
            } else {
                String termsOfUse = terms.getTermsOfUse();
                // Terms of use can be null if you create the dataset with JSON.
                if (termsOfUse != null) {
                    license.add("text", termsOfUse);
                }
            }

            job.add("license", license);
        }

        job.add("includedInDataCatalog", Json.createObjectBuilder()
                .add("@type", "DataCatalog")
                .add("name", datasetVersion.getRootDataverseNameforCitation())
                .add("url", dataverseSiteUrl)
        );

        String installationBrandName = BrandingUtil.getInstallationBrandName(datasetVersion.getRootDataverseNameforCitation());
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

        List<String> funderNames = datasetVersion.getFunders();
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
        List<String> spatialCoverages = datasetVersion.getSpatialCoverages(commaSeparated);
        if (!spatialCoverages.isEmpty()) {
            JsonArrayBuilder spatialArray = Json.createArrayBuilder();
            for (String spatialCoverage : spatialCoverages) {
                spatialArray.add(spatialCoverage);
            }
            job.add("spatialCoverage", spatialArray);
        }

        List<FileMetadata> fileMetadatasSorted = datasetVersion.getFileMetadatasSorted();
        if (fileMetadatasSorted != null && !fileMetadatasSorted.isEmpty()) {
            JsonArrayBuilder fileArray = Json.createArrayBuilder();
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
                if (hideSchemaDotOrgDownloadUrls != null && hideSchemaDotOrgDownloadUrls.equals("true")) {
                    // no-op
                } else {
                    if (FileUtil.isPubliclyDownloadable(fileMetadata)) {
                        String nullDownloadType = null;
                        fileObject.add("contentUrl", dataverseSiteUrl + FileUtil.getFileDownloadUrlPath(nullDownloadType, fileMetadata.getDataFile().getId(), false));
                    }
                }
                fileArray.add(fileObject);
            }
            job.add("distribution", fileArray);
        }
        
        return job.build().toString();
    }
}
