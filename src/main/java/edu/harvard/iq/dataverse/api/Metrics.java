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
 * These endpoints look a bit heavy because they check for a timely cached value to use before responding.
 * The caching code resides here because the this is the point at which JSON is generated and this JSON was deemed the easiest to cache.
 * 
 * @author pdurbin, madunlap
 */
@Path("info/metrics")
public class Metrics extends AbstractApiBean {

    @GET
    @Path("dataverses/byMonth")
    public Response getDataversesByMonthCurrent() {
        return getDataversesByMonth(MetricsUtil.getCurrentMonth());
    }

    @GET
    @Path("dataverses/byMonth/{yyyymm}")
    public Response getDataversesByMonth(@PathParam("yyyymm") String yyyymm) {
        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            String metricName = "dataversesByMonth";
            String jsonString = metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm);
            
            if(null == jsonString) { //run query and save
                Long count = metricsSvc.dataversesByMonth(sanitizedyyyymm);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName,sanitizedyyyymm, jsonString), true); //if not using cache save new
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));
            
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

    @GET
    @Path("datasets/byMonth")
    public Response getDatasetsByMonthCurrent() {
        return getDatasetsByMonth(MetricsUtil.getCurrentMonth());
    }

    @GET
    @Path("datasets/byMonth/{yyyymm}")
    public Response getDatasetsByMonth(@PathParam("yyyymm") String yyyymm) {
        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            String metricName = "datasetsByMonth";
            String jsonString = metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm);

            if(null == jsonString) { //run query and save
                Long count = metricsSvc.datasetsByMonth(sanitizedyyyymm);
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
    @Path("files/byMonth")
    public Response getFilesByMonthCurrent() {
        return getFilesByMonth(MetricsUtil.getCurrentMonth());
    }

    @GET
    @Path("files/byMonth/{yyyymm}")
    public Response getFilesByMonth(@PathParam("yyyymm") String yyyymm) {
        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            String metricName = "filesByMonth";
            String jsonString = metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm);
            
            if(null == jsonString) { //run query and save
                Long count = metricsSvc.filesByMonth(sanitizedyyyymm);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName,sanitizedyyyymm, jsonString), true); //if not using cache save new
            }
            
            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

    @GET
    @Path("downloads/byMonth")
    public Response getDownloadsByMonthCurrent() {
        return getDownloadsByMonth(MetricsUtil.getCurrentMonth());
    }

    @GET
    @Path("downloads/byMonth/{yyyymm}")
    public Response getDownloadsByMonth(@PathParam("yyyymm") String yyyymm) {
        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            String metricName = "downloadsByMonth";
            String jsonString = metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm);
            
            if(null == jsonString) { //run query and save
                Long count = metricsSvc.downloadsByMonth(sanitizedyyyymm);
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
        try {
            String metricName = "dataversesByCategory";
            String jsonArrayString = metricsSvc.returnUnexpiredCacheAllTime(metricName);
            
            if(null == jsonArrayString) { //run query and save
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
        try {
            String metricName = "datasetsBySubject";
            String jsonArrayString = metricsSvc.returnUnexpiredCacheAllTime(metricName);
            
            if(null == jsonArrayString) { //run query and save
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