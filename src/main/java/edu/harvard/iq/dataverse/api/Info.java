package edu.harvard.iq.dataverse.api;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("info")
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
    @Path("apiTermsOfUse")
    public Response getTermsOfUse() {
        return ok(systemConfig.getApiTermsOfUse());
    }

    /*
     * On Dataverse 6.0 Payara was updated and the openapi endpoint stopped working: https://github.com/payara/Payara/issues/6369
     * 
     * This caused the url /openapi/ to stop working and caused an exception on the server: https://github.com/IQSS/dataverse/issues/9981
     * 
     * We incorporated the SmallRye OpenAPI plugin on our POM (https://github.com/smallrye/smallrye-open-api/tree/main/tools/maven-plugin) 
     * which will generate files for YAML and JSON formats and deposit them on edu/harvard/iq/dataverse/openapi/
     * these files will be provided by this endpoint depending on the format requested.
     * 
     */
    @GET
    @Path("openapi/{format}")
    public Response getOpenapiSpec(@PathParam("format") String format) {
        
        //We use the lowercase of the specified format to define the mediatype and will be used as the file extension.
        String requestedFormat = format.toLowerCase();
        MediaType mediaType = null;

        if (requestedFormat.equals("json")){
            mediaType = MediaType.APPLICATION_JSON_TYPE;
        } else if (requestedFormat.equals("yaml")){
            mediaType = MediaType.TEXT_PLAIN_TYPE;
        } else {
            //If the format is not supported we will return a 400 Bad Request
            List<String> args = Arrays.asList(format);
            String bundleResponse = BundleUtil.getStringFromBundle("info.api.exception.invalid.format", args);
            return error(Response.Status.BAD_REQUEST, bundleResponse);
        }
        
        try {
            //We create an input stream based on the requested format and will return the content of the file as a response.
            String baseFileName = "edu/harvard/iq/dataverse/openapi/dataverse_openapi." + requestedFormat;
            InputStream openapiDefinitionStream  = Info.class.getClassLoader().getResourceAsStream(baseFileName); 
            return Response.ok().entity(IOUtils.toString(openapiDefinitionStream, StandardCharsets.UTF_8))
                        .type(mediaType).build();
        } catch (Exception ex) {
            //If a supported file is not found we will return a 400 Bad Request with an exception.
            logger.log(Level.SEVERE, "OpenApi Definition format not found " + format + ":" + ex.getMessage(), ex);
            List<String> args = Arrays.asList(format);
            String bundleResponse = BundleUtil.getStringFromBundle("info.api.exception", args);
            return error(Response.Status.BAD_REQUEST, bundleResponse);  
        }        
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

    private Response getSettingResponseByKey(SettingsServiceBean.Key key) {
        String setting = settingsService.getValueForKey(key);
        if (setting != null) {
            return ok(Json.createObjectBuilder().add("message", setting));
        } else {
            return notFound("Setting " + key + " not found");
        }
    }
}
