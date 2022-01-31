package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.common.BrandingUtil;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.common.MarkupChecker;
import edu.harvard.iq.dataverse.common.NullSafeJsonBuilder;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetAuthor;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRelPublication;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.FileUtil.ApiDownloadType;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.Tuple;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class JsonLdBuilder {
    // TODO: Consider moving this comment into the Exporter code.
    // The export subsystem assumes there is only
    // one metadata export in a given format per dataset (it uses the current
    // released (published) version. This JSON fragment is generated for a
    // specific released version - and we can have multiple released versions.
    // So something will need to be modified to accommodate this. -- L.A.

    private DataFileServiceBean dataFileService;
    private SettingsServiceBean settingsService;
    private SystemConfig systemConfig;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public JsonLdBuilder() {
    }

    @Inject
    public JsonLdBuilder(DataFileServiceBean dataFileService, SettingsServiceBean settingsService, SystemConfig systemConfig) {
        this.dataFileService = dataFileService;
        this.settingsService = settingsService;
        this.systemConfig = systemConfig;
    }

    // -------------------- LOGIC --------------------

    /**
     * We call the export format "Schema.org JSON-LD" and extensive Javadoc can
     * be found in {@link SchemaDotOrgExporter}.
     */
    public String buildJsonLd(DatasetVersion datasetVersion) {
        // We show published datasets only for "datePublished" field below.
        if (!datasetVersion.isReleased()) {
            return "";
        }

        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("@context", "http://schema.org");
        job.add("@type", "Dataset");
        // Note that whenever you use "@id" you should also use "identifier" and vice versa.
        job.add("@id", datasetVersion.getDataset().getPersistentURL());
        job.add("identifier", datasetVersion.getDataset().getPersistentURL());
        job.add("name", datasetVersion.getParsedTitle());
        JsonArrayBuilder authors = Json.createArrayBuilder();
        for (DatasetAuthor datasetAuthor : datasetVersion.getDatasetAuthors()) {
            JsonObjectBuilder author = Json.createObjectBuilder();
            String name = datasetAuthor.getName().getValue();
            DatasetField authorAffiliation = datasetAuthor.getAffiliation();
            String affiliation = null;
            if (authorAffiliation != null) {
                affiliation = datasetAuthor.getAffiliation().getValue();
            }

            String authorType = "orcid".equalsIgnoreCase(datasetAuthor.getIdType()) ||
                    ExportUtil.isPerson(name) ? "Person" : "Organization";
            author.add("@type", authorType);
            author.add("name", name);

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
        List<String> descriptions = getDescriptionsPlainText(datasetVersion);
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

        for (String topic : getTopicClassifications(datasetVersion)) {
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

        List<String> timePeriodsCovered = getTimePeriodsCovered(datasetVersion);
        if (timePeriodsCovered.size() > 0) {
            JsonArrayBuilder temporalCoverage = Json.createArrayBuilder();
            for (String timePeriod : timePeriodsCovered) {
                temporalCoverage.add(timePeriod);
            }
            job.add("temporalCoverage", temporalCoverage);
        }

        List<FileMetadata> filesMetadata = datasetVersion.getFileMetadatas();
        JsonObjectBuilder license = Json.createObjectBuilder().add("@type", "CreativeWork");

        if(hasSameTermsForAllFiles(filesMetadata)) {
            FileTermsOfUse firstFileTerms = filesMetadata.get(0).getTermsOfUse();
            FileTermsOfUse.TermsOfUseType termsType = firstFileTerms.getTermsOfUseType();

            if(termsType.equals(FileTermsOfUse.TermsOfUseType.LICENSE_BASED)) {
                license.add("name", firstFileTerms.getLicense().getName());
                license.add("url", firstFileTerms.getLicense().getUrl());
            } else if (termsType.equals(FileTermsOfUse.TermsOfUseType.RESTRICTED)) {
                license.add("name", "Restricted access");
            } else {
                license.add("name", "All rights reserved");
            }
        } else {
            license.add("name", "Different licenses or terms for individual files");
        }

        job.add("license", license);


        job.add("includedInDataCatalog", Json.createObjectBuilder()
                .add("@type", "DataCatalog")
                .add("name", datasetVersion.getRootDataverseNameForCitation())
                .add("url", systemConfig.getDataverseSiteUrl())
        );

        String installationBrandName = BrandingUtil.getInstallationBrandName(datasetVersion.getRootDataverseNameForCitation());
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

        List<String> funderNames = getFunders(datasetVersion);
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

        List<String> spatialCoverages = getSpatialCoverages(datasetVersion);
        if (!spatialCoverages.isEmpty()) {
            JsonArrayBuilder spatialArray = Json.createArrayBuilder();
            for (String spatialCoverage : spatialCoverages) {
                spatialArray.add(spatialCoverage);
            }
            job.add("spatialCoverage", spatialArray);
        }

        if(!datasetVersion.getDataset().hasActiveEmbargo()) {
            List<FileMetadata> fileMetadatasSorted = datasetVersion.getAllFilesMetadataSorted();
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
                    fileObject.add("encodingFormat", fileMetadata.getDataFile().getContentType());
                    fileObject.add("contentSize", fileMetadata.getDataFile().getFilesize());
                    fileObject.add("description", fileMetadata.getDescription());
                    fileObject.add("@id", filePidUrlAsString);
                    fileObject.add("identifier", filePidUrlAsString);
                    String hideSchemaDotOrgDownloadUrls = settingsService.getValueForKey(SettingsServiceBean.Key.HideSchemaDotOrgDownloadUrls);
                    if (hideSchemaDotOrgDownloadUrls != null && hideSchemaDotOrgDownloadUrls.equals("true")) {
                        // no-op
                    } else {
                        if (FileUtil.isPubliclyDownloadable(fileMetadata)) {
                            fileObject.add("contentUrl", systemConfig.getDataverseSiteUrl() + FileUtil.getFileDownloadUrlPath(ApiDownloadType.DEFAULT, fileMetadata.getDataFile().getId(), false));
                        }
                    }
                    fileArray.add(fileObject);
                }
                job.add("distribution", fileArray);
            }
        }

        return job.build().toString();
    }

    // -------------------- PRIVATE --------------------

    List<String> getFunders(DatasetVersion datasetVersion) {
        List<String> funders = datasetVersion.extractSubfields(DatasetFieldConstant.contributor,
                Arrays.asList(DatasetFieldConstant.contributorName, DatasetFieldConstant.contributorType))
                .stream()
                .map(e -> Tuple.of(e.get(DatasetFieldConstant.contributorName), e.get(DatasetFieldConstant.contributorType)))
                .filter(t -> t._1 != null && t._2 != null && "Funder".equals(t._2.getDisplayValue()))
                .map(t -> t._1.getDisplayValue())
                .collect(Collectors.toList());
        funders.addAll(datasetVersion.extractSubfields(DatasetFieldConstant.grantNumber,
                Collections.singletonList(DatasetFieldConstant.grantNumberAgency))
                .stream()
                .map(e -> e.get(DatasetFieldConstant.grantNumberAgency))
                .filter(a -> a != null && !a.getDisplayValue().isEmpty())
                .map(DatasetField::getDisplayValue)
                .collect(Collectors.toList()));
        return funders;
    }

    List<String> getTimePeriodsCovered(DatasetVersion datasetVersion) {
        return datasetVersion.extractSubfields(DatasetFieldConstant.timePeriodCovered,
                Arrays.asList(DatasetFieldConstant.timePeriodCoveredStart, DatasetFieldConstant.timePeriodCoveredEnd))
                .stream()
                .map(e -> Tuple.of(e.get(DatasetFieldConstant.timePeriodCoveredStart), e.get(DatasetFieldConstant.timePeriodCoveredEnd)))
                .filter(t -> t._1 != null && !t._1.isEmptyForDisplay() && t._2 != null && !t._2.isEmptyForDisplay())
                .map(t -> t._1.getValue() + "/" + t._2.getValue())
                .collect(Collectors.toList());
    }


    List<String> getSpatialCoverages(DatasetVersion version) {
        List<String> subfields = Arrays.asList(DatasetFieldConstant.city, DatasetFieldConstant.state,
                DatasetFieldConstant.country, DatasetFieldConstant.otherGeographicCoverage);
        return version.extractSubfields(DatasetFieldConstant.geographicCoverage, subfields).stream()
                .map(s -> subfields.stream()
                        .map(s::get)
                        .filter(v -> v != null && !v.isEmptyForDisplay())
                        .map(DatasetField::getValue)
                        .collect(Collectors.joining(", ")))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
    }

    public List<String> getTopicClassifications(DatasetVersion version) {
        return version.getCompoundChildFieldValues(DatasetFieldConstant.topicClassification,
                Collections.singletonList(DatasetFieldConstant.topicClassValue));
    }

    private List<String> getDescriptionsPlainText(DatasetVersion version) {
        List<String> descriptions = new ArrayList<>();
        for (DatasetField dsf : version.getDatasetFields()) {
            if (!DatasetFieldConstant.description.equals(dsf.getDatasetFieldType().getName())
                    || dsf.getDatasetFieldsChildren().isEmpty()) {
                continue;
            }
            String description = "";
            for (DatasetField subField : dsf.getDatasetFieldsChildren()) {
                if (DatasetFieldConstant.descriptionText.equals(subField.getDatasetFieldType().getName())
                        && !subField.isEmptyForDisplay()) {
                    description = subField.getValue();
                }
            }
            descriptions.add(MarkupChecker.stripAllTags(description));
        }
        return descriptions;
    }

    private boolean hasSameTermsForAllFiles(List<FileMetadata> filesMetadata) {
        if (filesMetadata.isEmpty()) {
            return false;
        }
        FileTermsOfUse firstTermsOfUse = filesMetadata.get(0).getTermsOfUse();

        for (FileMetadata fileMetadata : filesMetadata) {
            if (!dataFileService.isSameTermsOfUse(firstTermsOfUse, fileMetadata.getTermsOfUse())) {
                return false;
            }
        }

        return true;
    }
}
