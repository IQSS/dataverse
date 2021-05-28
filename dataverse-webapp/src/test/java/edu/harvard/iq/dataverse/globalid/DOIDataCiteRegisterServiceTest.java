package edu.harvard.iq.dataverse.globalid;

import com.google.api.client.util.Lists;
import edu.harvard.iq.dataverse.persistence.cache.DOIDataCiteRegisterCache;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DOIDataCiteRegisterServiceTest {

    @Mock
    private EntityManager em;

    @Mock
    private TypedQuery typedQuery;

    @Mock
    private DataCiteMdsApiClient dataCiteRESTfulClient;

    @InjectMocks
    private DOIDataCiteRegisterService doiDataCiteRegisterService;

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should create DataCite xml file")
    void getMetadataFromDvObject() {

        // given & when
        String xml = DOIDataCiteRegisterService.getMetadataFromDvObject(
                "doi:test", Collections.emptyMap(), createDataset());

        // then
        assertThat(xml).isNotBlank();
    }

    @Test
    @DisplayName("Should remove non-breaking spaces from description")
    void getMetadataFromDvObject__removeNonBreakingSpaces() {

        // given
        Dataset dataset = createDataset();
        DatasetVersion version = mock(DatasetVersion.class);
        dataset.setVersions(Collections.singletonList(version));
        when(version.getDescriptionPlainText()).thenReturn("&nbsp;Description&nbsp;&nbsp;&nbsp;1&nbsp;");
        when(version.getRootDataverseNameforCitation()).thenReturn("");
        when(version.getTitle()).thenReturn("");

        // when
        String xml = DOIDataCiteRegisterService.getMetadataFromDvObject(
                "doi:test", Collections.emptyMap(), dataset);

        // then
        assertThat(xml).contains(
                "<description descriptionType=\"Abstract\">\u00A0Description\u00A0\u00A0\u00A01\u00A0</description>");
    }

    @Test
    void reserveIdentifier() throws IOException {
        //given
        String identifier = "Testid";
        Dataset dataset = createDataset();
        dataset.setIdentifier(identifier);

        //when
        Mockito.when(em.createNamedQuery("DOIDataCiteRegisterCache.findByDoi",
                                         DOIDataCiteRegisterCache.class))
               .thenReturn(typedQuery);

        Mockito.when(((typedQuery).getResultList()))
               .thenReturn(Lists.newArrayList());

        Mockito.when(dataCiteRESTfulClient.postMetadata(Mockito.any())).thenReturn(
                DOIDataCiteRegisterService.getMetadataFromDvObject(identifier, new HashMap<>(), dataset));

        String uploadedXml = doiDataCiteRegisterService.reserveIdentifier(identifier, new HashMap<>(), dataset);

        //then
        Mockito.verify(em, times(1)).merge(Mockito.any(DOIDataCiteRegisterCache.class));
        assertThat(uploadedXml).isEqualTo("<resource xsi:schemaLocation=\"http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd\"\n" +
                                                  "          xmlns=\"http://datacite.org/schema/kernel-4\"\n" +
                                                  "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                                                  "    <identifier identifierType=\"DOI\">Testid</identifier>\n" +
                                                  "    <creators></creators>\n" +
                                                  "    <titles>\n" +
                                                  "        <title></title>\n" +
                                                  "    </titles>\n" +
                                                  "    <publisher>:unav</publisher>\n" +
                                                  "    <publicationYear>9999</publicationYear>\n" +
                                                  "    <resourceType resourceTypeGeneral=\"Dataset\"/>\n" +
                                                  "    \n" +
                                                  "    <descriptions>\n" +
                                                  "        <description descriptionType=\"Abstract\"></description>\n" +
                                                  "    </descriptions>\n" +
                                                  "    <contributors></contributors>\n" +
                                                  "</resource>\n");

    }

    @Test
    void getMetadataForDeactivateIdentifier() {
        //given
        String identifier = "Testid";
        Dataset dataset = createDataset();
        dataset.setIdentifier(identifier);

        final HashMap<String, String> metadata = new HashMap<>();
        metadata.put("datacite.title", "testTitle");
        metadata.put("datacite.publicationyear", "2002");

        //when
        String metadataForDeactivateIdentifier = DOIDataCiteRegisterService
                .getMetadataForDeactivateIdentifier(identifier, metadata, dataset);

        //then
        assertThat(metadataForDeactivateIdentifier).isEqualTo("<resource xsi:schemaLocation=\"http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd\"\n" +
                                                                      "          xmlns=\"http://datacite.org/schema/kernel-4\"\n" +
                                                                      "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                                                                      "    <identifier identifierType=\"DOI\">Testid</identifier>\n" +
                                                                      "    <creators></creators>\n" +
                                                                      "    <titles>\n" +
                                                                      "        <title>testTitle</title>\n" +
                                                                      "    </titles>\n" +
                                                                      "    <publisher>:unav</publisher>\n" +
                                                                      "    <publicationYear>2002</publicationYear>\n" +
                                                                      "    <resourceType resourceTypeGeneral=\"Dataset\"/>\n" +
                                                                      "    \n" +
                                                                      "    <descriptions>\n" +
                                                                      "        <description descriptionType=\"Abstract\">:unav</description>\n" +
                                                                      "    </descriptions>\n" +
                                                                      "    <contributors></contributors>\n" +
                                                                      "</resource>\n");

    }

    // -------------------- PRIVATE --------------------

    private Dataset createDataset() {
        Dataverse dataverse = new Dataverse();
        Dataset dataset = new Dataset();
        dataset.setOwner(dataverse);
        return dataset;
    }
}