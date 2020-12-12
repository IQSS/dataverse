package edu.harvard.iq.dataverse.util;

/*
Eko Indarto
 */

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataaccess.SwiftAccessIO;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.util.List;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

public class SignpostingResources {
    SystemConfig systemConfig;
    DatasetVersion workingDatasetVersion;
    public SignpostingResources(SystemConfig systemConfig, DatasetVersion workingDatasetVersion){
        this.systemConfig = systemConfig;
        this.workingDatasetVersion = workingDatasetVersion;
    }

    public String getLinks(){
        String identifierSchema = "";
        List<DatasetAuthor> dal = workingDatasetVersion.getDatasetAuthors();
        for (DatasetAuthor da:dal){
             /*
                else if author has VIAF
                    Link: <http://viaf.org/viaf/:id/>; rel="author"
                else if author has ISNI
                    Link: <http://www.isni.org/:id>; rel="author"
             */
            if (da.getIdValue() != null && !da.getIdValue().isEmpty()) {
                if (da.getIdType().equals("ORCID"))
                    identifierSchema += ", <https://orcid.org/" + da.getIdValue() + "> ; rel=\"author\"";
                else if (da.getIdType().equals("ISNI"))
                    identifierSchema += ", <https://isni.org/isni/" + da.getIdValue() + "> ; rel=\"author\"";
                else if (da.getIdType().equals("ScopusID"))
                    identifierSchema += ", <https://www.scopus.com/authid/detail.uri?authorId=" + da.getIdValue() + "> ; rel=\"author\"";

            }
        }
        Dataset ds = workingDatasetVersion.getDataset();
        String citeAs = "<" + ds.getPersistentURL() + "> ; rel=\"cite-as\"";
        String describedby = ""; //not only crosscite.
        if (ds.getProtocol().equals("doi")) {
            describedby = ", <https://citation.crosscite.org/format?style=bibtex&doi="+ ds.getAuthority() + "/"
                    + ds.getIdentifier() + "> ; rel=\"describedby\" \n" + "; type=\"application/vnd.datacite.datacite+json";
        }
        describedby += ", <" + systemConfig.getDataverseSiteUrl() + "/api/datasets/export?exporter=schema.org&persistentId="
                + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier() + "> ; rel=\"describedby\" \n" + "; type=\"application/json+ld\"";
        String type = ", <https://schema.org/AboutPage> ; rel=\"type\"";

        String lic = ""; //non only CC0
        if (workingDatasetVersion.getTermsOfUseAndAccess().getLicense() == TermsOfUseAndAccess.License.CC0){
            //On the current Dataverse, only None and CC0. In the signposting protocol: cardinality is 1
            lic = ", <https://creativecommons.org/licenses/by/4.0/> ; rel=\"license\"";
            //http://creativecommons.org/licenses/by/4.0
        }

        String linkset = ", <" + systemConfig.getDataverseSiteUrl() + "/api/datasets/:persistentId/versions/"
                + workingDatasetVersion.getVersionNumber() + "." + workingDatasetVersion.getMinorVersionNumber()
                + "/linkset?persistentId=" + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier() + "> ; rel=\"linkset\" ; type=\"application/linkset+json\"";

        return citeAs + type + identifierSchema + lic + linkset + describedby;
    }


    public JsonArrayBuilder getJsonLinkset() {
        Dataset ds = workingDatasetVersion.getDataset();
//        String pid =  ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier() ;
//        String anchor = systemConfig.getDataverseSiteUrl() + "/dataset.xhtml?persistentId=" + pid;
        String landingPage = systemConfig.getDataverseSiteUrl() + "/dataset.xhtml?persistentId=" + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier() ;
        List<DatasetAuthor> dal = workingDatasetVersion.getDatasetAuthors();
        JsonArrayBuilder authors = Json.createArrayBuilder();
        for (DatasetAuthor da:dal){
             /*
                else if author has VIAF
                    Link: <http://viaf.org/viaf/:id/>; rel="author"
                else if author has ISNI
                    Link: <http://www.isni.org/:id>; rel="author"
             */
            if (da.getIdType().equals("ORCID")){
                authors.add(jsonObjectBuilder().add("href", "https://orcid.org/" + da.getIdValue() ));
            }
        }

        JsonArrayBuilder items = Json.createArrayBuilder();
        List<DataFile> dfs = ds.getFiles();

        List<FileMetadata> fms = workingDatasetVersion.getFileMetadatas();
        for (FileMetadata fm:fms){
            DataFile df = fm.getDataFile();
            items.add(jsonObjectBuilder().add("href", getPublicDownloadUrl(df)).add("type",df.getContentType()));
        }

        String lic = "";
        if (workingDatasetVersion.getTermsOfUseAndAccess().getLicense() == TermsOfUseAndAccess.License.CC0){
            //On the current Dataverse, only None and CC0. In the signposting protocol: cardinality is 1
            lic = "https://creativecommons.org/licenses/by/4.0/";
            //http://creativecommons.org/licenses/by/4.0
        }
        JsonArrayBuilder jab = Json.createArrayBuilder();
        jab.add(jsonObjectBuilder().add("href", "https://citation.crosscite.org/format?style=bibtex&doi="+ ds.getAuthority() + "/"
        + ds.getIdentifier()).add("type","application/vnd.datacite.datacite+json"));
//        jab.add(jsonObjectBuilder().add("href", systemConfig.getDataverseSiteUrl() + "/api/datasets/export?exporter=schema.org&persistentId=" + ds.getProtocol() + ":" + ds.getAuthority() + "/" + ds.getIdentifier()).add("type","application/vnd.datacite.datacite+json"));
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
