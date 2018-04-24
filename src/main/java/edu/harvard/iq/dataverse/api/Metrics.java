package edu.harvard.iq.dataverse.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("info/metrics")
public class Metrics extends AbstractApiBean {

    @GET
    @Path("dataverses/byCategory")
    public Response getDataversesByCategory() {
        return allowCors(ok(metricsSvc.dataversesByCategory()));
    }

    @GET
    @Path("dataverses/byMonth")
    public Response getDataversesByMonth() {
        return allowCors(ok(metricsSvc.dataversesByMonth()));
    }

    @GET
    @Path("downloads/byMonth")
    public Response getDownloadsByMonth() {
        return allowCors(ok(metricsSvc.downloadsByMonth()));
    }

    @GET
    @Path("files/byMonth")
    public Response getFilesByMonth() {
        return allowCors(ok(metricsSvc.filesByMonth()));
    }

    @GET
    @Path("datasets/byMonth")
    public Response getDatasetsByMonth() {
        return allowCors(ok(metricsSvc.datasetsByMonth()));
    }

    @GET
    @Path("datasets/bySubject")
    public Response getDatasetsBySubject() {
        return allowCors(ok(metricsSvc.datasetsBySubject()));
    }

}
