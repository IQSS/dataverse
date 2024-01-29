package edu.harvard.iq.dataverse.api;

import com.nimbusds.openid.connect.sdk.Prompt;
import edu.harvard.iq.dataverse.api.auth.WrappedAuthErrorResponse;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc.OIDCAuthProvider;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.ClockUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("info")
public class Info extends AbstractApiBean {
    @Inject
    @ClockUtil.LocalTime
    Clock clock;
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
    @Path("login/{provider}")
    public Response login(@PathParam("provider") String providerId) throws WrappedAuthErrorResponse {
        // same code as in BearerTokenAuth Module
        // get all OIDC Provider
        List<OIDCAuthProvider> providers = authSvc.getAuthenticationProviderIdsOfType(OIDCAuthProvider.class).stream()
                .map(provIds -> (OIDCAuthProvider) authSvc.getAuthenticationProvider(provIds))
                .collect(Collectors.toUnmodifiableList());
        // If not OIDC Provider are configured we cannot validate a Token
        if(providers.isEmpty()){
//            logger.log(Level.WARNING, "Bearer token detected, no OIDC provider configured");
            throw new WrappedAuthErrorResponse("BEARER_TOKEN_DETECTED_NO_OIDC_PROVIDER_CONFIGURED");
        }else{
            // check if the requested provider exists
            Optional<OIDCAuthProvider> oidcAuthProvider =providers.stream().filter(s-> {
                return s.getId().equals(providerId);
            }).findAny();
            if(oidcAuthProvider.isPresent()){
                // this part is copied and adapted for OAuth2LoginBackingBean...

                SecureRandom rand = new SecureRandom();

                String base = oidcAuthProvider.get().getId() + "~" + this.clock.millis()
                        + "~" + rand.nextInt(1000)
                        + "";

                String encrypted = StringUtil.encrypt(base, oidcAuthProvider.get().getClientSecret());
                final String state = oidcAuthProvider.get().getId() + "~" + encrypted;
                // create AuthZ request, in contrast to the normal one we explicit set Prompt.Type.CONSENT
                // Since we want a different behavior after authz, we pass a property (`?signup=true`) to the callback URL.
                String url =oidcAuthProvider.get().buildAuthzUrl(state,systemConfig.getOAuth2CallbackUrl()+"?signup=true",Optional.of(Prompt.Type.CONSENT));
                return Response.temporaryRedirect(URI.create(url)).build();
            }
        }
        return ok("this did not work");
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
