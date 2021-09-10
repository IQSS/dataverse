package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfoTest {

    @Mock
    private SettingsServiceBean settingsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    protected SystemConfig systemConfig;

    @Mock
    private AuthenticationServiceBean authSvc;

    @Mock
    private LicenseRepository licenseRepository;

    @Mock
    protected PrivateUrlServiceBean privateUrlSvc;

    AuthenticatedUser authenticatedUser = new AuthenticatedUser();

    @InjectMocks
    private Info endpoint = new Info(settingsService, licenseRepository, systemConfig);


    @BeforeEach
    void setUp() {
        String token = "123456";
        when(request.getParameter("key")).thenReturn(token);
        authenticatedUser.setSuperuser(true);
        when(authSvc.lookupUser(token)).thenReturn(authenticatedUser);
        when(systemConfig.isReadonlyMode()).thenReturn(true);
    }

    // -------------------- TESTS --------------------

    @Test
    void getActiveLicenses() {
        // given
        List<License> activeLicenses = createActiveLicenses("License 1", "License 2");
        Mockito.when(licenseRepository.findActiveOrderedByPosition()).thenReturn(activeLicenses);

        // when
        Response response = endpoint.getActiveLicenses();

        // then
        String json = (String) response.getEntity();
        assertThat(json)
                .containsSequence("\"status\"", ":", "\"OK\"")
                .containsSequence("\"code\"", ":", "200")
                .containsSequence("\"license\"", ":", "\"License 1\"")
                .containsSequence("\"license\"", ":", "\"License 2\"");
    }

    // -------------------- PRIVATE --------------------

    private List<License> createActiveLicenses(String... names) {
        List<License> licenses = new ArrayList<>();
        for (String name : names) {
            License license = new License();
            license.setName(name);
            license.setActive(true);
            licenses.add(license);
        }
        return licenses;
    }
}
