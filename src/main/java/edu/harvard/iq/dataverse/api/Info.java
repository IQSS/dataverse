package edu.harvard.iq.dataverse.api;

import java.util.logging.Logger;
import edu.harvard.iq.dataverse.customization.CustomizationConstants;
import jakarta.ws.rs.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.gdcc.spi.export.Exporter;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.XMLExporter;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("info")
@Tag(name = "Info", description = "General information about the Dataverse installation.")
public class Info extends AbstractApiBean {

    @EJB
    SettingsServiceBean settingsService;
    
    @EJB
    SystemConfig systemConfig;

    private static final Logger logger = Logger.getLogger(Info.class.getCanonicalName());

    @GET
    @Path("settings/:DatasetPublishPopupCustomText")
    @Operation(summary = "Returns dataset publish popup text",
            description = "Returns the custom text shown in the dataset publish popup when the setting is configured.")
    public Response getDatasetPublishPopupCustomText() {
        return getSettingResponseByKey(SettingsServiceBean.Key.DatasetPublishPopupCustomText);
    }

    @GET
    @Path("settings/:DatasetSubmitForReviewPopupCustomText")
    @Operation(summary = "Returns dataset review popup text",
            description = "Returns the custom text shown when a dataset is submitted for review and the setting is configured.")
    public Response DatasetSubmitForReviewPopupCustomText() {
        return getSettingResponseByKey(SettingsServiceBean.Key.DatasetSubmitForReviewPopupCustomText);
    }

    @GET
    @Path("settings/:PublishDatasetDisclaimerText")
    @Operation(summary = "Returns dataset publish disclaimer text",
            description = "Returns the disclaimer text shown during dataset publication when the setting is configured.")
    public Response getPublishDatasetDisclaimerText() {
        return getSettingResponseByKey(SettingsServiceBean.Key.PublishDatasetDisclaimerText);
    }

    @GET
    @Path("settings/:SubmitForReviewDatasetDisclaimerText")
    @Operation(summary = "Returns dataset review disclaimer text",
            description = "Returns the disclaimer text shown when a dataset is submitted for review and the setting is configured.")
    public Response getSubmitForReviewDatasetDisclaimerText() {
        return getSettingResponseByKey(SettingsServiceBean.Key.SubmitForReviewDatasetDisclaimerText);
    }

    @GET
    @Path("settings/:MaxEmbargoDurationInMonths")
    @Operation(summary = "Returns the maximum embargo duration",
            description = "Returns the configured maximum embargo duration in months when the setting is configured.")
    public Response getMaxEmbargoDurationInMonths() {
        return getSettingResponseByKey(SettingsServiceBean.Key.MaxEmbargoDurationInMonths);
    }

    @GET
    @Path("version")
    @Operation(summary = "Returns version and build information",
            description = "Returns the Dataverse application version and build identifier.")
    @APIResponse(responseCode = "200",
                 description = "Version and build information")
    public Response getInfo() {
        String versionStr = systemConfig.getVersion(true);
        String[] comps = versionStr.split("build",2);
        String version = comps[0].trim();
        JsonValue build = comps.length > 1 ? Json.createArrayBuilder().add(comps[1].trim()).build().get(0) : JsonValue.NULL;
        return ok(Json.createObjectBuilder()
                .add("version", version)
                .add("build", build));
    }

    @GET
    @Path("server")
    @Operation(summary = "Returns the server name",
            description = "Returns the configured fully qualified domain name for this Dataverse installation.")
    public Response getServer() {
        return ok(JvmSettings.FQDN.lookup());
    }

    @GET
    @Path("applicationTermsOfUse")
    @Operation(summary = "Returns application terms of use",
            description = "Returns the general application terms of use that users agree to during signup, optionally localized by language code.")
    @APIResponse(responseCode = "200",
                 description = "Application Terms of Use (General Terms of Use) that must be agreed to at signup.")
    public Response getApplicationTermsOfUse(
            @Parameter(description = "Two-character language code.",
                    required = false,
                    example = "en",
                    schema = @Schema(type = SchemaType.STRING))
            @QueryParam("lang") String lang) {
        return ok(systemConfig.getApplicationTermsOfUse(lang));
    }

    @GET
    @Path("apiTermsOfUse")
    @Operation(summary = "Returns API terms of use",
            description = "Returns the configured API terms of use for this Dataverse installation.")
    public Response getTermsOfUse() {
        return ok(systemConfig.getApiTermsOfUse());
    }

    @GET
    @Path("settings/incompleteMetadataViaApi")
    @Operation(summary = "Returns incomplete metadata API policy",
            description = "Returns whether API requests may create or update datasets with incomplete metadata.")
    public Response getAllowsIncompleteMetadata() {
        return ok(JvmSettings.API_ALLOW_INCOMPLETE_METADATA.lookupOptional(Boolean.class).orElse(false));
    }

    @GET
    @Path("zipDownloadLimit")
    @Operation(summary = "Returns the ZIP download limit",
            description = "Returns the configured maximum size for ZIP downloads in bytes.")
    public Response getZipDownloadLimit() {
        long zipDownloadLimit = SystemConfig.getLongLimitFromStringOrDefault(settingsSvc.getValueForKey(SettingsServiceBean.Key.ZipDownloadLimit), SystemConfig.defaultZipDownloadLimit);
        return ok(zipDownloadLimit);
    }

    @GET
    @Path("exportFormats")
    @Operation(summary = "Lists export formats",
            description = "Returns dataset export formats with display name, media type, harvestability, user-interface visibility, and XML metadata when available.")
    public Response getExportFormats() {
        JsonObjectBuilder responseModel = Json.createObjectBuilder();
        ExportService instance = ExportService.getInstance();
        for (String[] labels : instance.getExportersLabels()) {
            try {
                Exporter exporter = instance.getExporter(labels[1]);
                JsonObjectBuilder exporterObject = Json.createObjectBuilder().add("displayName", labels[0])
                        .add("mediaType", exporter.getMediaType()).add("isHarvestable", exporter.isHarvestable())
                        .add("isVisibleInUserInterface", exporter.isAvailableToUsers());
                if (exporter instanceof XMLExporter xmlExporter) {
                    exporterObject.add("XMLNameSpace", xmlExporter.getXMLNameSpace())
                            .add("XMLSchemaLocation", xmlExporter.getXMLSchemaLocation())
                            .add("XMLSchemaVersion", xmlExporter.getXMLSchemaVersion());
                }
                responseModel.add(labels[1], exporterObject);
            }
            catch (ExportException ex){
                logger.warning("Failed to get: " + labels[1]);
                logger.warning(ex.getLocalizedMessage());
            }
        }
        return ok(responseModel);
    }

    @GET
    @Path("settings/customization/{customizationFileType}")
    @Operation(summary = "Returns a customization file",
            description = "Returns the configured customization file for the requested customization type.")
    public Response getCustomizationFile(
            @Parameter(description = "Customization file type to retrieve.", required = true)
            @PathParam("customizationFileType") String customizationFileType) {
        String type = customizationFileType != null ? customizationFileType.toLowerCase() : "";
        if (!CustomizationConstants.validTypes.contains(type)) {
            return badRequest("Customization type unknown or missing. Must be one of the following: " + CustomizationConstants.validTypes);
        }
        Client client = ClientBuilder.newClient();
        WebTarget endpoint = client.target("http://localhost:8080/CustomizationFilesServlet");
        Response response = endpoint.queryParam("customFileType", type)
                .request(MediaType.MEDIA_TYPE_WILDCARD)
                .get();

        if (response.getLength() < 1) {
            return notFound(type + " not found.");
        } else {
            return response;
        }
    }

    private Response getSettingResponseByKey(SettingsServiceBean.Key key) {
        String setting = settingsService.getValueForKey(key);
        if (setting != null) {
            return ok(Json.createObjectBuilder().add("message", setting));
        } else {
            return notFound("Setting " + key + " not found");
        }
    }
    
}
