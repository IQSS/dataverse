package edu.harvard.iq.dataverse.export.datacite;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResource.Creator;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResource.RelatedIdentifier;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static edu.harvard.iq.dataverse.persistence.MocksFactory.create;
import static edu.harvard.iq.dataverse.persistence.MocksFactory.createCVV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * This is only minimal test for the class, as the more comprehensive tests
 * are located in DOIDataCiteRegisterServiceTest.
 */
class DataCiteResourceCreatorTest {

    private Dataset dataset;

    @BeforeEach
    void setUp() {
        dataset = new Dataset();
        DatasetVersion version = new DatasetVersion();
        Dataverse dataverse = new Dataverse();
        version.setDataset(dataset);
        dataset.setOwner(dataverse);
        dataset.setVersions(Collections.singletonList(version));
    }


    @Test
    void create__basic() {
        // when
        DataCiteResource dataCiteResource = new DataCiteResourceCreator()
                .create("doi:10.5072/FK2/ZJLYL1", null, dataset);

        // then
        assertThat(dataCiteResource)
                .extracting(r -> r.getIdentifier().getValue(), DataCiteResource::getPublisher, DataCiteResource::getPublicationYear)
                .containsExactly("10.5072/FK2/ZJLYL1", ":unav", "9999");
        assertThat(dataCiteResource)
                .extracting(r -> r.getCreators().size(),
                        r -> r.getContributors().size(),
                        r -> r.getRelatedIdentifiers().size(),
                        r -> r.getFundingReferences().size())
                .containsExactly(0, 0, 0, 0);
    }

    @Test
    void create__withRelatedIdentifiers() {
        // given
        DatasetVersion version = dataset.getLatestVersion();
        version.setDatasetFields(Arrays.asList(
                create(DatasetFieldConstant.publication, "",
                        create(DatasetFieldConstant.publicationIDType, "DOI"),
                        create(DatasetFieldConstant.publicationIDNumber, "doi:10.5072/FK2/QENON1"),
                        create(DatasetFieldConstant.publicationRelationType, "IsPartOf")),
                create(DatasetFieldConstant.relatedDataset, "",
                        create(DatasetFieldConstant.relatedDatasetIDType, "url"),
                        create(DatasetFieldConstant.relatedDatasetURL, "http://localhost:8080/id-abc"),
                        create(DatasetFieldConstant.relatedDatasetRelationType, "IsPartOf")),
                create(DatasetFieldConstant.relatedMaterial, "",
                        create(DatasetFieldConstant.relatedMaterialIDType, "arXiv"),
                        create(DatasetFieldConstant.relatedMaterialIDNumber, "8675309"),
                        create(DatasetFieldConstant.relatedMaterialRelationType, "IsCitedBy"))));

        // when
        DataCiteResource dataCiteResource = new DataCiteResourceCreator().create("doi:10.5072/FK2/ZJLYL1", null, dataset);

        // then
        assertThat(dataCiteResource.getRelatedIdentifiers())
                .extracting(RelatedIdentifier::getValue, RelatedIdentifier::getRelatedIdentifierType,
                        RelatedIdentifier::getRelationType)
                .containsExactlyInAnyOrder(
                        tuple("doi:10.5072/FK2/QENON1", "DOI", "IsPartOf"),
                        tuple("http://localhost:8080/id-abc", "URL", "IsPartOf"),
                        tuple("8675309", "arXiv", "IsCitedBy"));
    }

    @Test
    void create__withAuthors() {
        // given
        DatasetVersion version = dataset.getLatestVersion();
        version.setDatasetFields(Arrays.asList(
                create(DatasetFieldConstant.author, "",
                        create(DatasetFieldConstant.authorName, "Doe, John"),
                        create(DatasetFieldConstant.authorAffiliationIdentifier, "https://ror.org/010203040"),
                        create(DatasetFieldConstant.authorAffiliation, "Some University"),
                        createCVV(DatasetFieldConstant.authorIdType, "ORCID"),
                        create(DatasetFieldConstant.authorIdValue, "0000-0001-0002-0003")),
                create(DatasetFieldConstant.author, "",
                        create(DatasetFieldConstant.authorName, "Doe, Jane"),
                        createCVV(DatasetFieldConstant.authorIdType, "ISNI"),
                        create(DatasetFieldConstant.authorIdValue, "0001000200030004"))
                ));

        // when
        DataCiteResource dataCiteResource = new DataCiteResourceCreator().create("", null, dataset);

        // then
        assertThat(dataCiteResource.getCreators())
                .extracting(
                        Creator::getCreatorName,
                        c -> c.getAffiliation() != null ? c.getAffiliation().getAffiliationIdentifierScheme() : null,
                        c -> c.getAffiliation() != null ? c.getAffiliation().getAffiliationIdentifier() : null,
                        c -> c.getAffiliation() != null ? c.getAffiliation().getValue() : null,
                        c -> c.getNameIdentifier() != null ? c.getNameIdentifier().getNameIdentifierScheme() : null,
                        c -> c.getNameIdentifier() != null ? c.getNameIdentifier().getSchemeURI() : null,
                        c -> c.getNameIdentifier() != null ? c.getNameIdentifier().getValue() : null)
                .containsExactlyInAnyOrder(
                        tuple("Doe, John", "ROR", "https://ror.org/010203040", "Some University", "ORCID", "https://orcid.org/", "0000-0001-0002-0003"),
                        tuple("Doe, Jane", null, null, null, "ISNI", "http://isni.org/isni/", "0001000200030004"));
    }
}