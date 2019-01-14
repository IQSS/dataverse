package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Metric;
import edu.harvard.iq.dataverse.metrics.MetricsUtil;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
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

    /** Dataverses */
    
    @GET
    @Path("dataverses")
    public Response getDataversesAllTime() {
        return getDataversesToMonth(MetricsUtil.getCurrentMonth());
    }
    
    @Deprecated //for better path
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
            String jsonString = metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.dataversesToMonth(sanitizedyyyymm);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, jsonString));
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));

        //TODO: Eventually the catch in each endpoint should be more specific
        //          and more general errors should be logged.
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }
    
    @GET
    @Path("dataverses/pastDays/{days}")
    public Response getDataversesPastDays(@PathParam("days") int days) {
        String metricName = "dataversesPastDays";
        
        if(days < 1) {
            return allowCors(error(BAD_REQUEST, "Invalid parameter for number of days."));
        }
        try {
            String jsonString = metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), null);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.dataversesPastDays(days);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, String.valueOf(days), null, jsonString));
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
            String jsonArrayString = metricsSvc.returnUnexpiredCacheAllTime(metricName, null);

            if (null == jsonArrayString) { //run query and save
                JsonArrayBuilder jsonArrayBuilder = MetricsUtil.dataversesByCategoryToJson(metricsSvc.dataversesByCategory());
                jsonArrayString = jsonArrayBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, null, null, jsonArrayString));
            }

            return allowCors(ok(MetricsUtil.stringToJsonArrayBuilder(jsonArrayString)));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }
    
    @GET
    @Path("dataverses/bySubject")
    public Response getDataversesBySubject() {
        String metricName = "dataversesBySubject";
        
        try {
            String jsonArrayString = metricsSvc.returnUnexpiredCacheAllTime(metricName, null);

            if (null == jsonArrayString) { //run query and save
                JsonArrayBuilder jsonArrayBuilder = MetricsUtil.dataversesBySubjectToJson(metricsSvc.dataversesBySubject());
                jsonArrayString = jsonArrayBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, null, null, jsonArrayString));
            }

            return allowCors(ok(MetricsUtil.stringToJsonArrayBuilder(jsonArrayString)));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }
    
    /** Datasets */
    
    @GET
    @Path("datasets")
    public Response getDatasetsAllTime(@QueryParam("dataLocation") String dataLocation) {
        return getDatasetsToMonth(MetricsUtil.getCurrentMonth(), dataLocation);
    }
    
    @Deprecated //for better path
    @GET
    @Path("datasets/toMonth")
    public Response getDatasetsToMonthCurrent(@QueryParam("dataLocation") String dataLocation) {
        return getDatasetsToMonth(MetricsUtil.getCurrentMonth(), dataLocation);
    }

    @GET
    @Path("datasets/toMonth/{yyyymm}")
    public Response getDatasetsToMonth(@PathParam("yyyymm") String yyyymm, @QueryParam("dataLocation") String dataLocation) {
        String metricName = "datasetsToMonth";

        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            String validDataLocation = MetricsUtil.validateDataLocationStringType(dataLocation);
            String jsonString = metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, validDataLocation);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.datasetsToMonth(sanitizedyyyymm, validDataLocation);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, sanitizedyyyymm, validDataLocation, jsonString));
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));

        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }
    
    @GET
    @Path("datasets/pastDays/{days}")
    public Response getDatasetsPastDays(@PathParam("days") int days, @QueryParam("dataLocation") String dataLocation) {
        String metricName = "datasetsPastDays";
        
        if(days < 1) {
            return allowCors(error(BAD_REQUEST, "Invalid parameter for number of days."));
        }
        try {
            String validDataLocation = MetricsUtil.validateDataLocationStringType(dataLocation);
            String jsonString = metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), validDataLocation);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.datasetsPastDays(days, validDataLocation);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, String.valueOf(days), validDataLocation, jsonString));
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));

        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }
    
    @GET
    @Path("datasets/bySubject")
    public Response getDatasetsBySubject(@QueryParam("dataLocation") String dataLocation) {
        return getDatasetsBySubjectToMonth(MetricsUtil.getCurrentMonth(), dataLocation);
    }
  
    @GET
    @Path("datasets/bySubject/toMonth/{yyyymm}")
    public Response getDatasetsBySubjectToMonth(@PathParam("yyyymm") String yyyymm, @QueryParam("dataLocation") String dataLocation) {
        String metricName = "datasetsBySubjectToMonth";

        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            String validDataLocation = MetricsUtil.validateDataLocationStringType(dataLocation);
            String jsonArrayString = metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, validDataLocation);
            
            if (null == jsonArrayString) { //run query and save
                JsonArrayBuilder jsonArrayBuilder = MetricsUtil.datasetsBySubjectToJson(metricsSvc.datasetsBySubjectToMonth(sanitizedyyyymm, validDataLocation));
                jsonArrayString = jsonArrayBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, sanitizedyyyymm, validDataLocation, jsonArrayString));
            }

            return allowCors(ok(MetricsUtil.stringToJsonArrayBuilder(jsonArrayString)));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }
    
    /** Files */
    @GET
    @Path("files")
    public Response getFilesAllTime(@QueryParam("dataLocation") String dataLocation) {
        return getFilesToMonth(MetricsUtil.getCurrentMonth(), dataLocation);
    }
    
    @Deprecated //for better path
    @GET
    @Path("files/toMonth")
    public Response getFilesToMonthCurrent(@QueryParam("dataLocation") String dataLocation) {
        return getFilesToMonth(MetricsUtil.getCurrentMonth(), dataLocation);
    }

    @GET
    @Path("files/toMonth/{yyyymm}")
    public Response getFilesToMonth(@PathParam("yyyymm") String yyyymm, @QueryParam("dataLocation") String dataLocation) {
        String metricName = "filesToMonth";

        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            String validDataLocation = MetricsUtil.validateDataLocationStringType(dataLocation);
            String jsonString = metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, validDataLocation);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.filesToMonth(sanitizedyyyymm, validDataLocation);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, sanitizedyyyymm, validDataLocation, jsonString));
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }
    
    @GET
    @Path("files/pastDays/{days}")
    public Response getFilesPastDays(@PathParam("days") int days, @QueryParam("dataLocation") String dataLocation) {
        String metricName = "filesPastDays";
        
        if(days < 1) {
            return allowCors(error(BAD_REQUEST, "Invalid parameter for number of days."));
        }
        try {
            String validDataLocation = MetricsUtil.validateDataLocationStringType(dataLocation);
            String jsonString = metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), validDataLocation);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.filesPastDays(days, validDataLocation);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, String.valueOf(days), validDataLocation, jsonString));
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));

        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

    /** Downloads */
    
    @GET
    @Path("downloads")
    public Response getDownloadsAllTime() {
        return getDownloadsToMonth(MetricsUtil.getCurrentMonth());
    }
    
    @Deprecated //for better path
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
            String jsonString = metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.downloadsToMonth(sanitizedyyyymm);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, jsonString));
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }
    
    @GET
    @Path("downloads/pastDays/{days}")
    public Response getDownloadsPastDays(@PathParam("days") int days) {
        String metricName = "downloadsPastDays";
        
        if(days < 1) {
            return allowCors(error(BAD_REQUEST, "Invalid parameter for number of days."));
        }
        try {
            String jsonString = metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), null);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.downloadsPastDays(days);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, String.valueOf(days), null, jsonString));
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));

        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

}
