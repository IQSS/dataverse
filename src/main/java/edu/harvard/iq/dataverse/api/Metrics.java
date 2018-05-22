package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Metric;
import edu.harvard.iq.dataverse.metrics.MetricsUtil;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map.Entry;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * API endpoints for various metrics.
 *
 * These endpoints look a bit heavy because they check for a timely cached value
 * to use before responding. The caching code resides here because the this is
 * the point at which JSON is generated and this JSON was deemed the easiest to
 * cache.
 *
 * @author pdurbin, madunlap
 */
@Path("info/metrics")
public class Metrics extends AbstractApiBean {

    @GET
    @Path("dataverses/toMonth")
    public Response getDataversesToMonthCurrent() {
        return getDataversesToMonth(MetricsUtil.getCurrentMonth());
    }

    @GET
    @Path("dataverses/toMonth/{yyyymm}")
    public Response getDataversesToMonth(@PathParam("yyyymm") String yyyymm) {
        String metricName = "dataversesToMonth";

        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            String jsonString = metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.dataversesToMonth(sanitizedyyyymm);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, sanitizedyyyymm, jsonString), true); //if not using cache save new
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));

        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

    @GET
    @Path("datasets/toMonth")
    public Response getDatasetsToMonthCurrent() {
        return getDatasetsToMonth(MetricsUtil.getCurrentMonth());
    }

    @GET
    @Path("datasets/toMonth/{yyyymm}")
    public Response getDatasetsToMonth(@PathParam("yyyymm") String yyyymm) {
        String metricName = "datasetsToMonth";

        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            String jsonString = metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.datasetsToMonth(sanitizedyyyymm);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, sanitizedyyyymm, jsonString), true); //if not using cache save new
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));

        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

    @GET
    @Path("files/toMonth")
    public Response getFilesToMonthCurrent() {
        return getFilesToMonth(MetricsUtil.getCurrentMonth());
    }

    @GET
    @Path("files/toMonth/{yyyymm}")
    public Response getFilesToMonth(@PathParam("yyyymm") String yyyymm) {
        String metricName = "filesToMonth";

        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            String jsonString = metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.filesToMonth(sanitizedyyyymm);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, sanitizedyyyymm, jsonString), true); //if not using cache save new
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

    @GET
    @Path("downloads/toMonth")
    public Response getDownloadsToMonthCurrent() {
        return getDownloadsToMonth(MetricsUtil.getCurrentMonth());
    }

    @GET
    @Path("downloads/toMonth/{yyyymm}")
    public Response getDownloadsToMonth(@PathParam("yyyymm") String yyyymm) {
        String metricName = "downloadsToMonth";

        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            String jsonString = metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.downloadsToMonth(sanitizedyyyymm);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, sanitizedyyyymm, jsonString), true); //if not using cache save new
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

    @GET
    @Path("dataverses/byCategory")
    public Response getDataversesByCategory() {
        String metricName = "dataversesByCategory";

        try {
            String jsonArrayString = metricsSvc.returnUnexpiredCacheAllTime(metricName);

            if (null == jsonArrayString) { //run query and save
                JsonArrayBuilder jsonArrayBuilder = MetricsUtil.dataversesByCategoryToJson(metricsSvc.dataversesByCategory());
                jsonArrayString = jsonArrayBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, jsonArrayString), false);
            }

            return allowCors(ok(MetricsUtil.stringToJsonArrayBuilder(jsonArrayString)));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

    @GET
    @Path("datasets/bySubject")
    public Response getDatasetsBySubject() {
        String metricName = "datasetsBySubject";

        try {
            String jsonArrayString = metricsSvc.returnUnexpiredCacheAllTime(metricName);

            if (null == jsonArrayString) { //run query and save
                JsonArrayBuilder jsonArrayBuilder = MetricsUtil.datasetsBySubjectToJson(metricsSvc.datasetsBySubject());
                jsonArrayString = jsonArrayBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, jsonArrayString), false);
            }

            return allowCors(ok(MetricsUtil.stringToJsonArrayBuilder(jsonArrayString)));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }
}
