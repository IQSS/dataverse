package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.RoleAssignment;
import static edu.harvard.iq.dataverse.api.AbstractApiBean.error;
import edu.harvard.iq.dataverse.api.dto.ExplicitGroupDTO;
import edu.harvard.iq.dataverse.api.dto.RoleAssignmentDTO;
import edu.harvard.iq.dataverse.api.dto.RoleDTO;
import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.AddRoleAssigneesToExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.AssignRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateNewDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataverseLinkingDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ImportDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.LinkDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListDataverseContentCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListExplicitGroupsCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListFacetsCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListMetadataBlocksCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ListRoleAssignments;
import edu.harvard.iq.dataverse.engine.command.impl.ListRolesCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetResult;
import edu.harvard.iq.dataverse.engine.command.impl.MoveDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RemoveRoleAssigneesFromExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseDefaultContributorRoleCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDataverseMetadataBlocksCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateExplicitGroupCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import static edu.harvard.iq.dataverse.util.StringUtil.nonEmpty;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.brief;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonParsingException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.toJsonArray;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import javax.persistence.NoResultException;

/**
 * A REST API for dataverses.
 *
 * @author michael
 */
@Stateless
@Path("dataverses")
public class Dataverses extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Dataverses.class.getCanonicalName());

    @EJB
    ExplicitGroupServiceBean explicitGroupSvc;

    @EJB
    ImportServiceBean importService;

    @POST
    public Response addRoot(String body) {
        logger.info("Creating root dataverse");
        return addDataverse(body, "");
    }

    @POST
    @Path("{identifier}")
    public Response addDataverse(String body, @PathParam("identifier") String parentIdtf) {

        Dataverse d;
        JsonObject dvJson;
        try (StringReader rdr = new StringReader(body)) {
            dvJson = Json.createReader(rdr).readObject();
            d = jsonParser().parseDataverse(dvJson);
        } catch (JsonParsingException jpe) {
            logger.log(Level.SEVERE, "Json: {0}", body);
            return error(Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage());
        } catch (JsonParseException ex) {
            logger.log(Level.SEVERE, "Error parsing dataverse from json: " + ex.getMessage(), ex);
            return error(Response.Status.BAD_REQUEST,
                    "Error parsing the POSTed json into a dataverse: " + ex.getMessage());
        }

        try {
            if (!parentIdtf.isEmpty()) {
                Dataverse owner = findDataverseOrDie(parentIdtf);
                d.setOwner(owner);
            }

            // set the dataverse - contact relationship in the contacts
            for (DataverseContact dc : d.getDataverseContacts()) {
                dc.setDataverse(d);
            }

            AuthenticatedUser u = findAuthenticatedUserOrDie();
            d = execCommand(new CreateDataverseCommand(d, createDataverseRequest(u), null, null));
            return created("/dataverses/" + d.getAlias(), json(d));
        } catch (WrappedResponse ww) {
            Throwable cause = ww.getCause();
            StringBuilder sb = new StringBuilder();
            if (cause == null) {
                return ww.refineResponse("cause was null!");
            }
            while (cause.getCause() != null) {
                cause = cause.getCause();
                if (cause instanceof ConstraintViolationException) {
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                        sb.append(" Invalid value: <<<").append(violation.getInvalidValue()).append(">>> for ")
                                .append(violation.getPropertyPath()).append(" at ")
                                .append(violation.getLeafBean()).append(" - ")
                                .append(violation.getMessage());
                    }
                }
            }
            String error = sb.toString();
            if (!error.isEmpty()) {
                logger.log(Level.INFO, error);
                return ww.refineResponse(error);
            }
            return ww.getResponse();

        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append("Error creating dataverse.");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                if (cause instanceof ConstraintViolationException) {
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                        sb.append(" Invalid value: <<<").append(violation.getInvalidValue()).append(">>> for ")
                                .append(violation.getPropertyPath()).append(" at ")
                                .append(violation.getLeafBean()).append(" - ")
                                .append(violation.getMessage());
                    }
                }
            }
            logger.log(Level.SEVERE, sb.toString());
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Error creating dataverse: " + sb.toString());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error creating dataverse", ex);
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Error creating dataverse: " + ex.getMessage());

        }
    }

    @POST
    @Path("{identifier}/datasets")
    public Response createDataset(String jsonBody, @PathParam("identifier") String parentIdtf) {
        try {
            User u = findUserOrDie();
            Dataverse owner = findDataverseOrDie(parentIdtf);
            Dataset ds = parseDataset(jsonBody);
            ds.setOwner(owner);

            if (ds.getVersions().isEmpty()) {
                return badRequest("Please provide initial version in the dataset json");
            }

            // clean possible version metadata
            DatasetVersion version = ds.getVersions().get(0);
            version.setMinorVersionNumber(null);
            version.setVersionNumber(null);
            version.setVersionState(DatasetVersion.VersionState.DRAFT);

            ds.setAuthority(null);
            ds.setIdentifier(null);
            ds.setProtocol(null);
            ds.setGlobalIdCreateTime(null);

            Dataset managedDs = execCommand(new CreateNewDatasetCommand(ds, createDataverseRequest(u)));
            return created("/datasets/" + managedDs.getId(),
                    Json.createObjectBuilder()
                            .add("id", managedDs.getId())
                            .add("persistentId", managedDs.getGlobalIdString())
            );

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @POST
    @Path("{identifier}/datasets/:import")
    public Response importDataset(String jsonBody, @PathParam("identifier") String parentIdtf, @QueryParam("pid") String pidParam, @QueryParam("release") String releaseParam) {
        try {
            User u = findUserOrDie();
            Dataverse owner = findDataverseOrDie(parentIdtf);
            Dataset ds = parseDataset(jsonBody);
            ds.setOwner(owner);

            if (ds.getVersions().isEmpty()) {
                return badRequest("Supplied json must contain a single dataset version.");
            }

            DatasetVersion version = ds.getVersions().get(0);
            if (version.getVersionState() == null) {
                version.setVersionState(DatasetVersion.VersionState.DRAFT);
            }

            if (nonEmpty(pidParam)) {
                if (!GlobalId.verifyImportCharacters(pidParam)) {
                    return badRequest("PID parameter contains characters that are not allowed by the Dataverse application. On import, the PID must only contain characters specified in this regex: " + BundleUtil.getStringFromBundle("pid.allowedCharacters"));
                }
                Optional<GlobalId> maybePid = GlobalId.parse(pidParam);
                if (maybePid.isPresent()) {
                    ds.setGlobalId(maybePid.get());
                } else {
                    // unparsable PID passed. Terminate.
                    return badRequest("Cannot parse the PID parameter '" + pidParam + "'. Make sure it is in valid form - see Dataverse Native API documentation.");
                }
            }

            if (ds.getIdentifier() == null) {
                return badRequest("Please provide a persistent identifier, either by including it in the JSON, or by using the pid query parameter.");
            }
            boolean shouldRelease = StringUtil.isTrue(releaseParam);
            DataverseRequest request = createDataverseRequest(u);

            if (shouldRelease) {
                DatasetVersion latestVersion = ds.getLatestVersion();
                latestVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
                latestVersion.setVersionNumber(1l);
                latestVersion.setMinorVersionNumber(0l);
                if (latestVersion.getCreateTime() != null) {
                    latestVersion.setCreateTime(new Date());
                }
                if (latestVersion.getLastUpdateTime() != null) {
                    latestVersion.setLastUpdateTime(new Date());
                }
            }

            Dataset managedDs = execCommand(new ImportDatasetCommand(ds, request));
            JsonObjectBuilder responseBld = Json.createObjectBuilder()
                    .add("id", managedDs.getId())
                    .add("persistentId", managedDs.getGlobalIdString());

            if (shouldRelease) {
                PublishDatasetResult res = execCommand(new PublishDatasetCommand(managedDs, request, false, shouldRelease));
                responseBld.add("releaseCompleted", res.isCompleted());
            }

            return created("/datasets/" + managedDs.getId(), responseBld);

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    // TODO decide if I merge importddi with import just below (xml and json on same api, instead of 2 api)
    @POST
    @Path("{identifier}/datasets/:importddi")
    public Response importDatasetDdi(String xml, @PathParam("identifier") String parentIdtf, @QueryParam("pid") String pidParam, @QueryParam("release") String releaseParam) throws ImportException {
        try {
            User u = findUserOrDie();
            if (!u.isSuperuser()) {
                return error(Status.FORBIDDEN, "Not a superuser");
            }
            Dataverse owner = findDataverseOrDie(parentIdtf);
            Dataset ds = null;
            try {
                ds = jsonParser().parseDataset(importService.ddiToJson(xml));
            }
            catch (JsonParseException jpe) {
                return badRequest("Error parsing datas as Json: "+jpe.getMessage());
            }
            ds.setOwner(owner);
            if (nonEmpty(pidParam)) {
                if (!GlobalId.verifyImportCharacters(pidParam)) {
                    return badRequest("PID parameter contains characters that are not allowed by the Dataverse application. On import, the PID must only contain characters specified in this regex: " + BundleUtil.getStringFromBundle("pid.allowedCharacters"));
                }
                Optional<GlobalId> maybePid = GlobalId.parse(pidParam);
                if (maybePid.isPresent()) {
                    ds.setGlobalId(maybePid.get());
                } else {
                    // unparsable PID passed. Terminate.
                    return badRequest("Cannot parse the PID parameter '" + pidParam + "'. Make sure it is in valid form - see Dataverse Native API documentation.");
                }
            }

            boolean shouldRelease = StringUtil.isTrue(releaseParam);
            DataverseRequest request = createDataverseRequest(u);

            Dataset managedDs = null;
            if (nonEmpty(pidParam)) {
                managedDs = execCommand(new ImportDatasetCommand(ds, request));
            }
            else {
                managedDs = execCommand(new CreateNewDatasetCommand(ds, request));
            }

            JsonObjectBuilder responseBld = Json.createObjectBuilder()
                    .add("id", managedDs.getId())
                    .add("persistentId", managedDs.getGlobalIdString());

            if (shouldRelease) {
                DatasetVersion latestVersion = ds.getLatestVersion();
                latestVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
                latestVersion.setVersionNumber(1l);
                latestVersion.setMinorVersionNumber(0l);
                if (latestVersion.getCreateTime() != null) {
                    latestVersion.setCreateTime(new Date());
                }
                if (latestVersion.getLastUpdateTime() != null) {
                    latestVersion.setLastUpdateTime(new Date());
                }
                PublishDatasetResult res = execCommand(new PublishDatasetCommand(managedDs, request, false, shouldRelease));
                responseBld.add("releaseCompleted", res.isCompleted());
            }

            return created("/datasets/" + managedDs.getId(), responseBld);

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    private Dataset parseDataset(String datasetJson) throws WrappedResponse {
        try (StringReader rdr = new StringReader(datasetJson)) {
            return jsonParser().parseDataset(Json.createReader(rdr).readObject());
        } catch (JsonParsingException | JsonParseException jpe) {
            logger.log(Level.SEVERE, "Error parsing dataset json. Json: {0}", datasetJson);
            throw new WrappedResponse(error(Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage()));
        }
    }

    @GET
    @Path("{identifier}")
    public Response viewDataverse(@PathParam("identifier") String idtf) {
        return allowCors(response(req -> ok(json(execCommand(
                new GetDataverseCommand(req, findDataverseOrDie(idtf)))))));
    }

    @DELETE
    @Path("{identifier}")
    public Response deleteDataverse(@PathParam("identifier") String idtf) {
        return response(req -> {
            execCommand(new DeleteDataverseCommand(req, findDataverseOrDie(idtf)));
            return ok("Dataverse " + idtf + " deleted");
        });
    }

    @DELETE
    @Path("{linkingDataverseId}/deleteLink/{linkedDataverseId}")
    public Response deleteDataverseLinkingDataverse(@PathParam("linkingDataverseId") String linkingDataverseId, @PathParam("linkedDataverseId") String linkedDataverseId) {
        boolean index = true;
        return response(req -> {
            execCommand(new DeleteDataverseLinkingDataverseCommand(req, findDataverseOrDie(linkingDataverseId), findDataverseLinkingDataverseOrDie(linkingDataverseId, linkedDataverseId), index));
            return ok("Link from Dataverse " + linkingDataverseId + " to linked Dataverse " + linkedDataverseId + " deleted");
        });
    }

    @GET
    @Path("{identifier}/metadatablocks")
    public Response listMetadataBlocks(@PathParam("identifier") String dvIdtf) {
        try {
            JsonArrayBuilder arr = Json.createArrayBuilder();
            final List<MetadataBlock> blocks = execCommand(new ListMetadataBlocksCommand(createDataverseRequest(findUserOrDie()), findDataverseOrDie(dvIdtf)));
            for (MetadataBlock mdb : blocks) {
                arr.add(brief.json(mdb));
            }
            return allowCors(ok(arr));
        } catch (WrappedResponse we) {
            return we.getResponse();
        }
    }

    @POST
    @Path("{identifier}/metadatablocks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setMetadataBlocks(@PathParam("identifier") String dvIdtf, String blockIds) {

        List<MetadataBlock> blocks = new LinkedList<>();
        try {
            for (JsonValue blockId : Util.asJsonArray(blockIds).getValuesAs(JsonValue.class)) {
                MetadataBlock blk = (blockId.getValueType() == ValueType.NUMBER)
                        ? findMetadataBlock(((JsonNumber) blockId).longValue())
                        : findMetadataBlock(((JsonString) blockId).getString());
                if (blk == null) {
                    return error(Response.Status.BAD_REQUEST, "Can't find metadata block '" + blockId + "'");
                }
                blocks.add(blk);
            }
        } catch (Exception e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());
        }

        try {
            execCommand(new UpdateDataverseMetadataBlocksCommand.SetBlocks(createDataverseRequest(findUserOrDie()), findDataverseOrDie(dvIdtf), blocks));
            return ok("Metadata blocks of dataverse " + dvIdtf + " updated.");

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @GET
    @Path("{identifier}/metadatablocks/:isRoot")
    public Response getMetadataRoot_legacy(@PathParam("identifier") String dvIdtf) {
        return getMetadataRoot(dvIdtf);
    }

    @GET
    @Path("{identifier}/metadatablocks/isRoot")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetadataRoot(@PathParam("identifier") String dvIdtf) {
        return response(req -> {
            final Dataverse dataverse = findDataverseOrDie(dvIdtf);
            if (permissionSvc.request(req)
                    .on(dataverse)
                    .has(Permission.EditDataverse)) {
                return ok(dataverse.isMetadataBlockRoot());
            } else {
                return error(Status.FORBIDDEN, "Not authorized");
            }
        });
    }

    @POST
    @Path("{identifier}/metadatablocks/:isRoot")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response setMetadataRoot_legacy(@PathParam("identifier") String dvIdtf, String body) {
        return setMetadataRoot(dvIdtf, body);
    }

    @PUT
    @Path("{identifier}/metadatablocks/isRoot")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response setMetadataRoot(@PathParam("identifier") String dvIdtf, String body) {
        return response(req -> {
            final boolean root = parseBooleanOrDie(body);
            final Dataverse dataverse = findDataverseOrDie(dvIdtf);
            execCommand(new UpdateDataverseMetadataBlocksCommand.SetRoot(req, dataverse, root));
            return ok("Dataverse " + dataverse.getName() + " is now a metadata  " + (root ? "" : "non-") + "root");
        });
    }

    @GET
    @Path("{identifier}/facets/")
    /**
     * return list of facets for the dataverse with alias `dvIdtf`
     */
    public Response listFacets(@PathParam("identifier") String dvIdtf) {
        try {
            User u = findUserOrDie();
            DataverseRequest r = createDataverseRequest(u);
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            JsonArrayBuilder fs = Json.createArrayBuilder();
            for (DataverseFacet f : execCommand(new ListFacetsCommand(r, dataverse))) {
                fs.add(f.getDatasetFieldType().getName());
            }
            return allowCors(ok(fs));
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }

    @POST
    @Path("{identifier}/facets")
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * (not publicly documented) API endpoint for assigning facets to a
     * dataverse. `curl -X POST -H "X-Dataverse-key: $ADMIN_KEY"
     * http://localhost:8088/api/dataverses/$dv/facets --upload-file foo.json`;
     * where foo.json contains a list of datasetField names, works as expected
     * (judging by the UI). This triggers a 500 when '-d @foo.json' is used.
     */
    public Response setFacets(@PathParam("identifier") String dvIdtf, String facetIds) {

        List<DatasetFieldType> facets = new LinkedList<>();
        for (JsonString facetId : Util.asJsonArray(facetIds).getValuesAs(JsonString.class)) {
            DatasetFieldType dsfType = findDatasetFieldType(facetId.getString());
            if (dsfType == null) {
                return error(Response.Status.BAD_REQUEST, "Can't find dataset field type '" + facetId + "'");
            } else if (!dsfType.isFacetable()) {
                return error(Response.Status.BAD_REQUEST, "Dataset field type '" + facetId + "' is not facetable");
            }
            facets.add(dsfType);
        }

        try {
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            // by passing null for Featured Dataverses and DataverseFieldTypeInputLevel, those are not changed
            execCommand(new UpdateDataverseCommand(dataverse, facets, null, createDataverseRequest(findUserOrDie()), null));
            return ok("Facets of dataverse " + dvIdtf + " updated.");

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    // FIXME: This listContent method is way too optimistic, always returning "ok" and never "error".
    // TODO: Investigate why there was a change in the timeframe of when pull request #4350 was merged
    // (2438-4295-dois-for-files branch) such that a contributor API token no longer allows this method
    // to be called without a PermissionException being thrown.
    @GET
    @Path("{identifier}/contents")
    public Response listContent(@PathParam("identifier") String dvIdtf) throws WrappedResponse {

        DvObject.Visitor<JsonObjectBuilder> ser = new DvObject.Visitor<JsonObjectBuilder>() {
            @Override
            public JsonObjectBuilder visit(Dataverse dv) {
                return Json.createObjectBuilder().add("type", "dataverse")
                        .add("id", dv.getId())
                        .add("title", dv.getName());
            }

            @Override
            public JsonObjectBuilder visit(Dataset ds) {
                return json(ds).add("type", "dataset");
            }

            @Override
            public JsonObjectBuilder visit(DataFile df) {
                throw new UnsupportedOperationException("Files don't live directly in Dataverses");
            }
        };

        return allowCors(response(req -> ok(
                execCommand(new ListDataverseContentCommand(req, findDataverseOrDie(dvIdtf)))
                        .stream()
                        .map(dvo -> (JsonObjectBuilder) dvo.accept(ser))
                        .collect(toJsonArray()))
        ));
    }

    @GET
    @Path("{identifier}/roles")
    public Response listRoles(@PathParam("identifier") String dvIdtf) {
        return response(req -> ok(
                execCommand(new ListRolesCommand(req, findDataverseOrDie(dvIdtf)))
                        .stream().map(r -> json(r))
                        .collect(toJsonArray())
        ));
    }

    @POST
    @Path("{identifier}/roles")
    public Response createRole(RoleDTO roleDto, @PathParam("identifier") String dvIdtf) {
        return response(req -> ok(json(execCommand(new CreateRoleCommand(roleDto.asRole(), req, findDataverseOrDie(dvIdtf))))));
    }

    @GET
    @Path("{identifier}/assignments")
    public Response listAssignments(@PathParam("identifier") String dvIdtf) {
        return response(req -> ok(
                execCommand(new ListRoleAssignments(req, findDataverseOrDie(dvIdtf)))
                        .stream()
                        .map(a -> json(a))
                        .collect(toJsonArray())
        ));
    }

    /**
     * This code for setting a dataverse logo via API was started when initially
     * investigating https://github.com/IQSS/dataverse/issues/3559 but it isn't
     * finished so it's commented out. See also * "No functionality should be
     * GUI-only. Make all functionality reachable via the API" at
     * https://github.com/IQSS/dataverse/issues/3440
     */
//    File tempDir;
//
//    private void createTempDir(Dataverse editDv) {
//        try {
//            File tempRoot = java.nio.file.Files.createDirectories(Paths.get("../docroot/logos/temp")).toFile();
//            tempDir = java.nio.file.Files.createTempDirectory(tempRoot.toPath(), editDv.getId().toString()).toFile();
//        } catch (IOException e) {
//            throw new RuntimeException("Error creating temp directory", e); // improve error handling
//        }
//    }
//
//    private DataverseTheme initDataverseTheme(Dataverse editDv) {
//        DataverseTheme dvt = new DataverseTheme();
//        dvt.setLinkColor(DEFAULT_LINK_COLOR);
//        dvt.setLogoBackgroundColor(DEFAULT_LOGO_BACKGROUND_COLOR);
//        dvt.setBackgroundColor(DEFAULT_BACKGROUND_COLOR);
//        dvt.setTextColor(DEFAULT_TEXT_COLOR);
//        dvt.setDataverse(editDv);
//        return dvt;
//    }
//
//    @PUT
//    @Path("{identifier}/logo")
//    @Consumes(MediaType.MULTIPART_FORM_DATA)
//    public Response setDataverseLogo(@PathParam("identifier") String dvIdtf,
//            @FormDataParam("file") InputStream fileInputStream,
//            @FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
//            @QueryParam("key") String apiKey) {
//        boolean disabled = true;
//        if (disabled) {
//            return error(Status.FORBIDDEN, "Setting the dataverse logo via API needs more work.");
//        }
//        try {
//            final DataverseRequest req = createDataverseRequest(findUserOrDie());
//            final Dataverse editDv = findDataverseOrDie(dvIdtf);
//
//            logger.finer("entering fileUpload");
//            if (tempDir == null) {
//                createTempDir(editDv);
//                logger.finer("created tempDir");
//            }
//            File uploadedFile;
//            try {
//                String fileName = contentDispositionHeader.getFileName();
//
//                uploadedFile = new File(tempDir, fileName);
//                if (!uploadedFile.exists()) {
//                    uploadedFile.createNewFile();
//                }
//                logger.finer("created file");
//                File file = null;
//                file = FileUtil.inputStreamToFile(fileInputStream);
//                if (file.length() > systemConfig.getUploadLogoSizeLimit()) {
//                    return error(Response.Status.BAD_REQUEST, "File is larger than maximum size: " + systemConfig.getUploadLogoSizeLimit() + ".");
//                }
//                java.nio.file.Files.copy(fileInputStream, uploadedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//                logger.finer("copied inputstream to file");
//                editDv.setDataverseTheme(initDataverseTheme(editDv));
//                editDv.getDataverseTheme().setLogo(fileName);
//
//            } catch (IOException e) {
//                logger.finer("caught IOException");
//                logger.throwing("ThemeWidgetFragment", "handleImageFileUpload", e);
//                throw new RuntimeException("Error uploading logo file", e); // improve error handling
//            }
//            // If needed, set the default values for the logo
//            if (editDv.getDataverseTheme().getLogoFormat() == null) {
//                editDv.getDataverseTheme().setLogoFormat(DataverseTheme.ImageFormat.SQUARE);
//            }
//            logger.finer("end handelImageFileUpload");
//            UpdateDataverseThemeCommand cmd = new UpdateDataverseThemeCommand(editDv, uploadedFile, req);
//            Dataverse saved = execCommand(cmd);
//
//            /**
//             * @todo delete the temp file:
//             * docroot/logos/temp/1148114212463761832421/cc0.png
//             */
//            return ok("logo uploaded: " + saved.getDataverseTheme().getLogo());
//        } catch (WrappedResponse ex) {
//            return error(Status.BAD_REQUEST, "problem uploading logo: " + ex);
//        }
//    }
    @POST
    @Path("{identifier}/assignments")
    public Response createAssignment(RoleAssignmentDTO ra, @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey) {

        try {
            final DataverseRequest req = createDataverseRequest(findUserOrDie());
            final Dataverse dataverse = findDataverseOrDie(dvIdtf);

            RoleAssignee assignee = findAssignee(ra.getAssignee());
            if (assignee == null) {
                return error(Status.BAD_REQUEST, "Assignee not found");
            }

            DataverseRole theRole;
            Dataverse dv = dataverse;
            theRole = null;
            while ((theRole == null) && (dv != null)) {
                for (DataverseRole aRole : rolesSvc.availableRoles(dv.getId())) {
                    if (aRole.getAlias().equals(ra.getRole())) {
                        theRole = aRole;
                        break;
                    }
                }
                dv = dv.getOwner();
            }
            if (theRole == null) {
                return error(Status.BAD_REQUEST, "Can't find role named '" + ra.getRole() + "' in dataverse " + dataverse);
            }
            String privateUrlToken = null;

            return ok(json(execCommand(new AssignRoleCommand(assignee, theRole, dataverse, req, privateUrlToken))));

        } catch (WrappedResponse ex) {
            logger.log(Level.WARNING, "Can''t create assignment: {0}", ex.getMessage());
            return ex.getResponse();
        }
    }

    @DELETE
    @Path("{identifier}/assignments/{id}")
    public Response deleteAssignment(@PathParam("id") long assignmentId, @PathParam("identifier") String dvIdtf) {
        RoleAssignment ra = em.find(RoleAssignment.class, assignmentId);
        if (ra != null) {
            try {
                findDataverseOrDie(dvIdtf);
                execCommand(new RevokeRoleCommand(ra, createDataverseRequest(findUserOrDie())));
                return ok("Role " + ra.getRole().getName()
                        + " revoked for assignee " + ra.getAssigneeIdentifier()
                        + " in " + ra.getDefinitionPoint().accept(DvObject.NamePrinter));
            } catch (WrappedResponse ex) {
                return ex.getResponse();
            }
        } else {
            return error(Status.NOT_FOUND, "Role assignment " + assignmentId + " not found");
        }
    }

    @POST
    @Path("{identifier}/actions/:publish")
    public Response publishDataverse(@PathParam("identifier") String dvIdtf) {
        try {
            Dataverse dv = findDataverseOrDie(dvIdtf);
            return ok(json(execCommand(new PublishDataverseCommand(createDataverseRequest(findAuthenticatedUserOrDie()), dv))));

        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @POST
    @Path("{identifier}/groups/")
    public Response createExplicitGroup(ExplicitGroupDTO dto, @PathParam("identifier") String dvIdtf) {
        return response(req -> {
            ExplicitGroupProvider prv = explicitGroupSvc.getProvider();
            ExplicitGroup newGroup = dto.apply(prv.makeGroup());

            newGroup = execCommand(new CreateExplicitGroupCommand(req, findDataverseOrDie(dvIdtf), newGroup));

            String groupUri = String.format("%s/groups/%s", dvIdtf, newGroup.getGroupAliasInOwner());
            return created(groupUri, json(newGroup));
        });
    }

    @GET
    @Path("{identifier}/groups/")
    public Response listGroups(@PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey) {
        return response(req -> ok(
                execCommand(new ListExplicitGroupsCommand(req, findDataverseOrDie(dvIdtf)))
                        .stream().map(eg -> json(eg))
                        .collect(toJsonArray())
        ));
    }

    @GET
    @Path("{identifier}/groups/{aliasInOwner}")
    public Response getGroupByOwnerAndAliasInOwner(@PathParam("identifier") String dvIdtf,
            @PathParam("aliasInOwner") String grpAliasInOwner) {
        return response(req -> ok(json(findExplicitGroupOrDie(findDataverseOrDie(dvIdtf),
                req,
                grpAliasInOwner))));
    }

    @PUT
    @Path("{identifier}/groups/{aliasInOwner}")
    public Response updateGroup(ExplicitGroupDTO groupDto,
            @PathParam("identifier") String dvIdtf,
            @PathParam("aliasInOwner") String grpAliasInOwner) {
        return response(req -> ok(json(execCommand(
                new UpdateExplicitGroupCommand(req,
                        groupDto.apply(findExplicitGroupOrDie(findDataverseOrDie(dvIdtf), req, grpAliasInOwner)))))));
    }
    
    @PUT
    @Path("{identifier}/defaultContributorRole/{roleAlias}")
    public Response updateDefaultContributorRole(
            @PathParam("identifier") String dvIdtf,
            @PathParam("roleAlias") String roleAlias) {

        DataverseRole defaultRole;
        
        if (roleAlias.equals(DataverseRole.NONE)) {
            defaultRole = null;
        } else {
            try {
                Dataverse dv = findDataverseOrDie(dvIdtf);
                defaultRole = rolesSvc.findCustomRoleByAliasAndOwner(roleAlias, dv.getId());
            } catch (Exception nre) {
                List<String> args = Arrays.asList(roleAlias);
                String retStringError = BundleUtil.getStringFromBundle("dataverses.api.update.default.contributor.role.failure.role.not.found", args);
                return error(Status.NOT_FOUND, retStringError);
            }

            if (!defaultRole.doesDvObjectClassHavePermissionForObject(Dataset.class)) {
                List<String> args = Arrays.asList(roleAlias);
                String retStringError = BundleUtil.getStringFromBundle("dataverses.api.update.default.contributor.role.failure.role.does.not.have.dataset.permissions", args);
                return error(Status.BAD_REQUEST, retStringError);
            }

        }

        try {
            Dataverse dv = findDataverseOrDie(dvIdtf);
            
            String defaultRoleName = defaultRole == null ? BundleUtil.getStringFromBundle("permission.default.contributor.role.none.name") : defaultRole.getName();

            return response(req -> {
                execCommand(new UpdateDataverseDefaultContributorRoleCommand(defaultRole, req, dv));
                List<String> args = Arrays.asList(dv.getDisplayName(), defaultRoleName);
                String retString = BundleUtil.getStringFromBundle("dataverses.api.update.default.contributor.role.success", args);
                return ok(retString);
            });

        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

    }

    @DELETE
    @Path("{identifier}/groups/{aliasInOwner}")
    public Response deleteGroup(@PathParam("identifier") String dvIdtf,
            @PathParam("aliasInOwner") String grpAliasInOwner) {
        return response(req -> {
            execCommand(new DeleteExplicitGroupCommand(req,
                    findExplicitGroupOrDie(findDataverseOrDie(dvIdtf), req, grpAliasInOwner)));
            return ok("Group " + dvIdtf + "/" + grpAliasInOwner + " deleted");
        });
    }

    @POST
    @Path("{identifier}/groups/{aliasInOwner}/roleAssignees")
    @Consumes("application/json")
    public Response addRoleAssingees(List<String> roleAssingeeIdentifiers,
            @PathParam("identifier") String dvIdtf,
            @PathParam("aliasInOwner") String grpAliasInOwner) {
        return response(req -> ok(
                json(
                    execCommand(
                                new AddRoleAssigneesToExplicitGroupCommand(req,
                                        findExplicitGroupOrDie(findDataverseOrDie(dvIdtf), req, grpAliasInOwner),
                                        new TreeSet<>(roleAssingeeIdentifiers))))));
    }

    @PUT
    @Path("{identifier}/groups/{aliasInOwner}/roleAssignees/{roleAssigneeIdentifier: .*}")
    public Response addRoleAssingee(@PathParam("identifier") String dvIdtf,
            @PathParam("aliasInOwner") String grpAliasInOwner,
            @PathParam("roleAssigneeIdentifier") String roleAssigneeIdentifier) {
        return addRoleAssingees(Collections.singletonList(roleAssigneeIdentifier), dvIdtf, grpAliasInOwner);
    }

    @DELETE
    @Path("{identifier}/groups/{aliasInOwner}/roleAssignees/{roleAssigneeIdentifier: .*}")
    public Response deleteRoleAssingee(@PathParam("identifier") String dvIdtf,
            @PathParam("aliasInOwner") String grpAliasInOwner,
            @PathParam("roleAssigneeIdentifier") String roleAssigneeIdentifier) {
        return response(req -> ok(json(execCommand(
                new RemoveRoleAssigneesFromExplicitGroupCommand(req,
                        findExplicitGroupOrDie(findDataverseOrDie(dvIdtf), req, grpAliasInOwner),
                        Collections.singleton(roleAssigneeIdentifier))))));
    }

    private ExplicitGroup findExplicitGroupOrDie(DvObject dv, DataverseRequest req, String groupIdtf) throws WrappedResponse {
        ExplicitGroup eg = execCommand(new GetExplicitGroupCommand(req, dv, groupIdtf));
        if (eg == null) {
            throw new WrappedResponse(notFound("Can't find " + groupIdtf + " in dataverse " + dv.getId()));
        }
        return eg;
    }

    @GET
    @Path("{identifier}/links")
    public Response listLinks(@PathParam("identifier") String dvIdtf) {
        try {
            User u = findUserOrDie();
            Dataverse dv = findDataverseOrDie(dvIdtf);
            if (!u.isSuperuser()) {
                return error(Status.FORBIDDEN, "Not a superuser");
            }

            List<Dataverse> dvsThisDvHasLinkedToList = dataverseSvc.findDataversesThisIdHasLinkedTo(dv.getId());
            JsonArrayBuilder dvsThisDvHasLinkedToBuilder = Json.createArrayBuilder();
            for (Dataverse dataverse : dvsThisDvHasLinkedToList) {
                dvsThisDvHasLinkedToBuilder.add(dataverse.getAlias());
            }

            List<Dataverse> dvsThatLinkToThisDvList = dataverseSvc.findDataversesThatLinkToThisDvId(dv.getId());
            JsonArrayBuilder dvsThatLinkToThisDvBuilder = Json.createArrayBuilder();
            for (Dataverse dataverse : dvsThatLinkToThisDvList) {
                dvsThatLinkToThisDvBuilder.add(dataverse.getAlias());
            }

            List<Dataset> datasetsThisDvHasLinkedToList = dataverseSvc.findDatasetsThisIdHasLinkedTo(dv.getId());
            JsonArrayBuilder datasetsThisDvHasLinkedToBuilder = Json.createArrayBuilder();
            for (Dataset dataset : datasetsThisDvHasLinkedToList) {
                datasetsThisDvHasLinkedToBuilder.add(dataset.getLatestVersion().getTitle());
            }

            JsonObjectBuilder response = Json.createObjectBuilder();
            response.add("dataverses that the " + dv.getAlias() + " dataverse has linked to", dvsThisDvHasLinkedToBuilder);
            response.add("dataverses that link to the " + dv.getAlias(), dvsThatLinkToThisDvBuilder);
            response.add("datasets that the " + dv.getAlias() + " has linked to", datasetsThisDvHasLinkedToBuilder);
            return ok(response);

        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @POST
    @Path("{id}/move/{targetDataverseAlias}")
    public Response moveDataverse(@PathParam("id") String id, @PathParam("targetDataverseAlias") String targetDataverseAlias, @QueryParam("forceMove") Boolean force) {
        try {
            User u = findUserOrDie();
            Dataverse dv = findDataverseOrDie(id);
            Dataverse target = findDataverseOrDie(targetDataverseAlias);
            if (target == null) {
                return error(Response.Status.BAD_REQUEST, "Target Dataverse not found.");
            }
            execCommand(new MoveDataverseCommand(
                    createDataverseRequest(u), dv, target, force
            ));
            return ok("Dataverse moved successfully");
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @PUT
    @Path("{linkedDataverseAlias}/link/{linkingDataverseAlias}")
    public Response linkDataverse(@PathParam("linkedDataverseAlias") String linkedDataverseAlias, @PathParam("linkingDataverseAlias") String linkingDataverseAlias) {
        try {
            User u = findUserOrDie();
            Dataverse linked = findDataverseOrDie(linkedDataverseAlias);
            Dataverse linking = findDataverseOrDie(linkingDataverseAlias);
            if (linked == null) {
                return error(Response.Status.BAD_REQUEST, "Linked Dataverse not found.");
            }
            if (linking == null) {
                return error(Response.Status.BAD_REQUEST, "Linking Dataverse not found.");
            }
            execCommand(new LinkDataverseCommand(
                    createDataverseRequest(u), linking, linked
            ));
            return ok("Dataverse " + linked.getAlias() + " linked successfully to " + linking.getAlias());
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

}
