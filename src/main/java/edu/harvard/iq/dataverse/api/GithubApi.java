package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonReader;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("datasets")
public class GithubApi extends AbstractApiBean {

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
    public Response importGithubRepo(@PathParam("id") String idSupplied) {
        return ok("FIXME: download GitHub repo as zip and create a file in dataverse, putting metadata about the repo into the file description.");
    }

}
