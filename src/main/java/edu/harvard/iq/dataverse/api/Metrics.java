package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Metric;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountUtil;
import edu.harvard.iq.dataverse.metrics.MetricsUtil;
import edu.harvard.iq.dataverse.util.FileUtil;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Variant;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

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
@Tag(name = "Info", description = "General information about the Dataverse installation.")
public class Metrics extends AbstractApiBean {
    private static final Logger logger = Logger.getLogger(Metrics.class.getName());

    /** Dataverses */

    @GET
    @Path("dataverses")
    @Operation(summary = "Calculates total dataverse count",
            description = "Calculates the number of released dataverses through the current month, optionally scoped to a released parent dataverse.")
    public Response getDataversesAllTime(@Context UriInfo uriInfo,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        return getDataversesToMonth(uriInfo, MetricsUtil.getCurrentMonth(), parentAlias);
    }

    @GET
    @Path("dataverses/monthly")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates monthly dataverse counts",
            description = "Calculates a monthly time series of released dataverse counts as JSON or CSV, optionally scoped to a released parent dataverse.")
    public Response getDataversesTimeSeries(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        String metricName = "dataverses";
        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save

            jsonArray = metricsSvc.getDataversesTimeSeries(uriInfo, d);
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }
        MediaType requestedType = getVariant(req, MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE);
        if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonArrayOfObjectsToCSV(jsonArray, MetricsUtil.DATE, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "dataverses.timeseries.csv");
    }

    @GET
    @Path("dataverses/toMonth/{yyyymm}")
    @Operation(summary = "Calculates dataverse count through a month",
            description = "Calculates the number of released dataverses through the specified month, optionally scoped to a released parent dataverse.")
    public Response getDataversesToMonth(@Context UriInfo uriInfo,
            @Parameter(description = "Year and month cutoff for the metric, formatted as YYYYMM.", required = true)
            @PathParam("yyyymm") String yyyymm,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "dataversesToMonth";

        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));

        if (null == jsonObj) { // run query and save
            Long count = metricsSvc.dataversesToMonth(sanitizedyyyymm, d);
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonObj.toString()));
        }

        return ok(jsonObj);

    }

    @GET
    @Path("dataverses/pastDays/{days}")
    @Operation(summary = "Calculates recent dataverse count",
            description = "Calculates the number of released dataverses created during the specified number of past days, optionally scoped to a released parent dataverse.")
    public Response getDataversesPastDays(@Context UriInfo uriInfo,
            @Parameter(description = "Positive number of past days included in the metric.", required = true)
            @PathParam("days") int days,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "dataversesPastDays";

        if (days < 1) {
            return error(BAD_REQUEST, "Invalid parameter for number of days.");
        }
        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), null, d));

        if (null == jsonObj) { // run query and save
            Long count = metricsSvc.dataversesPastDays(days, d);
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, String.valueOf(days), null, d, jsonObj.toString()));
        }

        return ok(jsonObj);

    }

    @GET
    @Path("dataverses/byCategory")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates dataverse counts by category",
            description = "Calculates released dataverse counts grouped by dataverse category as JSON or CSV, optionally scoped to a released parent dataverse.")
    public Response getDataversesByCategory(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "dataversesByCategory";

        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save
            jsonArray = MetricsUtil.dataversesByCategoryToJson(metricsSvc.dataversesByCategory(d)).build();
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }
        MediaType requestedType = getVariant(req, MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE);
        if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonArrayOfObjectsToCSV(jsonArray, MetricsUtil.CATEGORY, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "dataverses.byCategory.csv");
    }

    @GET
    @Path("dataverses/bySubject")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates dataverse counts by subject",
            description = "Calculates released dataverse counts grouped by subject as JSON or CSV, optionally scoped to a released parent dataverse.")
    public Response getDataversesBySubject(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "dataversesBySubject";

        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save
            jsonArray = MetricsUtil.dataversesBySubjectToJson(metricsSvc.dataversesBySubject(d)).build();
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }

        MediaType requestedType = getVariant(req, MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE);
        if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonArrayOfObjectsToCSV(jsonArray, MetricsUtil.SUBJECT, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "dataverses.bySubject.csv");
    }

    /** Datasets */

    @GET
    @Path("datasets")
    @Operation(summary = "Calculates total dataset count",
            description = "Calculates the number of released datasets through the current month, optionally filtered by storage location and parent dataverse.")
    public Response getDatasetsAllTime(@Context UriInfo uriInfo,
            @Parameter(description = "Storage location filter for the dataset metric.")
            @QueryParam("dataLocation") String dataLocation,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        return getDatasetsToMonth(uriInfo, MetricsUtil.getCurrentMonth(), dataLocation, parentAlias);
    }

    @GET
    @Path("datasets/monthly")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates monthly dataset counts",
            description = "Calculates a monthly time series of released dataset counts as JSON or CSV, optionally filtered by storage location and parent dataverse.")
    public Response getDatasetsTimeSeriest(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Storage location filter for the dataset metric.")
            @QueryParam("dataLocation") String dataLocation,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {

        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "dataLocation", "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        String metricName = "datasets";
        String validDataLocation = MetricsUtil.validateDataLocationStringType(dataLocation);
        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, validDataLocation, d));

        if (null == jsonArray) { // run query and save

            jsonArray = metricsSvc.getDatasetsTimeSeries(uriInfo, validDataLocation, d);
            metricsSvc.save(new Metric(metricName, null, validDataLocation, d, jsonArray.toString()));
        }
        MediaType requestedType = getVariant(req, MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE);
        if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonArrayOfObjectsToCSV(jsonArray, MetricsUtil.DATE, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "datasets.timeseries.csv");
    }

    @GET
    @Path("datasets/toMonth/{yyyymm}")
    @Operation(summary = "Calculates dataset count through a month",
            description = "Calculates the number of released datasets through the specified month, optionally filtered by storage location and parent dataverse.")
    public Response getDatasetsToMonth(@Context UriInfo uriInfo,
            @Parameter(description = "Year and month cutoff for the metric, formatted as YYYYMM.", required = true)
            @PathParam("yyyymm") String yyyymm,
            @Parameter(description = "Storage location filter for the dataset metric.")
            @QueryParam("dataLocation") String dataLocation,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "dataLocation", "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "datasetsToMonth";

        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        String validDataLocation = MetricsUtil.validateDataLocationStringType(dataLocation);
        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, validDataLocation, d));

        if (null == jsonObj) { // run query and save
            Long count = metricsSvc.datasetsToMonth(sanitizedyyyymm, validDataLocation, d);
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, validDataLocation, d, jsonObj.toString()));
        }

        return ok(jsonObj);

    }

    @GET
    @Path("datasets/pastDays/{days}")
    @Operation(summary = "Calculates recent dataset count",
            description = "Calculates the number of released datasets created during the specified number of past days, optionally filtered by storage location and parent dataverse.")
    public Response getDatasetsPastDays(@Context UriInfo uriInfo,
            @Parameter(description = "Positive number of past days included in the metric.", required = true)
            @PathParam("days") int days,
            @Parameter(description = "Storage location filter for the dataset metric.")
            @QueryParam("dataLocation") String dataLocation,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "dataLocation", "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "datasetsPastDays";

        if (days < 1) {
            return error(BAD_REQUEST, "Invalid parameter for number of days.");
        }
        String validDataLocation = MetricsUtil.validateDataLocationStringType(dataLocation);
        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), validDataLocation, d));

        if (null == jsonObj) { // run query and save
            Long count = metricsSvc.datasetsPastDays(days, validDataLocation, d);
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, String.valueOf(days), validDataLocation, d, jsonObj.toString()));
        }

        return ok(jsonObj);

    }

    @GET
    @Path("datasets/bySubject")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates dataset counts by subject",
            description = "Calculates released dataset counts grouped by subject through the current month as JSON or CSV, optionally filtered by storage location and parent dataverse.")
    public Response getDatasetsBySubject(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Storage location filter for the dataset metric.")
            @QueryParam("dataLocation") String dataLocation,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        return getDatasetsBySubjectToMonth(req, uriInfo, MetricsUtil.getCurrentMonth(), dataLocation, parentAlias);
    }

    @GET
    @Path("datasets/bySubject/toMonth/{yyyymm}")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates dataset counts by subject through a month",
            description = "Calculates released dataset counts grouped by subject through the specified month as JSON or CSV, optionally filtered by storage location and parent dataverse.")
    public Response getDatasetsBySubjectToMonth(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Year and month cutoff for the metric, formatted as YYYYMM.", required = true)
            @PathParam("yyyymm") String yyyymm,
            @Parameter(description = "Storage location filter for the dataset metric.")
            @QueryParam("dataLocation") String dataLocation,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "dataLocation", "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "datasetsBySubjectToMonth";

        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        String validDataLocation = MetricsUtil.validateDataLocationStringType(dataLocation);
        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, validDataLocation, d));

        if (null == jsonArray) { // run query and save
            jsonArray = MetricsUtil.datasetsBySubjectToJson(metricsSvc.datasetsBySubjectToMonth(sanitizedyyyymm, validDataLocation, d)).build();
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, validDataLocation, d, jsonArray.toString()));
        }
        MediaType requestedType = getVariant(req, MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE);
        if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonArrayOfObjectsToCSV(jsonArray, MetricsUtil.SUBJECT, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "datasets.bySubject.csv");
    }

    /** Files */
    @GET
    @Path("files")
    @Operation(summary = "Calculates total file count",
            description = "Calculates the number of released files through the current month, optionally scoped to a released parent dataverse.")
    public Response getFilesAllTime(@Context UriInfo uriInfo,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        return getFilesToMonth(uriInfo, MetricsUtil.getCurrentMonth(), parentAlias);
    }

    @GET
    @Path("files/monthly")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates monthly file counts",
            description = "Calculates a monthly time series of released file counts as JSON or CSV, optionally scoped to a released parent dataverse.")
    public Response getFilesTimeSeries(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        String metricName = "files";

        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save

            jsonArray = metricsSvc.filesTimeSeries(d);
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }
        MediaType requestedType = getVariant(req, MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE);
        if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonArrayOfObjectsToCSV(jsonArray, MetricsUtil.DATE, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "datafiles.timeseries.csv");
    }

    @GET
    @Path("files/toMonth/{yyyymm}")
    @Operation(summary = "Calculates file count through a month",
            description = "Calculates the number of released files through the specified month, optionally scoped to a released parent dataverse.")
    public Response getFilesToMonth(@Context UriInfo uriInfo,
            @Parameter(description = "Year and month cutoff for the metric, formatted as YYYYMM.", required = true)
            @PathParam("yyyymm") String yyyymm,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "filesToMonth";

        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        logger.fine("yyyymm: " + sanitizedyyyymm);
        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));
        logger.fine("Returned");
        if (null == jsonObj) { // run query and save
            logger.fine("Getting filesToMonth : " + sanitizedyyyymm + " dvId=" + ((d==null) ? "not sent" : d.getId()));
            Long count = metricsSvc.filesToMonth(sanitizedyyyymm, d);
            logger.fine("count = " + count);
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonObj.toString()));
        }

        return ok(jsonObj);
    }

    @GET
    @Path("files/pastDays/{days}")
    @Operation(summary = "Calculates recent file count",
            description = "Calculates the number of released files created during the specified number of past days, optionally scoped to a released parent dataverse.")
    public Response getFilesPastDays(@Context UriInfo uriInfo,
            @Parameter(description = "Positive number of past days included in the metric.", required = true)
            @PathParam("days") int days,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "filesPastDays";

        if (days < 1) {
            return error(BAD_REQUEST, "Invalid parameter for number of days.");
        }

        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), null, d));

        if (null == jsonObj) { // run query and save
            Long count = metricsSvc.filesPastDays(days, d);
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, String.valueOf(days), null, d, jsonObj.toString()));
        }

        return ok(jsonObj);

    }

    @GET
    @Path("/files/byType/monthly")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates monthly file counts by type",
            description = "Calculates a monthly time series of released file counts and sizes grouped by content type as JSON or CSV, optionally scoped to a released parent dataverse.")
    public Response getFilesByTypeTimeSeries(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        String metricName = "filesByTypeMonthly";

        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save
            // Only handling published right now
            jsonArray = metricsSvc.filesByTypeTimeSeries(d, true);
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }
        MediaType requestedType = getVariant(req, MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE);
        if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonArrayOfObjectsToCSV(jsonArray, MetricsUtil.DATE, MetricsUtil.CONTENTTYPE, MetricsUtil.COUNT, MetricsUtil.SIZE), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "datafiles.byType.timeseries.csv");
    }

    @GET
    @Path("/files/byType")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates file counts by type",
            description = "Calculates released file counts and sizes grouped by content type as JSON or CSV, optionally scoped to a released parent dataverse.")
    public Response getFilesByType(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "filesByType";

        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save
            jsonArray = metricsSvc.filesByType(d);
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }

        MediaType requestedType = getVariant(req, MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE);
        if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonArrayOfObjectsToCSV(jsonArray, MetricsUtil.CONTENTTYPE, MetricsUtil.COUNT, MetricsUtil.SIZE), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "datafiles.byType.csv");
    }

    /** Downloads */

    @GET
    @Path("downloads")
    @Operation(summary = "Calculates total download count",
            description = "Calculates the number of file downloads through the current month, optionally scoped to a released parent dataverse.")
    public Response getDownloadsAllTime(@Context UriInfo uriInfo,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        return getDownloadsToMonth(uriInfo, MetricsUtil.getCurrentMonth(), parentAlias);
    }

    @GET
    @Path("downloads/monthly")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates monthly download counts",
            description = "Calculates a monthly time series of file download counts as JSON or CSV, optionally scoped to a released parent dataverse.")
    public Response getDownloadsTimeSeries(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        String metricName = "downloads";

        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save
            // Only handling published right now
            jsonArray = metricsSvc.downloadsTimeSeries(d);
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }

        MediaType requestedType = getVariant(req, MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE);
        if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonArrayOfObjectsToCSV(jsonArray, MetricsUtil.DATE, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "downloads.timeseries.csv");
    }

    @GET
    @Path("downloads/toMonth/{yyyymm}")
    @Operation(summary = "Calculates download count through a month",
            description = "Calculates the number of file downloads through the specified month, optionally scoped to a released parent dataverse.")
    public Response getDownloadsToMonth(@Context UriInfo uriInfo,
            @Parameter(description = "Year and month cutoff for the metric, formatted as YYYYMM.", required = true)
            @PathParam("yyyymm") String yyyymm,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "downloadsToMonth";

        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));

        if (null == jsonObj) { // run query and save
            Long count;
            try {
                count = metricsSvc.downloadsToMonth(sanitizedyyyymm, d);
            } catch (ParseException e) {
                return error(BAD_REQUEST, "Unable to parse supplied date: " + e.getLocalizedMessage());
            }
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonObj.toString()));
        }

        return ok(jsonObj);
    }

    @GET
    @Path("downloads/pastDays/{days}")
    @Operation(summary = "Calculates recent download count",
            description = "Calculates the number of file downloads during the specified number of past days, optionally scoped to a released parent dataverse.")
    public Response getDownloadsPastDays(@Context UriInfo uriInfo,
            @Parameter(description = "Positive number of past days included in the metric.", required = true)
            @PathParam("days") int days,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "downloadsPastDays";

        if (days < 1) {
            return error(BAD_REQUEST, "Invalid parameter for number of days.");
        }
        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), null, d));

        if (null == jsonObj) { // run query and save
            Long count = metricsSvc.downloadsPastDays(days, d);
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, String.valueOf(days), null, d, jsonObj.toString()));
        }

        return ok(jsonObj);
    }

    /** Accounts */

    @GET
    @Path("accounts")
    @Operation(summary = "Calculates total account count",
            description = "Calculates the number of user accounts through the current month.")
    public Response getAccountsAllTime(@Context UriInfo uriInfo) {
        return getAccountsToMonth(uriInfo, MetricsUtil.getCurrentMonth());
    }

    @GET
    @Path("accounts/toMonth/{yyyymm}")
    @Operation(summary = "Calculates account count through a month",
            description = "Calculates the number of user accounts through the specified month.")
    public Response getAccountsToMonth(@Context UriInfo uriInfo,
            @Parameter(description = "Year and month cutoff for the metric, formatted as YYYYMM.", required = true)
            @PathParam("yyyymm") String yyyymm) {

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "accountsToMonth";
        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, null));

        if (null == jsonObj) { // run query and save
            Long count;
            try {
                count = metricsSvc.accountsToMonth(sanitizedyyyymm);
            } catch (ParseException e) {
                return error(BAD_REQUEST, "Unable to parse supplied date: " + e.getLocalizedMessage());
            }
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, null, jsonObj.toString()));
        }

        return ok(jsonObj);
    }

    @GET
    @Path("accounts/pastDays/{days}")
    @Operation(summary = "Calculates recent account count",
            description = "Calculates the number of user accounts created during the specified number of past days.")
    public Response getAccountsPastDays(@Context UriInfo uriInfo,
            @Parameter(description = "Positive number of past days included in the metric.", required = true)
            @PathParam("days") int days) {

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "accountsPastDays";

        if (days < 1) {
            return error(BAD_REQUEST, "Invalid parameter for number of days.");
        }

        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheDayBased(metricName, String.valueOf(days), null, null));

        if (null == jsonObj) { // run query and save
            Long count = metricsSvc.accountsPastDays(days);
            jsonObj = MetricsUtil.countToJson(count).build();
            metricsSvc.save(new Metric(metricName, String.valueOf(days), null, null, jsonObj.toString()));
        }

        return ok(jsonObj);
    }

    @GET
    @Path("accounts/monthly")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates monthly account counts",
            description = "Calculates a monthly time series of user account counts as JSON or CSV.")
    public Response getAccountsTimeSeries(@Context Request req, @Context UriInfo uriInfo) {

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "accounts";
        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, null));

        if (null == jsonArray) { // run query and save
            // Only handling published right now
            jsonArray = metricsSvc.accountsTimeSeries();
            metricsSvc.save(new Metric(metricName, null, null, null, jsonArray.toString()));
        }

        MediaType requestedType = getVariant(req, MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE);
        if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonArrayOfObjectsToCSV(jsonArray, MetricsUtil.DATE, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "accounts.timeseries.csv");
    }

    /** MakeDataCount */

    @GET
    @Path("makeDataCount/{metric}")
    @Operation(summary = "Calculates a Make Data Count metric",
            description = "Calculates the requested Make Data Count metric through the current month, optionally filtered by country and parent dataverse.")
    public Response getMakeDataCountMetricCurrentMonth(@Context UriInfo uriInfo,
            @Parameter(description = "Make Data Count metric name to return.", required = true)
            @PathParam("metric") String metricSupplied,
            @Parameter(description = "ISO country code used to filter the metric.")
            @QueryParam("country") String country,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        return getMakeDataCountMetricToMonth(uriInfo, metricSupplied, MetricsUtil.getCurrentMonth(), country, parentAlias);
    }

    @GET
    @Path("makeDataCount/{metric}/monthly")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates monthly Make Data Count metrics",
            description = "Calculates a monthly time series for the requested Make Data Count metric as JSON or CSV, optionally filtered by country and parent dataverse.")
    public Response getMakeDataCountMetricTimeSeries(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Make Data Count metric name to return.", required = true)
            @PathParam("metric") String metricSupplied,
            @Parameter(description = "ISO country code used to filter the metric.")
            @QueryParam("country") String country,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        MakeDataCountUtil.MetricType metricType = null;
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias", "country" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        try {
            metricType = MakeDataCountUtil.MetricType.fromString(metricSupplied);
        } catch (IllegalArgumentException ex) {
            return error(Response.Status.BAD_REQUEST, ex.getMessage());
        }
        if (country != null) {
            country = country.toLowerCase();

            if (!MakeDataCountUtil.isValidCountryCode(country)) {
                return error(Response.Status.BAD_REQUEST, "Country must be one of the ISO 1366 Country Codes");
            }
        }
        String metricName = "MDC-" + metricType.toString() + ((country == null) ? "" : "-" + country);

        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save
            // Only handling published right now
            jsonArray = metricsSvc.mdcMetricTimeSeries(metricType, country, d);
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }
        MediaType requestedType = getVariant(req, MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE);
        if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonArrayOfObjectsToCSV(jsonArray, MetricsUtil.DATE, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "makeDataCount." + metricType.toString() + ".timeseries.csv");
    }

    @GET
    @Path("makeDataCount/{metric}/toMonth/{yyyymm}")
    @Operation(summary = "Calculates a Make Data Count metric through a month",
            description = "Calculates the requested Make Data Count metric through the specified month, optionally filtered by country and parent dataverse.")
    public Response getMakeDataCountMetricToMonth(@Context UriInfo uriInfo,
            @Parameter(description = "Make Data Count metric name to return.", required = true)
            @PathParam("metric") String metricSupplied,
            @Parameter(description = "Year and month cutoff for the metric, formatted as YYYYMM.", required = true)
            @PathParam("yyyymm") String yyyymm,
            @Parameter(description = "ISO country code used to filter the metric.")
            @QueryParam("country") String country,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        MakeDataCountUtil.MetricType metricType = null;
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias", "country" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        try {
            metricType = MakeDataCountUtil.MetricType.fromString(metricSupplied);
        } catch (IllegalArgumentException ex) {
            return error(Response.Status.BAD_REQUEST, ex.getMessage());
        }
        if (country != null) {
            country = country.toLowerCase();

            if (!MakeDataCountUtil.isValidCountryCode(country)) {
                return error(Response.Status.BAD_REQUEST, "Country must be one of the ISO 1366 Country Codes");
            }
        }
        String metricName = "MDC-" + metricType.toString() + ((country == null) ? "" : "-" + country);

        String sanitizedyyyymm = null;
        if (yyyymm != null) {
            sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        }

        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));

        if (null == jsonObj) { // run query and save
            jsonObj = metricsSvc.getMDCDatasetMetrics(metricType, sanitizedyyyymm, country, d);
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonObj.toString()));
        }

        return ok(jsonObj);
    }
    
    @GET
    @Path("filedownloads")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates file download counts",
            description = "Calculates file download counts by file through the current month as JSON or CSV, optionally scoped to a released parent dataverse.")
    public Response getFileDownloadsAllTime(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        return getFileDownloadsToMonth(req, uriInfo, MetricsUtil.getCurrentMonth(), parentAlias);
    }
    
    @GET
    @Path("filedownloads/toMonth/{yyyymm}")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates file download counts through a month",
            description = "Calculates file download counts by file through the specified month as JSON or CSV, optionally scoped to a released parent dataverse.")
    public Response getFileDownloadsToMonth(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Year and month cutoff for the metric, formatted as YYYYMM.", required = true)
            @PathParam("yyyymm") String yyyymm,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "fileDownloads";

        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        JsonArray jsonArr = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));

        if (null == jsonArr) { // run query and save
            jsonArr = metricsSvc.fileDownloads(sanitizedyyyymm, d, false);
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonArr.toString()));
        }
        MediaType requestedType = getVariant(req, MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE);
        if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
            return ok(jsonArr);
        }
        return ok(FileUtil.jsonArrayOfObjectsToCSV(jsonArr, MetricsUtil.ID, MetricsUtil.PID, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "filedownloads.csv");
    }
    
    @GET
    @Path("filedownloads/monthly")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates monthly file download counts",
            description = "Calculates a monthly time series of file download counts by file as JSON or CSV, optionally scoped to a released parent dataverse.")
    public Response getFileDownloadsTimeSeries(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        String metricName = "fileDownloads";

        JsonArray jsonArr = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArr) { // run query and save
            // Only handling published right now
            jsonArr = metricsSvc.fileDownloadsTimeSeries(d, false);
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArr.toString()));
        }
        MediaType requestedType = getVariant(req, MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE);
        if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
            return ok(jsonArr);
        }
        return ok(FileUtil.jsonArrayOfObjectsToCSV(jsonArr, MetricsUtil.DATE, MetricsUtil.ID, MetricsUtil.PID, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "filedownloads.timeseries.csv");
    }

    @GET
    @Path("uniquedownloads")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates unique dataset download counts",
            description = "Calculates unique dataset download counts through the current month as JSON or CSV, optionally scoped to a released parent dataverse.")
    public Response getUniqueDownloadsAllTime(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        return getUniqueDownloadsToMonth(req, uriInfo, MetricsUtil.getCurrentMonth(), parentAlias);
    }

    @GET
    @Path("uniquedownloads/monthly")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates monthly unique dataset download counts",
            description = "Calculates a monthly time series of unique dataset download counts as JSON or CSV, optionally scoped to a released parent dataverse.")
    public Response getUniqueDownloadsTimeSeries(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        String metricName = "uniqueDownloads";

        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save
            // Only handling published right now
            jsonArray = metricsSvc.uniqueDownloadsTimeSeries(d);
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }
        MediaType requestedType = getVariant(req, MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE);
        if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonArrayOfObjectsToCSV(jsonArray, MetricsUtil.DATE, MetricsUtil.PID, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "uniquedownloads.timeseries.csv");
    }

    @GET
    @Path("uniquedownloads/toMonth/{yyyymm}")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates unique dataset download counts through a month",
            description = "Calculates unique dataset download counts through the specified month as JSON or CSV, optionally scoped to a released parent dataverse.")
    public Response getUniqueDownloadsToMonth(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Year and month cutoff for the metric, formatted as YYYYMM.", required = true)
            @PathParam("yyyymm") String yyyymm,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "uniqueDownloads";

        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));

        if (null == jsonArray) { // run query and save
            jsonArray = metricsSvc.uniqueDatasetDownloads(sanitizedyyyymm, d);
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonArray.toString()));
        }
        MediaType requestedType = getVariant(req, MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE);
        if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonArrayOfObjectsToCSV(jsonArray, MetricsUtil.PID, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "uniquedownloads.csv");
    }

    @GET
    @Path("uniquefiledownloads")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates unique file download counts",
            description = "Calculates unique file download counts by file through the current month as JSON or CSV, optionally scoped to a released parent dataverse.")
    public Response getUniqueFileDownloadsAllTime(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        return getUniqueFileDownloadsToMonth(req, uriInfo, MetricsUtil.getCurrentMonth(), parentAlias);
    }
    
    @GET
    @Path("uniquefiledownloads/toMonth/{yyyymm}")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates unique file download counts through a month",
            description = "Calculates unique file download counts by file through the specified month as JSON or CSV, optionally scoped to a released parent dataverse.")
    public Response getUniqueFileDownloadsToMonth(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Year and month cutoff for the metric, formatted as YYYYMM.", required = true)
            @PathParam("yyyymm") String yyyymm,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "uniquefileDownloads";

        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);
        JsonArray jsonArr = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));

        if (null == jsonArr) { // run query and save
            jsonArr = metricsSvc.fileDownloads(sanitizedyyyymm, d, true);
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonArr.toString()));
        }
        MediaType requestedType = getVariant(req, MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE);
        if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
            return ok(jsonArr);
        }
        return ok(FileUtil.jsonArrayOfObjectsToCSV(jsonArr, MetricsUtil.ID, MetricsUtil.PID, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "uniquefiledownloads.csv");
    }
    
    @GET
    @Path("uniquefiledownloads/monthly")
    @Produces("text/csv, application/json")
    @Operation(summary = "Calculates monthly unique file download counts",
            description = "Calculates a monthly time series of unique file download counts by file as JSON or CSV, optionally scoped to a released parent dataverse.")
    public Response getUniqueFileDownloadsTimeSeries(@Context Request req, @Context UriInfo uriInfo,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);
        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }
        String metricName = "uniquefileDownloads";

        JsonArray jsonArray = MetricsUtil.stringToJsonArray(metricsSvc.returnUnexpiredCacheAllTime(metricName, null, d));

        if (null == jsonArray) { // run query and save
            // Only handling published right now
            jsonArray = metricsSvc.fileDownloadsTimeSeries(d, true);
            metricsSvc.save(new Metric(metricName, null, null, d, jsonArray.toString()));
        }
        MediaType requestedType = getVariant(req, MediaType.valueOf(FileUtil.MIME_TYPE_CSV), MediaType.APPLICATION_JSON_TYPE);
        if ((requestedType != null) && (requestedType.equals(MediaType.APPLICATION_JSON_TYPE))) {
            return ok(jsonArray);
        }
        return ok(FileUtil.jsonArrayOfObjectsToCSV(jsonArray, MetricsUtil.DATE, MetricsUtil.ID, MetricsUtil.PID, MetricsUtil.COUNT), MediaType.valueOf(FileUtil.MIME_TYPE_CSV), "uniquefiledownloads.timeseries.csv");
    }
    
    @GET
    @Path("tree")
    @Operation(summary = "Calculates the dataverse tree metric",
            description = "Calculates the released dataverse tree metric through the current month, optionally scoped to a released parent dataverse.")
    public Response getDataversesTree(@Context UriInfo uriInfo,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {
        return getDataversesTreeToMonth(uriInfo, MetricsUtil.getCurrentMonth(), parentAlias);
    }

    @GET
    @Path("tree/toMonth/{yyyymm}")
    @Operation(summary = "Calculates the dataverse tree metric through a month",
            description = "Calculates the released dataverse tree metric through the specified month, optionally scoped to a released parent dataverse.")
    public Response getDataversesTreeToMonth(@Context UriInfo uriInfo,
            @Parameter(description = "Year and month cutoff for the metric, formatted as YYYYMM.", required = true)
            @PathParam("yyyymm") String yyyymm,
            @Parameter(description = "Alias of a released parent dataverse used to scope the metric.")
            @QueryParam("parentAlias") String parentAlias) {

        Dataverse d = findDataverseOrDieIfNotFound(parentAlias);

        try {
            errorIfUnrecongizedQueryParamPassed(uriInfo, new String[] { "parentAlias" });
        } catch (IllegalArgumentException ia) {
            return error(BAD_REQUEST, ia.getLocalizedMessage());
        }

        String metricName = "tree";
        String sanitizedyyyymm = MetricsUtil.sanitizeYearMonthUserInput(yyyymm);

        JsonObject jsonObj = MetricsUtil.stringToJsonObject(metricsSvc.returnUnexpiredCacheMonthly(metricName, sanitizedyyyymm, null, d));
        if (null == jsonObj) { // run query and save
            jsonObj = metricsSvc.getDataverseTree(d, sanitizedyyyymm, DatasetVersion.VersionState.RELEASED);
            metricsSvc.save(new Metric(metricName, sanitizedyyyymm, null, d, jsonObj.toString()));
        }
        return ok(jsonObj);
    }

    private void errorIfUnrecongizedQueryParamPassed(UriInfo uriDetails, String[] allowedQueryParams) throws IllegalArgumentException {
        for (String theKey : uriDetails.getQueryParameters().keySet()) {
            if (!Arrays.stream(allowedQueryParams).anyMatch(theKey::equals)) {
                throw new IllegalArgumentException("queryParameter " + theKey + " not supported for this endpont");
            }
        }

    }

    // Throws a WebApplicationException if alias is not null and Dataverse can't be
    // found
    private Dataverse findDataverseOrDieIfNotFound(String alias) {
        Dataverse d = null;
        if (alias != null) {
            d = dataverseSvc.findByAlias(alias);
            if (d == null) {
                throw new NotFoundException("No Dataverse with alias: " + alias);
            }
        }
        // For a public API - we only want released dataverses used as parentAliases
        // Code could be updated to handle draft info as well (with access controls in
        // place)
        if ((d != null) && !d.isReleased()) {
            d = null;
        }
        return d;
    }

    // Determine which content type matches the user's request
    private MediaType getVariant(Request req, MediaType... types) {
        List<Variant> vars = Variant
                .mediaTypes(types)
                .add()
                .build();
        return req.selectVariant(vars).getMediaType();
    }

}
