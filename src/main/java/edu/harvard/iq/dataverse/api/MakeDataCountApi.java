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
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.enterprise.concurrent.ManagedExecutorService;
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

    // Inject the managed executor service provided by the container
    @Resource(name = "concurrent/CitationUpdateExecutor")
    private ManagedExecutorService executorService;
    
    // Track the last execution time to implement rate limiting during Citation updates
    private static final AtomicLong lastExecutionTime = new AtomicLong(0);
    
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
    public Response updateCitationsForDataset(@PathParam("id") String id) {
        try {
            // First validate that the dataset exists and has a valid DOI
            final Dataset dataset = findDatasetOrDie(id);
            final GlobalId pid = dataset.getGlobalId();
            final PidProvider pidProvider = PidUtil.getPidProvider(pid.getProviderId());

            // Only supported for DOIs and for DataCite DOI providers
            if (!DataCiteDOIProvider.TYPE.equals(pidProvider.getProviderType())) {
                return error(Status.BAD_REQUEST, "Only DataCite DOI providers are supported");
            }

            // Submit the task to the managed executor service
            Future<?> future;
            try {
                future = executorService.submit(() -> {
                    try {
                        // Apply rate limiting if enabled
                        applyRateLimit();

                        // Process the citation update
                        boolean success = processCitationUpdate(dataset, pid, pidProvider);

                        // Update the last execution time after processing
                        lastExecutionTime.set(System.currentTimeMillis());

                        if (success) {
                            logger.fine("Successfully processed citation update for dataset " + id);
                        } else {
                            logger.warning("Failed to process citation update for dataset " + id);
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error processing citation update for dataset " + id, e);
                    }
                });

                JsonObjectBuilder output = Json.createObjectBuilder();
                output.add("status", "queued");
                output.add("message", "Citation update for dataset " + id + " has been queued for processing");
                return ok(output);
            } catch (RejectedExecutionException ree) {
                logger.warning("Citation update for dataset " + id + " was rejected: Queue is full");
                return error(Status.SERVICE_UNAVAILABLE,
                        "Citation update service is currently at capacity. Please try again later.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
    
    /**
     * Apply rate limiting by waiting if necessary
     */
    private void applyRateLimit() {
        // Check if rate limiting is enabled
        long minDelay = JvmSettings.API_MDC_UPDATE_MIN_DELAY_MS.lookupOptional(Long.class).orElse(0l);
        if(minDelay ==0) {
            return;
        }
        // Calculate how long to wait
        long lastExecution = lastExecutionTime.get();
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastExecution;
        
        // If not enough time has passed since the last execution, wait
        if (lastExecution > 0 && elapsedTime < minDelay) {
            long waitTime = minDelay - elapsedTime;
            logger.fine("Rate limiting: waiting " + waitTime + " ms before processing next citation update");
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warning("Rate limiting sleep interrupted: " + e.getMessage());
            }
        }
    }
    
    /**
     * Process the citation update for a dataset
     * This method contains the logic that was previously in updateCitationsForDataset
     * @return true if processing was successful, false otherwise
     */
    private boolean processCitationUpdate(Dataset dataset, GlobalId pid, PidProvider pidProvider) {
        String persistentId = pid.asRawIdentifier();
        
        // Request max page size and then loop to handle multiple pages
        URL url = null;
        try {
            url = new URI(JvmSettings.DATACITE_REST_API_URL.lookup(pidProvider.getId()) +
                          "/events?doi=" +
                          persistentId +
                          "&source=crossref&page[size]=1000&page[cursor]=1").toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            //Nominally this means a config error/ bad DATACITE_REST_API_URL for this provider
            logger.warning("Unable to create URL for " + persistentId + ", pidProvider " + pidProvider.getId());
            return false;
        }
        
        logger.fine("Retrieving Citations from " + url.toString());
        boolean nextPage = true;
        JsonArrayBuilder dataBuilder = Json.createArrayBuilder();
        
        try {
            do {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int status = connection.getResponseCode();
                if (status != 200) {
                    logger.warning("Failed to get citations from " + url.toString());
                    connection.disconnect();
                    return false;
                }
                
                JsonObject report;
                try (InputStream inStream = connection.getInputStream()) {
                    report = JsonUtil.getJsonObjectFromInputStream(inStream);
                } finally {
                    connection.disconnect();
                }
                
                JsonObject links = report.getJsonObject("links");
                JsonArray data = report.getJsonArray("data");
                Iterator<JsonValue> iter = data.iterator();
                while (iter.hasNext()) {
                    JsonValue citationValue = iter.next();
                    JsonObject citation = (JsonObject) citationValue;
                    
                    // Filter out relations we don't use (e.g. hasPart) to lower memory req. with many files
                    if (citation.containsKey("attributes")) {
                        JsonObject attributes = citation.getJsonObject("attributes");
                        if (attributes.containsKey("relation-type-id")) {
                            String relationshipType = attributes.getString("relation-type-id");
                            
                            // Only add citations with relationship types we care about
                            if (DatasetExternalCitationsServiceBean.inboundRelationships.contains(relationshipType) ||
                                    DatasetExternalCitationsServiceBean.outboundRelationships.contains(relationshipType)) {
                                dataBuilder.add(citationValue);
                            }
                        }
                    }
                }
                
                if (links.containsKey("next")) {
                    try {
                        url = new URI(links.getString("next")).toURL();
                        applyRateLimit();
                    } catch (URISyntaxException e) {
                        logger.warning("Unable to create URL from DataCite response: " + links.getString("next"));
                        return false;
                    }
                } else {
                    nextPage = false;
                }
                
                logger.fine("body of citation response: " + report.toString());
            } while (nextPage == true);
            
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
            
            logger.fine("Citation update completed for dataset " + dataset.getId() + 
                       " with " + datasetExternalCitations.size() + " citations");
            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error processing citation update for dataset " + dataset.getId(), e);
            return false;
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
