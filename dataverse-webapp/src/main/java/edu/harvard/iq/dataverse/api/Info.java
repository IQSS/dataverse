package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.dto.ActiveLicenseDTO;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.json.Json;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Path("info")
public class Info extends AbstractApiBean {

    private static final Logger logger = LoggerFactory.getLogger(Info.class);

    private SettingsServiceBean settingsService;
    private LicenseRepository licenseRepository;
    private SystemConfig systemConfig;

    // -------------------- CONSTRUCTORS --------------------

    public Info() { }

    @Inject
    public Info(SettingsServiceBean settingsService, LicenseRepository licenseRepository, SystemConfig systemConfig) {
        this.settingsService = settingsService;
        this.licenseRepository = licenseRepository;
        this.systemConfig = systemConfig;
    }

    // -------------------- LOGIC --------------------

    @GET
    @Path("settings/:DatasetPublishPopupCustomText")
    public Response getDatasetPublishPopupCustomText() {
        String setting = settingsService.getValueForKey(SettingsServiceBean.Key.DatasetPublishPopupCustomText);
        if (setting != null) {
            return ok(Json.createObjectBuilder().add("message", setting));
        } else {
            return notFound("Setting " + SettingsServiceBean.Key.DatasetPublishPopupCustomText + " not found");
        }
    }

    @GET
    @Path("version")
    public Response getInfo() {
        String versionStr = systemConfig.getVersionWithBuild();

        return allowCors(response(req -> ok(Json.createObjectBuilder().add("version", versionStr))));
    }

    @GET
    @Path("server")
    public Response getServer() {
        return response(req -> ok(systemConfig.getDataverseServer()));
    }

    @GET
    @Path("apiTermsOfUse")
    public Response getTermsOfUse() {
        return allowCors(response(req -> ok(systemConfig.getApiTermsOfUse())));
    }

    @GET
    @Path("activeLicenses")
    public Response getActiveLicenses() {
        List<ActiveLicenseDTO> activeLicenses = licenseRepository.findActiveOrderedByPosition().stream()
                .map(License::getName)
                .map(ActiveLicenseDTO::new)
                .collect(Collectors.toList());

        return allowCors(response(r -> ok(activeLicenses)));
    }
}
