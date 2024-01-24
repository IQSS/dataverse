package edu.harvard.iq.dataverse.util;

/**
  Eko Indarto, DANS
  Vic Ding, DANS

  This file prepares the resources used in Signposting

  Two configurable options allow changing the limit for the number of authors or datafiles (items) allowed in the level-1 header.
  If more than this number exists, no entries of that type are included in the level-1 header.
  See the documentation for the dataverse.signposting.level1-author-limit, and dataverse.signposting.level1-item-limit

  Also note that per the signposting spec, authors for which no PID/URL has been provided are not included in the signposting output.
  
 */

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

public class SignpostingResources {
    private static final Logger logger = Logger.getLogger(SignpostingResources.class.getCanonicalName());
    SystemConfig systemConfig;
    DatasetVersion workingDatasetVersion;
    static final String defaultFileTypeValue = "https://schema.org/Dataset";
    static final int defaultMaxLinks = 5;
    int maxAuthors;
    int maxItems;

    public SignpostingResources(SystemConfig systemConfig, DatasetVersion workingDatasetVersion, String authorLimitSetting, String itemLimitSetting) {
        this.systemConfig = systemConfig;
        this.workingDatasetVersion = workingDatasetVersion;
        maxAuthors = SystemConfig.getIntLimitFromStringOrDefault(authorLimitSetting, defaultMaxLinks);
        maxItems = SystemConfig.getIntLimitFromStringOrDefault(itemLimitSetting, defaultMaxLinks);
    }


    /**
     * Get key, values of signposting items and return as string
     *
     * @return comma delimited string
     */
    public String getLinks() {
        List<String> valueList = new LinkedList<>();
        Dataset ds = workingDatasetVersion.getDataset();

        String identifierSchema = getAuthorsAsString(getAuthorURLs(true));
        if (identifierSchema != null && !identifierSchema.isEmpty()) {
            valueList.add(identifierSchema);
        }

        if (!Objects.equals(ds.getPersistentURL(), "")) {
            String citeAs = "<" + ds.getPersistentURL() + ">;rel=\"cite-as\"";
            valueList.add(citeAs);
        }

        List<FileMetadata> fms = workingDatasetVersion.getFileMetadatas();
        String items = getItems(fms);
        if (items != null && !Objects.equals(items, "")) {
            valueList.add(items);
        }

        String describedby = "<" + ds.getGlobalId().asURL().toString() + ">;rel=\"describedby\"" + ";type=\"" + "application/vnd.citationstyles.csl+json\"";
        describedby += ",<" + systemConfig.getDataverseSiteUrl() + "/api/datasets/export?exporter=schema.org&persistentId="
                + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier() + ">;rel=\"describedby\"" + ";type=\"application/ld+json\"";
        valueList.add(describedby);

        String type = "<https://schema.org/AboutPage>;rel=\"type\"";
        type = "<https://schema.org/AboutPage>;rel=\"type\",<" + defaultFileTypeValue + ">;rel=\"type\"";
        valueList.add(type);

        String licenseString = "<" + DatasetUtil.getLicenseURI(workingDatasetVersion) + ">;rel=\"license\"";
        valueList.add(licenseString);

        String linkset = "<" + systemConfig.getDataverseSiteUrl() + "/api/datasets/:persistentId/versions/"
                + workingDatasetVersion.getVersionNumber() + "." + workingDatasetVersion.getMinorVersionNumber()
                + "/linkset?persistentId=" + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier() + "> ; rel=\"linkset\";type=\"application/linkset+json\"";
        valueList.add(linkset);
        logger.fine(String.format("valueList is: %s", valueList));

        return String.join(", ", valueList);
    }

    public JsonArrayBuilder getJsonLinkset() {
        Dataset ds = workingDatasetVersion.getDataset();
        GlobalId gid = ds.getGlobalId();
        String landingPage = systemConfig.getDataverseSiteUrl() + "/dataset.xhtml?persistentId=" + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier();
        JsonArrayBuilder authors = getJsonAuthors(getAuthorURLs(false));
        JsonArrayBuilder items = getJsonItems();

        String licenseString = DatasetUtil.getLicenseURI(workingDatasetVersion);

        JsonArrayBuilder mediaTypes = Json.createArrayBuilder();
        mediaTypes.add(
                jsonObjectBuilder().add(
                        "href",
                        gid.asURL().toString()
                ).add(
                        "type",
                        "application/vnd.citationstyles.csl+json"
                )
        );

        mediaTypes.add(
                jsonObjectBuilder().add(
                        "href",
                        systemConfig.getDataverseSiteUrl() + "/api/datasets/export?exporter=schema.org&persistentId=" + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier()
                ).add(
                        "type",
                        "application/ld+json"
                )
        );
        JsonArrayBuilder linksetJsonObj = Json.createArrayBuilder();

        JsonObjectBuilder mandatory;
        mandatory = jsonObjectBuilder().add("anchor", landingPage)
                .add("cite-as", Json.createArrayBuilder().add(jsonObjectBuilder().add("href", ds.getPersistentURL())))
                .add("type",
                        Json.createArrayBuilder().add(jsonObjectBuilder().add("href", "https://schema.org/AboutPage"))
                                .add(jsonObjectBuilder().add("href", defaultFileTypeValue)));

        if (authors != null) {
            mandatory.add("author", authors);
        }
        if (licenseString != null && !licenseString.isBlank()) {
            mandatory.add("license", jsonObjectBuilder().add("href", licenseString));
        }
        if (!mediaTypes.toString().isBlank()) {
            mandatory.add("describedby", mediaTypes);
        }
        if (items != null) {
            mandatory.add("item", items);
        }
        linksetJsonObj.add(mandatory);

        // remove scholarly type as shown already on landing page
        for (FileMetadata fm : workingDatasetVersion.getFileMetadatas()) {
            DataFile df = fm.getDataFile();
            JsonObjectBuilder itemAnchor = jsonObjectBuilder().add("anchor", getPublicDownloadUrl(df));
            itemAnchor.add("collection", Json.createArrayBuilder().add(jsonObjectBuilder()
                    .add("href", landingPage)));
            linksetJsonObj.add(itemAnchor);
        }

        return linksetJsonObj;
    }

    /*Method retrieves all the authors of a DatasetVersion with a valid URL and puts them in a list
     * @param limit - if true, will return an empty list (for level 1) if more than maxAuthor authors with URLs are found 
     */
    private List<String> getAuthorURLs(boolean limit) {
        List<String> authorURLs = new ArrayList<String>(maxAuthors);
        int visibleAuthorCounter = 0;

        for (DatasetAuthor da : workingDatasetVersion.getDatasetAuthors()) {
            logger.fine(String.format("idtype: %s; idvalue: %s, affiliation: %s; identifierUrl: %s", da.getIdType(),
                    da.getIdValue(), da.getAffiliation(), da.getIdentifierAsUrl()));
            String authorURL = getAuthorUrl(da);
            if (authorURL != null && !authorURL.isBlank()) {
                // return empty if number of visible author more than max allowed
                // >= since we're comparing before incrementing visibleAuthorCounter
                if (limit && visibleAuthorCounter >= maxAuthors) {
                    authorURLs.clear();
                    break;
                }
                authorURLs.add(authorURL);
                visibleAuthorCounter++;
                

            }
        }
        return authorURLs;
    }


    /**
     * Get Authors as string
     * For example:
     * if author has VIAF
     * Link: <http://viaf.org/viaf/:id/>; rel="author"
     *
     * @param datasetAuthorURLs list of all DatasetAuthors with a valid URL
     * @return all the author links in a string
     */
    private String getAuthorsAsString(List<String> datasetAuthorURLs) {
        String singleAuthorString;
        String identifierSchema = null;
        for (String authorURL : datasetAuthorURLs) {
                singleAuthorString = "<" + authorURL + ">;rel=\"author\"";
                if (identifierSchema == null) {
                    identifierSchema = singleAuthorString;
                } else {
                    identifierSchema = String.join(",", identifierSchema, singleAuthorString);
                }
        }
        logger.fine(String.format("identifierSchema: %s", identifierSchema));
        return identifierSchema;
    }

    /* 
     * 
     */
    private String getAuthorUrl(DatasetAuthor da) {

        final String identifierAsUrl = da.getIdentifierAsUrl();
        // First, try to get URL using the type and value
        if(identifierAsUrl != null) {
            return identifierAsUrl;
        }

        final String idValue = da.getIdValue();
        UrlValidator urlValidator = new UrlValidator(new String[]{"http", "https"});
        // Otherwise, try to use idValue as url if it's valid
        if(urlValidator.isValid(idValue)) {
            return idValue;
        }

        // No url found
        return null;
    }

    private JsonArrayBuilder getJsonAuthors(List<String> datasetAuthorURLs) {
        if(datasetAuthorURLs.isEmpty()) {
            return null;
        }
        JsonArrayBuilder authors = Json.createArrayBuilder();
        for (String authorURL : datasetAuthorURLs) {
                authors.add(jsonObjectBuilder().add("href", authorURL));
        }
        return authors;
    }

    private String getItems(List<FileMetadata> fms) {
        if (fms.size() > maxItems) {
            logger.fine(String.format("maxItem is %s and fms size is %s", maxItems, fms.size()));
            return null;
        }

        String itemString = null;
        for (FileMetadata fm : fms) {
            DataFile df = fm.getDataFile();
            if (itemString == null) {
                itemString = "<" + getPublicDownloadUrl(df) + ">;rel=\"item\";type=\"" + df.getContentType() + "\"";
            } else {
                itemString = String.join(",", itemString, "<" + getPublicDownloadUrl(df) + ">;rel=\"item\";type=\"" + df.getContentType() + "\"");
            }
        }
        return itemString;
    }

    private JsonArrayBuilder getJsonItems() {
        JsonArrayBuilder items = Json.createArrayBuilder();
        for (FileMetadata fm : workingDatasetVersion.getFileMetadatas()) {
            DataFile df = fm.getDataFile();
            items.add(jsonObjectBuilder().add("href", getPublicDownloadUrl(df)).add("type", df.getContentType()));
        }

        return items;
    }
    
    private String getPublicDownloadUrl(DataFile dataFile) {
        GlobalId gid = dataFile.getGlobalId();
        return FileUtil.getPublicDownloadUrl(systemConfig.getDataverseSiteUrl(),
                ((gid != null) ? gid.asString() : null), dataFile.getId());
    }
}
