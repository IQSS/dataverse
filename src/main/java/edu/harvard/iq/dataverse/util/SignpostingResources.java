package edu.harvard.iq.dataverse.util;

/*
  Eko Indarto, DANS
  Vic Ding, DANS

  This file prepares the resources used in Signposting

  It requires correspondence configuration to function well.
  The configuration key used is SignpostingConf.

  useDefaultFileType is an on/off switch during linkset creating time, it controls whether the default type is
  used, which is always Dataset

  The configuration can be modified during run time by the administrator.
 */

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetAuthor;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataaccess.SwiftAccessIO;
import edu.harvard.iq.dataverse.license.License;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

public class SignpostingResources {
    private static final Logger logger = Logger.getLogger(SignpostingResources.class.getCanonicalName());
    SystemConfig systemConfig;
    DatasetVersion workingDatasetVersion;
    JsonObject describedByJsonObj;
    Boolean useDefaultFileType;
    String defaultFileTypeValue;
    int maxAuthors;
    int maxItems;

    public SignpostingResources(SystemConfig systemConfig, DatasetVersion workingDatasetVersion, String jsonSetting) {
        this.systemConfig = systemConfig;
        this.workingDatasetVersion = workingDatasetVersion;
        if (jsonSetting == null) {
            jsonSetting = BundleUtil.getStringFromBundle("signposting.configuration.SignpostingConf");
        }
        JsonReader jsonReader = Json.createReader(new StringReader(jsonSetting));
        JsonObject spJsonSetting = jsonReader.readObject();
        jsonReader.close();
        describedByJsonObj = spJsonSetting.getJsonObject("describedby");
        useDefaultFileType = spJsonSetting.getBoolean("useDefaultFileType", true);
        defaultFileTypeValue = spJsonSetting.getString("defaultFileTypeValue", "https://schema.org/Dataset");
        maxAuthors = spJsonSetting.getInt("maxAuthors", 5);
        maxItems = spJsonSetting.getInt("maxItems", 5);
    }

    /**
     * Get identifier schema for each author
     * <p>
     * For example:
     * if author has VIAF
     * Link: <http://viaf.org/viaf/:id/>; rel="author"
     *
     * @param datasetAuthors list of all DatasetAuthor object
     * @return all the non empty author links in a string
     */
    private String getAuthorsIdSchema(List<DatasetAuthor> datasetAuthors) {
        String singleAuthorString;
        String identifierSchema = "";
        int visibleAuthorCounter = 0;
//        if (datasetAuthors.size() > maxAuthors) {return "";}
        for (DatasetAuthor da : datasetAuthors) {
            logger.info(String.format(
                    "idtype: %s; idvalue: %s, affiliation: %s; identifierUrl: %s",
                    da.getIdType(),
                    da.getIdValue(),
                    da.getAffiliation(),
                    da.getIdentifierAsUrl()
            ));

            String authorURL = "";
            authorURL = getAuthorUrl(da);
            if (!Objects.equals(authorURL, "")) {
                visibleAuthorCounter++;
                // return empty if number of visible author more than max allowed
                if (visibleAuthorCounter >= maxAuthors) return "";
                singleAuthorString = "<" + authorURL + ">;rel=\"author\"";
                if (Objects.equals(identifierSchema, "")) {
                    identifierSchema = singleAuthorString;
                } else {
                    identifierSchema = String.join(",", identifierSchema, singleAuthorString);
                }
            }
        }

        logger.info(String.format("identifierSchema: %s", identifierSchema));
        return identifierSchema;
    }

    /**
     * Get key, values of signposting items and return as string
     *
     * @return comma delimited string
     */
    public String getLinks() {
        List<String> valueList = new LinkedList<>();
        Dataset ds = workingDatasetVersion.getDataset();

        String identifierSchema = getAuthorsIdSchema(workingDatasetVersion.getDatasetAuthors());
        if (!identifierSchema.equals("")) {
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

        String describedby = "<" + describedByJsonObj.getString(ds.getProtocol()) + ds.getAuthority() + "/"
                + ds.getIdentifier() + ">;rel=\"describedby\"" + ";type=\"" + describedByJsonObj.getString("type") + "\"";
        describedby += ",<" + systemConfig.getDataverseSiteUrl() + "/api/datasets/export?exporter=schema.org&persistentId="
                + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier() + ">;rel=\"describedby\"" + ";type=\"application/json+ld\"";
        valueList.add(describedby);

        String type = "<https://schema.org/AboutPage>;rel=\"type\"";;
        if (useDefaultFileType) {
            type = "<https://schema.org/AboutPage>;rel=\"type\",<" + defaultFileTypeValue + ">;rel=\"type\"";
        }
        valueList.add(type);

        // TODO: support only CC0 now, should add flexible license support when flex-terms or multi-license is ready
//        License license = workingDatasetVersion.getTermsOfUseAndAccess().getLicense();
        License license = workingDatasetVersion.getTermsOfUseAndAccess().getLicense();
        String licenseString = license.getUri() + ";rel=\"license\"";
        valueList.add(licenseString);

        /*
        else {
            TODO: when multilicense is merged, should get license link from multilicense
             Thus the checking thus would not be necessary?
             Currently, when the License is None, which means bring-your-own-license,
             Mostly, we will not have a valid URI in the License, so we skip it here despite that signposting requires 1
             valid URI.
        }
        */

        String linkset = "<" + systemConfig.getDataverseSiteUrl() + "/api/datasets/:persistentId/versions/"
                + workingDatasetVersion.getVersionNumber() + "." + workingDatasetVersion.getMinorVersionNumber()
                + "/linkset?persistentId=" + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier() + "> ; rel=\"linkset\";type=\"application/linkset+json\"";
        valueList.add(linkset);
        logger.info(String.format("valueList is: %s", valueList));

        return String.join(", ", valueList);
    }

    private String getAuthorUrl(DatasetAuthor da) {
        String authorURL = "";
        if (da.getIdValue() != null && !da.getIdValue().trim().isEmpty()) {
            authorURL = da.getIdValue();
        } else if (da.getIdentifierAsUrl() != null && !da.getIdentifierAsUrl().trim().isEmpty()) {
            authorURL = da.getIdentifierAsUrl();
        }
        return authorURL;
    }

    private JsonArrayBuilder getJsonAuthors(List<DatasetAuthor> datasetAuthors) {
        JsonArrayBuilder authors = Json.createArrayBuilder();
        boolean returnNull = true;
        String authorURL = "";
        for (DatasetAuthor da : datasetAuthors) {
            authorURL = getAuthorUrl(da);
            if (!Objects.equals(authorURL, "")) {
                authors.add(jsonObjectBuilder().add("href", authorURL));
                returnNull = false;
            }
        }
        return returnNull ? null : authors;
    }

    private String getItems(List<FileMetadata> fms) {
        if (fms.size() > maxItems) {
            logger.info(String.format("maxItem is %s and fms size is %s", maxItems, fms.size()));
            return null;
        }

        String result = "";
        for (FileMetadata fm : fms) {
            DataFile df = fm.getDataFile();
            if (Objects.equals(result, "")) {
                 result = "<" + getPublicDownloadUrl(df) + ">;rel=\"item\";type=\"" + df.getContentType() + "\"";
            } else {
                 result = String.join(",", result, "<" + getPublicDownloadUrl(df) + ">;rel=\"item\";type=\"" + df.getContentType() + "\"");
            }
        }
        return result;
    }

    private JsonArrayBuilder getJsonItems(List<FileMetadata> fms) {
        JsonArrayBuilder items = Json.createArrayBuilder();
        for (FileMetadata fm : fms) {
            DataFile df = fm.getDataFile();
            items.add(jsonObjectBuilder().add("href", getPublicDownloadUrl(df)).add("type", df.getContentType()));
        }

        return items;
    }

    public JsonArrayBuilder getJsonLinkset() {
        Dataset ds = workingDatasetVersion.getDataset();
        String landingPage = systemConfig.getDataverseSiteUrl() + "/dataset.xhtml?persistentId=" + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier();
        JsonArrayBuilder authors = getJsonAuthors(workingDatasetVersion.getDatasetAuthors());

        List<FileMetadata> fms = workingDatasetVersion.getFileMetadatas();
        JsonArrayBuilder items = getJsonItems(fms);

        License license = workingDatasetVersion.getTermsOfUseAndAccess().getLicense();
        String licenseString = license.getUri().toString();

        JsonArrayBuilder mediaTypes = Json.createArrayBuilder();
        mediaTypes.add(
                jsonObjectBuilder().add(
                        "href",
                        describedByJsonObj.getString(ds.getProtocol()) + ds.getAuthority() + "/"
                                + ds.getIdentifier()
                ).add(
                        "type",
                        describedByJsonObj.getString("type")
                )
        );

        mediaTypes.add(
                jsonObjectBuilder().add(
                        "href",
                        systemConfig.getDataverseSiteUrl() + "/api/datasets/export?exporter=schema.org&persistentId=" + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier()
                ).add(
                        "type",
                        "application/json+ld"
                )
        );
        JsonArrayBuilder linksetJsonObj = Json.createArrayBuilder();

        JsonObjectBuilder mandatory;
        if (useDefaultFileType) {
            mandatory = jsonObjectBuilder()
                    .add("anchor", landingPage)
                    .add("cite-as", Json.createArrayBuilder().add(jsonObjectBuilder().add("href", ds.getPersistentURL())))
                    .add("type", Json.createArrayBuilder()
                            .add(jsonObjectBuilder().add("href", "https://schema.org/AboutPage"))
                            .add(jsonObjectBuilder().add("href", defaultFileTypeValue))
                    );
        } else {
            mandatory = jsonObjectBuilder()
                    .add("anchor", landingPage)
                    .add("cite-as", Json.createArrayBuilder().add(jsonObjectBuilder().add("href", ds.getPersistentURL())))
                    .add("type", Json.createArrayBuilder()
                            .add(jsonObjectBuilder().add("href", "https://schema.org/AboutPage"))
                    );
        }

        if (authors != null) {
            mandatory.add("author", authors);
        }
        if (licenseString != null && !Objects.equals(licenseString, "")) {
            mandatory.add("license", jsonObjectBuilder().add("href", licenseString));
        }
        if (!mediaTypes.toString().trim().isEmpty()) {
            mandatory.add("describedby", mediaTypes);
        }
        if (items != null) {
            mandatory.add("item", items);
        }
        linksetJsonObj.add(mandatory);

        // remove scholarly type as shown already on landing page
        for (FileMetadata fm : fms) {
            DataFile df = fm.getDataFile();
            JsonObjectBuilder itemAnchor = jsonObjectBuilder().add("anchor", getPublicDownloadUrl(df));
            itemAnchor.add("collection", Json.createArrayBuilder().add(jsonObjectBuilder()
                    .add("href", landingPage)));
            linksetJsonObj.add(itemAnchor);
        }

        return linksetJsonObj;
    }


    private String getPublicDownloadUrl(DataFile dataFile) {
        StorageIO<DataFile> storageIO = null;
        try {
            storageIO = dataFile.getStorageIO();
        } catch (IOException e) {
            logger.warning(String.format("Error getting storageID from file; original error message is: %s", e.getLocalizedMessage()));
        }

        if (storageIO instanceof SwiftAccessIO) {
            String fileDownloadUrl;
            SwiftAccessIO<DataFile> swiftIO = (SwiftAccessIO<DataFile>) storageIO;
            try {
                swiftIO.open();
            } catch (IOException e) {
                logger.warning(String.format("Error opening the swiftIO; original error message is: %s", e.getLocalizedMessage()));
            }

            //if its a public install, lets just give users the permanent URL!
            if (systemConfig.isPublicInstall()) {
                fileDownloadUrl = swiftIO.getRemoteUrl();
            } else {
                //TODO: if a user has access to this file, they should be given the swift url
                // perhaps even we could use this as the "private url"
                fileDownloadUrl = swiftIO.getTemporarySwiftUrl();
            }
            return fileDownloadUrl;

        }

        return FileUtil.getPublicDownloadUrl(systemConfig.getDataverseSiteUrl(), null, dataFile.getId());
    }
}
