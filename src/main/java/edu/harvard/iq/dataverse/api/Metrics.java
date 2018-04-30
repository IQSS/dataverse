package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.metrics.MetricsUtil;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        return getDataversesByMonth(getCurrentMonth());
    }

    @GET
    @Path("dataverses/byMonth/{yyyymm}")
    public Response getDataversesByMonth(@PathParam("yyyymm") String yyyymm) {
        try {
            return allowCors(ok(metricsSvc.dataversesByMonth(yyyymm)));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

    @GET
    @Path("datasets/byMonth")
    public Response getDatasetsByMonthCurrent() {
        return getDatasetsByMonth(getCurrentMonth());
    }

    @GET
    @Path("datasets/byMonth/{yyyymm}")
    public Response getDatasetsByMonth(@PathParam("yyyymm") String yyyymm) {
        try {
            return allowCors(ok(metricsSvc.datasetsByMonth(yyyymm)));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

    @GET
    @Path("files/byMonth")
    public Response getFilesByMonthCurrent() {
        return getFilesByMonth(getCurrentMonth());
    }

    @GET
    @Path("files/byMonth/{yyyymm}")
    public Response getFilesByMonth(@PathParam("yyyymm") String yyyymm) {
        try {
            return allowCors(ok(metricsSvc.filesByMonth(yyyymm)));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

    @GET
    @Path("downloads/byMonth")
    public Response getDownloadsByMonthCurrent() {
        return getDownloadsByMonth(getCurrentMonth());
    }

    @GET
    @Path("downloads/byMonth/{yyyymm}")
    public Response getDownloadsByMonth(@PathParam("yyyymm") String yyyymm) {
        try {
            return allowCors(ok(metricsSvc.downloadsByMonth(yyyymm)));
        } catch (Exception ex) {
            return allowCors(error(BAD_REQUEST, ex.getLocalizedMessage()));
        }
    }

    @GET
    @Path("dataverses/byCategory")
    public Response getDataversesByCategory() {
        return allowCors(ok(metricsSvc.dataversesByCategory()));
    }

    @GET
    @Path("datasets/bySubject")
    public Response getDatasetsBySubject() {
        return allowCors(ok(metricsSvc.datasetsBySubject()));
    }

    private String getCurrentMonth() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern(MetricsUtil.YEAR_AND_MONTH_PATTERN));
    }

}
