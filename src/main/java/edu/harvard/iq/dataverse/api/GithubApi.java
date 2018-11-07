package edu.harvard.iq.dataverse.api;

import com.mashape.unirest.http.exceptions.UnirestException;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.github.GithubUtil;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonReader;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("datasets")
public class GithubApi extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(GithubApi.class.getCanonicalName());

    @EJB
    IngestServiceBean ingestService;

    @GET
    @Path("{id}/github")
    public Response getGithubUrl(@PathParam("id") String idSupplied) {
        try {
            Dataset dataset = findDatasetOrDie(idSupplied);
            final NullSafeJsonBuilder jsonObject = jsonObjectBuilder()
                    .add("datasetId", dataset.getId())
                    .add("githubUrl", dataset.getGithubUrl());
            return ok(jsonObject);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @POST
    @Path("{id}/github/setUrl")
    public Response setGithubUrl(String body, @PathParam("id") String idSupplied) {
        try {
            Dataset datasetBeforeSave = findDatasetOrDie(idSupplied);
            JsonReader jsonReader = Json.createReader(new StringReader(body));
            String githubUrl = jsonReader.readObject().getString("githubUrl");
            datasetBeforeSave.setGithubUrl(githubUrl);
            Dataset savedDataset = datasetSvc.merge(datasetBeforeSave);
            final NullSafeJsonBuilder jsonObject = jsonObjectBuilder()
                    .add("datasetId", savedDataset.getId())
                    .add("githubUrl", savedDataset.getGithubUrl());
            return ok(jsonObject);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @POST
    @Path("{id}/github/import")
    public Response importGithubRepo(@PathParam("id") String idSupplied) throws IOException, CommandException, URISyntaxException, MalformedURLException, UnirestException {
        try {
            Dataset dataset = findDatasetOrDie(idSupplied);
            String githubUrl = dataset.getGithubUrl();
            // FIXME: First try to download the latest release, if any are available, then try the default branch, which may not be "master".
            String zipUrl = githubUrl + "/archive/master.zip";
            URI gitRepoAsUrl = new URI(githubUrl);
            String gitRepoName = gitRepoAsUrl.getPath().split("/")[2];
            String filename = "/tmp/" + gitRepoName + ".zip";
            boolean downloadFile = true;
            if (downloadFile) {
                try (BufferedInputStream in = new BufferedInputStream(new URL(zipUrl).openStream()); FileOutputStream fileOutputStream = new FileOutputStream(filename);) {
                    byte dataBuffer[] = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                        fileOutputStream.write(dataBuffer, 0, bytesRead);
                    }
                } catch (IOException ex) {
                    System.out.println("exception: " + ex);
                }
            }
            File initialFile = new File(filename);
            String doubleZippedFileName = "/tmp/doublezip.zip";
            ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(doubleZippedFileName));
            zip.putNextEntry(new ZipEntry(filename));
            int length;
            byte[] b = new byte[2048];
            InputStream in = new FileInputStream(initialFile);
            while ((length = in.read(b)) > 0) {
                zip.write(b, 0, length);
            }
            zip.closeEntry();
            zip.close();
            File doubleZippedFile = new File(doubleZippedFileName);
            InputStream targetStream = new FileInputStream(doubleZippedFile);
            DatasetVersion editVersion = dataset.getEditVersion();
            String uploadedZipFilename = gitRepoName + ".zip";
            String guessContentTypeForMe = null;
            List<DataFile> dataFilesIn = FileUtil.createDataFiles(editVersion, targetStream, uploadedZipFilename, guessContentTypeForMe, systemConfig);
            dataFilesIn.get(0).setDescription(GithubUtil.fetchGithubMetadata(gitRepoAsUrl).build().getString("metadata"));
            List<DataFile> dataFilesOut = ingestService.saveAndAddFilesToDataset(editVersion, dataFilesIn);
            DataverseRequest dataverseRequest = createDataverseRequest(findAuthenticatedUserOrDie());
            UpdateDatasetVersionCommand updateDatasetVersionCommand = new UpdateDatasetVersionCommand(dataset, dataverseRequest);
            Dataset updatedDataset = engineSvc.submit(updateDatasetVersionCommand);
            ingestService.startIngestJobsForDataset(dataset, findAuthenticatedUserOrDie());
            return ok("dataset id: " + updatedDataset.getId());
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

}
