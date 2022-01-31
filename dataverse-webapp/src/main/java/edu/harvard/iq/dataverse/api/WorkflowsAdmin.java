package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.annotations.ApiWriteOperation;
import edu.harvard.iq.dataverse.api.dto.WorkflowDTO;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import edu.harvard.iq.dataverse.workflow.WorkflowServiceBean;
import edu.harvard.iq.dataverse.workflow.execution.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.execution.WorkflowContext.TriggerType;
import edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionFacade;
import edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionService;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.workflow.execution.WorkflowContext.TriggerType.PostPublishDataset;

/**
 * API Endpoint for managing workflows.
 *
 * @author michael
 */
@Path("admin/workflows")
public class WorkflowsAdmin extends AbstractApiBean {

    public static final String IP_WHITELIST_KEY = "WorkflowsAdmin#IP_WHITELIST_KEY";

    @EJB
    WorkflowServiceBean workflows;

    @Inject
    private WorkflowExecutionService workflowExecutionService;

    @Inject
    private WorkflowExecutionFacade workflowExecutionFacade;

    @Inject
    private DatasetRepository datasetRepository;

    @POST
    @ApiWriteOperation
    public Response addWorkflow(JsonObject jsonWorkflow) {
        JsonParser jp = new JsonParser();
        try {
            Workflow wf = jp.parseWorkflow(jsonWorkflow);
            Workflow managedWf = workflows.save(wf);

            return created("/admin/workflows/" + managedWf.getId(),new WorkflowDTO.Converter().convert(managedWf));
        } catch (JsonParseException ex) {
            return badRequest("Can't parse Json: " + ex.getMessage());
        }
    }

    @GET
    public Response listWorkflows() {
        WorkflowDTO.Converter converter = new WorkflowDTO.Converter();
        return ok(workflows.listWorkflows().stream()
                .map(converter::convertMinimal)
                .collect(Collectors.toList()));
    }

    @PUT
    @ApiWriteOperation
    @Path("default/{triggerType}")
    public Response setDefault(@PathParam("triggerType") String triggerType, String identifier) {
        try {
            long idtf = Long.parseLong(identifier.trim());
            TriggerType tt = TriggerType.valueOf(triggerType);
            Optional<Workflow> wf = workflows.getWorkflow(idtf);
            if (wf.isPresent()) {
                workflows.setDefaultWorkflowId(tt, idtf);
                return ok("Default workflow id for trigger " + tt.name() + " set to " + idtf);
            } else {
                return notFound("Can't find workflow with id " + idtf);
            }
        } catch (NumberFormatException nfe) {
            return badRequest("workflow identifier has to be numeric.");
        } catch (IllegalArgumentException iae) {
            return badRequest("Unknown trigger type '" + triggerType + "'. Available triggers: " + Arrays.toString(TriggerType.values()));
        }
    }

    @GET
    @Path("default/")
    public Response listDefaults() {
        WorkflowDTO.Converter converter = new WorkflowDTO.Converter();
        Map<String, Object> dto = new HashMap<>();
        for (TriggerType trigger : TriggerType.values()) {
            dto.put(trigger.name(), workflows.getDefaultWorkflow(trigger)
                    .map(converter::convertMinimal)
                    .orElse(null));
        }
        return ok(dto);
    }

    @GET
    @Path("default/{triggerType}")
    public Response getDefault(@PathParam("triggerType") String triggerType) {
        try {
            return workflows.getDefaultWorkflow(TriggerType.valueOf(triggerType))
                    .map(wf -> ok(new WorkflowDTO.Converter().convert(wf)))
                    .orElse(notFound("no default workflow"));
        } catch (IllegalArgumentException iae) {
            return badRequest("Unknown trigger type '" + triggerType + "'. Available triggers: " + Arrays.toString(TriggerType.values()));
        }
    }

    @DELETE
    @ApiWriteOperation
    @Path("default/{triggerType}")
    public Response deleteDefault(@PathParam("triggerType") String triggerType) {
        try {
            workflows.setDefaultWorkflowId(TriggerType.valueOf(triggerType), null);
            return ok("default workflow for trigger " + triggerType + " unset.");
        } catch (IllegalArgumentException iae) {
            return badRequest("Unknown trigger type '" + triggerType + "'. Available triggers: " + Arrays.toString(TriggerType.values()));
        }
    }

    @GET
    @Path("/{identifier}")
    public Response getWorkflow(@PathParam("identifier") String identifier) {
        try {
            long id = Long.parseLong(identifier);
            return workflows.getWorkflow(id)
                    .map(w -> ok(new WorkflowDTO.Converter().convert(w)))
                    .orElse(notFound("Can't find workflow with id " + identifier));
        } catch (NumberFormatException nfe) {
            return badRequest("workflow identifier has to be numeric.");
        }
    }

    @DELETE
    @ApiWriteOperation
    @Path("/{id}")
    public Response deleteWorkflow(@PathParam("id") String id) {
        try {
            long idtf = Long.parseLong(id);
            return workflows.deleteWorkflow(idtf) ? ok("Workflow " + idtf + " deleted")
                    : notFound("workflow with id " + idtf + " not found");
        } catch (NumberFormatException nfe) {
            return badRequest("workflow identifier has to be numeric.");

        } catch (IllegalArgumentException e) {
            return forbidden("Cannot delete the default workflow. Please change the default workflow and try again.");

        } catch (Exception e) {
            Throwable cc = e;
            while (cc.getCause() != null) {
                cc = cc.getCause();
            }
            if (cc instanceof IllegalArgumentException) {
                return forbidden("Cannot delete the default workflow. Please change the default workflow and try again.");
            } else {
                throw e;
            }
        }
    }

    @GET
    @Path("/ip-whitelist")
    public Response getIpWhitelist() {
        return ok(settingsSvc.get(IP_WHITELIST_KEY));
    }

    @PUT
    @ApiWriteOperation
    @Path("/ip-whitelist")
    public Response setIpWhitelist(String body) {
        String ipList = body.trim();
        String[] ips = ipList.split(";");
        boolean allIpsOk = Arrays.stream(ips).allMatch(ip -> {
            try {
                IpAddress.valueOf(ip);
                return true;
            } catch (IllegalArgumentException iae) {
                return false;
            }
        });
        if (allIpsOk) {
            settingsSvc.set(IP_WHITELIST_KEY, ipList);
            return ok(settingsSvc.get(IP_WHITELIST_KEY));
        } else {
            return badRequest("Request contains illegal IP addresses.");
        }

    }

    @DELETE
    @ApiWriteOperation
    @Path("/ip-whitelist")
    public Response deleteIpWhitelist() {
        settingsSvc.delete(IP_WHITELIST_KEY);
        return ok("Restored whitelist to default (127.0.0.1;::1)");
    }

    @POST
    @ApiWriteOperation
    @Path("rerun")
    public Response rerunWorkflow(@QueryParam("type") String type) {
        try {
            AuthenticatedUser user = findAuthenticatedUserOrDie();
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Only superusers can run workflow");
            }
            if (type == null) {
                return error(Response.Status.BAD_REQUEST, "Missing processing type - 'type=failedOnly' or 'type=notPerformedOnly' is required");
            }

            int datasetProcessed = 0;
            for (Dataset dataset:datasetRepository.findAll()) {
                DatasetVersion released = dataset.getReleasedVersion();
                if (released != null) {
                    Optional<WorkflowExecution> result = workflowExecutionService.findLatestByTriggerTypeAndDatasetVersion(TriggerType.PostPublishDataset,
                            dataset.getId(), released.getVersionNumber(), released.getMinorVersionNumber());
                    if (result.isPresent() && type.equals("failedOnly")) {
                        WorkflowExecution execution = result.get();
                        if (execution.isFinished() && !execution.getLastStep().getFinishedSuccessfully()) {
                            Response response = rerun(dataset, released);
                            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                                return response;
                            }
                            datasetProcessed++;
                        }
                    } else if (!result.isPresent() && type.equals("notPerformedOnly")) {
                        Response response = rerun(dataset, released);
                        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                            return response;
                        }
                        datasetProcessed++;
                    }
                }
            }

            return ok("Processed " + datasetProcessed + " datasets");
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @POST
    @ApiWriteOperation
    @Path("{id}/rerun")
    public Response rerunWorkflow(@PathParam("id") String id, @QueryParam("version") String version) {
        try {
            AuthenticatedUser user = findAuthenticatedUserOrDie();
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Only superusers can run workflow");
            }
            Dataset ds = findDatasetOrDie(id);
            if (ds.isLockedFor(DatasetLock.Reason.Workflow)) {
                return error(Response.Status.CONFLICT, "Previous workflow hasn't finished yet");
            }
            DatasetVersion updateVersion = null;
            if (version != null) {
                for (DatasetVersion datasetVersion:ds.getVersions()) {
                    if (version.equals(datasetVersion.getVersionNumber() + "." + datasetVersion.getMinorVersionNumber())) {
                        updateVersion = datasetVersion;
                    }
                }

                if (updateVersion == null) {
                    return error(Response.Status.BAD_REQUEST, "Unknown version: " + version);
                } else if (updateVersion.isDraft()) {
                    return error(Response.Status.BAD_REQUEST, "Given version: " + version + " is draft.");
                } else {
                    return rerun(ds, updateVersion);
                }
            } else {
                return error(Response.Status.BAD_REQUEST, "Missing version number");
            }

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }

    }

    private Response rerun(Dataset dataset, DatasetVersion datasetVersion) throws WrappedResponse {
        Optional<Workflow> workflow = workflows.getDefaultWorkflow(PostPublishDataset);
        if (workflow.isPresent()) {
            if (dataset.isLockedFor(DatasetLock.Reason.Workflow)) {
                return error(Response.Status.CONFLICT, "Previous workflow hasn't finished yet.");
            }

            workflowExecutionFacade.start(
                workflow.get(), new WorkflowContext(PostPublishDataset, dataset.getId(), datasetVersion.getVersionNumber(), datasetVersion.getMinorVersionNumber(), createDataverseRequest(findUserOrDie()), true));
        }
        return ok("Datasets processed");
    }
}
