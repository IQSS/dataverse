package edu.harvard.iq.dataverse.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("info/metrics")
public class Metrics extends AbstractApiBean {

    @GET
    @Path("dataverses/byCategory")
    public Response getDataversesByCategory() {
        return allowCors(ok(metricsSvc.dataversesByCategory()));
    }

    // FIXME: return current month by default
    @GET
    @Path("dataverses/byMonth/{yyyymm}")
    public Response getDataversesByMonth(@PathParam("yyyymm") String yyyymm) {
        return allowCors(ok(metricsSvc.dataversesByMonth(yyyymm)));
    }

    // FIXME: return current month by default
    @GET
    @Path("downloads/byMonth/{yyyymm}")
    public Response getDownloadsByMonth(@PathParam("yyyymm") String yyyymm) {
        return allowCors(ok(metricsSvc.downloadsByMonth(yyyymm)));
    }

    // FIXME: return current month by default
    @GET
    @Path("files/byMonth/{yyyymm}")
    public Response getFilesByMonth(@PathParam("yyyymm") String yyyymm) {
        return allowCors(ok(metricsSvc.filesByMonth(yyyymm)));
    }

    // FIXME: return current month by default
    @GET
    @Path("datasets/byMonth/{yyyymm}")
    public Response getDatasetsByMonth(@PathParam("yyyymm") String yyyymm) {
        return allowCors(ok(metricsSvc.datasetsByMonth(yyyymm)));
    }

    @GET
    @Path("datasets/bySubject")
    public Response getDatasetsBySubject() {
        return allowCors(ok(metricsSvc.datasetsBySubject()));
    }

}
