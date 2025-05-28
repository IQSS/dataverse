package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.makedatacount.DatasetExternalCitations;
import edu.harvard.iq.dataverse.makedatacount.DatasetExternalCitationsServiceBean;
import edu.harvard.iq.dataverse.makedatacount.DatasetMetrics;
import edu.harvard.iq.dataverse.makedatacount.DatasetMetricsServiceBean;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountProcessState;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountProcessStateServiceBean;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.pidproviders.doi.datacite.DataCiteDOIProvider;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Note that there are makeDataCount endpoints in Datasets.java as well.
 */
@Path("admin/makeDataCount")
public class MakeDataCountApi extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(MakeDataCountApi.class.getCanonicalName());

    @EJB
    DatasetMetricsServiceBean datasetMetricsService;
    @EJB
    MakeDataCountProcessStateServiceBean makeDataCountProcessStateService;
    @EJB
    DatasetExternalCitationsServiceBean datasetExternalCitationsService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    SystemConfig systemConfig;

    /**
     * TODO: For each dataset, send the following:
     *
     * - views
     *
     * - downloads
     *
     * - citations (based on "Related Dataset"). "DataCite already supports
     * linking to publications in the DOI-related metadata that you submit with
     * your DOIs using the 'relatedIdentifier' element. See the DataCite
     * EventData Guide ( https://support.datacite.org/docs/eventdata-guide ).
     * These publication linkages are parsed and added to the EventData source.
     * So, the "hub" is used for reporting Investigations and Requests as
     * counts, whereas every individual citation event is reported in the DOI
     * metadata." mbjones at
     * https://github.com/IQSS/dataverse/issues/4821#issuecomment-440740205
     *
     * TODO: While we're working on sending citations for related *datasets* to
     * DataCite we should also strongly consider sending citations for related
     * *publications* (articles) as well. See
     * https://github.com/IQSS/dataverse/issues/2917 and
     * https://github.com/IQSS/dataverse/issues/2778
     */
    @POST
    @Path("sendToHub")
    public Response sendDataToHub() {
        String msg = "Data has been sent to Make Data Count";
        return ok(msg);
    }

    @POST
    @Path("{id}/addUsageMetricsFromSushiReport")
    public Response addUsageMetricsFromSushiReport(@PathParam("id") String id, @QueryParam("reportOnDisk") String reportOnDisk) {

        try {
            JsonObject report = JsonUtil.getJsonObjectFromFile(reportOnDisk);
            Dataset dataset = findDatasetOrDie(id);
            List<DatasetMetrics> datasetMetrics = datasetMetricsService.parseSushiReport(report, dataset);
            if (!datasetMetrics.isEmpty()) {
                for (DatasetMetrics dm : datasetMetrics) {
                    datasetMetricsService.save(dm);
                }
            }
        } catch (WrappedResponse ex) {
            logger.log(Level.SEVERE, null, ex);
            return error(Status.BAD_REQUEST, "Wrapped response: " + ex.getLocalizedMessage());

        } catch (IOException ex) {
            logger.log(Level.WARNING, ex.getMessage());
            return error(Status.BAD_REQUEST, "IOException: " + ex.getLocalizedMessage());
        }
        String msg = "Dummy Data has been added to dataset " + id;
        return ok(msg);
    }

    @POST
    @Path("/addUsageMetricsFromSushiReport")
    public Response addUsageMetricsFromSushiReportAll(@QueryParam("reportOnDisk") String reportOnDisk) {

        try {
            JsonObject report = JsonUtil.getJsonObjectFromFile(reportOnDisk);

            List<DatasetMetrics> datasetMetrics = datasetMetricsService.parseSushiReport(report, null);
            if (!datasetMetrics.isEmpty()) {
                for (DatasetMetrics dm : datasetMetrics) {
                    datasetMetricsService.save(dm);
                }
            }

        } catch (IOException ex) {
            logger.log(Level.WARNING, ex.getMessage());
            return error(Status.BAD_REQUEST, "IOException: " + ex.getLocalizedMessage());
        }
        String msg = "Usage Metrics Data has been added to all datasets from file  " + reportOnDisk;
        return ok(msg);
    }

    @POST
    @Path("{id}/updateCitationsForDataset")
    public Response updateCitationsForDataset(@PathParam("id") String id) throws IOException {
        try {
            Dataset dataset = findDatasetOrDie(id);
            GlobalId pid = dataset.getGlobalId();
            PidProvider pidProvider = PidUtil.getPidProvider(pid.getProviderId());
            // Only supported for DOIs and for DataCite DOI providers
            if(!DataCiteDOIProvider.TYPE.equals(pidProvider.getProviderType())) {
                return error(Status.BAD_REQUEST, "Only DataCite DOI providers are supported");
            }
            String persistentId = pid.toString();

            // DataCite wants "doi=", not "doi:".
            String authorityPlusIdentifier = persistentId.replaceFirst("doi:", "");
            // Request max page size and then loop to handle multiple pages
            URL url = null;
            try {
                url = new URI(JvmSettings.DATACITE_REST_API_URL.lookup(pidProvider.getId()) +
                              "/events?doi=" +
                              authorityPlusIdentifier +
                              "&source=crossref&page[size]=1000").toURL();
            } catch (URISyntaxException e) {
                //Nominally this means a config error/ bad DATACITE_REST_API_URL for this provider
                logger.warning("Unable to create URL for " + persistentId + ", pidProvider " + pidProvider.getId());
                return error(Status.INTERNAL_SERVER_ERROR, "Unable to create DataCite URL to retrieve citations.");
            }
            logger.fine("Retrieving Citations from " + url.toString());
            boolean nextPage = true;
            JsonArrayBuilder dataBuilder = Json.createArrayBuilder();

            int totalPages = 10000; // Default max page number to avoid infinite loop
            int currentPage = 0;
            String previousUrl = url.toString();
            do {
                currentPage++;
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int status = connection.getResponseCode();
                if (status != 200) {
                    logger.warning("Failed to get citations from " + url.toString());
                    connection.disconnect();
                    return error(Status.fromStatusCode(status), "Failed to get citations from " + url.toString());
                }
                JsonObject report;
                try (InputStream inStream = connection.getInputStream()) {
                    report = JsonUtil.getJsonObject(inStream);
                } finally {
                    connection.disconnect();
                }
                JsonObject meta = report.getJsonObject("meta");
                if(meta.containsKey("total-pages")) {
                    totalPages = Math.min(meta.getInt("total-pages"), totalPages);
                }
                JsonObject links = report.getJsonObject("links");
                JsonArray data = report.getJsonArray("data");
                Iterator<JsonValue> iter = data.iterator();
                while (iter.hasNext()) {
                    dataBuilder.add(iter.next());
                }
                if (links.containsKey("next")) {
                    try {
                        previousUrl = url.toString();
                        url = new URI(links.getString("next")).toURL();
                    } catch (URISyntaxException e) {
                        logger.warning("Unable to create URL from DataCite response: " + links.getString("next"));
                        return error(Status.INTERNAL_SERVER_ERROR, "Unable to retrieve all results from DataCite");
                    }
                } else {
                    nextPage = false;
                }
                logger.fine("body of citation response: " + report.toString());

                // Conditions to avoid infinite loop
                if (url.toString().equals(previousUrl)) {
                    logger.warning("Detected infinite loop on next URL: " + url + ". Breaking loop.");
                    nextPage = false;
                }
                if (currentPage == totalPages) {
                    logger.warning("Pagination exceeded " + totalPages + " pages. Breaking loop.");
                    nextPage = false;
                }
            } while (nextPage);
            JsonArray allData = dataBuilder.build();
            List<DatasetExternalCitations> datasetExternalCitations = datasetExternalCitationsService.parseCitations(allData);
            /*
             * ToDo: If this is the only source of citations, we should remove all the existing ones for the dataset and repopulate them.
             * As is, this call doesn't remove old citations if there are now none (legacy issue if we decide to stop counting certain types of citation
             * as we've done for 'hasPart').
             * If there are some, this call individually checks each one and if a matching item exists, it removes it and adds it back. Faster and better to delete all and
             * add the new ones.
             */
            if (!datasetExternalCitations.isEmpty()) {
                for (DatasetExternalCitations dm : datasetExternalCitations) {
                    datasetExternalCitationsService.save(dm);
                }
            }

            JsonObjectBuilder output = Json.createObjectBuilder();
            output.add("citationCount", datasetExternalCitations.size());
            return ok(output);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
    @GET
    @Path("{yearMonth}/processingState")
    public Response getProcessingState(@PathParam("yearMonth") String yearMonth) {
        MakeDataCountProcessState mdcps;
        try {
            mdcps = makeDataCountProcessStateService.getMakeDataCountProcessState(yearMonth);
        } catch (IllegalArgumentException e) {
            return error(Status.BAD_REQUEST,e.getMessage());
        }
        if (mdcps != null) {
            JsonObjectBuilder output = Json.createObjectBuilder();
            output.add("yearMonth", mdcps.getYearMonth());
            output.add("state", mdcps.getState().name());
            output.add("stateChangeTimestamp", mdcps.getStateChangeTime().toString());
            if (mdcps.getServer() != null && !mdcps.getServer().isBlank()) {
                output.add("server", mdcps.getServer());
            }
            return ok(output);
        } else {
            return error(Status.NOT_FOUND, "Could not find an existing process state for " + yearMonth);
        }
    }

    @POST
    @Path("{yearMonth}/processingState")
    public Response updateProcessingState(@PathParam("yearMonth") String yearMonth, @QueryParam("state") String state, @QueryParam("server") String server) {
        MakeDataCountProcessState mdcps;
        try {
            mdcps = makeDataCountProcessStateService.setMakeDataCountProcessState(yearMonth, state, server);
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }

        JsonObjectBuilder output = Json.createObjectBuilder();
        output.add("yearMonth", mdcps.getYearMonth());
        output.add("state", mdcps.getState().name());
        output.add("stateChangeTimestamp", mdcps.getStateChangeTime().toString());
        if ( mdcps.getServer() != null) {
            output.add("server", mdcps.getServer());
        }
        return ok(output);
    }

    @DELETE
    @Path("{yearMonth}/processingState")
    public Response deleteProcessingState(@PathParam("yearMonth") String yearMonth) {
        boolean deleted = makeDataCountProcessStateService.deleteMakeDataCountProcessState(yearMonth);
        if (deleted) {
            return ok("Processing State deleted for " + yearMonth);
        } else {
            return notFound("Processing State not found for " + yearMonth);
        }
    }
}
