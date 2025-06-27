package edu.harvard.iq.dataverse.api;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.FeatureFlags;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;

@Path("localcontexts")
public class LocalContexts extends AbstractApiBean {

    protected static final Logger logger = Logger.getLogger(LocalContexts.class.getName());

    @EJB
    DatasetServiceBean datasetService;

    @Inject
    DataverseRequestServiceBean dvRequestService;

    @EJB
    PermissionServiceBean permissionService;

    @GET
    @Path("/datasets/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @AuthRequired
    public Response getDatasetLocalContexts(@Context ContainerRequestContext crc, @PathParam("id") String id) {
        try {
            Dataset dataset = findDatasetOrDie(id);
            DataverseRequest req = createDataverseRequest(getRequestUser(crc));

            // Check if the user has edit dataset permission
            /* Feature flag to skip permission check
             * If you add the api-session-auth FeatureFlag, you can verify if the user has edit permissions
             * 
             */
            if (FeatureFlags.ADD_LOCAL_CONTEXTS_PERMISSION_CHECK.enabled() && !permissionService.userOn(req.getUser(), dataset).has(Permission.EditDataset)) {
                return error(Response.Status.FORBIDDEN,
                        "You do not have permission to query LocalContexts about this dataset.");
            }

            String localContextsUrl = JvmSettings.LOCALCONTEXTS_URL.lookupOptional().orElse(null);
            String localContextsApiKey = JvmSettings.LOCALCONTEXTS_API_KEY.lookupOptional().orElse(null);

            if (localContextsUrl == null || localContextsApiKey == null) {
                return error(Response.Status.NOT_FOUND, "LocalContexts API configuration is missing.");
            }

            String datasetDoi = dataset.getGlobalId().asString();
            String apiUrl = localContextsUrl + "api/v2/projects/?publication_doi=" + datasetDoi;
            logger.fine("URL used: " + apiUrl);
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(new URI(apiUrl))
                        .header("X-Api-Key", localContextsApiKey).GET().build();

                HttpResponse<String> response;

                response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // Assuming the response is already in JSON format
                    logger.fine("Response from search: " + response.body());
                    JsonObject jsonObject = JsonUtil.getJsonObject(response.body());
                    return ok(jsonObject);
                } else {
                    return error(Response.Status.SERVICE_UNAVAILABLE,
                            "Error from LocalContexts API: " + response.statusCode());
                }
            } catch (URISyntaxException e) {
                logger.warning(e.getMessage());
                return error(Response.Status.SERVICE_UNAVAILABLE, "LocalContexts connection misconfigured.");
            } catch (IOException | InterruptedException e) {
                logger.warning(e.getMessage());
                e.printStackTrace();
                return error(Response.Status.SERVICE_UNAVAILABLE, "Error contacting LocalContexts");

            }
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @GET
    @Path("/datasets/{id}/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchLocalContexts(@PathParam("id") String datasetId, @PathParam("projectId") String projectId) {
        try {
            Dataset dataset = findDatasetOrDie(datasetId);
            String localContextsUrl = JvmSettings.LOCALCONTEXTS_URL.lookupOptional().orElse(null);
            String localContextsApiKey = JvmSettings.LOCALCONTEXTS_API_KEY.lookupOptional().orElse(null);

            if (localContextsUrl == null || localContextsApiKey == null) {
                return error(Response.Status.NOT_FOUND, "LocalContexts API configuration is missing.");
            }

            String apiUrl = localContextsUrl + "api/v2/projects/" + projectId + "/";
            logger.fine("URL used: " + apiUrl);
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(new URI(apiUrl))
                        .header("X-Api-Key", localContextsApiKey).GET().build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // Parse the JSON response
                    JsonObject jsonResponse = JsonUtil.getJsonObject(response.body());
                    logger.fine("Response from get: " + JsonUtil.prettyPrint(jsonResponse));

                    // Check if the response contains a "publication_doi" key
                    if (jsonResponse.containsKey("external_ids")) {
                        JsonObject externalIds = jsonResponse.getJsonObject("external_ids");
                        if (externalIds.containsKey("publication_doi")) {
                            String responseDoi = externalIds.getString("publication_doi");
                            String datasetDoi = dataset.getGlobalId().asString();
                            // Compare the DOI from the response with the dataset's DOI
                            if (responseDoi.equals(datasetDoi)) {
                                // Return the JSON response as-is
                                return ok(jsonResponse);
                            } else {
                                // DOI mismatch, return 404
                                return error(Response.Status.NOT_FOUND,
                                        "LocalContexts information not found for this dataset.");
                            }
                        } else {
                            // "publication_doi" key not found in the response, return 404
                            return error(Response.Status.NOT_FOUND, "Invalid response from Local Contexts API.");
                        }
                    } else {
                        // "external_ids" key not found in the response, return 404
                        return error(Response.Status.NOT_FOUND, "Invalid response from Local Contexts API.");
                    }

                } else {
                    return error(Response.Status.SERVICE_UNAVAILABLE,
                            "Error from Local Contexts API: " + response.statusCode());
                }
            } catch (URISyntaxException e) {
                logger.warning(e.getMessage());
                return error(Response.Status.SERVICE_UNAVAILABLE, "LocalContexts connection misconfigured.");
            } catch (IOException | InterruptedException e) {
                logger.warning(e.getMessage());
                e.printStackTrace();
                return error(Response.Status.SERVICE_UNAVAILABLE, "Error contacting LocalContexts");
            }
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
}