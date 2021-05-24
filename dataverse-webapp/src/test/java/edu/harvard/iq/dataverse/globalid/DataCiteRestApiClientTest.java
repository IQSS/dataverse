package edu.harvard.iq.dataverse.globalid;

import com.github.tomakehurst.wiremock.WireMockServer;
import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import org.apache.http.client.HttpResponseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataCiteRestApiClientTest {

    @InjectMocks
    private DataCiteRestApiClient dataCiteRestApiClient;

    @Mock
    private SettingsServiceBean settingsService;
    
    private static WireMockServer wireMockServer;
    
    @BeforeAll
    public static void beforeAll() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
    }
    
    @BeforeEach
    public void beforeEach() throws IOException {
        wireMockServer.resetAll();
        dataCiteRestApiClient.postConstruct();

        when(settingsService.getValueForKey(Key.DoiDataCiteRestApiUrl)).thenReturn("http://localhost:" + wireMockServer.port());
    }
    
    @AfterAll
    public static void afterAll() {
        wireMockServer.stop();
    }
    
    // -------------------- TESTS --------------------
    
    @Test
    void findDoi() throws IOException {
        // given
        wireMockServer.stubFor(get(urlEqualTo("/dois/10.5438/0012"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(UnitTestUtils.readFileToString("datacite/find_doi_sample_response.json"))));

        // when
        DataCiteFindDoiResponse doiResponse = dataCiteRestApiClient.findDoi("10.5438", "0012");
        // then
        assertThat(doiResponse).extracting(DataCiteFindDoiResponse::getCitationCount)
            .isEqualTo(5);
    }
    
    @Test
    void findDoi_doi_identifier_with_shoulder() throws IOException {
        // given
        wireMockServer.stubFor(get(urlEqualTo("/dois/10.5438/FK2/0012"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(UnitTestUtils.readFileToString("datacite/find_doi_sample_response.json"))));

        // when
        DataCiteFindDoiResponse doiResponse = dataCiteRestApiClient.findDoi("10.5438", "FK2/0012");
        // then
        assertThat(doiResponse).extracting(DataCiteFindDoiResponse::getCitationCount)
            .isEqualTo(5);
    }
    
    @Test
    void findDoi_not_found() throws IOException {
        // given
        wireMockServer.stubFor(get(urlEqualTo("/dois/10.5438/0012"))
                .willReturn(aResponse().withStatus(404)));

        // when & then
        assertThatThrownBy(() -> dataCiteRestApiClient.findDoi("10.5438", "0012"))
            .isInstanceOf(HttpResponseException.class)
            .hasMessageContaining("status code: 404");
    }
}
