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
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.dataverse.DataverseUtil;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItem;
import edu.harvard.iq.dataverse.dataverse.featured.DataverseFeaturedItemServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.*;
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

import edu.harvard.iq.dataverse.util.json.JSONLDUtil;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import edu.harvard.iq.dataverse.util.json.JsonUtil;

import java.io.*;
import java.util.*;
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
import jakarta.ws.rs.core.StreamingOutput;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.xml.stream.XMLStreamException;

/**
 * A REST API for dataverses.
 *
 * @author michael
 */
@Stateless
@Path("dataverses")
public class Dataverses extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Dataverses.class.getCanonicalName());
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");

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
    DataverseFeaturedItemServiceBean dataverseFeaturedItemServiceBean;

    @POST
    @AuthRequired
    public Response addRoot(@Context ContainerRequestContext crc, String body) {
        logger.info("Creating root dataverse");
        return addDataverse(crc, body, "");
    }

    @POST
    @AuthRequired
    @Path("{identifier}")
    public Response addDataverse(@Context ContainerRequestContext crc, String body, @PathParam("identifier") String parentIdtf) {
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
            newDataverse = execCommand(new CreateDataverseCommand(newDataverse, createDataverseRequest(u), facets, inputLevels, metadataBlocks));
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
    public Response updateDataverse(@Context ContainerRequestContext crc, String body, @PathParam("identifier") String identifier) {
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
    public Response validateDatasetJson(@Context ContainerRequestContext crc, String body, @PathParam("identifier") String idtf) {
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
    public Response getDatasetSchema(@Context ContainerRequestContext crc, @PathParam("identifier") String idtf) {
        User u = getRequestUser(crc);

        try {
            String datasetSchema = execCommand(new GetDatasetSchemaCommand(createDataverseRequest(u), findDataverseOrDie(idtf)));
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
    public Response createDataset(@Context ContainerRequestContext crc, String jsonBody, @PathParam("identifier") String parentIdtf, @QueryParam("doNotValidate") String doNotValidateParam) {
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
            Dataset managedDs = null;
            try {
                managedDs = execCommand(new CreateNewDatasetCommand(ds, createDataverseRequest(u), null, validate));
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
    public Response createDatasetFromJsonLd(@Context ContainerRequestContext crc, String jsonLDBody, @PathParam("identifier") String parentIdtf) {
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
    public Response importDataset(@Context ContainerRequestContext crc, String jsonBody, @PathParam("identifier") String parentIdtf, @QueryParam("pid") String pidParam, @QueryParam("release") String releaseParam) {
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
    public Response importDatasetDdi(@Context ContainerRequestContext crc, String xml, @PathParam("identifier") String parentIdtf, @QueryParam("pid") String pidParam, @QueryParam("release") String releaseParam) {
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
    public Response recreateDataset(@Context ContainerRequestContext crc, String jsonLDBody, @PathParam("identifier") String parentIdtf) {
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
    public Response getDataverse(@Context ContainerRequestContext crc, @PathParam("identifier") String idtf, @QueryParam("returnOwners") boolean returnOwners, @QueryParam("returnChildCount") boolean returnChildCount) {
        return response(req -> {
            Dataverse dataverse = execCommand(new GetDataverseCommand(req, findDataverseOrDie(idtf)));
            boolean hideEmail = settingsService.isTrueForKey(SettingsServiceBean.Key.ExcludeEmailFromExport, false);
            return ok(json(dataverse, hideEmail, returnOwners, returnChildCount ? dataverseService.getChildCount(dataverse) : null));
        }, getRequestUser(crc));
    }

    @DELETE
    @AuthRequired
    @Path("{identifier}")
    public Response deleteDataverse(@Context ContainerRequestContext crc, @PathParam("identifier") String idtf) {
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
    public Response updateAttribute(@Context ContainerRequestContext crc, @PathParam("identifier") String identifier,
                                    @PathParam("attribute") String attribute, @QueryParam("value") String value) {
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
    public Response getInputLevels(@Context ContainerRequestContext crc, @PathParam("identifier") String identifier) {
        try {
            Dataverse dataverse = findDataverseOrDie(identifier);
            List<DataverseFieldTypeInputLevel> inputLevels = execCommand(new ListDataverseInputLevelsCommand(createDataverseRequest(getRequestUser(crc)), dataverse));
            return ok(jsonDataverseInputLevels(inputLevels));
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }

    @PUT
    @AuthRequired
    @Path("{identifier}/inputLevels")
    public Response updateInputLevels(@Context ContainerRequestContext crc, @PathParam("identifier") String identifier, String jsonBody) {
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
    @Path("{linkingDataverseId}/deleteLink/{linkedDataverseId}")
    public Response deleteDataverseLinkingDataverse(@Context ContainerRequestContext crc, @PathParam("linkingDataverseId") String linkingDataverseId, @PathParam("linkedDataverseId") String linkedDataverseId) {
        boolean index = true;
        return response(req -> {
            execCommand(new DeleteDataverseLinkingDataverseCommand(req, findDataverseOrDie(linkingDataverseId), findDataverseLinkingDataverseOrDie(linkingDataverseId, linkedDataverseId), index));
            return ok("Link from Dataverse " + linkingDataverseId + " to linked Dataverse " + linkedDataverseId + " deleted");
        }, getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/metadatablocks")
    public Response listMetadataBlocks(@Context ContainerRequestContext crc,
                                       @PathParam("identifier") String dvIdtf,
                                       @QueryParam("onlyDisplayedOnCreate") boolean onlyDisplayedOnCreate,
                                       @QueryParam("returnDatasetFieldTypes") boolean returnDatasetFieldTypes,
                                       @QueryParam("datasetType") String datasetTypeIn) {
        try {
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            DatasetType datasetType = datasetTypeSvc.getByName(datasetTypeIn);
            final List<MetadataBlock> metadataBlocks = execCommand(
                    new ListMetadataBlocksCommand(
                            createDataverseRequest(getRequestUser(crc)),
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
    public Response setMetadataBlocks(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf, String blockIds) {

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
    public Response getMetadataRoot_legacy(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf) {
        return getMetadataRoot(crc, dvIdtf);
    }

    @GET
    @AuthRequired
    @Path("{identifier}/metadatablocks/isRoot")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetadataRoot(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf) {
        return response(req -> {
            final Dataverse dataverse = findDataverseOrDie(dvIdtf);
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
    public Response setMetadataRoot_legacy(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf, String body) {
        return setMetadataRoot(crc, dvIdtf, body);
    }

    @PUT
    @AuthRequired
    @Path("{identifier}/metadatablocks/isRoot")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    public Response setMetadataRoot(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf, String body) {
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
    public Response listFacets(@Context ContainerRequestContext crc,
                               @PathParam("identifier") String dvIdtf,
                               @QueryParam("returnDetails") boolean returnDetails) {
        try {
            User user = getRequestUser(crc);
            DataverseRequest request = createDataverseRequest(user);
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
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
    public Response getFeaturedDataverses(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf,  String dvAliases) {

        try {
            User u = getRequestUser(crc);
            DataverseRequest r = createDataverseRequest(u);
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
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
    public Response setFeaturedDataverses(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf,  String dvAliases) {
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
    public Response deleteFeaturedCollections(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf) throws WrappedResponse {
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
    public Response setFacets(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf, String facetIds) {
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
    public Response listMetadataBlockFacets(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf) {
        try {
            User u = getRequestUser(crc);
            DataverseRequest request = createDataverseRequest(u);
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
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
    public Response setMetadataBlockFacets(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf, List<String> metadataBlockNames) {
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
    public Response updateMetadataBlockFacetsRoot(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf, String body) {
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
    public Response listContent(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf) throws WrappedResponse {

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
                execCommand(new ListDataverseContentCommand(req, findDataverseOrDie(dvIdtf)))
                        .stream()
                        .map(dvo -> (JsonObjectBuilder) dvo.accept(ser))
                        .collect(toJsonArray())
        ), getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/storagesize")
    public Response getStorageSize(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf, @QueryParam("includeCached") boolean includeCached) throws WrappedResponse {
                
        return response(req -> ok(MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.datasize"),
                execCommand(new GetDataverseStorageSizeCommand(req, findDataverseOrDie(dvIdtf), includeCached)))), getRequestUser(crc));
    }
    
    @GET
    @AuthRequired
    @Path("{identifier}/storage/quota")
    public Response getCollectionQuota(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf) throws WrappedResponse {
        try {
            Long bytesAllocated = execCommand(new GetCollectionQuotaCommand(createDataverseRequest(getRequestUser(crc)), findDataverseOrDie(dvIdtf)));
            if (bytesAllocated != null) {
                return ok(MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.storage.quota.allocation"),bytesAllocated));
            }
            return ok(BundleUtil.getStringFromBundle("dataverse.storage.quota.notdefined"));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    @POST
    @AuthRequired
    @Path("{identifier}/storage/quota/{bytesAllocated}")
    public Response setCollectionQuota(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf, @PathParam("bytesAllocated") Long bytesAllocated) throws WrappedResponse {
        try {
            execCommand(new SetCollectionQuotaCommand(createDataverseRequest(getRequestUser(crc)), findDataverseOrDie(dvIdtf), bytesAllocated));
            return ok(BundleUtil.getStringFromBundle("dataverse.storage.quota.updated"));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    @DELETE
    @AuthRequired
    @Path("{identifier}/storage/quota")
    public Response deleteCollectionQuota(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf) throws WrappedResponse {
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
    public Response getCollectionStorageUse(@Context ContainerRequestContext crc, @PathParam("identifier") String identifier) throws WrappedResponse {
        return response(req -> ok(MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.storage.use"),
                execCommand(new GetCollectionStorageUseCommand(req, findDataverseOrDie(identifier))))), getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/roles")
    public Response listRoles(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf) {
        return response(req -> ok(
                execCommand(new ListRolesCommand(req, findDataverseOrDie(dvIdtf)))
                        .stream().map(r -> json(r))
                        .collect(toJsonArray())
        ), getRequestUser(crc));
    }

    @POST
    @AuthRequired
    @Path("{identifier}/roles")
    public Response createRole(@Context ContainerRequestContext crc, RoleDTO roleDto, @PathParam("identifier") String dvIdtf) {
        return response(req -> ok(json(execCommand(new CreateRoleCommand(roleDto.asRole(), req, findDataverseOrDie(dvIdtf))))), getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/assignments")
    public Response listAssignments(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf) {
        return response(req -> ok(
                execCommand(new ListRoleAssignments(req, findDataverseOrDie(dvIdtf)))
                        .stream()
                        .map(a -> json(a))
                        .collect(toJsonArray())
        ), getRequestUser(crc));
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
//    TODO: Code duplicate in ThemeWidgetFragment. Maybe extract, make static and put some place else?
//          Important: at least use JvmSettings.DOCROOT_DIRECTORY and not the hardcoded location!
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
    @AuthRequired
    @Path("{identifier}/assignments")
    public Response createAssignment(@Context ContainerRequestContext crc, RoleAssignmentDTO ra, @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey) {

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
    public Response deleteAssignment(@Context ContainerRequestContext crc, @PathParam("id") long assignmentId, @PathParam("identifier") String dvIdtf) {
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
    public Response publishDataverse(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf) {
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
    public Response createExplicitGroup(@Context ContainerRequestContext crc, ExplicitGroupDTO dto, @PathParam("identifier") String dvIdtf) {
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
    public Response listGroups(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf, @QueryParam("key") String apiKey) {
        return response(req -> ok(
                execCommand(new ListExplicitGroupsCommand(req, findDataverseOrDie(dvIdtf)))
                        .stream().map(eg -> json(eg))
                        .collect(toJsonArray())
        ), getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{identifier}/groups/{aliasInOwner}")
    public Response getGroupByOwnerAndAliasInOwner(@Context ContainerRequestContext crc,
                                                   @PathParam("identifier") String dvIdtf,
                                                   @PathParam("aliasInOwner") String grpAliasInOwner) {
        return response(req -> ok(json(findExplicitGroupOrDie(findDataverseOrDie(dvIdtf),
                req,
                grpAliasInOwner))), getRequestUser(crc));
    }
    
    @GET
    @AuthRequired
    @Path("{identifier}/guestbookResponses/")
    public Response getGuestbookResponsesByDataverse(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf,
            @QueryParam("guestbookId") Long gbId, @Context HttpServletResponse response) {

        Dataverse dv;
        try {
            dv = findDataverseOrDie(dvIdtf);
            User u = getRequestUser(crc);
            DataverseRequest req = createDataverseRequest(u);
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
    public Response updateGroup(@Context ContainerRequestContext crc, ExplicitGroupDTO groupDto,
            @PathParam("identifier") String dvIdtf,
            @PathParam("aliasInOwner") String grpAliasInOwner) {
        return response(req -> ok(json(execCommand(
                new UpdateExplicitGroupCommand(req,
                        groupDto.apply(findExplicitGroupOrDie(findDataverseOrDie(dvIdtf), req, grpAliasInOwner)))))), getRequestUser(crc));
    }
    
    @PUT
    @AuthRequired
    @Path("{identifier}/defaultContributorRole/{roleAlias}")
    public Response updateDefaultContributorRole(
            @Context ContainerRequestContext crc,
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
            }, getRequestUser(crc));

        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

    }

    @DELETE
    @AuthRequired
    @Path("{identifier}/groups/{aliasInOwner}")
    public Response deleteGroup(@Context ContainerRequestContext crc,
                                @PathParam("identifier") String dvIdtf,
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
    public Response addRoleAssingees(@Context ContainerRequestContext crc,
                                     List<String> roleAssingeeIdentifiers,
                                     @PathParam("identifier") String dvIdtf,
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
    public Response addRoleAssingee(@Context ContainerRequestContext crc,
                                    @PathParam("identifier") String dvIdtf,
                                    @PathParam("aliasInOwner") String grpAliasInOwner,
                                    @PathParam("roleAssigneeIdentifier") String roleAssigneeIdentifier) {
        return addRoleAssingees(crc, Collections.singletonList(roleAssigneeIdentifier), dvIdtf, grpAliasInOwner);
    }

    @DELETE
    @AuthRequired
    @Path("{identifier}/groups/{aliasInOwner}/roleAssignees/{roleAssigneeIdentifier: .*}")
    public Response deleteRoleAssingee(@Context ContainerRequestContext crc,
                                       @PathParam("identifier") String dvIdtf,
                                       @PathParam("aliasInOwner") String grpAliasInOwner,
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
    public Response listLinks(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf) {
        try {
            User u = getRequestUser(crc);
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
    @AuthRequired
    @Path("{id}/move/{targetDataverseAlias}")
    public Response moveDataverse(@Context ContainerRequestContext crc, @PathParam("id") String id, @PathParam("targetDataverseAlias") String targetDataverseAlias, @QueryParam("forceMove") Boolean force) {
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
    public Response linkDataverse(@Context ContainerRequestContext crc, @PathParam("linkedDataverseAlias") String linkedDataverseAlias, @PathParam("linkingDataverseAlias") String linkingDataverseAlias) {
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
    @Path("{identifier}/userPermissions")
    public Response getUserPermissionsOnDataverse(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf) {
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
    public Response createFeaturedItem(@Context ContainerRequestContext crc,
                                       @PathParam("identifier") String dvIdtf,
                                       @FormDataParam("type") String type,
                                       @FormDataParam("dvObject") String dvObjectIdtf,
                                       @FormDataParam("content") String content,
                                       @FormDataParam("displayOrder") int displayOrder,
                                       @FormDataParam("file") InputStream imageFileInputStream,
                                       @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {
        Dataverse dataverse;
        DvObject dvObject = null;
        try {
            dataverse = findDataverseOrDie(dvIdtf);
            if (dvObjectIdtf != null) {
                dvObject = findDvoByIdAndTypeOrDie(dvObjectIdtf, type);
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
    public Response listFeaturedItems(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf) {
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
    public Response updateFeaturedItems(
            @Context ContainerRequestContext crc,
            @PathParam("dataverseId") String dvIdtf,
            @FormDataParam("id") List<Long> ids,
            @FormDataParam("content") List<String> contents,
            @FormDataParam("type") List<String> types,
            @FormDataParam("dvObject") List<String> dvObjectIdtf,
            @FormDataParam("displayOrder") List<Integer> displayOrders,
            @FormDataParam("keepFile") List<Boolean> keepFiles,
            @FormDataParam("fileName") List<String> fileNames,
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
                        ? findDvoByIdAndTypeOrDie(dvObjectIdtf.get(i), types.get(i)) : null;
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
    public Response deleteFeaturedItems(@Context ContainerRequestContext crc, @PathParam("identifier") String dvIdtf) {
        try {
            Dataverse dataverse = findDataverseOrDie(dvIdtf);
            execCommand(new UpdateDataverseFeaturedItemsCommand(createDataverseRequest(getRequestUser(crc)), dataverse, new ArrayList<>(), new ArrayMap<>()));
            return ok(BundleUtil.getStringFromBundle("dataverse.delete.featuredItems.success"));
        } catch (WrappedResponse e) {
            return e.getResponse();
        }
    }
}
