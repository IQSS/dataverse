package edu.harvard.iq.dataverse.util;

/**
 * Eko Indarto, DANS
 * Vic Ding, DANS
 *
 * This file prepares the resources used in Signposting
 *
 * It requires correspondence configuration to function well.
 * The configuration key used is SignpostingConf.
 * It is a json structure shown below
 *
 * useDefaultFileType is an on/off switch during linkset creating time, it controls whether the default type is
 * used, which is always Dataset
 * {
 *   "indetifier-schema": {"ORCID":"https://orcid.org/", "ISNI":"https://isni.org/isni/", "ScopusID":"https://www.scopus.com/authid/detail.uri?authorId="},
 *   "license": {"CC0":"https://creativecommons.org/licenses/by/4.0/", "MIT": "https://url", "APACHE":"https://url"},
 *   "cite-as": {"doi":"https://citation.crosscite.org/format?style=bibtex&doi=", "type":"application/vnd.datacite.datacite+json"},
 *   "useDefaultFileType": true,
 *   "defaultFileTypeValue": "https://schema.org/Dataset"
 * }
 *
 * The configuration can be modified during run time by the administrator.
 *
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
    JsonObject idschemaJsonObj;
    JsonObject licJsonObj;
    JsonObject citeAsJsonObj;
    Boolean useDefaultFileType;
    String defaultFileTypeValue;

    public SignpostingResources(SystemConfig systemConfig, DatasetVersion workingDatasetVersion, String jsonSetting){
        this.systemConfig = systemConfig;
        this.workingDatasetVersion = workingDatasetVersion;
        JsonReader jsonReader = Json.createReader(new StringReader(jsonSetting));
        JsonObject spJsonSetting = jsonReader.readObject();
        jsonReader.close();
        idschemaJsonObj = spJsonSetting.getJsonObject("indetifier-schema");
        licJsonObj = spJsonSetting.getJsonObject("license");
        citeAsJsonObj = spJsonSetting.getJsonObject("cite-as");
        useDefaultFileType = spJsonSetting.getBoolean("useDefaultFileType", true);
        defaultFileTypeValue = spJsonSetting.getString("defaultFileTypeValue", "https://schema.org/Dataset");
    }

    /**
     * Get identifier schema for each author
     *
     * Author may have identifiers from different providers
     * ORCID: the url format is https://orcid.org/:id
     * ISNI: the url format is https://isni.org/isni/:id
     * ScopusID: the url format is https://www.scopus.com/authid/detail.uri?authorId=:id
     * VIAF: the url format is http://viaf.org/viaf/:id
     *
     * For example:
     *      if author has VIAF
    *       Link: <http://viaf.org/viaf/:id/>; rel="author"
     *
     * @param datasetAuthors
     * @return
     */
    private String getIdentifierSchema(List<DatasetAuthor> datasetAuthors) {
        StringBuilder sb = new StringBuilder();
        String identifierSchema = "";

        for (DatasetAuthor da:datasetAuthors){
            if (da.getIdValue() != null && !da.getIdValue().isEmpty()) {
                sb.append("<").
                        append(idschemaJsonObj.getString(da.getIdType())).
                        append(da.getIdValue()).
                        append(">;rel=\"author\"");
                if (identifierSchema == "") {
                    identifierSchema = sb.toString();
                } else {
                    identifierSchema = String.join(",", identifierSchema, sb.toString());
                }
            }
        }
        return identifierSchema;
    }

    public String getLinks(){
        // TODO: individual value should be ignored if it's empty
        // list of the strings going to be returned
        List<String> valueList = new LinkedList<>();
        Dataset ds = workingDatasetVersion.getDataset();

        // signposting identifierSchema
        String identifierSchema = getIdentifierSchema(workingDatasetVersion.getDatasetAuthors());
        if (!identifierSchema.equals("")) {
            valueList.add(identifierSchema);
        }

        // Signposting citeAs
        if (!Objects.equals(ds.getPersistentURL(), "")) {
            String citeAs = "<" + ds.getPersistentURL() + ">;rel=\"cite-as\"";
            valueList.add(citeAs);
        }

        // Signposting describedby
        String describedby = "<" + citeAsJsonObj.getString(ds.getProtocol()) + ds.getAuthority() + "/"
                + ds.getIdentifier() + ">;rel=\"describedby\"" + "; type=\""+ citeAsJsonObj.getString("type") + "\"";
        describedby += ",<" + systemConfig.getDataverseSiteUrl() + "/api/datasets/export?exporter=schema.org&persistentId="
                + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier() + ">;rel=\"describedby\"" + ";type=\"application/json+ld\"";
        valueList.add(describedby);

        // Signposting type
        String type = "<https://schema.org/AboutPage>;rel=\"type\"";
        valueList.add(type);

        // Signposting license
        // TODO: support only CC0 now, should add flexible license support when flex-terms is ready
        String license = ""; //non only CC0
        if (workingDatasetVersion.getTermsOfUseAndAccess().getLicense() == TermsOfUseAndAccess.License.CC0){
            //On the current Dataverse, only None and CC0. In the signposting protocol: cardinality is 1
            license = "<https://creativecommons.org/publicdomain/zero/1.0/>;rel=\"license\"";
            valueList.add(license);
        }

        // Signposting linkset
        // TODO Fix: base url is empty in the linkset string
        // String url = settingsWrapper.getValueForKey(SettingsServiceBean.Key.ComputeBaseUrl);
        String linkset = "<" + systemConfig.getDataverseSiteUrl() + "/api/datasets/:persistentId/versions/"
                + workingDatasetVersion.getVersionNumber() + "." + workingDatasetVersion.getMinorVersionNumber()
                + "/linkset?persistentId=" + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier() + "> ; rel=\"linkset\";type=\"application/linkset+json\"";
        valueList.add(linkset);
        logger.info(String.format("valueList is: %s", valueList));

        return String.join(", ", valueList);
    }

    private JsonArrayBuilder getIdentifiersSchema(List<DatasetAuthor> datasetAuthors){
        JsonArrayBuilder authors = Json.createArrayBuilder();
        for (DatasetAuthor da:datasetAuthors){
            if (da.getIdValue() != null && !da.getIdValue().isEmpty()) {
                authors.add(jsonObjectBuilder().add("href", idschemaJsonObj.getString(da.getIdType())  + da.getIdValue()));
            }
        }
        return authors;
    }

    public JsonArrayBuilder getJsonLinkset() {
        Dataset ds = workingDatasetVersion.getDataset();
        String landingPage = systemConfig.getDataverseSiteUrl() + "/dataset.xhtml?persistentId=" + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier() ;
        JsonArrayBuilder authors = getIdentifiersSchema(workingDatasetVersion.getDatasetAuthors());

        JsonArrayBuilder items = Json.createArrayBuilder();
//        List<DataFile> dfs = ds.getFiles();

        List<FileMetadata> fms = workingDatasetVersion.getFileMetadatas();
        for (FileMetadata fm:fms){
            DataFile df = fm.getDataFile();
            items.add(jsonObjectBuilder().add("href", getPublicDownloadUrl(df)).add("type",df.getContentType()));
        }

        String license = "";
        if (workingDatasetVersion.getTermsOfUseAndAccess().getLicense() == TermsOfUseAndAccess.License.CC0){
            license = licJsonObj.getString(TermsOfUseAndAccess.License.CC0.name());
        }

        JsonArrayBuilder mediaTypes = Json.createArrayBuilder();
        mediaTypes.add(
                jsonObjectBuilder().add(
                        "href",
                        citeAsJsonObj.getJsonObject(ds.getProtocol() + ds.getAuthority() + "/" + ds.getIdentifier())
                ).add(
                        "type",
                        citeAsJsonObj.getString("type")
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
        JsonArrayBuilder linkset = Json.createArrayBuilder();
        JsonObjectBuilder mandatory = jsonObjectBuilder()
                .add("anchor", landingPage)
                .add("cite-as", Json.createArrayBuilder().add(jsonObjectBuilder().add("href", ds.getPersistentURL())))
                .add("type", Json.createArrayBuilder().add(jsonObjectBuilder().add("href", "https://schema.org/AboutPage")))
                .add("author", authors)
                .add("license", jsonObjectBuilder().add("href", license))
                .add("item", items).add("describedby", mediaTypes);
        linkset.add(mandatory);

        if (useDefaultFileType) {
            for (FileMetadata fm:fms){
                DataFile df = fm.getDataFile();
                JsonObjectBuilder itemAnchor = jsonObjectBuilder().add("anchor", getPublicDownloadUrl(df));
                itemAnchor.add("collection", Json.createArrayBuilder().add(jsonObjectBuilder()
                        .add("href", landingPage)).add(jsonObjectBuilder().add("type", defaultFileTypeValue)));
                linkset.add(itemAnchor);
            }
        } else {
            for (FileMetadata fm:fms){
                DataFile df = fm.getDataFile();
                JsonObjectBuilder itemAnchor = jsonObjectBuilder().add("anchor", getPublicDownloadUrl(df));
                itemAnchor.add("collection", Json.createArrayBuilder().add(jsonObjectBuilder()
                        .add("href", landingPage)));
                linkset.add(itemAnchor);
            }
        }

        return linkset;
    }


    private String getPublicDownloadUrl(DataFile dataFile) {
        try {
            StorageIO<DataFile> storageIO = dataFile.getStorageIO();
            if (storageIO instanceof SwiftAccessIO) {
                String fileDownloadUrl = null;
                try {
                    SwiftAccessIO<DataFile> swiftIO = (SwiftAccessIO<DataFile>) storageIO;
                    swiftIO.open();
                    //if its a public install, lets just give users the permanent URL!
                    if (systemConfig.isPublicInstall()){
                        fileDownloadUrl = swiftIO.getRemoteUrl();
                    } else {
                        //TODO: if a user has access to this file, they should be given the swift url
                        // perhaps even we could use this as the "private url"
                        fileDownloadUrl = swiftIO.getTemporarySwiftUrl();
                    }
                    return fileDownloadUrl;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        return FileUtil.getPublicDownloadUrl(systemConfig.getDataverseSiteUrl(), null, dataFile.getId());
    }
}