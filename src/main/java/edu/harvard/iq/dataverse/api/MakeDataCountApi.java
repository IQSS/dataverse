package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.makedatacount.DatasetMetrics;
import edu.harvard.iq.dataverse.makedatacount.DatasetMetricsServiceBean;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountUtil;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Note that there are makeDataCount endpoints in Datasets.java as well.
 */
@Path("admin/makeDataCount")
public class MakeDataCountApi extends AbstractApiBean {

    @EJB
    DatasetMetricsServiceBean datasetMetricsService;
    @EJB
    DatasetServiceBean datasetService;

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
            }

        } catch (IOException ex) {
            System.out.print(ex.getMessage());
        }
        String msg = "Dummy Data has been added to dataset " + id;
        return ok(msg);
    }

    @POST
    @Path("{id}/updateCitationsForDataset")
    public Response updateCitationsForDataset(@PathParam("id") String id) throws MalformedURLException, IOException {
        String msg = "updateCitationsForDataset called";
        Dataset dataset = null;
        try {
            // FIXME: Switch to findDatasetOrDie instead of blindly downloading citations for whatever DOI.
            // FIXME: remove this parseBooleanOrDie which is only here to throw WrappedResponse.
            parseBooleanOrDie("true");
//            dataset = findDatasetOrDie(id);
//            String authorityPlusIdentifier = dataset.getAuthority() + dataset.getIdentifier();
            String persistentId = getRequestParameter(":persistentId".substring(1));
            // DataCite wants "doi=", not "doi:".
            String authorityPlusIdentifier = persistentId.replaceFirst("doi:", "");
            // curl https://api.datacite.org/events?doi=10.7910/dvn/hqzoob&source=crossref
            URL url = new URL("https://api.datacite.org/events?doi=" + authorityPlusIdentifier + "&source=crossref");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            // TODO: Do something with non 200 status.
            System.out.println("status: " + status);
            JsonObject report = Json.createReader(connection.getInputStream()).readObject();
            List<DatasetMetrics> datasetMetrics = MakeDataCountUtil.parseCitations(report);
            JsonObjectBuilder output = Json.createObjectBuilder();
            output.add("citationCount", datasetMetrics.size());
            return ok(output);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

}
