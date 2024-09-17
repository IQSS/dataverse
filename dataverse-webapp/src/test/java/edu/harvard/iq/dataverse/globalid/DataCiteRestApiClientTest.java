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
import java.util.List;

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
        assertThat(doiResponse).extracting(DataCiteFindDoiResponse::getCitationCount).isEqualTo(5);

        DataCiteFindDoiResponse.Attributes attributes = doiResponse.getAttributes();

        assertThat(attributes.getPrefix()).isEqualTo("10.5438");
        assertThat(attributes.getSuffix()).isEqualTo("0012");
        assertThat(attributes.getPublisher()).isEqualTo("DataCite");
        assertThat(attributes.getPublicationYear()).isEqualTo(2020);

        assertThat(attributes.getTitles()).hasSize(1);
        assertThat(attributes.getTitles().get(0).getTitle()).isEqualTo("Example");
        assertThat(attributes.getTitles().get(0).getTitleType()).isNull();
        assertThat(attributes.getTitles().get(0).getLang()).isNull();

        assertThat(attributes.getDates()).hasSize(0);
        assertThat(attributes.getTypes().getResourceTypeGeneral()).isEqualTo("Dataset");

        assertThat(attributes.getCreators()).hasSize(1);
        DataCiteFindDoiResponse.Creator creator = attributes.getCreators().get(0);
        assertThat(creator.getName()).isEqualTo("Example, Example");

        assertThat(attributes.getContributors()).hasSize(0);
        assertThat(attributes.getDescriptions()).hasSize(0);
        assertThat(attributes.getFundingReferences()).hasSize(0);
        assertThat(attributes.getRelatedIdentifiers()).hasSize(0);
    }

    @Test
    void findDoi__complete() throws IOException {
        // given
        wireMockServer.stubFor(get(urlEqualTo("/dois/10.5438/0012"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(UnitTestUtils.readFileToString("datacite/find_doi_complete.json"))));

        // when
        DataCiteFindDoiResponse doiResponse = dataCiteRestApiClient.findDoi("10.5438", "0012");

        // then
        assertThat(doiResponse).extracting(DataCiteFindDoiResponse::getCitationCount).isEqualTo(1);

        DataCiteFindDoiResponse.Attributes attributes = doiResponse.getAttributes();

        assertThat(attributes.getPrefix()).isEqualTo("10.18150");
        assertThat(attributes.getSuffix()).isEqualTo("0013");
        assertThat(attributes.getPublisher()).isEqualTo("RepOD");
        assertThat(attributes.getPublicationYear()).isEqualTo(2023);

        assertThat(attributes.getTitles()).hasSize(1);
        assertThat(attributes.getTitles().get(0).getTitle())
                .isEqualTo("The influence of parental influences during breeding periods on the success of fishes, an example of the white shark. Season 2017_2");
        assertThat(attributes.getTitles().get(0).getTitleType()).isNull();
        assertThat(attributes.getTitles().get(0).getLang()).isNull();

        assertThat(attributes.getDates()).hasSize(1);
        assertThat(attributes.getDates().get(0).getDate()).isEqualTo("2023");
        assertThat(attributes.getDates().get(0).getDateType()).isEqualTo("Issued");

        assertThat(attributes.getTypes().getResourceTypeGeneral()).isEqualTo("Dataset");

        assertThat(attributes.getCreators()).hasSize(1);
        DataCiteFindDoiResponse.Creator creator = attributes.getCreators().get(0);
        assertThat(creator.getName()).isEqualTo("Tobka, Marcell");

        assertThat(attributes.getContributors()).hasSize(1);
        DataCiteFindDoiResponse.Contributor contributor = attributes.getContributors().get(0);
        assertThat(contributor.getName()).isEqualTo("Tobka, Macell");
        assertThat(contributor.getGivenName()).isEqualTo("Macell");
        assertThat(contributor.getFamilyName()).isEqualTo("Tobka");
        assertThat(contributor.getContributorType()).isEqualTo(DataCiteFindDoiResponse.ContributorType.ContactPerson);
        assertThat(contributor.getAffiliation()).hasSize(1);
        assertThat(contributor.getAffiliation().get(0))
                .isEqualTo("Department of Zoology, Faculty of Veterinary Medicine and Animal Science, Pozna\u0144 University of Life Sciences");
        assertThat(contributor.getNameIdentifiers()).hasSize(1);
        assertThat(contributor.getNameIdentifiers().get(0).getNameIdentifier())
                .isEqualTo("https://orcid.org/0000-0003-1511-074X");
        assertThat(contributor.getNameIdentifiers().get(0).getNameIdentifierScheme())
                .isEqualTo("ORCID");
        assertThat(contributor.getNameIdentifiers().get(0).getSchemeUri())
                .isEqualTo("https://orcid.org");

        assertThat(attributes.getDescriptions()).hasSize(1);
        assertThat(attributes.getDescriptions().get(0).getDescription())
                .isEqualTo("Data on weather conditions (temp., precipitation, wind speed), graphical data (photographs from UFV), " +
                        "movement data obtained by GPS-GSM tracking, graphical data (JPEG, MP4) from trail cameras from nets of the white shark, " +
                        "biometric data of white shark nestlings (xlsx, csv.)");
        assertThat(attributes.getDescriptions().get(0).getDescriptionType()).isEqualTo("Abstract");

        assertThat(attributes.getFundingReferences()).hasSize(1);
        assertThat(attributes.getFundingReferences().get(0).getAwardNumber()).isEqualTo("2016/23/D/NZ1/12810");
        assertThat(attributes.getFundingReferences().get(0).getAwardTitle()).isNull();
        assertThat(attributes.getFundingReferences().get(0).getFunderIdentifier()).isEqualTo("https://ror.org/000aaabbb");
        assertThat(attributes.getFundingReferences().get(0).getFunderIdentifierType()).isEqualTo("ROR");
        assertThat(attributes.getFundingReferences().get(0).getFunderName()).isEqualTo("Narodowe Centrum Nauki");

        assertThat(attributes.getRelatedIdentifiers()).hasSize(2);
        assertThat(attributes.getRelatedIdentifiers().get(1).getRelationType()).isEqualTo("References");
        assertThat(attributes.getRelatedIdentifiers().get(1).getRelatedIdentifier()).isEqualTo("10.18150/0014");
        assertThat(attributes.getRelatedIdentifiers().get(1).getRelatedIdentifierType()).isEqualTo("DOI");
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
