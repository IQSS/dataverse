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
 * The caching code resides here because the this is the point at which JSON is generated
 *   and this JSON was deemed the easiest to cache.
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
            String metricName = "dataversesByMonth";
            String jsonString = returnUnexpiredCacheMonthly(metricName, yyyymm);
            
            if(null == jsonString) { //run query and save
                Long count = metricsSvc.dataversesByMonth(yyyymm);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName,yyyymm, jsonString), true); //if not using cache save new
            }

            return allowCors(ok(stringToJsonObjectBuilder(jsonString)));
            
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
            String metricName = "datasetsByMonth";
            String jsonString = returnUnexpiredCacheMonthly(metricName, yyyymm);

            if(null == jsonString) { //run query and save
                Long count = metricsSvc.datasetsByMonth(yyyymm);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName,yyyymm, jsonString), true); //if not using cache save new
            }

            return allowCors(ok(stringToJsonObjectBuilder(jsonString)));

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
            JsonObjectBuilder countJson = MetricsUtil.countToJson(metricsSvc.filesByMonth(yyyymm));
            return allowCors(ok(countJson));
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
            JsonObjectBuilder countJson = MetricsUtil.countToJson(metricsSvc.downloadsByMonth(yyyymm));
            return allowCors(ok(countJson));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

    @GET
    @Path("dataverses/byCategory")
    public Response getDataversesByCategory() {
        try {
            JsonArrayBuilder dvByCat = MetricsUtil.dataversesByCategoryToJson(metricsSvc.dataversesByCategory());
            return allowCors(ok(dvByCat));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

    @GET
    @Path("datasets/bySubject")
    public Response getDatasetsBySubject() {
        try {
            String metricName = "datasetsBySubject";
            String jsonArrayString = returnUnexpiredCacheAllTime(metricName);
            
            if(null == jsonArrayString) { //run query and save
                JsonArrayBuilder jsonArrayBuilder = MetricsUtil.datasetsBySubjectToJson(metricsSvc.datasetsBySubject());
                jsonArrayString = jsonArrayBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, jsonArrayString), false);
            }
            
            return allowCors(ok(stringToJsonArrayBuilder(jsonArrayString)));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }
    
   
    /** Helper functions for metric caching **/ 
    
    public String returnUnexpiredCacheMonthly(String metricName, String yyyymm) throws Exception {
        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm); //remove other sanatize?
        Metric queriedMetric = metricsSvc.getMetric(metricName,sanitizedyyyymm);
        
        if(!metricsSvc.doWeQueryAgainMonthly(queriedMetric)) {
            return queriedMetric.getMetricValue();
        }
        return null;
    }
    
    public String returnUnexpiredCacheAllTime(String metricName) throws Exception{
        Metric queriedMetric = metricsSvc.getMetric(metricName);
        
        if(!metricsSvc.doWeQueryAgainMonthly(queriedMetric)) {
            return queriedMetric.getMetricValue();
        }
        return null;
    }
  
    //Responses need jsonObjectBuilder's to return correct json
    //This converts our json string (created for database storage)
    //into this type to send off to a user.
    //This requires first making a standard JsonObject which sadly Response won't take
    private JsonObjectBuilder stringToJsonObjectBuilder(String str) {
        JsonReader jsonReader = Json.createReader(new StringReader(str));
        JsonObject jo = jsonReader.readObject(); 
        jsonReader.close();
        
        JsonObjectBuilder job = Json.createObjectBuilder();

        for (Entry<String, JsonValue> entry : jo.entrySet()) {
            job.add(entry.getKey(), entry.getValue());
        }

        return job;
    }

    private JsonArrayBuilder stringToJsonArrayBuilder(String str) {
        JsonReader jsonReader = Json.createReader(new StringReader(str));
        JsonArray ja = jsonReader.readArray();
        jsonReader.close();

        JsonArrayBuilder jab = Json.createArrayBuilder();
       
        for (int i = 0; i < ja.size(); i++) {
            JsonObjectBuilder job = Json.createObjectBuilder();
                   
            for (Entry<String, JsonValue> entry : ja.getJsonObject(i).entrySet()) {
                job.add(entry.getKey(), entry.getValue());
            }
            
            jab.add(job);
        }
                
        return jab;
    }
    
}
