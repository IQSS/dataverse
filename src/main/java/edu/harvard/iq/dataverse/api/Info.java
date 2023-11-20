package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.api.exposedsettings.Setting;
import edu.harvard.iq.dataverse.api.exposedsettings.SettingGroup;
import edu.harvard.iq.dataverse.api.exposedsettings.SettingItem;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("info")
public class Info extends AbstractApiBean {

    private final SettingsServiceBean settingsService;
    private final SystemConfig systemConfig;

    private final SettingGroup dataverseSettingGroup;

    @Inject
    public Info(SettingsServiceBean settingsService, SystemConfig systemConfig) {
        this.settingsService = settingsService;
        this.systemConfig = systemConfig;
        dataverseSettingGroup = SettingGroup.getDataverseSettingGroup(systemConfig, settingsService);
    }

    public enum ExposedSettingsLookupMode {
        base, sub
    }

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
        String[] comps = versionStr.split("build", 2);
        String version = comps[0].trim();
        JsonValue build = comps.length > 1 ? Json.createArrayBuilder().add(comps[1].trim()).build().get(0) : JsonValue.NULL;

        return response(req -> ok(Json.createObjectBuilder().add("version", version).add("build", build)), getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("server")
    public Response getServer(@Context ContainerRequestContext crc) {
        return response(req -> ok(JvmSettings.FQDN.lookup()), getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("apiTermsOfUse")
    public Response getTermsOfUse(@Context ContainerRequestContext crc) {
        return response(req -> ok(systemConfig.getApiTermsOfUse()), getRequestUser(crc));
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
    @Path("exposedSettings")
    public Response getExposedSettings(@QueryParam("lookupMode") String mode) {
        return getExposedSettingsResponse(null, mode);
    }

    @GET
    @Path("exposedSettings/{path:.*}")
    public Response getExposedSettings(@PathParam("path") String path, @QueryParam("lookupMode") String mode) {
        return getExposedSettingsResponse(path, mode);
    }

    private Response getExposedSettingsResponse(String path, String mode) {
        ExposedSettingsLookupMode lookupMode;
        try {
            lookupMode = mode != null ? ExposedSettingsLookupMode.valueOf(mode) : ExposedSettingsLookupMode.base;
        } catch (IllegalArgumentException e) {
            return badRequest(BundleUtil.getStringFromBundle("info.api.exposedSettings.invalid.lookupMode", List.of(mode)));
        }
        SettingItem settingItem = ((path == null) ? dataverseSettingGroup : dataverseSettingGroup.getItem(path.split("/")));
        if (settingItem == null) {
            return notFound(BundleUtil.getStringFromBundle("info.api.exposedSettings.notFound"));
        }
        return ok(JsonPrinter.json(settingItem, lookupMode));
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
