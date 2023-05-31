package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.makedatacount.DatasetExternalCitations;
import edu.harvard.iq.dataverse.makedatacount.DatasetExternalCitationsServiceBean;
import edu.harvard.iq.dataverse.makedatacount.DatasetMetrics;
import edu.harvard.iq.dataverse.makedatacount.DatasetMetricsServiceBean;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Note that there are makeDataCount endpoints in Datasets.java as well.
 */
@Path("admin/makeDataCount")
public class MakeDataCountApi extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(MakeDataCountApi.class.getCanonicalName());

    @EJB
    DatasetMetricsServiceBean datasetMetricsService;
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

        JsonObject report;

        try (FileReader reader = new FileReader(reportOnDisk)) {
            report = Json.createReader(reader).readObject();
            Dataset dataset;
            try {
                dataset = findDatasetOrDie(id);
                List<DatasetMetrics> datasetMetrics = datasetMetricsService.parseSushiReport(report, dataset);
                if (!datasetMetrics.isEmpty()) {
                    for (DatasetMetrics dm : datasetMetrics) {
                        datasetMetricsService.save(dm);
                    }
                }
            } catch (WrappedResponse ex) {
                Logger.getLogger(MakeDataCountApi.class.getName()).log(Level.SEVERE, null, ex);
                return error(Status.BAD_REQUEST, "Wrapped response: " + ex.getLocalizedMessage());
            }

        } catch (IOException ex) {
            System.out.print(ex.getMessage());
            return error(Status.BAD_REQUEST, "IOException: " + ex.getLocalizedMessage());
        }
        String msg = "Dummy Data has been added to dataset " + id;
        return ok(msg);
    }

    @POST
    @Path("/addUsageMetricsFromSushiReport")
    public Response addUsageMetricsFromSushiReportAll(@PathParam("id") String id, @QueryParam("reportOnDisk") String reportOnDisk) {

        JsonObject report;

        try (FileReader reader = new FileReader(reportOnDisk)) {
            report = Json.createReader(reader).readObject();

            List<DatasetMetrics> datasetMetrics = datasetMetricsService.parseSushiReport(report, null);
            if (!datasetMetrics.isEmpty()) {
                for (DatasetMetrics dm : datasetMetrics) {
                    datasetMetricsService.save(dm);
                }
            }

        } catch (IOException ex) {
            System.out.print(ex.getMessage());
            return error(Status.BAD_REQUEST, "IOException: " + ex.getLocalizedMessage());
        }
        String msg = "Usage Metrics Data has been added to all datasets from file  " + reportOnDisk;
        return ok(msg);
    }

    @POST
    @Path("{id}/updateCitationsForDataset")
    public Response updateCitationsForDataset(@PathParam("id") String id) throws MalformedURLException, IOException {
        try {
            Dataset dataset = findDatasetOrDie(id);
            String persistentId = dataset.getGlobalId().toString();
            //ToDo - if this isn't a DOI?
            // DataCite wants "doi=", not "doi:".
            String authorityPlusIdentifier = persistentId.replaceFirst("doi:", "");
            // Request max page size and then loop to handle multiple pages
            URL url = new URL(JvmSettings.DATACITE_REST_API_URL.lookup() +
                              "/events?doi=" +
                              authorityPlusIdentifier +
                              "&source=crossref&page[size]=1000");
            logger.fine("Retrieving Citations from " + url.toString());
            boolean nextPage = true;
            JsonArrayBuilder dataBuilder = Json.createArrayBuilder();
            do {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int status = connection.getResponseCode();
                if (status != 200) {
                    logger.warning("Failed to get citations from " + url.toString());
                    return error(Status.fromStatusCode(status), "Failed to get citations from " + url.toString());
                }
                JsonObject report = Json.createReader(connection.getInputStream()).readObject();
                JsonObject links = report.getJsonObject("links");
                JsonArray data = report.getJsonArray("data");
                Iterator<JsonValue> iter = data.iterator();
                while (iter.hasNext()) {
                    dataBuilder.add(iter.next());
                }
                if (links.containsKey("next")) {
                    url = new URL(links.getString("next"));
                } else {
                    nextPage = false;
                }
                logger.fine("body of citation response: " + report.toString());
            } while (nextPage == true);
            JsonArray allData = dataBuilder.build();
            List<DatasetExternalCitations> datasetExternalCitations = datasetExternalCitationsService.parseCitations(allData);
            /*
             * ToDo: If this is the only source of citations, we should remove all the existing ones for the dataset and repopuate them.
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

}
