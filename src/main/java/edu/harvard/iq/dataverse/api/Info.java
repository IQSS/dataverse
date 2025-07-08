package edu.harvard.iq.dataverse.api;

import java.util.logging.Logger;

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
@Tag(name = "info", description = "General information about the Dataverse installation.")
public class Info extends AbstractApiBean {

    @EJB
    SettingsServiceBean settingsService;
    
    @EJB
    SystemConfig systemConfig;

    private static final Logger logger = Logger.getLogger(Info.class.getCanonicalName());

    @GET
    @Path("settings/:DatasetPublishPopupCustomText")
    public Response getDatasetPublishPopupCustomText() {
        return getSettingResponseByKey(SettingsServiceBean.Key.DatasetPublishPopupCustomText);
    }

    @GET
    @Path("settings/:MaxEmbargoDurationInMonths")
    public Response getMaxEmbargoDurationInMonths() {
        return getSettingResponseByKey(SettingsServiceBean.Key.MaxEmbargoDurationInMonths);
    }

    @GET
    @Path("version")
    @Operation(summary = "Get version and build information", description = "Get version and build information")
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
    public Response getServer() {
        return ok(JvmSettings.FQDN.lookup());
    }

    @GET
    @Path("applicationTermsOfUse")
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
    public Response getTermsOfUse() {
        return ok(systemConfig.getApiTermsOfUse());
    }

    @GET
    @Path("settings/incompleteMetadataViaApi")
    public Response getAllowsIncompleteMetadata() {
        return ok(JvmSettings.API_ALLOW_INCOMPLETE_METADATA.lookupOptional(Boolean.class).orElse(false));
    }

    @GET
    @Path("zipDownloadLimit")
    public Response getZipDownloadLimit() {
        long zipDownloadLimit = SystemConfig.getLongLimitFromStringOrDefault(settingsSvc.getValueForKey(SettingsServiceBean.Key.ZipDownloadLimit), SystemConfig.defaultZipDownloadLimit);
        return ok(zipDownloadLimit);
    }

    @GET
    @Path("exportFormats")
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
    public Response getCustomizationFile(@PathParam("customizationFileType") String customizationFileType) {
        Client client = ClientBuilder.newClient();
        WebTarget endpoint = client.target("http://localhost:8080/CustomizationFilesServlet");
        Response response = endpoint.queryParam("customFileType", customizationFileType)
                .request(MediaType.TEXT_HTML)
                .get();

        if (response.getLength() < 1) {
            return notFound(customizationFileType + " not found.");
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
