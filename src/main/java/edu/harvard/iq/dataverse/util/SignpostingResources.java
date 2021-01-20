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
 * {
 *   "indetifier-schema": {"ORCID":"https://orcid.org/", "ISNI":"https://isni.org/isni/", "ScopusID":"https://www.scopus.com/authid/detail.uri?authorId="},
 *   "license": {"CCO":"https://creativecommons.org/licenses/by/4.0/", "MIT": "https://url", "APACHE":"https://url"},
 *   "cite-as": {"doi":"https://citation.crosscite.org/format?style=bibtex&doi=", "type":"application/vnd.datacite.datacite+json"}
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
import java.util.List;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

public class SignpostingResources {
    private static final Logger logger = Logger.getLogger(SignpostingResources.class.getCanonicalName());
    SystemConfig systemConfig;
    DatasetVersion workingDatasetVersion;
    JsonObject idschemaJsonObj;
    JsonObject licJsonObj;
    JsonObject citeAsJsonObj;

    public SignpostingResources(SystemConfig systemConfig, DatasetVersion workingDatasetVersion, String jsonSetting){
        this.systemConfig = systemConfig;
        this.workingDatasetVersion = workingDatasetVersion;
        JsonReader jsonReader = Json.createReader(new StringReader(jsonSetting));
        JsonObject spJsonSetting = jsonReader.readObject();
        jsonReader.close();
        idschemaJsonObj = spJsonSetting.getJsonObject("indetifier-schema");
        licJsonObj = spJsonSetting.getJsonObject("license");
        citeAsJsonObj = spJsonSetting.getJsonObject("cite-as");
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
        logger.info(String.format("within getidentifiershcema, it is: [%s]", identifierSchema));
        return identifierSchema;
    }

    public String getLinks(){
        Dataset ds = workingDatasetVersion.getDataset();

        // signposting identifierSchema
        String identifierSchema = getIdentifierSchema(workingDatasetVersion.getDatasetAuthors());
        logger.info(String.format("identifierSchema is: %s", identifierSchema));

        // Signposting citeAs
        String citeAs = "<" + ds.getPersistentURL() + ">;rel=\"cite-as\"";
        logger.info(String.format("citeAs is: %s", citeAs));

        // Signposting describedby
        String describedby = "<" + citeAsJsonObj.getString(ds.getProtocol()) + ds.getAuthority() + "/"
                + ds.getIdentifier() + ">;rel=\"describedby\"" + "; type=\""+ citeAsJsonObj.getString("type") + "\"";
        describedby += ",<" + systemConfig.getDataverseSiteUrl() + "/api/datasets/export?exporter=schema.org&persistentId="
                + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier() + ">;rel=\"describedby\"" + ";type=\"application/json+ld\"";
        logger.info(String.format("describedby is: %s", describedby));

        // Signposting type
        String type = "<https://schema.org/AboutPage>;rel=\"type\"";
        logger.info(String.format("type is: %s", type));

        // Signposting license
        String license = ""; //non only CC0
        if (workingDatasetVersion.getTermsOfUseAndAccess().getLicense() == TermsOfUseAndAccess.License.CC0){
            //On the current Dataverse, only None and CC0. In the signposting protocol: cardinality is 1
            license = "<https://creativecommons.org/publicdomain/zero/1.0/>;rel=\"license\"";
        } else {
            // TODO: should get license from ds when flexible license is there
            license = ";rel=\"license\"";
        }
        logger.info(String.format("license is: %s", license));

        // Signposting linkset
        String linkset = "<" + systemConfig.getDataverseSiteUrl() + "/api/datasets/:persistentId/versions/"
                + workingDatasetVersion.getVersionNumber() + "." + workingDatasetVersion.getMinorVersionNumber()
                + "/linkset?persistentId=" + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier() + "> ; rel=\"linkset\";type=\"application/linkset+json\"";
        logger.info(String.format("linkset is: %s", linkset));

        return String.join(", ", citeAs, type, identifierSchema, license, linkset, describedby);
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
        List<DataFile> dfs = ds.getFiles();

        List<FileMetadata> fms = workingDatasetVersion.getFileMetadatas();
        for (FileMetadata fm:fms){
            DataFile df = fm.getDataFile();
            items.add(jsonObjectBuilder().add("href", getPublicDownloadUrl(df)).add("type",df.getContentType()));
        }

        String lic = "";
        if (workingDatasetVersion.getTermsOfUseAndAccess().getLicense() == TermsOfUseAndAccess.License.CC0){
            lic = licJsonObj.getString(TermsOfUseAndAccess.License.CC0.name());
        }
        JsonArrayBuilder jab = Json.createArrayBuilder();
        jab.add(jsonObjectBuilder().add("href", citeAsJsonObj.getJsonObject(ds.getProtocol())+ ds.getAuthority() + "/"
                + ds.getIdentifier()).add("type",citeAsJsonObj.getJsonObject("type")));
        jab.add(jsonObjectBuilder().add("href", systemConfig.getDataverseSiteUrl() + "/api/datasets/export?exporter=schema.org&persistentId=" + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier()).add("type","application/json+ld"));
        JsonArrayBuilder linkset = Json.createArrayBuilder();
        JsonObjectBuilder mandatory = jsonObjectBuilder()
                .add("anchor", landingPage)
                .add("cite-as", Json.createArrayBuilder().add(jsonObjectBuilder().add("href", ds.getPersistentURL())))
                .add("type", Json.createArrayBuilder().add(jsonObjectBuilder().add("href", "https://schema.org/AboutPage")))
                .add("author", authors)
                .add("license", jsonObjectBuilder().add("href", lic))
                .add("item", items).add("describedby", jab);
        linkset.add(mandatory);
        for (FileMetadata fm:fms){
            DataFile df = fm.getDataFile();
            JsonObjectBuilder itemAnchor = jsonObjectBuilder().add("anchor", getPublicDownloadUrl(df));
            itemAnchor.add("collection", Json.createArrayBuilder().add(jsonObjectBuilder()
            .add("href", landingPage)).add(jsonObjectBuilder().add("type","text/html")));
            linkset.add(itemAnchor);
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