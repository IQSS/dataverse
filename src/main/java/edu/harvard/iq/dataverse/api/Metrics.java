package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Metric;
import edu.harvard.iq.dataverse.api.AbstractApiBean.WrappedResponse;
import edu.harvard.iq.dataverse.makedatacount.DatasetMetrics;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountUtil;
import edu.harvard.iq.dataverse.metrics.MetricsUtil;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;

import java.util.Arrays;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import javax.ws.rs.core.UriInfo;

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
    private static final Logger logger = Logger.getLogger(Metrics.class.getName());

    /** Dataverses */
    
    @GET
    @Path("dataverses")
    public Response getDataversesAllTime(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        return getDataversesToMonth(uriInfo, MetricsUtil.getCurrentMonth(), parentAlias);
    }
    


    @Deprecated //for better path
    @GET
    @Path("dataverses/toMonth")
    public Response getDataversesToMonthCurrent(@Context UriInfo uriInfo) {
        return getDataversesToMonth(uriInfo, MetricsUtil.getCurrentMonth(), null);
    }
    
    @GET
    @Path("dataverses/toMonth/{yyyymm}")
    public Response getDataversesToMonth(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try { 
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{"parentAlias"});
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
            
        String metricName = "dataversesToMonth";

        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));

            if (null == jsonObj) { //run query and save
                Long count = metricsSvc.dataversesToMonth(sanitizedyyyymm, d);
                jsonObj = MetricsUtil.countToJson(count).build();
                metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonObj.toString()));
            }

            return ok(jsonObj);

        //TODO: Eventually the catch in each endpoint should be more specific
        //          and more general errors should be logged.
        } catch (Exception ex) {
            return error(BAD_REQUEST, ex.getLocalizedMessage());
        }
    }
    
    @GET
    @Path("dataverses/pastDays/{days}")
    public Response getDataversesPastDays(@Context UriInfo uriInfo, @PathParam("days") int days, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try { 
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{"parentAlias"});
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
            
        String metricName = "dataversesPastDays";
        
        if(days < 1) {
            return error(BAD_REQUEST, "Invalid parameter for number of days.");
        }
        try {
            JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), null, d));

            if (null == jsonObj) { //run query and save
                Long count = metricsSvc.dataversesPastDays(days, d);
                jsonObj = MetricsUtil.countToJson(count).build();
                metricsSvc.save(new Metric(metricName, String.valueOf(days), null, d, jsonObj.toString()));
            }

            return ok(jsonObj);

        } catch (Exception ex) {
            return error(BAD_REQUEST, ex.getLocalizedMessage());
        }
    }
    
    @GET
    @Path("dataverses/byCategory")
    public Response getDataversesByCategory(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try { 
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{"parentAlias"});
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
            
        String metricName = "dataversesByCategory";

        try {
            JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

            if (null == jsonArray) { //run query and save
                jsonArray = MetricsUtil.dataversesByCategoryToJson(metricsSvc.dataversesByCategory(d)).build();
                metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
            }

            return ok(jsonArray);
        } catch (Exception ex) {
            return error(BAD_REQUEST, ex.getLocalizedMessage());
        }
    }
    
    @GET
    @Path("dataverses/bySubject")
    public Response getDataversesBySubject(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try { 
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{"parentAlias"});
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
            
        String metricName = "dataversesBySubject";
        
        try {
            JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

            if (null == jsonArray) { //run query and save
                jsonArray = MetricsUtil.dataversesBySubjectToJson(metricsSvc.dataversesBySubject(d)).build();
                metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
            }

            return ok(jsonArray);
        } catch (Exception ex) {
            return error(BAD_REQUEST, ex.getLocalizedMessage());
        }
    }
    
    /** Datasets */
    
    @GET
    @Path("datasets")
    public Response getDatasetsAllTime(@Context UriInfo uriInfo, @QueryParam("dataLocation") String dataLocation, @QueryParam("parentAlias") String parentAlias) {

        return getDatasetsToMonth(uriInfo, MetricsUtil.getCurrentMonth(), dataLocation, parentAlias );
    }
    
    @Deprecated //for better path
    @GET
    @Path("datasets/toMonth")
    public Response getDatasetsToMonthCurrent(@Context UriInfo uriInfo, @QueryParam("dataLocation") String dataLocation) {
        return getDatasetsToMonth(uriInfo, MetricsUtil.getCurrentMonth(), dataLocation, null);
    }

    @GET
    @Path("datasets/toMonth/{yyyymm}")
    public Response getDatasetsToMonth(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm, @QueryParam("dataLocation") String dataLocation, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try { 
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{"dataLocation","parentAlias"});
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
            
        String metricName = "datasetsToMonth";

        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            String validDataLocation = MetricsUtil.validateDataLocationStringType(dataLocation);
            JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, validDataLocation, d));

            if (null == jsonObj) { //run query and save
                Long count = metricsSvc.datasetsToMonth(sanitizedyyyymm, validDataLocation, d);
                jsonObj = MetricsUtil.countToJson(count).build();
                metricsSvc.save(new Metric(metricName, sanitizedyyyymm, validDataLocation, d, jsonObj.toString()));
            }

            return ok(jsonObj);

        } catch (Exception ex) {
            return error(BAD_REQUEST, ex.getLocalizedMessage());
        }
    }
    
    @GET
    @Path("datasets/pastDays/{days}")
    public Response getDatasetsPastDays(@Context UriInfo uriInfo, @PathParam("days") int days, @QueryParam("dataLocation") String dataLocation, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try { 
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{"dataLocation","parentAlias"});
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
            
        String metricName = "datasetsPastDays";
        
        if(days < 1) {
            return error(BAD_REQUEST, "Invalid parameter for number of days.");
        }
        try {
            String validDataLocation = MetricsUtil.validateDataLocationStringType(dataLocation);
            JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), validDataLocation, d));

            if (null == jsonObj) { //run query and save
                Long count = metricsSvc.datasetsPastDays(days, validDataLocation, d);
                jsonObj = MetricsUtil.countToJson(count).build();
                metricsSvc.save(new Metric(metricName, String.valueOf(days), validDataLocation, d, jsonObj.toString()));
            }

            return ok(jsonObj);

        } catch (Exception ex) {
            return error(BAD_REQUEST, ex.getLocalizedMessage());
        }
    }
    
    @GET
    @Path("datasets/bySubject")
    public Response getDatasetsBySubject(@Context UriInfo uriInfo, @QueryParam("dataLocation") String dataLocation, @QueryParam("parentAlias") String parentAlias) {
        return getDatasetsBySubjectToMonth(uriInfo, MetricsUtil.getCurrentMonth(), dataLocation, parentAlias);
    }
  
    @GET
    @Path("datasets/bySubject/toMonth/{yyyymm}")
    public Response getDatasetsBySubjectToMonth(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm, @QueryParam("dataLocation") String dataLocation, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try { 
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{"dataLocation", "parentAlias"});
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
            
        String metricName = "datasetsBySubjectToMonth";

        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            String validDataLocation = MetricsUtil.validateDataLocationStringType(dataLocation);
            JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, validDataLocation, d));
            
            if (null == jsonArray) { //run query and save
                jsonArray = MetricsUtil.datasetsBySubjectToJson(metricsSvc.datasetsBySubjectToMonth(sanitizedyyyymm, validDataLocation, d)).build();
                metricsSvc.save(new Metric(metricName, sanitizedyyyymm, validDataLocation, d, jsonArray.toString()));
            }

            return ok(jsonArray);
        } catch (Exception ex) {
            return error(BAD_REQUEST, ex.getLocalizedMessage());
        }
    }
    
    /** Files */
    @GET
    @Path("files")
    public Response getFilesAllTime(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        return getFilesToMonth(uriInfo, MetricsUtil.getCurrentMonth(), parentAlias);
    }
    
    @Deprecated //for better path
    @GET
    @Path("files/toMonth")
    public Response getFilesToMonthCurrent(@Context UriInfo uriInfo) {
        return getFilesToMonth(uriInfo, MetricsUtil.getCurrentMonth(), null);
    }

    @GET
    @Path("files/toMonth/{yyyymm}")
    public Response getFilesToMonth(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try { 
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{"parentAlias"});
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
            
        String metricName = "filesToMonth";

        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            logger.fine("yyyymm: " + sanitizedyyyymm);
            JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));
            logger.fine("Returned");
            if (null == jsonObj) { //run query and save
                logger.fine("Getting filesToMonth : " + sanitizedyyyymm + " dvId=" +d.getId() );
                Long count = metricsSvc.filesToMonth(sanitizedyyyymm, d);
                logger.fine("count = " + count);
                jsonObj = MetricsUtil.countToJson(count).build();
                metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonObj.toString()));
            }

            return ok(jsonObj);
        } catch (Exception ex) {
            return error(BAD_REQUEST, ex.getLocalizedMessage());
        }
    }
    
    @GET
    @Path("files/pastDays/{days}")
    public Response getFilesPastDays(@Context UriInfo uriInfo, @PathParam("days") int days, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try { 
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{"parentAlias"});
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
            
        String metricName = "filesPastDays";
        
        if(days < 1) {
            return error(BAD_REQUEST, "Invalid parameter for number of days.");
        }
        try {
            JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), null, d));

            if (null == jsonObj) { //run query and save
                Long count = metricsSvc.filesPastDays(days, d);
                jsonObj = MetricsUtil.countToJson(count).build();
                metricsSvc.save(new Metric(metricName, String.valueOf(days), null, d, jsonObj.toString()));
            }

            return ok(jsonObj);

        } catch (Exception ex) {
            return error(BAD_REQUEST, ex.getLocalizedMessage());
        }
    }

    /** Downloads */
    
    @GET
    @Path("downloads")
    public Response getDownloadsAllTime(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        return getDownloadsToMonth(uriInfo, MetricsUtil.getCurrentMonth(), parentAlias);
    }
    
    @Deprecated //for better path
    @GET
    @Path("downloads/toMonth")
    public Response getDownloadsToMonthCurrent(@Context UriInfo uriInfo) {
        return getDownloadsToMonth(uriInfo, MetricsUtil.getCurrentMonth(), null);
    }

    @GET
    @Path("downloads/toMonth/{yyyymm}")
    public Response getDownloadsToMonth(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm, @QueryParam("parentAlias") String parentAlias) {                
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try { 
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{"parentAlias"});
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        
        String metricName = "downloadsToMonth";
        
        try {
            
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));

            if (null == jsonObj) { //run query and save
                Long count = metricsSvc.downloadsToMonth(sanitizedyyyymm, d);
                jsonObj = MetricsUtil.countToJson(count).build();
                metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonObj.toString()));
            }

            return ok(jsonObj);
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        } catch (Exception ex) {
            return error(BAD_REQUEST, ex.getLocalizedMessage());
        }
    }
    
    @GET
    @Path("downloads/pastDays/{days}")
    public Response getDownloadsPastDays(@Context UriInfo uriInfo, @PathParam("days") int days, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try { 
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{"parentAlias"});
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
                
        String metricName = "downloadsPastDays";
        
        if(days < 1) {
            return error(BAD_REQUEST, "Invalid parameter for number of days.");
        }
        try {
            JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), null, d));

            if (null == jsonObj) { //run query and save
                Long count = metricsSvc.downloadsPastDays(days, d);
                jsonObj = MetricsUtil.countToJson(count).build();
                metricsSvc.save(new Metric(metricName, String.valueOf(days), null, d, jsonObj.toString()));
            }

            return ok(jsonObj);

        } catch (Exception ex) {
            return error(BAD_REQUEST, ex.getLocalizedMessage());
        }
    }
    
    @GET
    @Path("/files/byType")
    public Response getFilesInDataverse(@Context UriInfo uriInfo, @QueryParam("parentAlias") String parentAlias) {
        return getFilesInDataverse(uriInfo,null, parentAlias);
    }        

    @GET
    @Path("/files/byType/toMonth/{yyyymm}")
    public Response getFilesInDataverse(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm, @QueryParam("parentAlias") String parentAlias) {

        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
                
        String metricName = "filesByType";
        try {
            String sanitizedyyyymm = null;
            if (yyyymm != null) {
                sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            }

            JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));

            if (null == jsonObj) { // run query and save
                jsonObj = metricsSvc.fileContents(d).build();
                metricsSvc.save(new Metric(metricName, null, null, d, jsonObj.toString()));
            }

            return ok(jsonObj);

        } catch (Exception ex) {
            return error(BAD_REQUEST, ex.getLocalizedMessage());
        }
    }
        
    @GET
    @Path("makeDataCount/{metric}")
    public Response getMakeDataCountMetricCurrentMonth(@PathParam("metric") String metricSupplied, @QueryParam("country") String country, @QueryParam("parentAlias") String parentAlias) {
        String nullCurrentMonth = null;
        return getMakeDataCountMetric(metricSupplied, nullCurrentMonth, country, parentAlias);
    }
    
    @GET
    @Path("makeDataCount/{metric}/toMonth/{yyyymm}")
    public Response getMakeDataCountMetric(@PathParam("metric") String metricSupplied, @PathParam("yyyymm") String yyyymm, @QueryParam("country") String country, @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        try {
            NullSafeJsonBuilder jsonObjectBuilder = jsonObjectBuilder();
            MakeDataCountUtil.MetricType metricType = null;
            try {
                metricType = MakeDataCountUtil.MetricType.fromString(metricSupplied);
            } catch (IllegalArgumentException ex) {
                return error(Response.Status.BAD_REQUEST, ex.getMessage());
            }
            String metricName = "MDC-" + metricType.toString();

            String sanitizedyyyymm = null;
            if (yyyymm != null) {
                sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            }
            
            JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));

            if (null == jsonObj) { //run query and save
                jsonObj =  metricsSvc.getDatasetMetricsByDatasetForDisplay(metricType, sanitizedyyyymm, country, d).build();
                metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonObj.toString()));
            }

            return ok(jsonObj);
        } catch (Exception ex) {
            return error(BAD_REQUEST, ex.getLocalizedMessage());
        }
    }

    

    private void errorIfUnrecongizedQueryParamPassed(UriInfo uriDetails, String[] allowedQueryParams) throws IllegalArgumentException {
        for(String theKey : uriDetails.getQueryParameters().keySet()) {
            if(!Arrays.stream(allowedQueryParams).anyMatch(theKey::equals)) {
                throw new IllegalArgumentException("queryParameter " + theKey + " not supported for this endpont");
            }
        }
        
    }
    
    //Throws a WebApplicationException if alias is not null and Dataverse can't be found
    private Dataverse findDataverseOrDieIfNotFound(String alias) {
        Dataverse d = null;
        if (alias != null) {
            d = dataverseSvc.findByAlias(alias);
            if (d == null) {
                throw new NotFoundException("No Dataverse with alias: " + alias);
            }
        }
        return d;
    }
    
}
