package edu.harvard.iq.dataverse.api;

import com.google.common.collect.Lists;
import com.google.api.client.util.ArrayMap;
import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.api.datadeposit.SwordServiceBean;
import edu.harvard.iq.dataverse.api.dto.*;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.api.imports.ImportException;
import edu.harvard.iq.dataverse.api.imports.ImportServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.dataverse.DataverseUtil;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItemServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.*;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.ConstraintViolationUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import static edu.harvard.iq.dataverse.util.StringUtil.nonEmpty;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.*;

import edu.harvard.iq.dataverse.util.json.*;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.json.*;
import jakarta.json.JsonValue.ValueType;
import jakarta.json.stream.JsonParsingException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import javax.xml.stream.XMLStreamException;

/**
 * A REST API for dataverses.
 *
 * @author michael
 */
@Stateless
@Path("dataverses")
@Tag(name = "Dataverses", description = "Dataverse collection metadata, datasets, roles, groups, links, templates, storage, and featured content.")
public class Dataverses extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Dataverses.class.getCanonicalName());

    @EJB
    ExplicitGroupServiceBean explicitGroupSvc;

    @EJB
    ImportServiceBean importService;
    
    @EJB
    SettingsServiceBean settingsService;
    
    @EJB
    GuestbookResponseServiceBean guestbookResponseService;
    
    @EJB
    GuestbookServiceBean guestbookService;
    
    @EJB
    DataverseServiceBean dataverseService;

    @EJB
    DataverseLinkingServiceBean linkingService;

    @EJB
    FeaturedDataverseServiceBean featuredDataverseService;

    @EJB
    SwordServiceBean swordService;

    @EJB
    PermissionServiceBean permissionService;
    
    @EJB
    TemplateServiceBean templateService;

    @EJB
    DataverseFeaturedItemServiceBean dataverseFeaturedItemServiceBean;
    
    @POST
    @AuthRequired
    @Operation(summary = "Create the root dataverse",
            description = "Creates the root dataverse from a Dataverse JSON payload.")
    public Response addRoot(@Context ContainerRequestContext crc,
            @RequestBody(description = "Dataverse JSON for the root collection.")
            String body) {
        logger.info("Creating root dataverse");
        return addDataverse(crc, body, "");
    }

    @POST
    @AuthRequired
    @Path("{identifier}")
    @Operation(summary = "Create a child dataverse",
            description = "Creates a dataverse under the selected parent and returns the created collection metadata.")
    public Response addDataverse(@Context ContainerRequestContext crc,
            @RequestBody(description = "Dataverse JSON including alias, name, contact, metadata block, input level, and facet settings.")
            String body,
            @Parameter(description = "Parent dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String parentIdtf) {
        Dataverse newDataverse;
        try {
            newDataverse = parseAndValidateAddDataverseRequestBody(body);
        } catch (JsonParsingException jpe) {
            return error(Status.BAD_REQUEST, MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.create.error.jsonparse"), jpe.getMessage()));
        } catch (JsonParseException ex) {
            return error(Status.BAD_REQUEST, MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.create.error.jsonparsetodataverse"), ex.getMessage()));
        }

        try {
            List<DataverseFieldTypeInputLevel> inputLevels = parseInputLevels(body, newDataverse);
            List<MetadataBlock> metadataBlocks = parseMetadataBlocks(body);
            List<DatasetFieldType> facets = parseFacets(body);

            if (!parentIdtf.isEmpty()) {
                Dataverse owner = findDataverseOrDie(parentIdtf);
                newDataverse.setOwner(owner);
            }

            AuthenticatedUser u = getRequestAuthenticatedUserOrDie(crc);
            newDataverse = execCommand(new CreateDataverseCommand(newDataverse, createDataverseRequest(u), facets, inputLevels, metadataBlocks, true));
            return created("/dataverses/" + newDataverse.getAlias(), json(newDataverse));

        } catch (WrappedResponse ww) {
            return handleWrappedResponse(ww);
        } catch (EJBException ex) {
            return handleEJBException(ex, "Error creating dataverse.");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error creating dataverse", ex);
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Error creating dataverse: " + ex.getMessage());
        }
    }

    private Dataverse parseAndValidateAddDataverseRequestBody(String body) throws JsonParsingException, JsonParseException {
        try {
            JsonObject addDataverseJson = JsonUtil.getJsonObject(body);
            return jsonParser().parseDataverse(addDataverseJson);
        } catch (JsonParsingException jpe) {
            logger.log(Level.SEVERE, "Json: {0}", body);
            throw jpe;
        } catch (JsonParseException ex) {
            logger.log(Level.SEVERE, "Error parsing dataverse from json: " + ex.getMessage(), ex);
            throw ex;
        }
    }

    @PUT
    @AuthRequired
    @Path("{identifier}")
    @Operation(summary = "Revise a dataverse",
            description = "Updates dataverse metadata and optional metadata block, input level, and facet settings.")
    public Response updateDataverse(@Context ContainerRequestContext crc,
            @RequestBody(description = "Dataverse update JSON with metadata and optional configuration sections.")
            String body,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String identifier) {
        Dataverse dataverse;
        try {
            dataverse = findDataverseOrDie(identifier);
        } catch (WrappedResponse e) {
            return e.getResponse();
        }

        DataverseDTO updatedDataverseDTO;
        try {
            updatedDataverseDTO = parseAndValidateUpdateDataverseRequestBody(body);
        } catch (JsonParsingException jpe) {
            return error(Status.BAD_REQUEST, MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.create.error.jsonparse"), jpe.getMessage()));
        } catch (JsonParseException ex) {
            return error(Status.BAD_REQUEST, MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.create.error.jsonparsetodataverse"), ex.getMessage()));
        }

        try {
            List<DataverseFieldTypeInputLevel> inputLevels = parseInputLevels(body, dataverse);
            List<MetadataBlock> metadataBlocks = parseMetadataBlocks(body);
            List<DatasetFieldType> facets = parseFacets(body);

            AuthenticatedUser u = getRequestAuthenticatedUserOrDie(crc);
            dataverse = execCommand(new UpdateDataverseCommand(dataverse, facets, null, createDataverseRequest(u), inputLevels, metadataBlocks, updatedDataverseDTO));
            return ok(json(dataverse));

        } catch (WrappedResponse ww) {
            return handleWrappedResponse(ww);
        } catch (EJBException ex) {
            return handleEJBException(ex, "Error updating dataverse.");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error updating dataverse", ex);
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Error updating dataverse: " + ex.getMessage());
        }
    }

    private DataverseDTO parseAndValidateUpdateDataverseRequestBody(String body) throws JsonParsingException, JsonParseException {
        try {
            JsonObject updateDataverseJson = JsonUtil.getJsonObject(body);
            return jsonParser().parseDataverseDTO(updateDataverseJson);
        } catch (JsonParsingException jpe) {
            logger.log(Level.SEVERE, "Json: {0}", body);
            throw jpe;
        } catch (JsonParseException ex) {
            logger.log(Level.SEVERE, "Error parsing DataverseDTO from json: " + ex.getMessage(), ex);
            throw ex;
        }
    }

    /*
    return null - ignore
    return empty list - delete and inherit from parent
    return non-empty list - update
    */
    private List<DataverseFieldTypeInputLevel> parseInputLevels(String body, Dataverse dataverse) throws WrappedResponse {
        JsonObject metadataBlocksJson = getMetadataBlocksJson(body);
        JsonArray inputLevelsArray = metadataBlocksJson != null ? metadataBlocksJson.getJsonArray("inputLevels") : null;

        if (metadataBlocksJson != null && metadataBlocksJson.containsKey("inheritMetadataBlocksFromParent") && metadataBlocksJson.getBoolean("inheritMetadataBlocksFromParent")) {
            return Lists.newArrayList(); // delete
        }
        return parseInputLevels(inputLevelsArray, dataverse);
    }

    /*
    return null - ignore
    return empty list - delete and inherit from parent
    return non-empty list - update
    */
    private List<MetadataBlock> parseMetadataBlocks(String body) throws WrappedResponse {
        JsonObject metadataBlocksJson = getMetadataBlocksJson(body);
        JsonArray metadataBlocksArray = metadataBlocksJson != null ? metadataBlocksJson.getJsonArray("metadataBlockNames") : null;

        if (metadataBlocksArray != null && metadataBlocksJson.containsKey("inheritMetadataBlocksFromParent") && metadataBlocksJson.getBoolean("inheritMetadataBlocksFromParent")) {
            String errorMessage = MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.metadatablocks.error.containslistandinheritflag"), "metadataBlockNames", "inheritMetadataBlocksFromParent");
            throw new WrappedResponse(badRequest(errorMessage));
        }
        if (metadataBlocksJson != null && metadataBlocksJson.containsKey("inheritMetadataBlocksFromParent") && metadataBlocksJson.getBoolean("inheritMetadataBlocksFromParent")) {
            return Lists.newArrayList(); // delete and inherit from parent
        }

        return parseNewDataverseMetadataBlocks(metadataBlocksArray);
    }

    /*
    return null - ignore
    return empty list - delete and inherit from parent
    return non-empty list - update
    */
    private List<DatasetFieldType> parseFacets(String body) throws WrappedResponse {
        JsonObject metadataBlocksJson = getMetadataBlocksJson(body);
        JsonArray facetsArray = metadataBlocksJson != null ? metadataBlocksJson.getJsonArray("facetIds") : null;

        if (facetsArray != null && metadataBlocksJson.containsKey("inheritFacetsFromParent") && metadataBlocksJson.getBoolean("inheritFacetsFromParent")) {
            String errorMessage = MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.metadatablocks.error.containslistandinheritflag"), "facetIds", "inheritFacetsFromParent");
            throw new WrappedResponse(badRequest(errorMessage));
        }

        if (metadataBlocksJson != null && metadataBlocksJson.containsKey("inheritFacetsFromParent") && metadataBlocksJson.getBoolean("inheritFacetsFromParent")) {
            return Lists.newArrayList(); // delete and inherit from parent
        }

        return parseFacets(facetsArray);
    }

    private JsonObject getMetadataBlocksJson(String body) {
        JsonObject dataverseJson = JsonUtil.getJsonObject(body);
        return dataverseJson.getJsonObject("metadataBlocks");
    }

    private Response handleWrappedResponse(WrappedResponse ww) {
        String error = ConstraintViolationUtil.getErrorStringForConstraintViolations(ww.getCause());
        if (!error.isEmpty()) {
            logger.log(Level.INFO, error);
            return ww.refineResponse(error);
        }
        return ww.getResponse();
    }

    private Response handleEJBException(EJBException ex, String action) {
        Throwable cause = ex;
        StringBuilder sb = new StringBuilder();
        sb.append(action);
        while (cause.getCause() != null) {
            cause = cause.getCause();
            if (cause instanceof ConstraintViolationException) {
                sb.append(ConstraintViolationUtil.getErrorStringForConstraintViolations(cause));
            }
        }
        logger.log(Level.SEVERE, sb.toString());
        return error(Response.Status.INTERNAL_SERVER_ERROR, sb.toString());
    }

    private List<MetadataBlock> parseNewDataverseMetadataBlocks(JsonArray metadataBlockNamesArray) throws WrappedResponse {
        if (metadataBlockNamesArray == null) {
            return null;
        }
        List<MetadataBlock> selectedMetadataBlocks = new ArrayList<>();
        for (JsonString metadataBlockName : metadataBlockNamesArray.getValuesAs(JsonString.class)) {
            MetadataBlock metadataBlock = metadataBlockSvc.findByName(metadataBlockName.getString());
            if (metadataBlock == null) {
                String errorMessage = MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.metadatablocks.error.invalidmetadatablockname"), metadataBlockName);
                throw new WrappedResponse(badRequest(errorMessage));
            }
            selectedMetadataBlocks.add(metadataBlock);
        }

        return selectedMetadataBlocks;
    }

    @POST
    @AuthRequired
    @Path("{identifier}/validateDatasetJson")
    @Consumes("application/json")
    @Operation(summary = "Validate dataset JSON",
            description = "Validates a dataset JSON payload against the selected dataverse.")
    public Response validateDatasetJson(@Context ContainerRequestContext crc,
            @RequestBody(description = "Dataset JSON to validate.")
            String body,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String idtf) {
        User u = getRequestUser(crc);
        try {
            String validationMessage = execCommand(new ValidateDatasetJsonCommand(createDataverseRequest(u), findDataverseOrDie(idtf), body));
            return ok(validationMessage);
        } catch (WrappedResponse ex) {
            Logger.getLogger(Dataverses.class.getName()).log(Level.SEVERE, null, ex);
            return ex.getResponse();
        }
    }
    
    @GET
    @AuthRequired
    @Path("{identifier}/datasetSchema")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read dataset schema for a dataverse",
            description = "Returns the dataset JSON schema generated for the selected dataverse.")
    public Response getDatasetSchema(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String idtf) {
        DataverseRequest req = createDataverseRequest(getRequestUser(crc));

        try {
            String datasetSchema = execCommand(new GetDatasetSchemaCommand(req, findDataverseUserCanSeeOrDie(idtf, req)));
            JsonObject jsonObject = JsonUtil.getJsonObject(datasetSchema);
            return Response.ok(jsonObject).build();
        } catch (WrappedResponse ex) {
            Logger.getLogger(Dataverses.class.getName()).log(Level.SEVERE, null, ex);
            return ex.getResponse();
        }
    }
            
    

    @POST
    @AuthRequired
    @Path("{identifier}/datasets")
    @Consumes("application/json")
    @Operation(summary = "Create a dataset from JSON",
            description = "Creates a draft dataset in the selected dataverse and returns its id and persistent identifier.")
    public Response createDataset(@Context ContainerRequestContext crc,
            @RequestBody(description = "Dataset JSON containing at least one dataset version.")
            String jsonBody,
            @Parameter(description = "Dataverse alias, id, or persistent identifier for the dataset owner.", required = true)
            @PathParam("identifier") String parentIdtf,
            @Parameter(description = "When true and incomplete metadata is allowed, skip full metadata validation.")
            @QueryParam("doNotValidate") String doNotValidateParam) {
        try {
            logger.fine("Json is: " + jsonBody);
            User u = getRequestUser(crc);
            Dataverse owner = findDataverseOrDie(parentIdtf);
            Dataset ds = parseDataset(jsonBody);
            ds.setOwner(owner);
            // Will make validation happen always except for the (rare) occasion of all three conditions are true
            boolean validate = ! ( u.isAuthenticated() && StringUtil.isTrue(doNotValidateParam) &&
                JvmSettings.API_ALLOW_INCOMPLETE_METADATA.lookupOptional(Boolean.class).orElse(false) );

            if (ds.getVersions().isEmpty()) {
                return badRequest(BundleUtil.getStringFromBundle("dataverses.api.create.dataset.error.mustIncludeVersion"));
            }
            
            if (!ds.getFiles().isEmpty() && !u.isSuperuser()){
                return badRequest(BundleUtil.getStringFromBundle("dataverses.api.create.dataset.error.superuserFiles"));
            }

            //Throw BadRequestException if metadataLanguage isn't compatible with setting
            DataverseUtil.checkMetadataLangauge(ds, owner, settingsService.getBaseMetadataLanguageMap(null, true));

            // clean possible version metadata
            DatasetVersion version = ds.getVersions().get(0);

            if (!validate && (version.getDatasetAuthors().isEmpty() || version.getDatasetAuthors().stream().anyMatch(a -> a.getName() == null || a.getName().isEmpty()))) {
                return badRequest(BundleUtil.getStringFromBundle("dataverses.api.create.dataset.error.mustIncludeAuthorName"));
            }

            version.setMinorVersionNumber(null);
            version.setVersionNumber(null);
            version.setVersionState(DatasetVersion.VersionState.DRAFT);
            version.getTermsOfUseAndAccess().setFileAccessRequest(true);
            version.getTermsOfUseAndAccess().setDatasetVersion(version);

            ds.setAuthority(null);
            ds.setIdentifier(null);
            ds.setProtocol(null);
            ds.setGlobalIdCreateTime(null);
            Dataset managedDs;
            try {
                managedDs = execCommand(new CreateNewDatasetCommand(ds, createDataverseRequest(u), validate, true));
            } catch (WrappedResponse ww) {
                Throwable cause = ww.getCause();
                StringBuilder sb = new StringBuilder();
                if (cause == null) {
                    return ww.refineResponse("cause was null!");
                }
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                    if (cause instanceof ConstraintViolationException) {
                        sb.append(ConstraintViolationUtil.getErrorStringForConstraintViolations(cause));
                    }
                }
                String error = sb.toString();
                if (!error.isEmpty()) {
                    logger.log(Level.INFO, error);
                    return ww.refineResponse(error);
                }
                return ww.getResponse();
            }

            return created("/datasets/" + managedDs.getId(),
                    Json.createObjectBuilder()
                            .add("id", managedDs.getId())
                            .add("persistentId", managedDs.getGlobalId().asString())
            );

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    @POST
    @AuthRequired
    @Path("{identifier}/datasets")
    @Consumes("application/ld+json, application/json-ld")
    @Operation(summary = "Create a dataset from JSON-LD",
            description = "Creates a draft dataset in the selected dataverse from JSON-LD metadata.")
    public Response createDatasetFromJsonLd(@Context ContainerRequestContext crc,
            @RequestBody(description = "JSON-LD dataset metadata.")
            String jsonLDBody,
            @Parameter(description = "Dataverse alias, id, or persistent identifier for the dataset owner.", required = true)
            @PathParam("identifier") String parentIdtf) {
        try {
            User u = getRequestUser(crc);
            Dataverse owner = findDataverseOrDie(parentIdtf);
            Dataset ds = new Dataset();

            ds.setOwner(owner);
            ds = JSONLDUtil.updateDatasetMDFromJsonLD(ds, jsonLDBody, metadataBlockSvc, datasetFieldSvc, false, false, licenseSvc, datasetTypeSvc);
            
            ds.setOwner(owner);

            // clean possible dataset/version metadata
            DatasetVersion version = ds.getVersions().get(0);
            version.setMinorVersionNumber(null);
            version.setVersionNumber(null);
            version.setVersionState(DatasetVersion.VersionState.DRAFT);
            version.getTermsOfUseAndAccess().setFileAccessRequest(true);
            version.getTermsOfUseAndAccess().setDatasetVersion(version);

            ds.setAuthority(null);
            ds.setIdentifier(null);
            ds.setProtocol(null);
            ds.setGlobalIdCreateTime(null);
            
            //Throw BadRequestException if metadataLanguage isn't compatible with setting
            DataverseUtil.checkMetadataLangauge(ds, owner, settingsService.getBaseMetadataLanguageMap(null, true));

            Dataset managedDs = execCommand(new CreateNewDatasetCommand(ds, createDataverseRequest(u)));
            return created("/datasets/" + managedDs.getId(),
                    Json.createObjectBuilder()
                            .add("id", managedDs.getId())
                            .add("persistentId", managedDs.getGlobalId().asString())
            );

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        } catch (Exception ex) {
            return error(Status.BAD_REQUEST, ex.getLocalizedMessage());
        }
    }

    @POST
    @AuthRequired
    @Path("{identifier}/datasets/:import")
    @Operation(summary = "Import a dataset from JSON",
            description = "Imports a dataset into the selected dataverse, optionally assigning a supplied PID and release state.")
    public Response importDataset(@Context ContainerRequestContext crc,
            @RequestBody(description = "Dataset JSON to import.")
            String jsonBody,
            @Parameter(description = "Dataverse alias, id, or persistent identifier for the dataset owner.", required = true)
            @PathParam("identifier") String parentIdtf,
            @Parameter(description = "Persistent identifier to assign to the imported dataset.")
            @QueryParam("pid") String pidParam,
            @Parameter(description = "When true, import the dataset as released.")
            @QueryParam("release") String releaseParam) {
        try {
            User u = getRequestUser(crc);
            if (!u.isSuperuser()) {
                return error(Status.FORBIDDEN, "Not a superuser");
            }
            Dataverse owner = findDataverseOrDie(parentIdtf);
            Dataset ds = parseDataset(jsonBody);
            ds.setOwner(owner);

            if (ds.getVersions().isEmpty()) {
                return badRequest("Supplied json must contain a single dataset version.");
            }

            //Throw BadRequestException if metadataLanguage isn't compatible with setting
            DataverseUtil.checkMetadataLangauge(ds, owner, settingsService.getBaseMetadataLanguageMap(null, true));

            DatasetVersion version = ds.getVersions().get(0);
            if (version.getVersionState() == null) {
                version.setVersionState(DatasetVersion.VersionState.DRAFT);
            }

            if (nonEmpty(pidParam)) {
                if (!GlobalId.verifyImportCharacters(pidParam)) {
                    return badRequest("PID parameter contains characters that are not allowed by the Dataverse application. On import, the PID must only contain characters specified in this regex: " + BundleUtil.getStringFromBundle("pid.allowedCharacters"));
                }
                Optional<GlobalId> maybePid = PidProvider.parse(pidParam);
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

            PidProvider pidProvider = PidUtil.getPidProvider(ds.getGlobalId().getProviderId());
            if (pidProvider == null || !pidProvider.canManagePID()) {
                return badRequest("Cannot import a dataset that has a PID that doesn't match the server's settings");
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
                    .add("persistentId", managedDs.getGlobalId().asString());

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
    @AuthRequired
    @Path("{identifier}/datasets/:importddi")
    @Operation(summary = "Import a dataset from DDI XML",
            description = "Converts DDI XML to dataset metadata and imports it into the selected dataverse.")
    public Response importDatasetDdi(@Context ContainerRequestContext crc,
            @RequestBody(description = "DDI XML document to import.")
            String xml,
            @Parameter(description = "Dataverse alias, id, or persistent identifier for the dataset owner.", required = true)
            @PathParam("identifier") String parentIdtf,
            @Parameter(description = "Persistent identifier to assign to the imported dataset.")
            @QueryParam("pid") String pidParam,
            @Parameter(description = "When true, import the dataset as released.")
            @QueryParam("release") String releaseParam) {
        try {
            User u = getRequestUser(crc);
            if (!u.isSuperuser()) {
                return error(Status.FORBIDDEN, "Not a superuser");
            }
            Dataverse owner = findDataverseOrDie(parentIdtf);
            Dataset ds = null;
            try {
                ds = jsonParser().parseDataset(importService.ddiToJson(xml));
                DataverseUtil.checkMetadataLangauge(ds, owner, settingsService.getBaseMetadataLanguageMap(null, true));
            } catch (JsonParseException jpe) {
                return badRequest("Error parsing data as Json: "+jpe.getMessage());
            } catch (ImportException e) {
                return badRequest("Invalid DOI found in the XML: "+e.getMessage());
            } catch (XMLStreamException e) {
                return badRequest("Invalid file content: "+e.getMessage());
            }

            swordService.addDatasetSubjectIfMissing(ds.getLatestVersion());

            ds.setOwner(owner);
            if (nonEmpty(pidParam)) {
                if (!GlobalId.verifyImportCharacters(pidParam)) {
                    return badRequest("PID parameter contains characters that are not allowed by the Dataverse application. On import, the PID must only contain characters specified in this regex: " + BundleUtil.getStringFromBundle("pid.allowedCharacters"));
                }
                Optional<GlobalId> maybePid = PidProvider.parse(pidParam);
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
                    .add("persistentId", managedDs.getGlobalId().toString());

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

    @POST
    @AuthRequired
    @Path("{identifier}/datasets/:startmigration")
    @Consumes("application/ld+json, application/json-ld")
    @Operation(summary = "Start migrated dataset recreation",
            description = "Creates a draft dataset from published JSON-LD metadata for a migration workflow.")
    public Response recreateDataset(@Context ContainerRequestContext crc,
            @RequestBody(description = "Published dataset JSON-LD metadata to recreate as a draft.")
            String jsonLDBody,
            @Parameter(description = "Dataverse alias, id, or persistent identifier for the dataset owner.", required = true)
            @PathParam("identifier") String parentIdtf) {
        try {
            User u = getRequestUser(crc);
            if (!u.isSuperuser()) {
                return error(Status.FORBIDDEN, "Not a superuser");
            }
            Dataverse owner = findDataverseOrDie(parentIdtf);
            
            Dataset ds = new Dataset();

            ds.setOwner(owner);
            ds = JSONLDUtil.updateDatasetMDFromJsonLD(ds, jsonLDBody, metadataBlockSvc, datasetFieldSvc, false, true, licenseSvc, datasetTypeSvc);
          //ToDo - verify PID is one Dataverse can manage (protocol/authority/shoulder match)
          if (!PidUtil.getPidProvider(ds.getGlobalId().getProviderId()).canManagePID()) {
              throw new BadRequestException(
                      "Cannot recreate a dataset that has a PID that doesn't match the server's settings");
          }
            if(!dvObjectSvc.isGlobalIdLocallyUnique(ds.getGlobalId())) {
                throw new BadRequestException("Cannot recreate a dataset whose PID is already in use");
            }
            
            //Throw BadRequestException if metadataLanguage isn't compatible with setting
            DataverseUtil.checkMetadataLangauge(ds, owner, settingsService.getBaseMetadataLanguageMap(null, true));


            if (ds.getVersions().isEmpty()) {
                return badRequest("Supplied json must contain a single dataset version.");
            }

            DatasetVersion version = ds.getVersions().get(0);
            if (!version.isPublished()) {
                throw new BadRequestException("Cannot recreate a dataset that hasn't been published.");
            }
            //While the datasetversion whose metadata we're importing has been published, we consider it in draft until the API caller adds files and then completes the migration
            version.setVersionState(DatasetVersion.VersionState.DRAFT);

            DataverseRequest request = createDataverseRequest(u);

            Dataset managedDs = execCommand(new ImportDatasetCommand(ds, request));
            JsonObjectBuilder responseBld = Json.createObjectBuilder()
                    .add("id", managedDs.getId())
                    .add("persistentId", managedDs.getGlobalId().toString());

            return created("/datasets/" + managedDs.getId(), responseBld);

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    private Dataset parseDataset(String datasetJson) throws WrappedResponse {
        try {
            return jsonParser().parseDataset(JsonUtil.getJsonObject(datasetJson));
        } catch (JsonParsingException | JsonParseException jpe) {
            String message = jpe.getLocalizedMessage();
            logger.log(Level.SEVERE, "Error parsing dataset JSON. message: {0}", message);
            logger.log(Level.SEVERE, "Error parsing dataset json. Json: {0}", datasetJson);
            throw new WrappedResponse(error(Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage()));
        }
    }

    @GET
    @AuthRequired
    @Path("{identifier}")
    @Operation(summary = "Read a dataverse",
            description = "Returns dataverse metadata with optional owner and child-count information.")
    public Response getDataverse(@Context ContainerRequestContext crc,
                                 @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
                                 @PathParam("identifier") String idtf,
                                 @Parameter(description = "Whether to include owner information.")
                                 @QueryParam("returnOwners") boolean returnOwners,
                                 @Parameter(description = "Whether to include the number of child objects.")
                                 @QueryParam("returnChildCount") boolean returnChildCount,
                                 @Parameter(description = "Whether to include email fields when the caller has permission to ignore the email-exclusion setting.")
                                 @QueryParam("ignoreSettingExcludeEmailFromExport") Boolean ignoreSettingToExcludeEmailFromExport) {
        return response(req -> {
            Dataverse dataverse = execCommand(new GetDataverseCommand(req, findDataverseUserCanSeeOrDie(idtf, req)));
            boolean hideEmail = settingsService.isTrueForKey(SettingsServiceBean.Key.ExcludeEmailFromExport, false);

            // Check to see if the caller wants to ignore the ExcludeEmailFromExport setting and that they have permission to do so
            boolean ignoreSettingExcludeEmailFromExport = ignoreSettingToExcludeEmailFromExport != null ? ignoreSettingToExcludeEmailFromExport : false;
            if (hideEmail && ignoreSettingExcludeEmailFromExport && permissionService.userOn(getRequestUser(crc), dataverse).has(Permission.EditDataverse)) {
                hideEmail = false;
            }

            return ok(json(dataverse, hideEmail, returnOwners, false, returnChildCount ? dataverseService.getChildCount(dataverse) : null));
        }, getRequestUser(crc));
    }

    @DELETE
    @AuthRequired
    @Path("{identifier}")
    @Operation(summary = "Remove a dataverse",
            description = "Deletes the selected dataverse collection.")
    public Response deleteDataverse(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String idtf) {
        return response(req -> {
            execCommand(new DeleteDataverseCommand(req, findDataverseOrDie(idtf)));
            return ok("Dataverse " + idtf + " deleted");
        }, getRequestUser(crc));
    }

    /**
     * Endpoint to change attributes of a Dataverse collection.
     *
     * @apiNote Example curl command:
     *          <code>curl -X PUT -d "test" http://localhost:8080/api/dataverses/$ALIAS/attribute/alias</code>
     *          to change the alias of the collection named $ALIAS to "test".
     */
    @PUT
    @AuthRequired
    @Path("{identifier}/attribute/{attribute}")
    @Operation(summary = "Change a dataverse attribute",
            description = "Updates one supported dataverse attribute value.")
    public Response updateAttribute(@Context ContainerRequestContext crc,
                                    @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
                                    @PathParam("identifier") String identifier,
                                    @Parameter(description = "Dataverse attribute name to change.", required = true)
                                    @PathParam("attribute") String attribute,
                                    @Parameter(description = "New attribute value.")
                                    @QueryParam("value") String value) {
        try {
            Dataverse dataverse = findDataverseOrDie(identifier);
            Object formattedValue = formatAttributeValue(attribute, value);
            dataverse = execCommand(new UpdateDataverseAttributeCommand(createDataverseRequest(getRequestUser(crc)), dataverse, attribute, formattedValue));
            return ok("Update successful", JsonPrinter.json(dataverse));
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }

    private Object formatAttributeValue(String attribute, String value) throws WrappedResponse {
        if (List.of("filePIDsEnabled","requireFilesToPublishDataset").contains(attribute)) {
            return parseBooleanOrDie(value);
        }
        return value;
    }

    @GET
    @AuthRequired
    @Path("{identifier}/inputLevels")
    @Operation(summary = "Read dataverse input levels",
            description = "Returns dataset field input-level settings for a dataverse.")
    public Response getInputLevels(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String identifier) {
        try {
            DataverseRequest req = createDataverseRequest(getRequestUser(crc));
            Dataverse dataverse = findDataverseUserCanSeeOrDie(identifier, req);
            List<DataverseFieldTypeInputLevel> inputLevels = execCommand(new ListDataverseInputLevelsCommand(req, dataverse));
            return ok(jsonDataverseInputLevels(inputLevels));
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }

    @PUT
    @AuthRequired
    @Path("{identifier}/inputLevels")
    @Operation(summary = "Assign dataverse input levels",
            description = "Replaces dataset field input-level settings for a dataverse.")
    public Response updateInputLevels(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String identifier,
            @RequestBody(description = "JSON array of input-level settings for dataset fields.")
            String jsonBody) {
        try {
            Dataverse dataverse = findDataverseOrDie(identifier);
            List<DataverseFieldTypeInputLevel> newInputLevels = parseInputLevels(Json.createReader(new StringReader(jsonBody)).readArray(), dataverse);
            execCommand(new UpdateDataverseInputLevelsCommand(dataverse, createDataverseRequest(getRequestUser(crc)), newInputLevels));
            return ok(BundleUtil.getStringFromBundle("dataverse.update.success"), JsonPrinter.json(dataverse));
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }

    private List<DataverseFieldTypeInputLevel> parseInputLevels(JsonArray inputLevelsArray, Dataverse dataverse) throws WrappedResponse {
        if (inputLevelsArray == null) {
            return null;
        }
        List<DataverseFieldTypeInputLevel> newInputLevels = new ArrayList<>();
        for (JsonValue value : inputLevelsArray) {
            JsonObject inputLevel = (JsonObject) value;
            String datasetFieldTypeName = inputLevel.getString("datasetFieldTypeName");
            DatasetFieldType datasetFieldType = datasetFieldSvc.findByName(datasetFieldTypeName);

            if (datasetFieldType == null) {
                String errorMessage = MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.inputlevels.error.invalidfieldtypename"), datasetFieldTypeName);
                throw new WrappedResponse(badRequest(errorMessage));
            }

            boolean required = inputLevel.getBoolean("required");
            boolean include = inputLevel.getBoolean("include");
            Boolean displayOnCreate = null;
            if(inputLevel.containsKey("displayOnCreate")) {
                displayOnCreate = inputLevel.getBoolean("displayOnCreate", false);
            }

            if (required && !include) {
                String errorMessage = MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.inputlevels.error.cannotberequiredifnotincluded"), datasetFieldTypeName);
                throw new WrappedResponse(badRequest(errorMessage));
            }

            newInputLevels.add(new DataverseFieldTypeInputLevel(datasetFieldType, dataverse, required, include, displayOnCreate));
        }

        return newInputLevels;
    }

    private List<DatasetFieldType> parseFacets(JsonArray facetsArray) throws WrappedResponse {
        if (facetsArray == null) {
            return null;
        }
        List<DatasetFieldType> facets = new LinkedList<>();
        for (JsonString facetId : facetsArray.getValuesAs(JsonString.class)) {
            DatasetFieldType dsfType = findDatasetFieldType(facetId.getString());
            if (dsfType == null) {
                throw new WrappedResponse(badRequest(MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.facets.error.fieldtypenotfound"), facetId.getString())));
            } else if (!dsfType.isFacetable()) {
                throw new WrappedResponse(badRequest(MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.facets.error.fieldtypenotfacetable"), facetId.getString())));
            }
            facets.add(dsfType);
        }
        return facets;
    }

    @DELETE
    @AuthRequired
    @Path("{linkedDataverseId}/deleteLink/{linkingDataverseId}")
    @Operation(summary = "Remove a dataverse link",
            description = "Deletes a link from one dataverse to another linked dataverse.")
    public Response deleteDataverseLinkingDataverse(@Context ContainerRequestContext crc,
            @Parameter(description = "Alias or id of the dataverse containing the link.", required = true)
            @PathParam("linkingDataverseId") String linkingDataverseId,
            @Parameter(description = "Alias or id of the linked dataverse to remove.", required = true)
            @PathParam("linkedDataverseId") String linkedDataverseId) {
        boolean index = true;
        return response(req -> {
            execCommand(new DeleteDataverseLinkingDataverseCommand(req, findDataverseOrDie(linkingDataverseId), findDataverseLinkingDataverseOrDie(linkingDataverseId, linkedDataverseId), index));
            return ok("Link from Dataverse " + linkingDataverseId + " to linked Dataverse " + linkedDataverseId + " deleted");
        }, getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/metadatablocks")
    @Operation(summary = "Read dataverse metadata blocks",
            description = "Lists metadata blocks configured for a dataverse, optionally including dataset field types.")
    public Response listMetadataBlocks(@Context ContainerRequestContext crc,
                                       @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
                                       @PathParam("identifier") String dvIdtf,
                                       @Parameter(description = "When true, include only metadata blocks displayed on dataset creation.")
                                       @QueryParam("onlyDisplayedOnCreate") boolean onlyDisplayedOnCreate,
                                       @Parameter(description = "Whether to include dataset field type definitions.")
                                       @QueryParam("returnDatasetFieldTypes") boolean returnDatasetFieldTypes,
                                       @Parameter(description = "Dataset type name used to filter metadata blocks.")
                                       @QueryParam("datasetType") String datasetTypeIn) {
        try {
            DataverseRequest req = createDataverseRequest(getRequestUser(crc));
            Dataverse dataverse = findDataverseUserCanSeeOrDie(dvIdtf, req);
            DatasetType datasetType = datasetTypeSvc.getByName(datasetTypeIn);
            final List<MetadataBlock> metadataBlocks = execCommand(
                    new ListMetadataBlocksCommand(
                            req,
                            dataverse,
                            onlyDisplayedOnCreate,
                            datasetType
                    )
            );
            return ok(json(metadataBlocks, returnDatasetFieldTypes, onlyDisplayedOnCreate, dataverse, datasetType));
        } catch (WrappedResponse we) {
            return we.getResponse();
        }
    }

    @POST
    @AuthRequired
    @Path("{identifier}/metadatablocks")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Assign metadata blocks to a dataverse",
            description = "Replaces the metadata blocks configured directly on a dataverse.")
    public Response setMetadataBlocks(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @RequestBody(description = "JSON array of metadata block ids or names.")
            String blockIds) {

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
            execCommand(new UpdateDataverseMetadataBlocksCommand.SetBlocks(createDataverseRequest(getRequestUser(crc)), findDataverseOrDie(dvIdtf), blocks));
            return ok("Metadata blocks of dataverse " + dvIdtf + " updated.");

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @GET
    @AuthRequired
    @Path("{identifier}/metadatablocks/:isRoot")
    @Operation(summary = "Read metadata-block root status through legacy route",
            description = "Returns whether a dataverse is a metadata-block root using the legacy route.")
    public Response getMetadataRoot_legacy(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {
        return getMetadataRoot(crc, dvIdtf);
    }

    @GET
    @AuthRequired
    @Path("{identifier}/metadatablocks/isRoot")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read metadata-block root status",
            description = "Returns whether a dataverse is a metadata-block root.")
    public Response getMetadataRoot(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {
        return response(req -> {
            final Dataverse dataverse = findDataverseUserCanSeeOrDie(dvIdtf, req);
            if (permissionSvc.request(req)
                    .on(dataverse)
                    .has(Permission.EditDataverse)) {
                return ok(dataverse.isMetadataBlockRoot());
            } else {
                return error(Status.FORBIDDEN, "Not authorized");
            }
        }, getRequestUser(crc));
    }

    @POST
    @AuthRequired
    @Path("{identifier}/metadatablocks/:isRoot")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    @Operation(summary = "Set metadata-block root status through legacy route",
            description = "Changes whether a dataverse is a metadata-block root using the legacy route.")
    @RequestBody(description = "Boolean text indicating whether the dataverse is a metadata-block root.")
    public Response setMetadataRoot_legacy(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @RequestBody(description = "Boolean text indicating whether the dataverse is a metadata-block root.")
            String body) {
        return setMetadataRoot(crc, dvIdtf, body);
    }

    @PUT
    @AuthRequired
    @Path("{identifier}/metadatablocks/isRoot")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    @Operation(summary = "Assign metadata-block root status",
            description = "Changes whether a dataverse is a metadata-block root.")
    @RequestBody(description = "Boolean text indicating whether the dataverse is a metadata-block root.")
    public Response setMetadataRoot(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @RequestBody(description = "Boolean text indicating whether the dataverse is a metadata-block root.")
            String body) {
        return response(req -> {
            final boolean root = parseBooleanOrDie(body);
            final Dataverse dataverse = findDataverseOrDie(dvIdtf);
            execCommand(new UpdateDataverseMetadataBlocksCommand.SetRoot(req, dataverse, root));
            return ok("Dataverse " + dataverse.getName() + " is now a metadata  " + (root ? "" : "non-") + "root");
        }, getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/facets/")
    /**
     * return list of facets for the dataverse with alias `dvIdtf`
     */
    @Operation(summary = "Read dataverse facets",
            description = "Lists dataset field facets configured for a dataverse, optionally including detailed facet metadata.")
    public Response listFacets(@Context ContainerRequestContext crc,
                               @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
                               @PathParam("identifier") String dvIdtf,
                               @Parameter(description = "Whether to include detailed facet metadata.")
                               @QueryParam("returnDetails") boolean returnDetails) {
        try {
            User user = getRequestUser(crc);
            DataverseRequest request = createDataverseRequest(user);
            Dataverse dataverse = findDataverseUserCanSeeOrDie(dvIdtf, request);
            List<DataverseFacet> dataverseFacets = execCommand(new ListFacetsCommand(request, dataverse));

            if (returnDetails) {
                return ok(jsonDataverseFacets(dataverseFacets));
            } else {
                JsonArrayBuilder facetsBuilder = Json.createArrayBuilder();
                for (DataverseFacet facet : dataverseFacets) {
                    facetsBuilder.add(facet.getDatasetFieldType().getName());
                }
                return ok(facetsBuilder);
            }
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }

    @GET
    @AuthRequired
    @Path("{identifier}/featured")
    /*
    Allows user to get the collections that are featured by a given collection
    probably more for SPA than end user
    */
    @Operation(summary = "Read featured dataverses",
            description = "Lists aliases for dataverses featured by the selected dataverse.")
    public Response getFeaturedDataverses(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @RequestBody(description = "Unused request body accepted by the endpoint.")
            String dvAliases) {

        try {
            User u = getRequestUser(crc);
            DataverseRequest r = createDataverseRequest(u);
            Dataverse dataverse = findDataverseUserCanSeeOrDie(dvIdtf, r);
            JsonArrayBuilder fs = Json.createArrayBuilder();
            for (Dataverse f : execCommand(new ListFeaturedCollectionsCommand(r, dataverse))) {
                fs.add(f.getAlias());
            }
            return ok(fs);
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }


    @POST
    @AuthRequired
    @Path("{identifier}/featured")
    /**
     * Allows user to set featured dataverses - must have edit dataverse permission
     *
     */
    @Operation(summary = "Assign featured dataverses",
            description = "Adds eligible child or linked dataverses to the featured list for a dataverse.")
    public Response setFeaturedDataverses(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @RequestBody(description = "JSON array of dataverse aliases to feature.")
            String dvAliases) {
        List<Dataverse> dvsFromInput = new LinkedList<>();


        try {

            for (JsonString dvAlias : Util.asJsonArray(dvAliases).getValuesAs(JsonString.class)) {
                Dataverse dvToBeFeatured = dataverseService.findByAlias(dvAlias.getString());
                if (dvToBeFeatured == null) {
                    return error(Response.Status.BAD_REQUEST, "Can't find dataverse collection with alias '" + dvAlias + "'");
                }
                dvsFromInput.add(dvToBeFeatured);
            }

            if (dvsFromInput.isEmpty()) {
                return error(Response.Status.BAD_REQUEST, "Please provide a valid Json array of dataverse collection aliases to be featured.");
            }

            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            List<Dataverse> featuredSource = new ArrayList<>();
            List<Dataverse> featuredTarget = new ArrayList<>();
            featuredSource.addAll(dataverseService.findAllPublishedByOwnerId(dataverse.getId()));
            featuredSource.addAll(linkingService.findLinkedDataverses(dataverse.getId()));
            List<DataverseFeaturedDataverse> featuredList = featuredDataverseService.findByDataverseId(dataverse.getId());

            if (featuredSource.isEmpty()) {
                return error(Response.Status.BAD_REQUEST, "There are no collections avaialble to be featured in Dataverse collection '" + dataverse.getDisplayName() + "'.");
            }

            for (DataverseFeaturedDataverse dfd : featuredList) {
                Dataverse fd = dfd.getFeaturedDataverse();
                featuredTarget.add(fd);
                featuredSource.remove(fd);
            }

            for (Dataverse test : dvsFromInput) {
                if (featuredTarget.contains(test)) {
                    return error(Response.Status.BAD_REQUEST, "Dataverse collection '" + test.getDisplayName() + "' is already featured in Dataverse collection '" + dataverse.getDisplayName() + "'.");
                }

                if (featuredSource.contains(test)) {
                    featuredTarget.add(test);
                } else {
                    return error(Response.Status.BAD_REQUEST, "Dataverse collection '" + test.getDisplayName() + "' may not be featured in Dataverse collection '" + dataverse.getDisplayName() + "'.");
                }

            }
            // by passing null for Facets and DataverseFieldTypeInputLevel, those are not changed
            execCommand(new UpdateDataverseCommand(dataverse, null, featuredTarget, createDataverseRequest(getRequestUser(crc)), null));
            return ok("Featured Dataverses of dataverse " + dvIdtf + " updated.");

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        } catch (JsonParsingException jpe){
            return error(Response.Status.BAD_REQUEST, "Please provide a valid Json array of dataverse collection aliases to be featured.");
        }

    }

    @DELETE
    @AuthRequired
    @Path("{identifier}/featured")
    @Operation(summary = "Clear featured dataverses",
            description = "Removes all featured dataverses from the selected dataverse.")
    public Response deleteFeaturedCollections(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) throws WrappedResponse {
        try {
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            List<Dataverse> featuredTarget = new ArrayList<>();
            execCommand(new UpdateDataverseCommand(dataverse, null, featuredTarget, createDataverseRequest(getRequestUser(crc)), null));
            return ok(BundleUtil.getStringFromBundle("dataverses.api.delete.featured.collections.successful"));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @POST
    @AuthRequired
    @Path("{identifier}/facets")
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * (not publicly documented) API endpoint for assigning facets to a
     * dataverse. `curl -X POST -H "X-Dataverse-key: $ADMIN_KEY"
     * http://localhost:8088/api/dataverses/$dv/facets --upload-file foo.json`;
     * where foo.json contains a list of datasetField names, works as expected
     * (judging by the UI). This triggers a 500 when '-d @foo.json' is used.
     */
    @Operation(summary = "Assign dataverse facets",
            description = "Replaces the dataset field facets configured directly on a dataverse.")
    public Response setFacets(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @RequestBody(description = "JSON array of dataset field type names to use as facets.")
            String facetIds) {
        JsonArray jsonArray = Util.asJsonArray(facetIds);
        List<DatasetFieldType> facets;
        try {
            facets = parseFacets(jsonArray);
        } catch (WrappedResponse e) {
            return e.getResponse();
        }

        try {
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            // by passing null for Featured Dataverses and DataverseFieldTypeInputLevel, those are not changed
            execCommand(new UpdateDataverseCommand(dataverse, facets, null, createDataverseRequest(getRequestUser(crc)), null));
            return ok("Facets of dataverse " + dvIdtf + " updated.");

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @GET
    @AuthRequired
    @Path("{identifier}/metadatablockfacets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Read metadata block facets",
            description = "Returns metadata block facet settings for a dataverse.")
    public Response listMetadataBlockFacets(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {
        try {
            User u = getRequestUser(crc);
            DataverseRequest request = createDataverseRequest(u);
            Dataverse dataverse = findDataverseUserCanSeeOrDie(dvIdtf, request);
            List<DataverseMetadataBlockFacet> metadataBlockFacets = Optional.ofNullable(execCommand(new ListMetadataBlockFacetsCommand(request, dataverse))).orElse(Collections.emptyList());
            List<DataverseMetadataBlockFacetDTO.MetadataBlockDTO> metadataBlocksDTOs = metadataBlockFacets.stream()
                    .map(item -> new DataverseMetadataBlockFacetDTO.MetadataBlockDTO(item.getMetadataBlock().getName(), item.getMetadataBlock().getLocaleDisplayFacet()))
                    .collect(Collectors.toList());
            DataverseMetadataBlockFacetDTO response = new DataverseMetadataBlockFacetDTO(dataverse.getId(), dataverse.getAlias(), dataverse.isMetadataBlockFacetRoot(), metadataBlocksDTOs);
            return Response.ok(response).build();
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }

    @POST
    @AuthRequired
    @Path("{identifier}/metadatablockfacets")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Assign metadata block facets",
            description = "Replaces metadata block facets on a dataverse that is a metadata-block-facet root.")
    public Response setMetadataBlockFacets(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @RequestBody(description = "JSON array of metadata block names to use as facets.")
            List<String> metadataBlockNames) {
        try {
            Dataverse dataverse = findDataverseOrDie(dvIdtf);

            if(!dataverse.isMetadataBlockFacetRoot()) {
                return badRequest(String.format("Dataverse: %s must have metadata block facet root set to true", dvIdtf));
            }

            List<DataverseMetadataBlockFacet> metadataBlockFacets = new LinkedList<>();
            for(String metadataBlockName: metadataBlockNames) {
                MetadataBlock metadataBlock = findMetadataBlock(metadataBlockName);
                if (metadataBlock == null) {
                    return badRequest(String.format("Invalid metadata block name: %s", metadataBlockName));
                }

                DataverseMetadataBlockFacet metadataBlockFacet = new DataverseMetadataBlockFacet();
                metadataBlockFacet.setDataverse(dataverse);
                metadataBlockFacet.setMetadataBlock(metadataBlock);
                metadataBlockFacets.add(metadataBlockFacet);
            }

            execCommand(new UpdateMetadataBlockFacetsCommand(createDataverseRequest(getRequestUser(crc)), dataverse, metadataBlockFacets));
            return ok(String.format("Metadata block facets updated. DataverseId: %s blocks: %s", dvIdtf, metadataBlockNames));

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @POST
    @AuthRequired
    @Path("{identifier}/metadatablockfacets/isRoot")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Assign metadata block facet root status",
            description = "Changes whether a dataverse is a metadata-block-facet root.")
    @RequestBody(description = "Boolean text indicating whether the dataverse is a metadata-block-facet root.")
    public Response updateMetadataBlockFacetsRoot(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @RequestBody(description = "Boolean text indicating whether the dataverse is a metadata-block-facet root.")
            String body) {
        try {
            final boolean blockFacetsRoot = parseBooleanOrDie(body);
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            if(dataverse.isMetadataBlockFacetRoot() == blockFacetsRoot) {
                return ok(String.format("No update needed, dataverse already consistent with new value. DataverseId: %s blockFacetsRoot: %s", dvIdtf, blockFacetsRoot));
            }

            execCommand(new UpdateMetadataBlockFacetRootCommand(createDataverseRequest(getRequestUser(crc)), dataverse, blockFacetsRoot));
            return ok(String.format("Metadata block facets root updated. DataverseId: %s blockFacetsRoot: %s", dvIdtf, blockFacetsRoot));

        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    // FIXME: This listContent method is way too optimistic, always returning "ok" and never "error".
    // TODO: Investigate why there was a change in the timeframe of when pull request #4350 was merged
    // (2438-4295-dois-for-files branch) such that a contributor API token no longer allows this method
    // to be called without a PermissionException being thrown.
    @GET
    @AuthRequired
    @Path("{identifier}/contents")
    @Operation(summary = "Read dataverse contents",
            description = "Lists direct child dataverses and datasets for a dataverse.")
    public Response listContent(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) throws WrappedResponse {

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

        return response(req -> ok(
                execCommand(new ListDataverseContentCommand(req, findDataverseUserCanSeeOrDie(dvIdtf, req)))
                        .stream()
                        .map(dvo -> (JsonObjectBuilder) dvo.accept(ser))
                        .collect(toJsonArray())
        ), getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/storagesize")
    @Operation(summary = "Read dataverse storage size",
            description = "Returns the storage size for a dataverse, optionally including cached size data.")
    public Response getStorageSize(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @Parameter(description = "Whether to include cached storage size values.")
            @QueryParam("includeCached") boolean includeCached) throws WrappedResponse {
                
        return response(req -> ok(MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.datasize"),
                execCommand(new GetDataverseStorageSizeCommand(req, findDataverseUserCanSeeOrDie(dvIdtf, req), includeCached)))), getRequestUser(crc));
    }
    
    @GET
    @AuthRequired
    @Path("{identifier}/storage/quota")
    @Operation(summary = "Read dataverse storage quota",
            description = "Returns the storage quota for a dataverse, optionally including inherited quota values.")
    public Response getCollectionQuota(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @Parameter(description = "Whether to include inherited quota settings in the response.")
            @QueryParam("showInherited") boolean showInherited) throws WrappedResponse {
        try {
            DataverseRequest request = createDataverseRequest(getRequestUser(crc));
            Long bytesAllocated = execCommand(new GetCollectionQuotaCommand(request, findDataverseUserCanSeeOrDie(dvIdtf, request), showInherited));
            if (bytesAllocated != null) {
                return ok(MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.storage.quota.allocation"),bytesAllocated));
            }
            return ok(BundleUtil.getStringFromBundle("dataverse.storage.quota.notdefined"));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    @PUT
    @AuthRequired
    @Path("{identifier}/storage/quota")
    @Operation(summary = "Assign dataverse storage quota",
            description = "Sets the dataverse storage quota in bytes.")
    public Response setCollectionQuota(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @RequestBody(description = "Quota value in bytes.")
            String value) throws WrappedResponse {
        try {
            Long bytesAllocated; 
            try {
                bytesAllocated = Long.parseLong(value);
            } catch (NumberFormatException nfe){
                return error(Status.BAD_REQUEST, value + " is not a valid number of bytes");
            }
            execCommand(new SetCollectionQuotaCommand(createDataverseRequest(getRequestUser(crc)), findDataverseOrDie(dvIdtf), bytesAllocated));
            return ok(BundleUtil.getStringFromBundle("dataverse.storage.quota.updated"));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    @DELETE
    @AuthRequired
    @Path("{identifier}/storage/quota")
    @Operation(summary = "Clear dataverse storage quota",
            description = "Deletes the explicit storage quota for a dataverse.")
    public Response deleteCollectionQuota(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) throws WrappedResponse {
        try {
            execCommand(new DeleteCollectionQuotaCommand(createDataverseRequest(getRequestUser(crc)), findDataverseOrDie(dvIdtf)));
            return ok(BundleUtil.getStringFromBundle("dataverse.storage.quota.deleted"));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    /**
     *
     * @param crc
     * @param identifier
     * @return
     * @throws edu.harvard.iq.dataverse.api.AbstractApiBean.WrappedResponse 
     * @todo: add an optional parameter that would force the recorded storage use
     * to be recalculated (or should that be a POST version of this API?)
     */
    @GET
    @AuthRequired
    @Path("{identifier}/storage/use")
    @Operation(summary = "Read dataverse storage use",
            description = "Returns the recorded storage use for a dataverse.")
    public Response getCollectionStorageUse(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String identifier) throws WrappedResponse {
        return response(req -> ok(MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.storage.use"),
                execCommand(new GetCollectionStorageUseCommand(req, findDataverseUserCanSeeOrDie(identifier, req))))), getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/roles")
    @Operation(summary = "Read dataverse roles",
            description = "Lists roles available on a dataverse.")
    public Response listRoles(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {
        return response(req -> ok(
                execCommand(new ListRolesCommand(req, findDataverseUserCanSeeOrDie(dvIdtf, req)))
                        .stream().map(r -> json(r))
                        .collect(toJsonArray())
        ), getRequestUser(crc));
    }

    @POST
    @AuthRequired
    @Path("{identifier}/roles")
    @Operation(summary = "Create a dataverse role",
            description = "Creates a role scoped to the selected dataverse.")
    public Response createRole(@Context ContainerRequestContext crc,
            @RequestBody(description = "Role definition containing alias, name, description, and permissions.")
            RoleDTO roleDto,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {
        return response(req -> ok(json(execCommand(new CreateRoleCommand(roleDto.asRole(), req, findDataverseOrDie(dvIdtf))))), getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/assignments")
    @Operation(summary = "Read dataverse role assignments",
            description = "Lists role assignments defined on a dataverse.")
    public Response listAssignments(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {
        return response(req -> ok(
                execCommand(new ListRoleAssignments(req, findDataverseUserCanSeeOrDie(dvIdtf, req)))
                        .stream()
                        .map(a -> json(a))
                        .collect(toJsonArray())
        ), getRequestUser(crc));
    }

    @POST
    @AuthRequired
    @Path("{identifier}/assignments")
    @Operation(summary = "Grant a dataverse role assignment",
            description = "Assigns a role to a role assignee on a dataverse.")
    public Response createAssignment(@Context ContainerRequestContext crc,
            @RequestBody(description = "Role assignment containing assignee identifier and role alias.")
            RoleAssignmentDTO ra,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @Parameter(hidden = true)
            @QueryParam("key") String apiKey) {

        try {
            final DataverseRequest req = createDataverseRequest(getRequestUser(crc));
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
    @AuthRequired
    @Path("{identifier}/assignments/{id}")
    @Operation(summary = "Remove a dataverse role assignment",
            description = "Revokes a role assignment from the selected dataverse.")
    public Response deleteAssignment(@Context ContainerRequestContext crc,
            @Parameter(description = "Role assignment database id.", required = true)
            @PathParam("id") long assignmentId,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {
        RoleAssignment ra = em.find(RoleAssignment.class, assignmentId);
        if (ra != null) {
            try {
                findDataverseOrDie(dvIdtf);
                execCommand(new RevokeRoleCommand(ra, createDataverseRequest(getRequestUser(crc))));
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
    @AuthRequired
    @Path("{identifier}/actions/:publish")
    @Operation(summary = "Publish a dataverse",
            description = "Publishes the selected dataverse collection.")
    public Response publishDataverse(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {
        try {
            Dataverse dv = findDataverseOrDie(dvIdtf);
            return ok(json(execCommand(new PublishDataverseCommand(createDataverseRequest(getRequestAuthenticatedUserOrDie(crc)), dv))));

        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @POST
    @AuthRequired
    @Path("{identifier}/groups/")
    @Operation(summary = "Create an explicit group",
            description = "Creates an explicit group owned by a dataverse.")
    public Response createExplicitGroup(@Context ContainerRequestContext crc,
            @RequestBody(description = "Explicit group definition.")
            ExplicitGroupDTO dto,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {
        return response(req -> {
            ExplicitGroupProvider prv = explicitGroupSvc.getProvider();
            ExplicitGroup newGroup = dto.apply(prv.makeGroup());

            newGroup = execCommand(new CreateExplicitGroupCommand(req, findDataverseOrDie(dvIdtf), newGroup));

            String groupUri = String.format("%s/groups/%s", dvIdtf, newGroup.getGroupAliasInOwner());
            return created(groupUri, json(newGroup));
        }, getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/groups/")
    @Operation(summary = "Read explicit groups",
            description = "Lists explicit groups owned by a dataverse.")
    public Response listGroups(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @Parameter(hidden = true)
            @QueryParam("key") String apiKey) {
        return response(req -> ok(
                execCommand(new ListExplicitGroupsCommand(req, findDataverseUserCanSeeOrDie(dvIdtf, req)))
                        .stream().map(eg -> json(eg))
                        .collect(toJsonArray())
        ), getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/groups/{aliasInOwner}")
    @Operation(summary = "Read an explicit group",
            description = "Returns one explicit group owned by a dataverse.")
    public Response getGroupByOwnerAndAliasInOwner(@Context ContainerRequestContext crc,
                                                   @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
                                                   @PathParam("identifier") String dvIdtf,
                                                   @Parameter(description = "Group alias within the dataverse.", required = true)
                                                   @PathParam("aliasInOwner") String grpAliasInOwner) {
        return response(req -> ok(json(findExplicitGroupOrDie(findDataverseUserCanSeeOrDie(dvIdtf, req),
                req,
                grpAliasInOwner))), getRequestUser(crc));
    }
    
    @GET
    @AuthRequired
    @Path("{identifier}/guestbookResponses/")
    @Operation(summary = "Export guestbook responses for a dataverse",
            description = "Streams guestbook response data for datasets in a dataverse as CSV.")
    public Response getGuestbookResponsesByDataverse(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @Parameter(description = "Guestbook database id used to filter responses.")
            @QueryParam("guestbookId") Long gbId, @Context HttpServletResponse response) {

        Dataverse dv;
        try {
            User u = getRequestUser(crc);
            DataverseRequest req = createDataverseRequest(u);
            dv = findDataverseUserCanSeeOrDie(dvIdtf, req);

            if (permissionSvc.request(req)
                    .on(dv)
                    .has(Permission.EditDataverse)) {
            } else {
                return error(Status.FORBIDDEN, "Not authorized");
            }

        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

        StreamingOutput stream = new StreamingOutput() {

            @Override
            public void write(OutputStream os) throws IOException,
                    WebApplicationException {

                Map<Integer, Object> customQandAs = guestbookResponseService.mapCustomQuestionAnswersAsStrings(dv.getId(), gbId);
                Map<Integer, String> datasetTitles = guestbookResponseService.mapDatasetTitles(dv.getId());

                List<Object[]> guestbookResults = guestbookResponseService.getGuestbookResults(dv.getId(), gbId);
                os.write("Guestbook, Dataset, Dataset PID, Date, Type, File Name, File Id, File PID, User Name, Email, Institution, Position, Custom Questions\n".getBytes());
                for (Object[] result : guestbookResults) {
                    StringBuilder sb = guestbookResponseService.convertGuestbookResponsesToCSV(customQandAs, datasetTitles, result);
                    os.write(sb.toString().getBytes());
                }
            }
        };
        return Response.ok(stream).build();
    }
    
    @PUT
    @AuthRequired
    @Path("{identifier}/groups/{aliasInOwner}")
    @Operation(summary = "Revise an explicit group",
            description = "Updates an explicit group owned by a dataverse.")
    public Response updateGroup(@Context ContainerRequestContext crc,
            @RequestBody(description = "Explicit group definition to apply.")
            ExplicitGroupDTO groupDto,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @Parameter(description = "Group alias within the dataverse.", required = true)
            @PathParam("aliasInOwner") String grpAliasInOwner) {
        return response(req -> ok(json(execCommand(
                new UpdateExplicitGroupCommand(req,
                        groupDto.apply(findExplicitGroupOrDie(findDataverseOrDie(dvIdtf), req, grpAliasInOwner)))))), getRequestUser(crc));
    }
    
    @PUT
    @AuthRequired
    @Path("{identifier}/defaultContributorRole/{roleAlias}")
    @Operation(summary = "Assign a default contributor role",
            description = "Sets the default contributor role used for datasets created in a dataverse.")
    public Response updateDefaultContributorRole(
            @Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @Parameter(description = "Role alias to use as the default contributor role, or none.", required = true)
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
            }, getRequestUser(crc));

        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

    }

    @GET
    @AuthRequired
    @Path("{identifier}/defaultContributorRole")
    @Operation(summary = "Read the default contributor role",
            description = "Returns the default contributor role configured for a dataverse.")
    public Response getDefaultContributorRole(
            @Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {

        return response(req -> ok(
                json(
                        execCommand(
                                new GetCollectionDefaultContributorRoleCommand(req,
                                        findDataverseOrDie(dvIdtf))))), getRequestUser(crc));

    }

    @DELETE
    @AuthRequired
    @Path("{identifier}/groups/{aliasInOwner}")
    @Operation(summary = "Remove an explicit group",
            description = "Deletes an explicit group owned by a dataverse.")
    public Response deleteGroup(@Context ContainerRequestContext crc,
                                @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
                                @PathParam("identifier") String dvIdtf,
                                @Parameter(description = "Group alias within the dataverse.", required = true)
                                @PathParam("aliasInOwner") String grpAliasInOwner) {
        return response(req -> {
            execCommand(new DeleteExplicitGroupCommand(req,
                    findExplicitGroupOrDie(findDataverseOrDie(dvIdtf), req, grpAliasInOwner)));
            return ok("Group " + dvIdtf + "/" + grpAliasInOwner + " deleted");
        }, getRequestUser(crc));
    }

    @POST
    @AuthRequired
    @Path("{identifier}/groups/{aliasInOwner}/roleAssignees")
    @Consumes("application/json")
    @Operation(summary = "Append role assignees to an explicit group",
            description = "Adds multiple role assignee identifiers to an explicit group.")
    public Response addRoleAssingees(@Context ContainerRequestContext crc,
                                     @RequestBody(description = "JSON array of role assignee identifiers to add.")
                                     List<String> roleAssingeeIdentifiers,
                                     @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
                                     @PathParam("identifier") String dvIdtf,
                                     @Parameter(description = "Group alias within the dataverse.", required = true)
                                     @PathParam("aliasInOwner") String grpAliasInOwner) {
        return response(req -> ok(
                json(
                    execCommand(
                                new AddRoleAssigneesToExplicitGroupCommand(req,
                                        findExplicitGroupOrDie(findDataverseOrDie(dvIdtf), req, grpAliasInOwner),
                                        new TreeSet<>(roleAssingeeIdentifiers))))), getRequestUser(crc));
    }

    @PUT
    @AuthRequired
    @Path("{identifier}/groups/{aliasInOwner}/roleAssignees/{roleAssigneeIdentifier: .*}")
    @Operation(summary = "Append one role assignee to an explicit group",
            description = "Adds one role assignee identifier to an explicit group.")
    public Response addRoleAssingee(@Context ContainerRequestContext crc,
                                    @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
                                    @PathParam("identifier") String dvIdtf,
                                    @Parameter(description = "Group alias within the dataverse.", required = true)
                                    @PathParam("aliasInOwner") String grpAliasInOwner,
                                    @Parameter(description = "Role assignee identifier to add to the group.", required = true)
                                    @PathParam("roleAssigneeIdentifier") String roleAssigneeIdentifier) {
        return addRoleAssingees(crc, Collections.singletonList(roleAssigneeIdentifier), dvIdtf, grpAliasInOwner);
    }

    @DELETE
    @AuthRequired
    @Path("{identifier}/groups/{aliasInOwner}/roleAssignees/{roleAssigneeIdentifier: .*}")
    @Operation(summary = "Remove one role assignee from an explicit group",
            description = "Removes one role assignee identifier from an explicit group.")
    public Response deleteRoleAssingee(@Context ContainerRequestContext crc,
                                       @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
                                       @PathParam("identifier") String dvIdtf,
                                       @Parameter(description = "Group alias within the dataverse.", required = true)
                                       @PathParam("aliasInOwner") String grpAliasInOwner,
                                       @Parameter(description = "Role assignee identifier to remove from the group.", required = true)
                                       @PathParam("roleAssigneeIdentifier") String roleAssigneeIdentifier) {
        return response(req -> ok(json(execCommand(
                new RemoveRoleAssigneesFromExplicitGroupCommand(req,
                        findExplicitGroupOrDie(findDataverseOrDie(dvIdtf), req, grpAliasInOwner),
                        Collections.singleton(roleAssigneeIdentifier))))), getRequestUser(crc));
    }

    private ExplicitGroup findExplicitGroupOrDie(DvObject dv, DataverseRequest req, String groupIdtf) throws WrappedResponse {
        ExplicitGroup eg = execCommand(new GetExplicitGroupCommand(req, dv, groupIdtf));
        if (eg == null) {
            throw new WrappedResponse(notFound("Can't find " + groupIdtf + " in dataverse " + dv.getId()));
        }
        return eg;
    }

    @GET
    @AuthRequired
    @Path("{identifier}/links")
    @Operation(summary = "Read dataverse links",
            description = "Returns dataverses and datasets linked from a dataverse and dataverses linking to it.")
    public Response listLinks(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {
        try {
            User u = getRequestUser(crc);
            DataverseRequest req = createDataverseRequest(u);
            Dataverse dv = findDataverseUserCanSeeOrDie(dvIdtf, req);
            if (!u.isSuperuser()) {
                return error(Status.FORBIDDEN, "Not a superuser");
            }

            List<Dataverse> dvsThisDvHasLinkedToList = dataverseSvc.findDataversesThisIdHasLinkedTo(dv.getId());
            JsonArrayBuilder dvsThisDvHasLinkedToBuilder = Json.createArrayBuilder();
            for (Dataverse dataverse : dvsThisDvHasLinkedToList) {
                JsonObjectBuilder job = Json.createObjectBuilder();
                job.add("id", dataverse.getId());
                job.add("alias", dataverse.getAlias());
                job.add("displayName", dataverse.getDisplayName());
                dvsThisDvHasLinkedToBuilder.add(job);
            }

            List<Dataverse> dvsThatLinkToThisDvList = dataverseSvc.findDataversesThatLinkToThisDvId(dv.getId());
            JsonArrayBuilder dvsThatLinkToThisDvBuilder = Json.createArrayBuilder();
            for (Dataverse dataverse : dvsThatLinkToThisDvList) {
                JsonObjectBuilder job = Json.createObjectBuilder();
                job.add("id", dataverse.getId());
                job.add("alias", dataverse.getAlias());
                job.add("displayName", dataverse.getDisplayName());
                dvsThatLinkToThisDvBuilder.add(job);
            }

            List<Dataset> datasetsThisDvHasLinkedToList = dataverseSvc.findDatasetsThisIdHasLinkedTo(dv.getId());
            JsonArrayBuilder datasetsThisDvHasLinkedToBuilder = Json.createArrayBuilder();
            for (Dataset dataset : datasetsThisDvHasLinkedToList) {
                JsonObjectBuilder ds = new NullSafeJsonBuilder();
                ds.add("title", dataset.getLatestVersion().getTitle());
                ds.add("identifier",  dataset.getProtocol() + ":" + dataset.getAuthority() + "/" + dataset.getIdentifier());
                datasetsThisDvHasLinkedToBuilder.add(ds);
            }

            JsonObjectBuilder response = Json.createObjectBuilder();
            response.add("linkedDataverses", dvsThisDvHasLinkedToBuilder);
            response.add("dataversesLinkingToThis", dvsThatLinkToThisDvBuilder);
            response.add("linkedDatasets", datasetsThisDvHasLinkedToBuilder);
            return ok(response);

        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @POST
    @AuthRequired
    @Path("{id}/move/{targetDataverseAlias}")
    @Operation(summary = "Move a dataverse",
            description = "Moves a dataverse collection to a different parent dataverse.")
    public Response moveDataverse(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier to move.", required = true)
            @PathParam("id") String id,
            @Parameter(description = "Alias of the target parent dataverse.", required = true)
            @PathParam("targetDataverseAlias") String targetDataverseAlias,
            @Parameter(description = "Whether to force the move when warnings would otherwise block it.")
            @QueryParam("forceMove") Boolean force) {
        try {
            User u = getRequestUser(crc);
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
    @AuthRequired
    @Path("{linkedDataverseAlias}/link/{linkingDataverseAlias}")
    @Operation(summary = "Link one dataverse from another",
            description = "Creates a link from one dataverse to another dataverse.")
    public Response linkDataverse(@Context ContainerRequestContext crc,
            @Parameter(description = "Alias of the dataverse being linked.", required = true)
            @PathParam("linkedDataverseAlias") String linkedDataverseAlias,
            @Parameter(description = "Alias of the dataverse that will contain the link.", required = true)
            @PathParam("linkingDataverseAlias") String linkingDataverseAlias) {
        try {
            User u = getRequestUser(crc);
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
    
    @GET
    @AuthRequired
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{identifier}/{type}/linkingDataverses")
    @Operation(summary = "Search dataverses for linking",
            description = "Lists dataverses that can be linked to the selected dataverse object, optionally filtered by search text and existing link status.")
    public Response getLinkingDataverseList(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse object id or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @Parameter(description = "Search text used to filter dataverses.")
            @QueryParam("searchTerm") String searchTerm,
            @Parameter(description = "Whether to return dataverses already linking to the object.")
            @QueryParam("alreadyLinking") boolean alreadyLinking,
            @Parameter(description = "Dataverse object type used to resolve the identifier.", required = true)
            @PathParam("type") String type) {

        try {

            DvObject dvObject = findDvoByIdAndTypeOrDie(dvIdtf, type, false);
            List<Dataverse> dataversesForLinking = execCommand(new GetLinkingDataverseListCommand(
                    createDataverseRequest(getRequestUser(crc)),
                    dvObject,
                    searchTerm,
                    alreadyLinking
            ));

            JsonArrayBuilder dvBuilder = Json.createArrayBuilder();
            if (dataversesForLinking != null && !dataversesForLinking.isEmpty()) {
                for (Dataverse dv : dataversesForLinking) {
                    dvBuilder.add(json(dv, true));
                }
            }
            return ok(dvBuilder);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }
    
    

    @GET
    @AuthRequired
    @Path("{identifier}/userPermissions")
    @Operation(summary = "Read user permissions on a dataverse",
            description = "Returns booleans for the requesting user's key dataverse permissions.")
    public Response getUserPermissionsOnDataverse(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {
        Dataverse dataverse;
        try {
            dataverse = findDataverseOrDie(dvIdtf);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        User requestUser = getRequestUser(crc);
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("canAddDataverse", permissionService.userOn(requestUser, dataverse).has(Permission.AddDataverse));
        jsonObjectBuilder.add("canAddDataset", permissionService.userOn(requestUser, dataverse).has(Permission.AddDataset));
        jsonObjectBuilder.add("canViewUnpublishedDataverse", permissionService.userOn(requestUser, dataverse).has(Permission.ViewUnpublishedDataverse));
        jsonObjectBuilder.add("canEditDataverse", permissionService.userOn(requestUser, dataverse).has(Permission.EditDataverse));
        jsonObjectBuilder.add("canManageDataversePermissions", permissionService.userOn(requestUser, dataverse).has(Permission.ManageDataversePermissions));
        jsonObjectBuilder.add("canPublishDataverse", permissionService.userOn(requestUser, dataverse).has(Permission.PublishDataverse));
        jsonObjectBuilder.add("canDeleteDataverse", permissionService.userOn(requestUser, dataverse).has(Permission.DeleteDataverse));
        return ok(jsonObjectBuilder);
    }

    @POST
    @AuthRequired
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("{identifier}/featuredItems")
    @Operation(summary = "Create a dataverse featured item",
            description = "Creates a featured item with text, optional linked object, display order, and optional image.")
    @RequestBody(description = "Multipart featured item creation request with text, ordering, optional linked object, and optional image.")
    public Response createFeaturedItem(@Context ContainerRequestContext crc,
                                       @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
                                       @PathParam("identifier") String dvIdtf,
                                       @Parameter(description = "Featured item type used to resolve an optional linked object.")
                                       @FormDataParam("type") String type,
                                       @Parameter(description = "Identifier of the optional dataverse object linked by this featured item.")
                                       @FormDataParam("dvObjectIdentifier") String dvObjectIdtf,
                                       @Parameter(description = "Featured item text or link content.")
                                       @FormDataParam("content") String content,
                                       @Parameter(description = "Display order for the featured item.")
                                       @FormDataParam("displayOrder") int displayOrder,
                                       @Parameter(description = "Featured item image file.")
                                       @FormDataParam("file") InputStream imageFileInputStream,
                                       @Parameter(description = "Uploaded image file metadata.")
                                       @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {
        Dataverse dataverse;
        DvObject dvObject = null;
        try {
            dataverse = findDataverseOrDie(dvIdtf);
            if (dvObjectIdtf != null) {
                dvObject = findDvoByIdAndTypeOrDie(dvObjectIdtf, type, true);
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        try {
            NewDataverseFeaturedItemDTO newDataverseFeaturedItemDTO = NewDataverseFeaturedItemDTO.fromFormData(content, displayOrder, imageFileInputStream, contentDispositionHeader, type, dvObject);
            DataverseFeaturedItem dataverseFeaturedItem = execCommand(new CreateDataverseFeaturedItemCommand(
                    createDataverseRequest(getRequestUser(crc)),
                    dataverse,
                    newDataverseFeaturedItemDTO
            ));
            return ok(json(dataverseFeaturedItem));
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }

    @GET
    @AuthRequired
    @Path("{identifier}/featuredItems")
    @Operation(summary = "Read dataverse featured items",
            description = "Lists featured items configured on a dataverse.")
    public Response listFeaturedItems(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {
        try {
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            List<DataverseFeaturedItem> featuredItems = execCommand(new ListDataverseFeaturedItemsCommand(createDataverseRequest(getRequestUser(crc)), dataverse));
            return ok(jsonDataverseFeaturedItems(featuredItems));
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }

    @PUT
    @AuthRequired
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("{dataverseId}/featuredItems")
    @Operation(summary = "Replace dataverse featured items",
            description = "Creates, updates, reorders, and removes featured items for a dataverse from multipart form lists.")
    @RequestBody(description = "Multipart featured item list update with ids, text, ordering, linked objects, keep-image flags, and uploaded images.")
    public Response updateFeaturedItems(
            @Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("dataverseId") String dvIdtf,
            @Parameter(description = "Featured item ids; use 0 for new items.")
            @FormDataParam("id") List<Long> ids,
            @Parameter(description = "Featured item text or link content values.")
            @FormDataParam("content") List<String> contents,
            @Parameter(description = "Featured item type values.")
            @FormDataParam("type") List<String> types,
            @Parameter(description = "Optional linked dataverse object identifiers.")
            @FormDataParam("dvObjectIdentifier") List<String> dvObjectIdtf,
            @Parameter(description = "Display order values.")
            @FormDataParam("displayOrder") List<Integer> displayOrders,
            @Parameter(description = "Flags indicating whether to keep each existing image.")
            @FormDataParam("keepFile") List<Boolean> keepFiles,
            @Parameter(description = "File names used to match uploaded images to featured items.")
            @FormDataParam("fileName") List<String> fileNames,
            @Parameter(description = "Uploaded image file parts.")
            @FormDataParam("file") List<FormDataBodyPart> files) {
        try {
            if (ids == null || contents == null || displayOrders == null || keepFiles == null || fileNames == null) {
                throw new WrappedResponse(error(Response.Status.BAD_REQUEST,
                        BundleUtil.getStringFromBundle("dataverse.update.featuredItems.error.missingInputParams")));
            }

            int size = ids.size();
            if (types == null || types.isEmpty()) {
                types = new ArrayList<>(Collections.nCopies(size, null));
            }
            if (dvObjectIdtf == null || dvObjectIdtf.isEmpty()) {
                dvObjectIdtf = new ArrayList<>(Collections.nCopies(size, null));
            }

            if (contents.size() != size || displayOrders.size() != size || keepFiles.size() != size || fileNames.size() != size ||
                    types.size() != size || dvObjectIdtf.size() != size) {
                throw new WrappedResponse(error(Response.Status.BAD_REQUEST,
                        BundleUtil.getStringFromBundle("dataverse.update.featuredItems.error.inputListsSizeMismatch")));
            }

            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            List<NewDataverseFeaturedItemDTO> newItems = new ArrayList<>();
            Map<DataverseFeaturedItem, UpdatedDataverseFeaturedItemDTO> itemsToUpdate = new HashMap<>();

            for (int i = 0; i < contents.size(); i++) {
                String fileName = fileNames.get(i);
                InputStream fileInputStream = null;
                FormDataContentDisposition contentDisposition = null;

                if (files != null) {
                    Optional<FormDataBodyPart> matchingFile = files.stream()
                            .filter(file -> fileName.equals(FileUtil.decodeFileName(file.getFormDataContentDisposition().getFileName())))
                            .findFirst();

                    if (matchingFile.isPresent()) {
                        fileInputStream = matchingFile.get().getValueAs(InputStream.class);
                        contentDisposition = matchingFile.get().getFormDataContentDisposition();
                    }
                }

                // ignore dvObject if the id is missing or an empty string
                DvObject dvObject = dvObjectIdtf.get(i) != null && !dvObjectIdtf.get(i).isEmpty()
                        ? findDvoByIdAndTypeOrDie(dvObjectIdtf.get(i), types.get(i), true) : null;
                if (ids.get(i) == 0) {
                    newItems.add(NewDataverseFeaturedItemDTO.fromFormData(
                            contents.get(i), displayOrders.get(i), fileInputStream, contentDisposition, types.get(i), dvObject));
                } else {
                    DataverseFeaturedItem existingItem = dataverseFeaturedItemServiceBean.findById(ids.get(i));
                    if (existingItem == null) {
                        throw new WrappedResponse(error(Response.Status.NOT_FOUND,
                                MessageFormat.format(BundleUtil.getStringFromBundle("dataverseFeaturedItems.errors.notFound"), ids.get(i))));
                    }
                    itemsToUpdate.put(existingItem, UpdatedDataverseFeaturedItemDTO.fromFormData(
                            contents.get(i), displayOrders.get(i), keepFiles.get(i), fileInputStream, contentDisposition, types.get(i), dvObject));
                }
            }

            List<DataverseFeaturedItem> featuredItems = execCommand(new UpdateDataverseFeaturedItemsCommand(
                    createDataverseRequest(getRequestUser(crc)),
                    dataverse,
                    newItems,
                    itemsToUpdate
            ));

            return ok(jsonDataverseFeaturedItems(featuredItems));
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @DELETE
    @AuthRequired
    @Path("{identifier}/featuredItems")
    @Operation(summary = "Clear dataverse featured items",
            description = "Removes all featured items from a dataverse.")
    public Response deleteFeaturedItems(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {
        try {
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            execCommand(new UpdateDataverseFeaturedItemsCommand(createDataverseRequest(getRequestUser(crc)), dataverse, new ArrayList<>(), new ArrayMap<>()));
            return ok(BundleUtil.getStringFromBundle("dataverse.delete.featuredItems.success"));
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }

    @GET
    @AuthRequired
    @Path("{identifier}/templates")
    @Operation(summary = "Read dataverse templates",
            description = "Lists metadata templates available in a dataverse.")
    public Response getTemplates(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {
        try {
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            return ok(jsonTemplates(execCommand(new ListDataverseTemplatesCommand(createDataverseRequest(getRequestUser(crc)), dataverse))));
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }
    
    @GET
    @AuthRequired
    @Path("{id}/template/")
    @Operation(summary = "Read a metadata template",
            description = "Returns one metadata template by database id.")
    public Response getTemplate(@Context ContainerRequestContext crc,
            @Parameter(description = "Template database id.", required = true)
            @PathParam("id") Long templateId) {
        try {
            Template template = templateService.find(templateId);
            if (template == null){
                return error(Response.Status.NOT_FOUND, "Template with id " + templateId + " -  not found.");
            }
            return ok(jsonTemplate(execCommand(new GetTemplateCommand(createDataverseRequest(getRequestUser(crc)), template))));
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }

    @POST
    @AuthRequired
    @Path("{identifier}/templates")
    @Operation(summary = "Create a metadata template",
            description = "Creates a metadata template owned by a dataverse.")
    public Response createTemplate(@Context ContainerRequestContext crc,
            @RequestBody(description = "Template JSON containing template metadata fields and settings.")
            String body,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {
        try {
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            TemplateDTO newTemplateDTO;
            try {
                newTemplateDTO = TemplateDTO.fromRequestBody(body, jsonParser());
            } catch (JsonParseException ex) {
                return error(Status.BAD_REQUEST, MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.createTemplate.error.jsonParseMetadataFields"), ex.getMessage()));
            }
            Template created = execCommand(new CreateTemplateCommand(newTemplateDTO.toTemplate(), createDataverseRequest(getRequestUser(crc)), dataverse, true));
            
            return created("/dataverses/template/" + created.getId(), jsonTemplate(created));
        
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }

    @PUT
    @AuthRequired
    @Path("{templateId}/metadata")
    @Operation(summary = "Revise metadata template fields",
            description = "Updates a template name, field values, and field instructions.")
    public Response updateTemplateMetadata(@Context ContainerRequestContext crc,
            @RequestBody(description = "Template metadata JSON containing name, fields, and optional instructions.")
            String body,
            @Parameter(description = "Template database id.", required = true)
            @PathParam("templateId") Long templateId,
            @Parameter(description = "When true, replace matching field values instead of merging.")
            @QueryParam("replace") boolean replaceData) {
        try {
            Template template = findTemplateOrDie(templateId);
            Dataverse dataverse = template.getDataverse();
            boolean nameOnly = false;
            JsonObject json = JsonUtil.getJsonObject(body);

            /*
            You can also set a new name for your template in the json
            */
            if (json.containsKey("name") && !json.getString("name").isBlank()) {
                template.setName(json.getString("name"));
                nameOnly = true;
            }

            List<DatasetField> updatedFields = new ArrayList<>();
            // if it doesn't contain fields, instructions or name it better have a single dataset field
            // to be updated
            if (json.getJsonArray("fields") == null) {
                if (!json.containsKey("instructions") && !json.containsKey("name")) {
                    updatedFields.add(jsonParser().parseField(json, Boolean.FALSE, replaceData));
                }
            } else {
                updatedFields = jsonParser().parseMultipleFields(json, replaceData);
            }

            Map<String, String> instructionsMap = jsonParser().parseRequestBodyInstructionsMap(json);

            // if we're only updating the name then return the metadata and instructions to previous
            nameOnly = nameOnly && updatedFields.isEmpty() && instructionsMap == null;

            if (nameOnly) {
                updatedFields = template.getDatasetFields();
                instructionsMap = template.getInstructionsMap();
            }

            Template updated = execCommand(new UpdateTemplateFieldsCommand(template, dataverse, updatedFields, instructionsMap, replaceData, createDataverseRequest(getRequestUser(crc))));

            return created("/dataverses/template/" + updated.getId(), jsonTemplate(updated));
        } catch (JsonParseException ex) {
            logger.log(Level.SEVERE, "Semantic error parsing dataset update Json: " + ex.getMessage(), ex);
            return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("datasets.api.editMetadata.error.parseUpdate", List.of(ex.getMessage())));

        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }

    @PUT
    @AuthRequired
    @Path("{templateId}/licenseTerms")
    @Operation(summary = "Revise metadata template license terms",
            description = "Updates the license or custom terms configured on a metadata template.")
    public Response updateTemplateLicenseTerms(@Context ContainerRequestContext crc,
            @RequestBody(description = "License update request containing a license name, URI, or custom terms.")
            LicenseUpdateRequest requestBody,
            @Parameter(description = "Template database id.", required = true)
            @PathParam("templateId") Long templateId,
            @Parameter(description = "Unused replace flag accepted by the endpoint.")
            @QueryParam("replace") boolean replaceData) {
        try {
            Template template = findTemplateOrDie(templateId);
            Dataverse dataverse = template.getDataverse();

            if (requestBody.getName() != null && !requestBody.getName().isBlank()) {
                String licenseName = requestBody.getName();
                License license = licenseSvc.getByNameOrUri(licenseName);
                if (license == null) {
                    return notFound(BundleUtil.getStringFromBundle("datasets.api.updateLicense.licenseNotFound", List.of(licenseName)));
                }

                execCommand(new UpdateTemplateLicenseCommand(createDataverseRequest(getRequestUser(crc)), template, dataverse, license));
                return ok(BundleUtil.getStringFromBundle("dataverses.api.update.template.license.success"));
            } else if (requestBody.getCustomTerms() != null) {
                CustomTermsDTO customTerms = requestBody.getCustomTerms();
                execCommand(new UpdateTemplateLicenseCommand(createDataverseRequest(getRequestUser(crc)), template, dataverse, customTerms.toTermsOfUseAndAccess()));
                return ok(BundleUtil.getStringFromBundle("dataverses.api.update.template.license.success"));
            } else {
                return badRequest(BundleUtil.getStringFromBundle("datasets.api.updateLicense.licenseNameIsEmpty"));
            }

        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }

    @PUT
    @AuthRequired
    @Path("{templateId}/access")
    @Operation(summary = "Revise metadata template terms of access",
            description = "Updates terms of use and access settings on a metadata template.")
    public Response updateTemplateTermsOfAccess(@Context ContainerRequestContext crc,
            @RequestBody(description = "JSON terms-of-access payload for the metadata template.")
            String jsonBody,
            @Parameter(description = "Template database id.", required = true)
            @PathParam("templateId") Long templateId) {
        try {

            boolean publicInstall = settingsSvc.isTrueForKey(SettingsServiceBean.Key.PublicInstall, false);

            Template template = findTemplateOrDie(templateId);

            JsonObject json = JsonUtil.getJsonObject(jsonBody);

            TermsOfUseAndAccess toua = jsonParser().parseTermsOfAccess(json);

            if (publicInstall && (toua.isFileAccessRequest() || !toua.getTermsOfAccess().isEmpty())){
                return error(BAD_REQUEST, "Setting File Access Request or Terms of Access is not permitted on a public installation.");
            }

            execCommand(new UpdateTemplateTermsOfAccessCommand(createDataverseRequest(getRequestUser(crc)), template, template.getDataverse(), toua ));

            return ok(BundleUtil.getStringFromBundle("dataverses.api.update.template.access.success"));

        } catch (JsonParseException ex) {
            logger.log(Level.SEVERE, "Semantic error parsing template terms update Json: " + ex.getMessage(), ex);
            return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("datasets.api.editMetadata.error.parseUpdate", List.of(ex.getMessage())));
        } catch (WrappedResponse ex) {
            logger.log(Level.SEVERE, "Update terms of access error: " + ex.getMessage(), ex);
            return ex.getResponse();
        }
    }

    @POST
    @AuthRequired
    @Path("{identifier}/template/default/{templateId}")
    @Operation(summary = "Assign the default metadata template",
            description = "Sets the default metadata template for a dataverse.")
    public Response setDefaultTemplate(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvId,
            @Parameter(description = "Template database id.", required = true)
            @PathParam("templateId") Long templateId) {

        try {

            Dataverse dataverse = findDataverseOrDie(dvId);
            Template template = findTemplateInDataverseOrParentsOrDie(templateId, dataverse);
            DataverseRequest dvReq = createDataverseRequest(getRequestUser(crc));
            SetDefaultTemplateCommand command = new SetDefaultTemplateCommand(template, dvReq, dataverse);
            
            execCommand(command);

            return ok(BundleUtil.getStringFromBundle("dataverse.setDefaultTemplate.success"));
        
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }

    @DELETE
    @AuthRequired
    @Path("{identifier}/template/default")
    @Operation(summary = "Clear the default metadata template",
            description = "Removes the default metadata template setting from a dataverse.")
    public Response removeDefaultTemplate(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvId) {
        try {
            Dataverse dataverse = findDataverseOrDie(dvId);
            RemoveDefaultTemplateCommand command = new RemoveDefaultTemplateCommand(createDataverseRequest(getRequestUser(crc)), dataverse);
            execCommand(command);
            return ok(BundleUtil.getStringFromBundle("dataverse.removeDefaultTemplate.success"));
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }


    @GET
    @AuthRequired
    @Path("{identifier}/allowedMetadataLanguages")
    @Operation(summary = "Read allowed metadata language",
            description = "Returns the metadata language configured for a dataverse.")
    public Response getMetadataLanguage(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {
        return response(req -> {
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            return ok(jsonLanguage(execCommand(
                    new GetDataverseMetadataLanguageCommand(req, dataverse))));
        }, getRequestUser(crc));
    }

    @PUT
    @AuthRequired
    @Path("{identifier}/allowedMetadataLanguages/{metadataLanguage}")
    @Operation(summary = "Assign allowed metadata language",
            description = "Sets the metadata language configured for a dataverse.")
    public Response setMetadataLanguage(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf,
            @Parameter(description = "Metadata language code to assign.", required = true)
            @PathParam("metadataLanguage") String lang) {
        return response(req -> {
            Map<String, String> langMap = settingsService.getBaseMetadataLanguageMap(null, true);
            if (langMap.isEmpty()) {
                return badRequest("There are no metadata languages configured on this server");
            }
            if (!langMap.containsKey(lang)) {
                return badRequest("The specified metadata language " + lang + " is not allowed on this server!");
            }
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            return ok(jsonLanguage(execCommand(new SetDataverseMetadataLanguageCommand(req, dataverse, lang))));
        }, getRequestUser(crc));
    }
    
    @Path("{id}/template")
    @AuthRequired
    @DELETE
    @Operation(summary = "Remove a metadata template",
            description = "Deletes a metadata template and clears default-template references that point to it.")
    public Response deleteTemplate(@Context ContainerRequestContext crc,
            @Parameter(description = "Template database id.", required = true)
            @PathParam("id") long id) {

        Template doomed = templateService.find(id);
        if (doomed == null) {
            return error(Response.Status.NOT_FOUND, "Template with id " + id + " -  not found.");
        }

        Dataverse dv = doomed.getDataverse();
        List<Dataverse> dataverseWDefaultTemplate = templateService.findDataversesByDefaultTemplateId(doomed.getId());
        try {
            execCommand(new DeleteTemplateCommand(createDataverseRequest(getRequestUser(crc)), dv, doomed, dataverseWDefaultTemplate));
        } catch (WrappedResponse wr) {
            return handleWrappedResponse(wr);
        }

        return ok("Template " + doomed.getName() + " deleted.");
    }
    
    @GET
    @AuthRequired
    @Path("{identifier}/assignments/history")
    @Produces({ MediaType.APPLICATION_JSON, "text/csv" })
    @Operation(summary = "Read dataverse role assignment history",
            description = "Returns dataverse role assignment history as JSON or CSV.")
    public Response getRoleAssignmentHistory(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String id,
            @Context HttpHeaders headers) {
        return response(req -> {
            Dataverse dataverse = findDataverseOrDie(id);

            // user is authenticated
            AuthenticatedUser authenticatedUser = null;
            try {
                authenticatedUser = getRequestAuthenticatedUserOrDie(crc);
            } catch (WrappedResponse ex) {
                return error(Status.UNAUTHORIZED, "Authentication is required.");
            }

            return getRoleAssignmentHistoryResponse(dataverse, authenticatedUser, false, headers);
        }, getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/storageDriver")
    @Operation(summary = "Read dataverse storage driver",
            description = "Returns the storage driver configured for a dataverse, optionally resolving the effective inherited driver.")
    public Response getStorageDriver(@Context ContainerRequestContext crc,
                                     @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
                                     @PathParam("identifier") String id,
                                     @Parameter(description = "Whether to resolve the effective inherited storage driver.")
                                     @QueryParam("getEffective") Boolean getEffective) throws WrappedResponse {

        Dataverse dataverse = findDataverseOrDie(id);

        return response(req -> {
            String storageDriver = execCommand(new GetDataverseStorageDriverCommand(req, dataverse, getEffective));
            return ok(JsonPrinter.jsonStorageDriver(storageDriver));
        }, getRequestUser(crc));
    }

    @PUT
    @AuthRequired
    @Path("{identifier}/storageDriver")
    @Operation(summary = "Assign dataverse storage driver",
            description = "Sets the storage driver for a dataverse by driver label.")
    public Response setStorageDriver(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String id,
            @RequestBody(description = "Storage driver label to assign.")
            String label) throws WrappedResponse {
        Dataverse dataverse = findDataverseOrDie(id);

        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        for (Entry<String, String> store: DataAccess.getStorageDriverLabels().entrySet()) {
            if(store.getKey().equals(label)) {
                dataverse.setStorageDriverId(store.getValue());
                return ok("Storage set to: " + store.getKey() + "/" + store.getValue());
            }
        }
        return error(Response.Status.BAD_REQUEST,
                "No Storage Driver found for : " + label);
    }

    @DELETE
    @AuthRequired
    @Path("{identifier}/storageDriver")
    @Operation(summary = "Reset dataverse storage driver",
            description = "Clears the explicit storage driver setting so the dataverse uses the default driver.")
    public Response resetStorageDriver(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String id) throws WrappedResponse {

        Dataverse dataverse = findDataverseOrDie(id);
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            if (!user.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "Superusers only.");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        dataverse.setStorageDriverId("");
        return ok("Storage reset to default: " + DataAccess.DEFAULT_STORAGE_DRIVER_IDENTIFIER);
    }

    @GET
    @AuthRequired
    @Path("{identifier}/allowedStorageDrivers")
    @Operation(summary = "Read allowed storage drivers",
            description = "Returns storage drivers available for the selected dataverse.")
    public Response listStorageDrivers(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String id) throws WrappedResponse {

        Dataverse dv = findDataverseOrDie(id);

        /*
         * TODO: This endpoint ad GetDataverseAllowedStorageDriverCommand needs to be completed implementing
         * the request from Jim Myers, which is to return the list of storage drivers that the dataverse can use.
         * Currently it will return the full list of drivers available.
         */
        try {
            AuthenticatedUser user = getRequestAuthenticatedUserOrDie(crc);
            DataverseRequest request = createDataverseRequest(user);
            GetDataverseAllowedStorageDriverCommand getAllowedStorageDriversCommand = new GetDataverseAllowedStorageDriverCommand(request, dv);
            return ok(execCommand(getAllowedStorageDriversCommand));
        } catch (WrappedResponse wr) {
            return handleWrappedResponse(wr);
        }
    }

    @GET
    @AuthRequired
    @Path("{identifier}/locallyFairRoleAssignees")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Lists locally FAIR role assignees",
            description = "Lists role assignee identifiers configured for locally FAIR metadata access in a dataverse.")
    public Response listLocallyFairRoleAssignees(@Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
            @PathParam("identifier") String dvIdtf) {
        try {
            User user = getRequestUser(crc);
            if (!user.isSuperuser()) {
                return error(Status.FORBIDDEN, "Not a superuser");
            }

            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            JsonArrayBuilder assignees = Json.createArrayBuilder();
            dataverse.getLocallyFAIRRoleAssigneeIdentifiers().stream()
                    .sorted()
                    .forEach(assignees::add);
            return ok(assignees);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @PUT
    @AuthRequired
    @Path("{identifier}/locallyFairRoleAssignees")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Replaces locally FAIR role assignees",
            description = "Replaces the full locally FAIR role assignee identifier set for a dataverse and reindexes the dataverse.")
    @RequestBody(description = "JSON array of role assignee identifiers to configure for locally FAIR metadata access.")
    public Response setLocallyFairRoleAssignees(@Context ContainerRequestContext crc,
                                                @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
                                                @PathParam("identifier") String dvIdtf,
                                                @RequestBody(description = "JSON array of role assignee identifiers to configure for locally FAIR metadata access.")
                                                List<String> roleAssigneeIdentifiers) {
        try {
            User user = getRequestUser(crc);
            if (!user.isSuperuser()) {
                return error(Status.FORBIDDEN, "Not a superuser");
            }

            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            Set<String> validatedIdentifiers = validateLocallyFairRoleAssigneeIdentifiers(roleAssigneeIdentifiers);
            dataverse.setLocallyFAIRRoleAssigneeIdentifiers(validatedIdentifiers);
            dataverseService.save(dataverse);
            dataverseService.index(dataverse, true);

            return ok(String.format("Locally FAIR role assignees updated for dataverse %s.", dvIdtf), jsonLocallyFairRoleAssignees(dataverse));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @PUT
    @AuthRequired
    @Path("{identifier}/locallyFairRoleAssignees/{roleAssigneeIdentifier: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Adds a locally FAIR role assignee",
            description = "Adds one role assignee identifier to the locally FAIR metadata access set for a dataverse and reindexes the dataverse.")
    public Response addLocallyFairRoleAssignee(@Context ContainerRequestContext crc,
                                               @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
                                               @PathParam("identifier") String dvIdtf,
                                               @Parameter(description = "Role assignee identifier to add, such as a user or group identifier.", required = true)
                                               @PathParam("roleAssigneeIdentifier") String roleAssigneeIdentifier) {
        try {
            User user = getRequestUser(crc);
            if (!user.isSuperuser()) {
                return error(Status.FORBIDDEN, "Not a superuser");
            }

            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            if (findAssignee(roleAssigneeIdentifier) == null) {
                return badRequest("Invalid role assignee identifier: " + roleAssigneeIdentifier);
            }

            dataverse.addLocallyFAIRRoleAssignee(roleAssigneeIdentifier);
            dataverseService.save(dataverse);
            dataverseService.index(dataverse, true);

            return ok(String.format("Locally FAIR role assignee added to dataverse %s.", dvIdtf), jsonLocallyFairRoleAssignees(dataverse));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @DELETE
    @AuthRequired
    @Path("{identifier}/locallyFairRoleAssignees/{roleAssigneeIdentifier: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Removes a locally FAIR role assignee",
            description = "Removes one role assignee identifier from the locally FAIR metadata access set for a dataverse and reindexes the dataverse.")
    public Response deleteLocallyFairRoleAssignee(@Context ContainerRequestContext crc,
                                                  @Parameter(description = "Dataverse alias, id, or persistent identifier.", required = true)
                                                  @PathParam("identifier") String dvIdtf,
                                                  @Parameter(description = "Role assignee identifier to remove from locally FAIR metadata access.", required = true)
                                                  @PathParam("roleAssigneeIdentifier") String roleAssigneeIdentifier) {
        try {
            User user = getRequestUser(crc);
            if (!user.isSuperuser()) {
                return error(Status.FORBIDDEN, "Not a superuser");
            }

            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            if(StringUtils.isBlank(roleAssigneeIdentifier) || !dataverse.getLocallyFAIRRoleAssigneeIdentifiers().contains(roleAssigneeIdentifier)) {
                return badRequest("Invalid role assignee identifier: " + roleAssigneeIdentifier);
            }
            dataverse.removeLocallyFAIRRoleAssignee(roleAssigneeIdentifier);
            dataverseService.save(dataverse);
            dataverseService.index(dataverse, true);

            return ok(String.format("Locally FAIR role assignee removed from dataverse %s.", dvIdtf), jsonLocallyFairRoleAssignees(dataverse));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    private Set<String> validateLocallyFairRoleAssigneeIdentifiers(List<String> roleAssigneeIdentifiers) throws WrappedResponse {
        if (roleAssigneeIdentifiers == null) {
            return Collections.emptySet();
        }

        Set<String> validatedIdentifiers = new TreeSet<>();
        for (String identifier : roleAssigneeIdentifiers) {
            if (findAssignee(identifier) == null) {
                throw new WrappedResponse(badRequest("Invalid role assignee identifier: " + identifier));
            }
            validatedIdentifiers.add(identifier);
        }
        return validatedIdentifiers;
    }

}
