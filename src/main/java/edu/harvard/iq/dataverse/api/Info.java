package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.auth.*;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import java.util.Arrays;
import java.util.List;

@Path("info")
public class Info extends AbstractApiBean {

    private final SettingsServiceBean settingsService;
    private final SystemConfig systemConfig;

    private final SettingGroup dataverseSettingGroup;

    private static final String SETTING_GROUP_DATAVERSE = "dataverse";
    private static final String SETTING_GROUP_API = "api";

    private static final String SETTING_NAME_API_TERMS_OF_USE = "apiTermsOfUse";
    private static final String SETTING_NAME_API_ALLOW_INCOMPLETE_METADATA = "apiAllowIncompleteMetadata";

    @Inject
    public Info(SettingsServiceBean settingsService, SystemConfig systemConfig) {
        this.settingsService = settingsService;
        this.systemConfig = systemConfig;

        List<SettingItem> dataverseSettingItems = List.of(
                new SettingGroup(SETTING_GROUP_API, List.of(
                        new Setting<>(SETTING_NAME_API_TERMS_OF_USE, systemConfig.getApiTermsOfUse()),
                        new Setting<>(SETTING_NAME_API_ALLOW_INCOMPLETE_METADATA, JvmSettings.API_ALLOW_INCOMPLETE_METADATA.lookupOptional(Boolean.class).orElse(false)),
                        new SettingGroup("dummy", List.of(
                                new Setting<>("dummySetting", 10L)
                        )))
                ));
        dataverseSettingGroup = new SettingGroup(SETTING_GROUP_DATAVERSE, dataverseSettingItems);
    }

    private abstract static class SettingItem {
        protected String name;

        public SettingItem(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static class SettingGroup extends SettingItem {
        private final List<SettingItem> itemList;

        public SettingGroup(String name, List<SettingItem> itemList) {
            super(name);
            this.itemList = itemList;
        }

        public String getName() {
            return name;
        }

        private SettingItem getItemByName(String name) {
            for (SettingItem item : itemList) {
                if (item.getName().equals(name)) {
                    return item;
                }
            }
            return null;
        }

        private List<SettingItem> getItemList() {
            return this.itemList;
        }

        private SettingItem getItem(String[] orderedNamesRoute) {
            String subItemName = orderedNamesRoute[0];
            if (orderedNamesRoute.length == 1) {
                return getItemByName(subItemName);
            }
            for (SettingItem settingItem : itemList) {
                if (settingItem.getName().equals(subItemName)) {
                    return ((SettingGroup) settingItem).getItem(Arrays.copyOfRange(orderedNamesRoute, 1, orderedNamesRoute.length));
                }
            }
            return null;
        }
    }

    private static class Setting<T> extends SettingItem {
        private final T value;

        private Setting(String name, T value) {
            super(name);
            this.value = value;
        }

        public T getValue() {
            return value;
        }
    }

    private enum ExposedSettingsLookupMode {
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
        return transformSettingItemToResponse(settingItem, lookupMode);
    }

    private Response transformSettingItemToResponse(SettingItem settingItem, ExposedSettingsLookupMode lookupMode) {
        if (settingItem instanceof Setting) {
            return ok(((Setting<?>) settingItem).getValue());
        } else {
            return ok(transformSettingItemListToJsonObjectBuilder(((SettingGroup) settingItem).getItemList(), lookupMode).build());
        }
    }

    private JsonObjectBuilder transformSettingItemListToJsonObjectBuilder(List<SettingItem> settingItems, ExposedSettingsLookupMode lookupMode) {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        for (SettingItem settingItem : settingItems) {
            if (settingItem instanceof Setting) {
                Object settingValue = ((Setting<?>) settingItem).getValue();
                if (settingValue instanceof String) {
                    objectBuilder.add(settingItem.getName(), (String) settingValue);
                } else if (settingValue instanceof Long) {
                    objectBuilder.add(settingItem.getName(), (Long) settingValue);
                } else if (settingValue instanceof Boolean) {
                    objectBuilder.add(settingItem.getName(), (Boolean) settingValue);
                }
            }
            if (settingItem instanceof SettingGroup) {
                if (lookupMode == ExposedSettingsLookupMode.base) {
                    jsonArrayBuilder.add(settingItem.getName());
                } else if (lookupMode == ExposedSettingsLookupMode.sub) {
                    JsonObjectBuilder groupObjectBuilder = Json.createObjectBuilder();
                    groupObjectBuilder.add(settingItem.getName(), transformSettingItemListToJsonObjectBuilder(((SettingGroup) settingItem).getItemList(), lookupMode));
                    jsonArrayBuilder.add(groupObjectBuilder);
                }
            }
        }
        objectBuilder.add("settingSubGroups", jsonArrayBuilder);
        return objectBuilder;
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
