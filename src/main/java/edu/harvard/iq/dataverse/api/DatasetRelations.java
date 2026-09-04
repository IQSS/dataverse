package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.api.dto.DatasetRelationDTO;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.datasetrelation.DatasetRelation;
import edu.harvard.iq.dataverse.datasetrelation.DatasetRelationType;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetRelationCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetRelationTypeCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetRelationCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetRelationTypeCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetLatestAccessibleDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ReplaceDatasetRelationsCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDefaultDatasetRelationTypeCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParsingException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.toJsonArray;

/** API resource for dataset relations and dataset relation types. */
@Path("datasets")
public class DatasetRelations extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(DatasetRelations.class.getCanonicalName());

    @PUT
    @AuthRequired
    @Path("{identifier}/relations")
    public Response replaceDatasetRelations(@Context ContainerRequestContext crc, String body,
            @PathParam("identifier") String id, @QueryParam("version") String versionNumber) {
        return response(req -> {
            try {
                Dataset dataset = findDatasetOrDie(id);
                DatasetVersion version = editableVersionOrResponse(req, crc, dataset, versionNumber);
                if (version == null) {
                    return forbidden(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.editForbidden"));
                }
                List<DatasetRelationDTO> relations = parseReplaceRequest(body);
                List<DatasetRelation> replaced = execCommand(new ReplaceDatasetRelationsCommand(version, relations, req,
                        versionNumber != null));
                return ok(replaced.stream().map(relation -> json(relation, dataset, false)).collect(toJsonArray()));
            } catch (WrappedResponse ex) {
                return ex.getResponse();
            } catch (JsonParsingException ex) {
                return error(Status.BAD_REQUEST, MessageFormat.format(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.jsonparse"), ex.getMessage()));
            } catch (JsonParseException ex) {
                return error(Status.BAD_REQUEST, MessageFormat.format(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.jsonparsetodatasetrelation"), ex.getMessage()));
            }
        }, getRequestUser(crc));
    }

    @POST
    @AuthRequired
    @Path("{identifier}/relations")
    public Response createSingleDatasetRelation(@Context ContainerRequestContext crc, String body,
            @PathParam("identifier") String id, @QueryParam("version") String versionNumber) {
        return response(req -> {
            try {
                DatasetRelationDTO relation = parseAddRequest(body);
                Dataset dataset = findDatasetOrDie(id);
                DatasetVersion version;
                if (versionNumber != null) {
                    version = editableVersionOrResponse(req, crc, dataset, versionNumber);
                    if (version == null) {
                        return forbidden(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.editForbidden"));
                    }
                } else {
                    boolean updateDraft = dataset.getLatestVersion().isDraft();
                    version = dataset.getOrCreateEditVersion();
                    if (!updateDraft) {
                        dataset = execCommand(new UpdateDatasetVersionCommand(dataset, req));
                        version = dataset.getLatestVersion();
                    }
                }
                DatasetRelation created = execCommand(new CreateDatasetRelationCommand(version, relation, req));
                return ok(json(created, dataset, false));
            } catch (WrappedResponse ex) {
                return ex.getResponse();
            } catch (JsonParsingException ex) {
                return error(Status.BAD_REQUEST, MessageFormat.format(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.jsonparse"), ex.getMessage()));
            } catch (JsonParseException ex) {
                return error(Status.BAD_REQUEST, MessageFormat.format(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.jsonparsetodatasetrelation"), ex.getMessage()));
            }
        }, getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/relations/{id}")
    public Response getSingleRelation(@Context ContainerRequestContext crc, @PathParam("identifier") String datasetId,
            @PathParam("id") String id, @QueryParam("includeMetadataBlocks") boolean includeMetadataBlocks) {
        return response(req -> {
            User user = getRequestUser(crc);
            Dataset dataset = findDatasetOrDie(datasetId);
            DatasetRelation relation = findDatasetRelationOrDie(id, datasetId, false);
            if (!relation.getDefinitionPoint().isReleased() && !permissionSvc.hasPermissionsFor(user,
                    relation.getDefinitionPoint().getDataset(), EnumSet.of(Permission.ViewUnpublishedDataset))) {
                return forbidden(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.viewForbidden"));
            }
            return ok(json(relation, dataset, includeMetadataBlocks));
        }, getRequestUser(crc));
    }

    @DELETE
    @AuthRequired
    @Path("{identifier}/relations/{id}")
    public Response deleteSingleRelation(@Context ContainerRequestContext crc, @PathParam("identifier") String datasetId,
            @PathParam("id") String id) {
        return response(req -> {
            try {
                DatasetRelation relation = findDatasetRelationOrDie(id, datasetId, true);
                execCommand(new DeleteDatasetRelationCommand(req, relation));
                return ok(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.delete.success"));
            } catch (WrappedResponse ex) {
                return ex.getResponse();
            }
        }, getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/relations")
    public Response listRelations(@Context ContainerRequestContext crc, @PathParam("identifier") String id,
            @QueryParam("includeMetadataBlocks") boolean includeMetadataBlocks, @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset, @QueryParam("type") List<String> relationTypeNames,
            @QueryParam("datasetType") List<String> datasetTypeNames, @QueryParam("source") List<String> relationSources,
            @QueryParam("version") String versionNumber, @QueryParam("showFacets") boolean showFacets) {
        return response(req -> {
            try {
                Dataset dataset = findDatasetOrDie(id);
                DatasetVersion version = accessibleVersionOrResponse(req, dataset, versionNumber);
                if (version == null) {
                    return notFound(MessageFormat.format(BundleUtil.getStringFromBundle("datasets.api.datasetRelation.error.datasetVersionNotFound"), dataset.getGlobalId().asString()));
                }
                int effectiveLimit = limit != null ? limit : 10;
                int effectiveOffset = offset != null ? offset : 0;
                List<DatasetRelation> relations = datasetRelationSvc.getDatasetRelationsFor(dataset, version, relationTypeNames,
                        datasetTypeNames, relationSources, effectiveLimit, effectiveOffset);
                long totalCount = datasetRelationSvc.getTotalDatasetRelationCountFor(dataset, version, relationTypeNames,
                        datasetTypeNames, relationSources);
                JsonObjectBuilder data = JsonUtil.createObjectBuilder()
                        .add("items", json(relations, dataset, includeMetadataBlocks));
                if (showFacets) {
                    data.add("facets", JsonUtil.createObjectBuilder()
                            // A facet omits its own filter so clients can offer alternative values.
                            .add("relationType", jsonFacetCounts(datasetRelationSvc.getDatasetRelationFacetCountsFor(dataset, version,
                                    "relationType", null, datasetTypeNames, relationSources)))
                            .add("datasetType", jsonFacetCounts(datasetRelationSvc.getDatasetRelationFacetCountsFor(dataset, version,
                                    "datasetType", relationTypeNames, null, relationSources))));
                }
                return Response.ok(JsonUtil.createObjectBuilder()
                        .add("status", ApiConstants.STATUS_OK)
                        .add("data", data)
                        .add("totalCount", totalCount)
                        .build()).type("application/json").build();
            } catch (WrappedResponse ex) {
                return ex.getResponse();
            }
        }, getRequestUser(crc));
    }

    private JsonArray jsonFacetCounts(List<Object[]> counts) {
        return counts.stream().map(count -> new NullSafeJsonBuilder()
                .add("name", count[0] != null ? count[0].toString() : null)
                .add("displayName", count[1] != null ? count[1].toString() : null)
                .add("description", count[2] != null ? count[2].toString() : null)
                .add("count", ((Number) count[3]).longValue()))
                .collect(toJsonArray()).build();
    }

    @POST
    @AuthRequired
    @Path("relationTypes")
    public Response addRelationType(@Context ContainerRequestContext crc, String jsonIn) {
        return response(req -> {
            JsonObject relationTypeJson = JsonUtil.getJsonObject(jsonIn);
            DatasetRelationType relationType = new DatasetRelationType(relationTypeJson.getString("name", null),
                    relationTypeJson.getString("displayName", null), relationTypeJson.getString("description", null));
            if (relationTypeJson.containsKey("inverse") && relationTypeJson.get("inverse").getValueType() == JsonValue.ValueType.OBJECT) {
                JsonObject inverse = relationTypeJson.getJsonObject("inverse");
                String inverseName = inverse.getString("name", null);
                if (inverseName != null && !inverseName.isEmpty()) {
                    if (inverseName.equals(relationType.getName())) {
                        relationType.setInverse(relationType);
                    } else {
                        new DatasetRelationType(inverseName, inverse.getString("displayName", null),
                                inverse.getString("description", null), relationType);
                    }
                }
            }
            try {
                execCommand(new CreateDatasetRelationTypeCommand(req, relationType));
                return ok(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.create.success"));
            } catch (WrappedResponse ex) {
                return ex.getResponse();
            }
        }, getRequestUser(crc));
    }

    @DELETE
    @AuthRequired
    @Path("relationTypes/{idOrName}")
    public Response deleteRelationType(@Context ContainerRequestContext crc, @PathParam("idOrName") String idOrName) {
        return response(req -> {
            try {
                execCommand(new DeleteDatasetRelationTypeCommand(req, findDatasetRelationTypeOrDie(idOrName)));
                return ok(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.delete.success"));
            } catch (WrappedResponse ex) {
                return ex.getResponse();
            }
        }, getRequestUser(crc));
    }

    @GET
    @Path("relationTypes")
    public Response getRelationTypes() {
        return ok(datasetRelationTypeSvc.listAll().stream().map(type -> json(type)).collect(toJsonArray()));
    }

    @GET
    @Path("relationTypes/{idOrName}")
    public Response getRelationType(@PathParam("idOrName") String idOrName) {
        try {
            return ok(json(findDatasetRelationTypeOrDie(idOrName)));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @GET
    @Path("relationTypes/defaultRelationType")
    public Response getDefaultRelationType() {
        DatasetRelationType relationType = datasetRelationTypeSvc.getDefault();
        return relationType == null
                ? notFound(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.error.default.notFound"))
                : ok(json(relationType));
    }

    @PUT
    @AuthRequired
    @Path("relationTypes/defaultRelationType/{idOrName}")
    public Response setDefaultRelationType(@Context ContainerRequestContext crc, @PathParam("idOrName") String idOrName) {
        return response(req -> {
            try {
                execCommand(new UpdateDefaultDatasetRelationTypeCommand(req, findDatasetRelationTypeOrDie(idOrName)));
                return ok(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.default.success"));
            } catch (WrappedResponse ex) {
                return ex.getResponse();
            }
        }, getRequestUser(crc));
    }

    private DatasetVersion editableVersionOrResponse(edu.harvard.iq.dataverse.engine.command.DataverseRequest request,
            ContainerRequestContext crc, Dataset dataset, String versionNumber) throws WrappedResponse {
        if (versionNumber == null) {
            return dataset.getLatestVersion();
        }
        return getRequestUser(crc).isSuperuser()
                ? findDatasetVersionOrDie(request, versionNumber, dataset, false, false) : null;
    }

    private DatasetVersion accessibleVersionOrResponse(edu.harvard.iq.dataverse.engine.command.DataverseRequest request,
            Dataset dataset, String versionNumber) throws WrappedResponse {
        return versionNumber != null ? findDatasetVersionOrDie(request, versionNumber, dataset, false, false)
                : execCommand(new GetLatestAccessibleDatasetVersionCommand(request, dataset, false, false));
    }

    private List<DatasetRelationDTO> parseReplaceRequest(String body) throws JsonParsingException, JsonParseException {
        try {
            List<DatasetRelationDTO> relations = new ArrayList<>();
            JsonArray json = JsonUtil.getJsonArray(body);
            for (JsonObject relation : json.getValuesAs(JsonObject.class)) {
                relations.add(jsonParser().parseDatasetRelationDTO(relation));
            }
            return relations;
        } catch (JsonParsingException ex) {
            logger.log(Level.SEVERE, "Json: {0}", body);
            throw ex;
        }
    }

    private DatasetRelationDTO parseAddRequest(String body) throws JsonParsingException, JsonParseException {
        try {
            return jsonParser().parseDatasetRelationDTO(JsonUtil.getJsonObject(body));
        } catch (JsonParsingException ex) {
            logger.log(Level.SEVERE, "Json: {0}", body);
            throw ex;
        }
    }
}
