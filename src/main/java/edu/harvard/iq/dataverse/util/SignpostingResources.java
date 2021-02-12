package edu.harvard.iq.dataverse.util;

/*
  Eko Indarto, DANS
  Vic Ding, DANS

  This file prepares the resources used in Signposting

  It requires correspondence configuration to function well.
  The configuration key used is SignpostingConf.
  It is a json structure shown below

  useDefaultFileType is an on/off switch during linkset creating time, it controls whether the default type is
  used, which is always Dataset

  The configuration can be modified during run time by the administrator.
 */

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataaccess.SwiftAccessIO;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;

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
    JsonObject licJsonObj;
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
        licJsonObj = spJsonSetting.getJsonObject("license");
        describedByJsonObj = spJsonSetting.getJsonObject("describedby");
        useDefaultFileType = spJsonSetting.getBoolean("useDefaultFileType", true);
        defaultFileTypeValue = spJsonSetting.getString("defaultFileTypeValue", "https://schema.org/Dataset");
        maxAuthors = spJsonSetting.getInt("maxAuthors", 5);
        maxItems = spJsonSetting.getInt("maxItems", 5);
    }

    /**
     * Get identifier schema for each author
     *
     * For example:
     * if author has VIAF
     * Link: <http://viaf.org/viaf/:id/>; rel="author"
     *
     * @param datasetAuthors list of all DatasetAuthor object
     * @return all the non empty author links in a string
     */
    private String getIdentifierSchema(List<DatasetAuthor> datasetAuthors) {
        String singleAuthorString;
        String identifierSchema = "";

        for (DatasetAuthor da : datasetAuthors) {
            logger.info(String.format(
                    "idtype: %s; idvalue: %s, affiliation: %s; identifierUrl: %s",
                    da.getIdType(),
                    da.getIdValue(),
                    da.getAffiliation(),
                    da.getIdentifierAsUrl()
            ));
            if (da.getIdentifierAsUrl() != null && !da.getIdentifierAsUrl().trim().isEmpty()) {
                singleAuthorString = "<" + da.getIdentifierAsUrl() + ">;rel=\"author\"";
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

        String identifierSchema = getIdentifierSchema(workingDatasetVersion.getDatasetAuthors());
        if (!identifierSchema.equals("")) {
            valueList.add(identifierSchema);
        }

        if (!Objects.equals(ds.getPersistentURL(), "")) {
            String citeAs = "<" + ds.getPersistentURL() + ">;rel=\"cite-as\"";
            valueList.add(citeAs);
        }

        String describedby = "<" + describedByJsonObj.getString(ds.getProtocol()) + ds.getAuthority() + "/"
                + ds.getIdentifier() + ">;rel=\"describedby\"" + ";type=\"" + describedByJsonObj.getString("type") + "\"";
        describedby += ",<" + systemConfig.getDataverseSiteUrl() + "/api/datasets/export?exporter=schema.org&persistentId="
                + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier() + ">;rel=\"describedby\"" + ";type=\"application/json+ld\"";
        valueList.add(describedby);

        String type = "<https://schema.org/AboutPage>;rel=\"type\"";
        valueList.add(type);

        // TODO: support only CC0 now, should add flexible license support when flex-terms is ready
        String license;
        if (workingDatasetVersion.getTermsOfUseAndAccess().getLicense() == TermsOfUseAndAccess.License.CC0) {
            // On the current Dataverse, only None and CC0. In the signposting protocol: cardinality is 1
            license = "<https://creativecommons.org/publicdomain/zero/1.0/>;rel=\"license\"";
            valueList.add(license);
        }

        String linkset = "<" + systemConfig.getDataverseSiteUrl() + "/api/datasets/:persistentId/versions/"
                + workingDatasetVersion.getVersionNumber() + "." + workingDatasetVersion.getMinorVersionNumber()
                + "/linkset?persistentId=" + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier() + "> ; rel=\"linkset\";type=\"application/linkset+json\"";
        valueList.add(linkset);
        logger.info(String.format("valueList is: %s", valueList));

        return String.join(", ", valueList);
    }

    private JsonArrayBuilder getIdentifiersSchema(List<DatasetAuthor> datasetAuthors) {
        if (datasetAuthors.size() > maxAuthors) return null;
        JsonArrayBuilder authors = Json.createArrayBuilder();
        boolean returnNull = true;
        for (DatasetAuthor da : datasetAuthors) {
            if (da.getIdentifierAsUrl() != null && !da.getIdentifierAsUrl().trim().isEmpty()) {
                authors.add(jsonObjectBuilder().add("href", da.getIdentifierAsUrl()));
                returnNull = false;
            }
        }
        return returnNull ? null : authors;
    }

    private JsonArrayBuilder getJsonItems(List<FileMetadata> fms) {
        if (fms.size() > maxItems) return null;
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
        JsonArrayBuilder authors = getIdentifiersSchema(workingDatasetVersion.getDatasetAuthors());

        List<FileMetadata> fms = workingDatasetVersion.getFileMetadatas();
        JsonArrayBuilder items = getJsonItems(fms);

        String license = "";
        if (workingDatasetVersion.getTermsOfUseAndAccess().getLicense() == TermsOfUseAndAccess.License.CC0) {
            license = licJsonObj.getString(TermsOfUseAndAccess.License.CC0.name());
        }

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
        JsonObjectBuilder mandatory = jsonObjectBuilder()
                .add("anchor", landingPage)
                .add("cite-as", Json.createArrayBuilder().add(jsonObjectBuilder().add("href", ds.getPersistentURL())))
                .add("type", Json.createArrayBuilder().add(jsonObjectBuilder().add("href", "https://schema.org/AboutPage")));

        if (authors != null) {
            mandatory.add("author", authors);
        }
        if (license != null && !license.trim().isEmpty()) {
            mandatory.add("license", jsonObjectBuilder().add("href", license));
        }
        if (!mediaTypes.toString().trim().isEmpty()) {
            mandatory.add("describedby", mediaTypes);
        }
        if (items != null) {
            mandatory.add("item", items);
        }
        linksetJsonObj.add(mandatory);

        if (useDefaultFileType) {
            for (FileMetadata fm : fms) {
                DataFile df = fm.getDataFile();
                JsonObjectBuilder itemAnchor = jsonObjectBuilder().add("anchor", getPublicDownloadUrl(df));
                itemAnchor.add("collection", Json.createArrayBuilder().add(jsonObjectBuilder()
                        .add("href", landingPage)).add(jsonObjectBuilder().add("type", defaultFileTypeValue)));
                linksetJsonObj.add(itemAnchor);
            }
        } else {
            for (FileMetadata fm : fms) {
                DataFile df = fm.getDataFile();
                JsonObjectBuilder itemAnchor = jsonObjectBuilder().add("anchor", getPublicDownloadUrl(df));
                itemAnchor.add("collection", Json.createArrayBuilder().add(jsonObjectBuilder()
                        .add("href", landingPage)));
                linksetJsonObj.add(itemAnchor);
            }
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
