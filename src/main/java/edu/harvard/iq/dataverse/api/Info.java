package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

@Path("info")
public class Info extends AbstractApiBean {

    @EJB
    SettingsServiceBean settingsService;
    
    @EJB
    SystemConfig systemConfig;

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
    @AuthRequired
    @Path("version")
    public Response getInfo(@Context ContainerRequestContext crc) {
        String versionStr = systemConfig.getVersion(true);
        String[] comps = versionStr.split("build",2);
        String version = comps[0].trim();
        JsonValue build = comps.length > 1 ? Json.createArrayBuilder().add(comps[1].trim()).build().get(0) : JsonValue.NULL;

        return response( req -> ok( Json.createObjectBuilder().add("version", version)
                                                              .add("build", build)), getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("server")
    public Response getServer(@Context ContainerRequestContext crc) {
        return response( req -> ok(JvmSettings.FQDN.lookup()), getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("apiTermsOfUse")
    public Response getTermsOfUse(@Context ContainerRequestContext crc) {
        return response( req -> ok(systemConfig.getApiTermsOfUse()), getRequestUser(crc));
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

    @GET
    @AuthRequired
    @Path("settingGroups/{groupName}")
    public Response getGroupSettings(@Context ContainerRequestContext crc, @PathParam("groupName") String groupName) {
        return ok("Not implemented!");
    }

    @GET
    @AuthRequired
    @Path("settingScopes/{groupName}/{settingName}")
    public Response getSetting(@Context ContainerRequestContext crc, @PathParam("groupName") String groupName, @PathParam("settingName") String settingName) {
        return ok("Not implemented!");
    }
}
