package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.metrics.MetricsUtil;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

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
            JsonObjectBuilder countJson = MetricsUtil.countToJson(metricsSvc.dataversesByMonth(yyyymm));
            return allowCors(ok(countJson));
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
            JsonObjectBuilder countJson = MetricsUtil.countToJson(metricsSvc.datasetsByMonth(yyyymm));
            return allowCors(ok(countJson));
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
            JsonArrayBuilder dvBySub = MetricsUtil.datasetsBySubjectToJson(metricsSvc.datasetsBySubject());
            return allowCors(ok(dvBySub));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }



}
