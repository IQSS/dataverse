package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Metric;
import edu.harvard.iq.dataverse.metrics.MetricsUtil;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * API endpoints for various metrics.
 * <p>
 * These endpoints look a bit heavy because they check for a timely cached value
 * to use before responding. The caching code resides here because the this is
 * the point at which JSON is generated and this JSON was deemed the easiest to
 * cache.
 *
 * @author pdurbin, madunlap
 */
@Path("info/metrics")
public class Metrics extends AbstractApiBean {

    /**
     * Dataverses
     */

    @GET
    @Path("dataverses")
    public Response getDataversesAllTime(@Context UriInfo uriInfo) {
        return getDataversesToMonth(uriInfo, MetricsUtil.getCurrentMonth());
    }

    @Deprecated //for better path
    @GET
    @Path("dataverses/toMonth")
    public Response getDataversesToMonthCurrent(@Context UriInfo uriInfo) {
        return getDataversesToMonth(uriInfo, MetricsUtil.getCurrentMonth());
    }

    @GET
    @Path("dataverses/toMonth/{yyyymm}")
    public Response getDataversesToMonth(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm) {
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{""});
        } catch (IllegalArgumentException ia) {
            return allowCors(error(BAD_REQUEST, ia.getLocalizedMessage()));
        }

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
    public Response getDataversesPastDays(@Context UriInfo uriInfo, @PathParam("days") int days) {
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{""});
        } catch (IllegalArgumentException ia) {
            return allowCors(error(BAD_REQUEST, ia.getLocalizedMessage()));
        }

        String metricName = "dataversesPastDays";

        if (days < 1) {
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
    public Response getDataversesByCategory(@Context UriInfo uriInfo) {
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{""});
        } catch (IllegalArgumentException ia) {
            return allowCors(error(BAD_REQUEST, ia.getLocalizedMessage()));
        }

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
    public Response getDataversesBySubject(@Context UriInfo uriInfo) {
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{""});
        } catch (IllegalArgumentException ia) {
            return allowCors(error(BAD_REQUEST, ia.getLocalizedMessage()));
        }

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

    /**
     * Datasets
     */

    @GET
    @Path("datasets")
    public Response getDatasetsAllTime(@Context UriInfo uriInfo, @QueryParam("dataLocation") String dataLocation) {
        return getDatasetsToMonth(uriInfo, MetricsUtil.getCurrentMonth(), dataLocation);
    }

    @Deprecated //for better path
    @GET
    @Path("datasets/toMonth")
    public Response getDatasetsToMonthCurrent(@Context UriInfo uriInfo, @QueryParam("dataLocation") String dataLocation) {
        return getDatasetsToMonth(uriInfo, MetricsUtil.getCurrentMonth(), dataLocation);
    }

    @GET
    @Path("datasets/toMonth/{yyyymm}")
    public Response getDatasetsToMonth(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm, @QueryParam("dataLocation") String dataLocation) {
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{"dataLocation"});
        } catch (IllegalArgumentException ia) {
            return allowCors(error(BAD_REQUEST, ia.getLocalizedMessage()));
        }

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
    public Response getDatasetsPastDays(@Context UriInfo uriInfo, @PathParam("days") int days, @QueryParam("dataLocation") String dataLocation) {
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{"dataLocation"});
        } catch (IllegalArgumentException ia) {
            return allowCors(error(BAD_REQUEST, ia.getLocalizedMessage()));
        }

        String metricName = "datasetsPastDays";

        if (days < 1) {
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
    public Response getDatasetsBySubject(@Context UriInfo uriInfo, @QueryParam("dataLocation") String dataLocation) {
        return getDatasetsBySubjectToMonth(uriInfo, MetricsUtil.getCurrentMonth(), dataLocation);
    }

    @GET
    @Path("datasets/bySubject/toMonth/{yyyymm}")
    public Response getDatasetsBySubjectToMonth(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm, @QueryParam("dataLocation") String dataLocation) {
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{"dataLocation"});
        } catch (IllegalArgumentException ia) {
            return allowCors(error(BAD_REQUEST, ia.getLocalizedMessage()));
        }

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

    /**
     * Files
     */
    @GET
    @Path("files")
    public Response getFilesAllTime(@Context UriInfo uriInfo) {
        return getFilesToMonth(uriInfo, MetricsUtil.getCurrentMonth());
    }

    @Deprecated //for better path
    @GET
    @Path("files/toMonth")
    public Response getFilesToMonthCurrent(@Context UriInfo uriInfo) {
        return getFilesToMonth(uriInfo, MetricsUtil.getCurrentMonth());
    }

    @GET
    @Path("files/toMonth/{yyyymm}")
    public Response getFilesToMonth(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm) {
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{""});
        } catch (IllegalArgumentException ia) {
            return allowCors(error(BAD_REQUEST, ia.getLocalizedMessage()));
        }

        String metricName = "filesToMonth";

        try {
            String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
            String jsonString = metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.filesToMonth(sanitizedyyyymm);
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
    @Path("files/pastDays/{days}")
    public Response getFilesPastDays(@Context UriInfo uriInfo, @PathParam("days") int days) {
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{""});
        } catch (IllegalArgumentException ia) {
            return allowCors(error(BAD_REQUEST, ia.getLocalizedMessage()));
        }

        String metricName = "filesPastDays";

        if (days < 1) {
            return allowCors(error(BAD_REQUEST, "Invalid parameter for number of days."));
        }
        try {
            String jsonString = metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), null);

            if (null == jsonString) { //run query and save
                Long count = metricsSvc.filesPastDays(days);
                JsonObjectBuilder jsonObjBuilder = MetricsUtil.countToJson(count);
                jsonString = jsonObjBuilder.build().toString();
                metricsSvc.save(new Metric(metricName, String.valueOf(days), null, jsonString));
            }

            return allowCors(ok(MetricsUtil.stringToJsonObjectBuilder(jsonString)));

        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

    /**
     * Downloads
     */

    @GET
    @Path("downloads")
    public Response getDownloadsAllTime(@Context UriInfo uriInfo) {
        return getDownloadsToMonth(uriInfo, MetricsUtil.getCurrentMonth());
    }

    @Deprecated //for better path
    @GET
    @Path("downloads/toMonth")
    public Response getDownloadsToMonthCurrent(@Context UriInfo uriInfo) {
        return getDownloadsToMonth(uriInfo, MetricsUtil.getCurrentMonth());
    }

    @GET
    @Path("downloads/toMonth/{yyyymm}")
    public Response getDownloadsToMonth(@Context UriInfo uriInfo, @PathParam("yyyymm") String yyyymm) {
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{""});
        } catch (IllegalArgumentException ia) {
            return allowCors(error(BAD_REQUEST, ia.getLocalizedMessage()));
        }

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
        } catch (IllegalArgumentException ia) {
            return allowCors(error(BAD_REQUEST, ia.getLocalizedMessage()));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

    @GET
    @Path("downloads/pastDays/{days}")
    public Response getDownloadsPastDays(@Context UriInfo uriInfo, @PathParam("days") int days) {
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[]{""});
        } catch (IllegalArgumentException ia) {
            return allowCors(error(BAD_REQUEST, ia.getLocalizedMessage()));
        }

        String metricName = "downloadsPastDays";

        if (days < 1) {
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

    private void errorIfUnrecongizedQueryParamPassed(UriInfo uriDetails, String[] allowedQueryParams) throws IllegalArgumentException {
        for (String theKey : uriDetails.getQueryParameters().keySet()) {
            if (!Arrays.stream(allowedQueryParams).anyMatch(theKey::equals)) {
                throw new IllegalArgumentException("queryParameter " + theKey + " not supported for this endpont");
            }
        }

    }

}
