package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.annotations.ApiWriteOperation;
import edu.harvard.iq.dataverse.api.converters.MailGroupConverter;
import edu.harvard.iq.dataverse.api.dto.MailDomainGroupDTO;
import edu.harvard.iq.dataverse.api.dto.ApiResponseDTO;
import edu.harvard.iq.dataverse.api.dto.ApiErrorResponseDTO;
import edu.harvard.iq.dataverse.authorization.groups.impl.mail.MailDomainGroupService;
import edu.harvard.iq.dataverse.persistence.group.MailDomainGroup;

import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("admin/groups/mail")
public class MailGroups {

    private MailGroupConverter groupConverter;
    private MailDomainGroupService groupService;

    // -------------------- CONSTRUCTORS --------------------

    public MailGroups() { }

    @Inject
    public MailGroups(MailGroupConverter groupConverter, MailDomainGroupService groupService) {
        this.groupConverter = groupConverter;
        this.groupService = groupService;
    }

    // -------------------- LOGIC --------------------

    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllGroups() {
        return createOkResponse(groupService.getAllGroups().stream()
                .map(groupConverter::toDTO)
                .collect(Collectors.toList()));
    }

    @Path("/")
    @PUT
    @ApiWriteOperation
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addOrUpdateGroup(MailDomainGroupDTO group) {
        try {
            groupService.saveOrUpdateGroup(groupConverter.toEntity(group));
            return createEmptyOkResponse();
        } catch (EJBException ee) {
            return handleConstraintViolationExceptionIfPresent(ee)
                    .orElseThrow(() -> ee);
        }
    }

    @Path("/{alias}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroup(@PathParam("alias") String alias) {
        Optional<MailDomainGroup> found = groupService.getGroup(alias);
        return found.isPresent()
                ? createOkResponse(groupConverter.toDTO(found.get()))
                : createErrorResponse(Response.Status.NOT_FOUND, String.format("Group [%s] was not found.", alias));
    }

    @Path("/{alias}")
    @DELETE
    @ApiWriteOperation
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGroup(@PathParam("alias") String alias) {
        return groupService.deleteGroup(alias).isPresent()
                ? createEmptyOkResponse()
                : createErrorResponse(Response.Status.NOT_FOUND, String.format("Group [%s] was not found.", alias));
    }

    // -------------------- PRIVATE --------------------

    private Response createErrorResponse(Response.Status status, String message) {
        return Response.status(status)
                .entity(ApiErrorResponseDTO.errorResponse(status.getStatusCode(), message))
                .build();
    }

    private <T> Response createOkResponse(T entity) {
        return Response.status(Response.Status.OK)
                .entity(new ApiResponseDTO<T>(Response.Status.OK, entity))
                .build();
    }

    private Response createEmptyOkResponse() {
        return Response.status(Response.Status.OK)
                .entity(new ApiResponseDTO<>(Response.Status.OK, null))
                .build();
    }

    private Optional<Response> handleConstraintViolationExceptionIfPresent(EJBException ee) {
        Throwable cause = ee.getCause();
        if (!(cause instanceof ConstraintViolationException)) {
            return Optional.empty();
        }
        ConstraintViolationException cve = (ConstraintViolationException) cause;
        return Optional.of(createErrorResponse(Response.Status.BAD_REQUEST,
                "Constraint violations found: " +
                        cve.getConstraintViolations().stream()
                                .map(v -> v.getMessage() + ": [" + v.getInvalidValue() + "]")
                                .collect(Collectors.joining("; "))));
    }
}
