package edu.harvard.iq.dataverse.globalid;

import com.google.api.client.util.Lists;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.cache.DOIDataCiteRegisterCache;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static edu.harvard.iq.dataverse.persistence.MocksFactory.create;
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

    @Mock
    private DatasetFieldServiceBean datasetFieldService;

    @InjectMocks
    private DOIDataCiteRegisterService doiDataCiteRegisterService;

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should create DataCite xml file")
    void getMetadataFromDvObject() {

        // given & when
        String xml = doiDataCiteRegisterService.getMetadataFromDvObject(
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
        when(version.getRootDataverseNameForCitation()).thenReturn("");
        when(version.getParsedTitle()).thenReturn("");

        // when
        String xml = doiDataCiteRegisterService.getMetadataFromDvObject("doi:test", Collections.emptyMap(), dataset);

        // then
        assertThat(xml).contains(
                "<description descriptionType=\"Abstract\">\u00A0Description\u00A0\u00A0\u00A01\u00A0</description>");
    }

    @Test
    void getMetadataFromDvObject_datasetWithAuthorAndFundingReference() {
        //given
        String identifier = "Testid";
        Dataset dataset = createDataset();
        dataset.setIdentifier(identifier);

        DatasetVersion dsv = new DatasetVersion();
        dsv.setDataset(dataset);
        addDatasetFieldsToDsVersion(dsv);

        dataset.setVersions(Collections.singletonList(dsv));

        final HashMap<String, String> metadata = new HashMap<>();
        metadata.put("datacite.publicationyear", "2002");

        //when
        String metadataFromDvObj = doiDataCiteRegisterService.getMetadataFromDvObject(identifier, metadata, dataset);

        //then
        assertThat(metadataFromDvObj).isEqualTo("<resource xsi:schemaLocation=\"http://datacite.org/schema/kernel-4 " +
                "http://schema.datacite.org/meta/kernel-4/metadata.xsd\"" +
                " xmlns=\"http://datacite.org/schema/kernel-4\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                "<identifier identifierType=\"DOI\">Testid</identifier>" +
                "<creators>" +
                "<creator>" +
                "<creatorName>authorName</creatorName>" +
                "<nameIdentifier schemeURI=\"https://orcid.org/\" nameIdentifierScheme=\"ORCID\">0000-0001-0002-0003</nameIdentifier>" +
                "<affiliation affiliationIdentifier=\"authorAffiliationIdentifier\" affiliationIdentifierScheme=\"ROR\">authorAffiliation</affiliation>" +
                "</creator>" +
                "</creators>" +
                "<titles>" +
                "<title>title</title>" +
                "</titles>" +
                "<publisher>:unav</publisher>" +
                "<publicationYear>2002</publicationYear>" +
                "<resourceType resourceTypeGeneral=\"Dataset\"/>" +
                "<descriptions>" +
                "<description descriptionType=\"Abstract\">dsDescriptionValue</description>" +
                "</descriptions>" +
                "<fundingReferences><fundingReference><funderName>grantNumberAgency</funderName><funderIdentifier " +
                "funderIdentifierType=\"ROR\">grantNumberAgencyIdentifier</funderIdentifier><awardNumber>grantNumberValue" +
                "</awardNumber></fundingReference></fundingReferences>" +
                "</resource>");

    }

    @Test
    void getMetadataFromDvObject_FundingReferenceWithNoAgency() {
        //given
        String identifier = "Testid";
        Dataset dataset = createDataset();
        dataset.setIdentifier(identifier);

        DatasetVersion dsv = new DatasetVersion();
        dsv.setDataset(dataset);
        addGrantFieldWithNoAgencyToDsVersion(dsv);

        dsv.getDatasetFieldByTypeName("grantNumber").get()
                .getDatasetFieldsChildren().removeIf(child -> child.getValue().equals("grantNumberAgency"));

        dataset.setVersions(Collections.singletonList(dsv));

        final HashMap<String, String> metadata = new HashMap<>();
        metadata.put("datacite.publicationyear", "2002");

        //when
        String metadataFromDvObj = doiDataCiteRegisterService.getMetadataFromDvObject(identifier, metadata, dataset);

        //then
        assertThat(metadataFromDvObj).isEqualTo("<resource xsi:schemaLocation=\"http://datacite.org/schema/kernel-4 " +
                "http://schema.datacite.org/meta/kernel-4/metadata.xsd\"" +
                " xmlns=\"http://datacite.org/schema/kernel-4\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                "<identifier identifierType=\"DOI\">Testid</identifier>" +
                "<titles>" +
                "<title></title>" +
                "</titles>" +
                "<publisher>:unav</publisher>" +
                "<publicationYear>2002</publicationYear>" +
                "<resourceType resourceTypeGeneral=\"Dataset\"/>" +
                "<descriptions>" +
                "<description descriptionType=\"Abstract\"/>" +
                "</descriptions>" +
                "</resource>");

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
                doiDataCiteRegisterService.getMetadataFromDvObject(identifier, new HashMap<>(), dataset));

        String uploadedXml = doiDataCiteRegisterService.reserveIdentifier(identifier, new HashMap<>(), dataset);

        //then
        Mockito.verify(em, times(1)).merge(Mockito.any(DOIDataCiteRegisterCache.class));
        assertThat(uploadedXml).isEqualTo("<resource xsi:schemaLocation=\"http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd\"" +
                                                  " xmlns=\"http://datacite.org/schema/kernel-4\"" +
                                                  " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                                                  "<identifier identifierType=\"DOI\">Testid</identifier>" +
                                                  "<titles><title></title></titles>" +
                                                  "<publisher>:unav</publisher>" +
                                                  "<publicationYear>9999</publicationYear>" +
                                                  "<resourceType resourceTypeGeneral=\"Dataset\"/>" +
                                                  "<descriptions>" +
                                                  "<description descriptionType=\"Abstract\"/>" +
                                                  "</descriptions>" +
                                                  "</resource>");

    }

    @Test
    void getMetadataForDeactivateIdentifier() {
        //given
        String identifier = "Testid";
        Dataset dataset = createDataset();
        dataset.setIdentifier(identifier);

        //when
        String metadataForDeactivateIdentifier = doiDataCiteRegisterService.getMetadataForDeactivateId(identifier);

        //then
        assertThat(metadataForDeactivateIdentifier).isEqualTo(
                "<resource xsi:schemaLocation=\"http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd\"" +
                  " xmlns=\"http://datacite.org/schema/kernel-4\"" +
                  " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
                  "<identifier identifierType=\"DOI\">Testid</identifier>" +
                  "<titles>" +
                  "<title>This item has been removed from publication</title>" +
                  "</titles>" +
                  "<publisher>:unav</publisher>" +
                  "<publicationYear>9999</publicationYear>" +
                  "<resourceType resourceTypeGeneral=\"Dataset\"/>" +
                  "<descriptions>" +
                  "<description descriptionType=\"Abstract\">:unav</description>" +
                  "</descriptions>" +
                  "</resource>");

    }

    // -------------------- PRIVATE --------------------

    private Dataset createDataset() {
        Dataverse dataverse = new Dataverse();
        Dataset dataset = new Dataset();
        dataset.setOwner(dataverse);
        return dataset;
    }

    private void addDatasetFieldsToDsVersion(DatasetVersion dsv) {
        List<DatasetField> fields = new ArrayList<>();

        addTitleField(fields);
        addDescriptionField(fields);
        addAuthorField(fields);
        addGrantNumberField(fields);

        dsv.setDatasetFields(fields);
    }

    private void addGrantFieldWithNoAgencyToDsVersion(DatasetVersion dsv) {
        DatasetField grantNumberField =
                create(DatasetFieldConstant.grantNumber, "",
                        create(DatasetFieldConstant.grantNumberAgencyIdentifier, "grantNumberAgencyIdentifier"),
                        create(DatasetFieldConstant.grantNumberProgram, "grantNumberProgram"),
                        create(DatasetFieldConstant.grantNumberValue, "grantNumberValue"));

        dsv.setDatasetFields(Collections.singletonList(grantNumberField));
    }

    private void addGrantNumberField(List<DatasetField> fields) {
        DatasetField grantNumberField =
                create(DatasetFieldConstant.grantNumber, "",
                        create(DatasetFieldConstant.grantNumberAgency, "grantNumberAgency"),
                        create(DatasetFieldConstant.grantNumberAgencyIdentifier, "grantNumberAgencyIdentifier"),
                        create(DatasetFieldConstant.grantNumberProgram, "grantNumberProgram"),
                        create(DatasetFieldConstant.grantNumberValue, "grantNumberValue"));

        fields.add(grantNumberField);
    }

    private void addAuthorField(List<DatasetField> fields) {
        DatasetField authorField =
                create(DatasetFieldConstant.author, "",
                        create(DatasetFieldConstant.authorName, "authorName"),
                        create(DatasetFieldConstant.authorAffiliation, "authorAffiliation"),
                        create(DatasetFieldConstant.authorAffiliationIdentifier, "authorAffiliationIdentifier"),
                        create(DatasetFieldConstant.authorIdType, "ORCID"),
                        create(DatasetFieldConstant.authorIdValue, "0000-0001-0002-0003"));

        fields.add(authorField);
    }

    private void addDescriptionField(List<DatasetField> fields) {
        DatasetField descriptionField =
                create(DatasetFieldConstant.description, "",
                        create(DatasetFieldConstant.descriptionText, "dsDescriptionValue"));

        fields.add(descriptionField);
    }

    private void addTitleField(List<DatasetField> fields) {
        fields.add(create(DatasetFieldConstant.title, "title"));
    }
}
