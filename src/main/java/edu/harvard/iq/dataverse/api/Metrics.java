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

    //MAD:
    //so the problem is I want to allow (harvested, not harvested, both). how to I describe that?
    //dataLocation : remote, local, all
    //Should we include the depricated methods? Yes I guess
    
    public String fakeDataLocale = null;
    
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
        //We need constants for dataLocation, where should they be stored?
        
        //MAD: Do i need to sanitize the queryParam? I don't think so because I'm just going to match on it...
        // Well I can create a helper method to prune out strings that don't match the ones we want...
        
        String metricName = "dataversesToMonth";

        try {
            //metricName = metricName + Metric.separator + MetricsUtil.validateDataLocationStringType(dataLocation);
            
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            String jsonString = metricsSvc.returnUnexpiredCacheMonthly(metricName, fakeDataLocale, sanitizedyyyymm);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.dataversesToMonth(sanitizedyyyymm);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, fakeDataLocale, sanitizedyyyymm, jsonString));//, true); //if not using cache save new
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));

        } catch (Exception ex) {
            //MAD: SHOULD WE BE LOGGING ERRORS?
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

    //MAD: all of thse need dataLocation and the correct metric constructor
    
    @GET
    @Path("dataverses/pastDays/{days}")
    public Response getDataversesPastDays(@PathParam("days") int days) {
        String metricName = "dataversesPastDays";
        
        if(days < 1) {
            return allowCors(error(BAD_REQUEST, "Invalid parameter for number of days."));
        }
        try {
            String jsonString = metricsSvc.returnUnexpiredCacheDayBased(metricName, fakeDataLocale, String.valueOf(days));

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.dataversesPastDays(fakeDataLocale, days);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, fakeDataLocale, String.valueOf(days), jsonString));//, true); //if not using cache save new
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
            String jsonArrayString = metricsSvc.returnUnexpiredCacheAllTime(metricName, fakeDataLocale);

            if (null == jsonArrayString) { //run query and save
                JsonArrayBuilder jsonArrayBuilder = MetricsUtil.dataversesByCategoryToJson(metricsSvc.dataversesByCategory(fakeDataLocale));
                jsonArrayString = jsonArrayBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, fakeDataLocale, jsonArrayString));//, false);
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
            String jsonArrayString = metricsSvc.returnUnexpiredCacheAllTime(metricName, fakeDataLocale);

            if (null == jsonArrayString) { //run query and save
                JsonArrayBuilder jsonArrayBuilder = MetricsUtil.dataversesBySubjectToJson(metricsSvc.dataversesBySubject(fakeDataLocale));
                jsonArrayString = jsonArrayBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, fakeDataLocale, jsonArrayString));//, false);
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
        return getDatasetsToMonth(dataLocation, MetricsUtil.getCurrentMonth());
    }
    
    @Deprecated //for better path
    @GET
    @Path("datasets/toMonth")
    public Response getDatasetsToMonthCurrent(@QueryParam("dataLocation") String dataLocation) {
        return getDatasetsToMonth(dataLocation, MetricsUtil.getCurrentMonth());
    }

    @GET
    @Path("datasets/toMonth/{yyyymm}")
    public Response getDatasetsToMonth(@QueryParam("dataLocation") String dataLocation, @PathParam("yyyymm") String yyyymm) {
        String metricName = "datasetsToMonth";

        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            String validDataLocation = MetricsUtil.validateDataLocationStringType(dataLocation);
            String jsonString = metricsSvc.returnUnexpiredCacheMonthly(metricName, validDataLocation, sanitizedyyyymm);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.datasetsToMonth(validDataLocation, sanitizedyyyymm);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, validDataLocation, sanitizedyyyymm, jsonString));//, true); //if not using cache save new
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));

        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }
    
    @GET
    @Path("datasets/pastDays/{days}")
    public Response getDatasetsPastDays(@PathParam("days") int days) {
        String metricName = "datasetsPastDays";
        
        if(days < 1) {
            return allowCors(error(BAD_REQUEST, "Invalid parameter for number of days."));
        }
        try {
            String jsonString = metricsSvc.returnUnexpiredCacheDayBased(metricName, fakeDataLocale, String.valueOf(days));

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.datasetsPastDays(fakeDataLocale, days);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, fakeDataLocale, String.valueOf(days), jsonString));//, true); //if not using cache save new
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));

        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }
    
    @GET
    @Path("datasets/bySubject")
    public Response getDatasetsBySubject() {
        return getDatasetsBySubjectToMonth(MetricsUtil.getCurrentMonth());
    }
  
    @GET
    @Path("datasets/bySubject/toMonth/{yyyymm}")
    public Response getDatasetsBySubjectToMonth(@PathParam("yyyymm") String yyyymm) {
        String metricName = "datasetsBySubjectToMonth";

        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            
            String jsonArrayString = metricsSvc.returnUnexpiredCacheMonthly(metricName, fakeDataLocale, sanitizedyyyymm);
            
            if (null == jsonArrayString) { //run query and save
                JsonArrayBuilder jsonArrayBuilder = MetricsUtil.datasetsBySubjectToJson(metricsSvc.datasetsBySubjectToMonth(fakeDataLocale, sanitizedyyyymm));
                jsonArrayString = jsonArrayBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, fakeDataLocale, sanitizedyyyymm, jsonArrayString));//, false);
            }

            return allowCors(ok(MetricsUtil.stringToJsonArrayBuilder(jsonArrayString)));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }
    
    /** Files */
    @GET
    @Path("files")
    public Response getFilesAllTime() {
        return getFilesToMonth(MetricsUtil.getCurrentMonth());
    }
    
    @Deprecated //for better path
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
            String jsonString = metricsSvc.returnUnexpiredCacheMonthly(metricName, fakeDataLocale, sanitizedyyyymm);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.filesToMonth(fakeDataLocale, sanitizedyyyymm);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, fakeDataLocale, sanitizedyyyymm, jsonString));//, true); //if not using cache save new
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }
    
    @GET
    @Path("files/pastDays/{days}")
    public Response getFilesPastDays(@PathParam("days") int days) {
        String metricName = "filesPastDays";
        
        if(days < 1) {
            return allowCors(error(BAD_REQUEST, "Invalid parameter for number of days."));
        }
        try {
            String jsonString = metricsSvc.returnUnexpiredCacheDayBased(metricName, fakeDataLocale, String.valueOf(days));

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.filesPastDays(fakeDataLocale, days);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, fakeDataLocale, String.valueOf(days), jsonString));//, true); //if not using cache save new
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
            String jsonString = metricsSvc.returnUnexpiredCacheMonthly(metricName, fakeDataLocale, sanitizedyyyymm);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.downloadsToMonth(fakeDataLocale, sanitizedyyyymm);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, fakeDataLocale, sanitizedyyyymm, jsonString));//, true); //if not using cache save new
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
            String jsonString = metricsSvc.returnUnexpiredCacheDayBased(metricName, fakeDataLocale, String.valueOf(days));

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.downloadsPastDays(fakeDataLocale, days);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, fakeDataLocale, String.valueOf(days), jsonString));//, true); //if not using cache save new
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));

        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

}
