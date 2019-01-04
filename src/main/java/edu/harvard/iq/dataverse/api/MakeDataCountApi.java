package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.makedatacount.DatasetMetrics;
import edu.harvard.iq.dataverse.makedatacount.DatasetMetricsServiceBean;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
    @Path("{id}/addDummyData")
    public Response addDummyData(@PathParam("id") String id) {

        JsonObject report;

        try (FileReader reader = new FileReader("//Users/skraffmi/NetBeansProjects/dataverse/src/test/java/edu/harvard/iq/dataverse/makedatacount/sushi_sample_logs.json")) {
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

}
