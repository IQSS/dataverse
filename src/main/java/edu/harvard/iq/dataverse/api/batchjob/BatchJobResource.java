package edu.harvard.iq.dataverse.api.batchjob;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.batch.entities.JobExecutionEntity;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Stateless
@Path("admin/batch")
public class BatchJobResource extends AbstractApiBean {

    private static String EMPTY_JSON_LIST = "[]";
    private static String EMPTY_JSON_OBJ = "{}";
    private static ObjectMapper mapper = new ObjectMapper();

    @GET
    @Path("/jobs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listBatchJobs() {
        try {
            final List<JobExecutionEntity> executionEntities = new ArrayList<>();
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            final Set<String> names = jobOperator.getJobNames();
            for (String name : names) {
                final int end = jobOperator.getJobInstanceCount(name);
                final List<JobInstance> jobInstances = jobOperator.getJobInstances(name, 0, end);
                for (JobInstance jobInstance : jobInstances) {
                    final List<JobExecution> executions = jobOperator.getJobExecutions(jobInstance);
                    for (JobExecution execution : executions) {
                        executionEntities.add(JobExecutionEntity.create(execution));
                    }
                }
            }
            return Response.ok("{ \"jobs\": \n" + mapper.writeValueAsString(executionEntities) + "\n}").build();
        } catch (Exception e) {
            return Response.ok(EMPTY_JSON_LIST).build();
        }
    }

    @GET
    @Path("/jobs/name/{jobName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listBatchJobsByName( @PathParam("jobName") String jobName) {
        try {
            final List<JobExecutionEntity> executionEntities = new ArrayList<>();
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            final int end = jobOperator.getJobInstanceCount(jobName);
            final List<JobInstance> jobInstances = jobOperator.getJobInstances(jobName, 0, end);
            for (JobInstance jobInstance : jobInstances) {
                final List<JobExecution> executions = jobOperator.getJobExecutions(jobInstance);
                for (JobExecution execution : executions) {
                    executionEntities.add(JobExecutionEntity.create(execution));
                }
            }
            return Response.ok("{ \"jobs\": \n" + mapper.writeValueAsString(executionEntities) + "\n}").build();
        } catch (Exception e) {
            return Response.ok(EMPTY_JSON_LIST).build();
        }
    }


    @GET
    @Path("/jobs/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listBatchJobById(@PathParam("jobId") String jobId) {
        try {
            JobExecution execution = BatchRuntime.getJobOperator().getJobExecution(Long.valueOf(jobId));
            return Response.ok(mapper.writeValueAsString(JobExecutionEntity.create(execution))).build();
        } catch (Exception e) {
            return Response.ok(EMPTY_JSON_OBJ).build();
        }
    }

}