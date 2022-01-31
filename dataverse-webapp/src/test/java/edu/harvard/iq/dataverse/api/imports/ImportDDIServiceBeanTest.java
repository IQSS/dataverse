package edu.harvard.iq.dataverse.api.imports;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FileMetadataDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockWithFieldsDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetFieldDTO;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static edu.harvard.iq.dataverse.api.dto.DatasetFieldDTOFactory.createCompound;
import static edu.harvard.iq.dataverse.api.dto.DatasetFieldDTOFactory.createMultipleCompound;
import static edu.harvard.iq.dataverse.api.dto.DatasetFieldDTOFactory.createMultiplePrimitive;
import static edu.harvard.iq.dataverse.api.dto.DatasetFieldDTOFactory.createPrimitive;
import static edu.harvard.iq.dataverse.api.dto.DatasetFieldDTOFactory.createVocabulary;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@ExtendWith(MockitoExtension.class)
public class ImportDDIServiceBeanTest {

    @InjectMocks
    private ImportDDIServiceBean importDdiService;

    // -------------------- TESTS --------------------

    @Test
    void doImport_should_parse_dataset_doi_global_id_from_stdyDscr() throws IOException, XMLStreamException, ImportException {
        // given
        String ddiXml = "<codeBook>"
                + "<stdyDscr><citation><titlStmt>"
                + "<IDNo agency=\"DOI\">doesntmatter:10.18150/FK2/AJ4VYO</IDNo>"
                + "</titlStmt></citation></stdyDscr>"
                + "</codeBook>";

        // when
        DatasetDTO datasetDto = importDdiService.doImport(ImportType.HARVEST, ddiXml);

        // then
        assertThat(datasetDto.getProtocol()).isEqualTo("doi");
        assertThat(datasetDto.getAuthority()).isEqualTo("10.18150");
        assertThat(datasetDto.getIdentifier()).isEqualTo("FK2/AJ4VYO");
        assertThat(datasetDto.getDatasetVersion().getVersionState()).isEqualTo(VersionState.RELEASED.name());
    }

    @Test
    void doImport_should_parse_dataset_handle_global_id_from_stdyDscr() throws IOException, XMLStreamException, ImportException {
        // given
        String ddiXml = "<codeBook>"
                + "<stdyDscr><citation><titlStmt>"
                + "<IDNo agency=\"handle\">doesntmatter:20.1000/100</IDNo>"
                + "</titlStmt></citation></stdyDscr>"
                + "</codeBook>";

        // when
        DatasetDTO datasetDto = importDdiService.doImport(ImportType.HARVEST, ddiXml);

        // then
        assertThat(datasetDto.getProtocol()).isEqualTo("hdl");
        assertThat(datasetDto.getAuthority()).isEqualTo("20.1000");
        assertThat(datasetDto.getIdentifier()).isEqualTo("100");
        assertThat(datasetDto.getDatasetVersion().getVersionState()).isEqualTo(VersionState.RELEASED.name());
    }

    @Test
    void doImport_should_parse_dataset_dara_doi_global_id_from_stdyDscr() throws IOException, XMLStreamException, ImportException {
        // given
        String ddiXml = "<codeBook>"
                + "<stdyDscr><citation><titlStmt>"
                + "<IDNo agency=\"dara\">10.18150/FK2/AJ4VYO</IDNo>"
                + "</titlStmt></citation></stdyDscr>"
                + "</codeBook>";

        // when
        DatasetDTO datasetDto = importDdiService.doImport(ImportType.HARVEST, ddiXml);

        // then
        assertThat(datasetDto.getProtocol()).isEqualTo("doi");
        assertThat(datasetDto.getAuthority()).isEqualTo("10.18150");
        assertThat(datasetDto.getIdentifier()).isEqualTo("FK2/AJ4VYO");
        assertThat(datasetDto.getDatasetVersion().getVersionState()).isEqualTo(VersionState.RELEASED.name());
    }

    @Test
    void doImport_should_parse_stdyDscr_citation_related_metadata_fields() throws XMLStreamException, ImportException, IOException {
        // given
        String ddiXml = UnitTestUtils.readFileToString("xml/export/ddi/dataset-all-ddi-metadata-fields.xml");

        // when
        DatasetDTO datasetDto = importDdiService.doImport(ImportType.HARVEST, ddiXml);

        // then
        DatasetVersionDTO datasetVersionDto = datasetDto.getDatasetVersion();

        assertThat(datasetVersionDto.getMetadataBlocks()).containsOnlyKeys("citation", "socialscience", "geospatial");

        MetadataBlockWithFieldsDTO citationBlock = datasetVersionDto.getMetadataBlocks().get("citation");

        assertThat(getField(DatasetFieldConstant.title, citationBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.title, "All metadata Dataset"));

        assertThat(getField(DatasetFieldConstant.subTitle, citationBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.subTitle, "Subtitle"));

        assertThat(getField(DatasetFieldConstant.alternativeTitle, citationBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.alternativeTitle, "variant of title"));

        assertThat(getField(DatasetFieldConstant.otherId, citationBlock)).isEqualTo(
                createMultipleCompound(DatasetFieldConstant.otherId,
                                               ImmutableList.of(
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.otherIdAgency, "Other id agency"),
                                                               createPrimitive(DatasetFieldConstant.otherIdValue, "123")),
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.otherIdAgency, "Other id agency 2"),
                                                               createPrimitive(DatasetFieldConstant.otherIdValue, "124"))
                                               )));

        assertThat(getField(DatasetFieldConstant.author, citationBlock)).isEqualTo(
                createMultipleCompound(DatasetFieldConstant.author,
                                               ImmutableList.of(
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.authorName, "First, Author"),
                                                               createPrimitive(DatasetFieldConstant.authorAffiliation, "author1 aff")),
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.authorName, "Second, Author"),
                                                               createPrimitive(DatasetFieldConstant.authorAffiliation, "author2 Aff"))
                                               )));

        assertThat(getField(DatasetFieldConstant.producer, citationBlock)).isEqualTo(
                createMultipleCompound(DatasetFieldConstant.producer,
                                               ImmutableList.of(
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.producerName, "Some, Producer"),
                                                               createPrimitive(DatasetFieldConstant.producerAbbreviation, "PRODU"),
                                                               createPrimitive(DatasetFieldConstant.producerAffiliation, "producer aff"),
                                                               createPrimitive(DatasetFieldConstant.producerLogo, "https://example.com/images/producer.jpg"))
                                               )));

        assertThat(getField(DatasetFieldConstant.productionDate, citationBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.productionDate, "2001-12-12"));

        assertThat(getField(DatasetFieldConstant.productionPlace, citationBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.productionPlace, "Some production place"));

        assertThat(getField(DatasetFieldConstant.software, citationBlock)).isEqualTo(
                createMultipleCompound(DatasetFieldConstant.software,
                                               ImmutableList.of(
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.softwareName, "Software1"),
                                                               createPrimitive(DatasetFieldConstant.softwareVersion, "v1")),
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.softwareName, "Software2"),
                                                               createPrimitive(DatasetFieldConstant.softwareVersion, "v2"))
                                               )));

        assertThat(getField(DatasetFieldConstant.grantNumber, citationBlock)).isEqualTo(
                createMultipleCompound(DatasetFieldConstant.grantNumber,
                                               ImmutableList.of(
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.grantNumberAgency, "Grant Agency 1"),
                                                               createPrimitive(DatasetFieldConstant.grantNumberValue, "GRANT.1")),
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.grantNumberAgency, "Grant Agency 2"),
                                                               createPrimitive(DatasetFieldConstant.grantNumberValue, "GRANT.2"))
                                               )));


        assertThat(getField(DatasetFieldConstant.distributor, citationBlock)).isEqualTo(
                createMultipleCompound(DatasetFieldConstant.distributor,
                                               ImmutableList.of(
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.distributorName, "Some, Distributor1"),
                                                               createPrimitive(DatasetFieldConstant.distributorAbbreviation, "DIST1"),
                                                               createPrimitive(DatasetFieldConstant.distributorAffiliation, "distributor1 aff"),
                                                               createPrimitive(DatasetFieldConstant.distributorURL, "http://distributor1.org")),
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.distributorName, "Some, Distributor2"),
                                                               createPrimitive(DatasetFieldConstant.distributorAbbreviation, "DIST2"),
                                                               createPrimitive(DatasetFieldConstant.distributorAffiliation, "distributor2 aff"),
                                                               createPrimitive(DatasetFieldConstant.distributorURL, "http://distributor2.org"))
                                               )));

        assertThat(getField(DatasetFieldConstant.datasetContact, citationBlock)).isEqualTo(
                createMultipleCompound(DatasetFieldConstant.datasetContact,
                                               ImmutableList.of(
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.datasetContactName, "First, Contact"),
                                                               createPrimitive(DatasetFieldConstant.datasetContactAffiliation, "contact1 Aff"),
                                                               createPrimitive(DatasetFieldConstant.datasetContactEmail, "contact1@example.com")),
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.datasetContactName, "Second, Contact"),
                                                               createPrimitive(DatasetFieldConstant.datasetContactAffiliation, "contact2 Aff"),
                                                               createPrimitive(DatasetFieldConstant.datasetContactEmail, "contact2@example.com"))
                                               )));


        assertThat(getField(DatasetFieldConstant.depositor, citationBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.depositor, "Some, Depositor"));

        assertThat(getField(DatasetFieldConstant.dateOfDeposit, citationBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.dateOfDeposit, "2021-02-15"));

        assertThat(getField(DatasetFieldConstant.distributionDate, citationBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.distributionDate, "1990"));

        assertThat(getField(DatasetFieldConstant.series, citationBlock)).isEqualTo(
                createCompound(DatasetFieldConstant.series,
                                       createPrimitive(DatasetFieldConstant.seriesName, "series name"),
                                       createPrimitive(DatasetFieldConstant.seriesInformation, "series info")));

        MetadataBlockWithFieldsDTO socialBlock = datasetVersionDto.getMetadataBlocks().get("socialscience");

        assertThat(getField(DatasetFieldConstant.datasetLevelErrorNotes, socialBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.datasetLevelErrorNotes, "notes on errors in dataset level"));

    }

    @Test
    void doImport_should_parse_stdyDscr_stdyInfo_related_metadata_fields() throws XMLStreamException, ImportException, IOException {
        // given
        String ddiXml = UnitTestUtils.readFileToString("xml/export/ddi/dataset-all-ddi-metadata-fields.xml");

        // when
        DatasetDTO datasetDto = importDdiService.doImport(ImportType.HARVEST, ddiXml);

        // then
        DatasetVersionDTO datasetVersionDto = datasetDto.getDatasetVersion();
        MetadataBlockWithFieldsDTO citationBlock = datasetVersionDto.getMetadataBlocks().get("citation");
        MetadataBlockWithFieldsDTO geospatialBlock = datasetVersionDto.getMetadataBlocks().get("geospatial");
        MetadataBlockWithFieldsDTO socialBlock = datasetVersionDto.getMetadataBlocks().get("socialscience");

        assertThat(getField(DatasetFieldConstant.keyword, citationBlock)).isEqualTo(
                createMultipleCompound(DatasetFieldConstant.keyword,
                                               ImmutableList.of(
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.keywordValue, "Astronomy and Astrophysics")),
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.keywordValue, "Chemistry")),
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.keywordValue, "keyword 1"),
                                                               createPrimitive(DatasetFieldConstant.keywordVocab, "LCSH"),
                                                               createPrimitive(DatasetFieldConstant.keywordVocabURI, "http://lcsg.com")),
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.keywordValue, "keyword 2"),
                                                               createPrimitive(DatasetFieldConstant.keywordVocab, "MeSH"),
                                                               createPrimitive(DatasetFieldConstant.keywordVocabURI, "http://mesh.com"))
                                               )));

        assertThat(getField(DatasetFieldConstant.topicClassification, citationBlock)).isEqualTo(
                createMultipleCompound(DatasetFieldConstant.topicClassification,
                                               ImmutableList.of(
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.topicClassValue, "topic 1"),
                                                               createPrimitive(DatasetFieldConstant.topicClassVocab, "dict"),
                                                               createPrimitive(DatasetFieldConstant.topicClassVocabURI, "http://dict.com")),
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.topicClassValue, "topic 2"),
                                                               createPrimitive(DatasetFieldConstant.topicClassVocab, "dict 2"),
                                                               createPrimitive(DatasetFieldConstant.topicClassVocabURI, "http://dict2.com"))
                                               )));

        assertThat(getField(DatasetFieldConstant.description, citationBlock)).isEqualTo(
                createMultipleCompound(DatasetFieldConstant.description,
                                               ImmutableList.of(
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.descriptionText, "<p>some description</p>"),
                                                               createPrimitive(DatasetFieldConstant.descriptionDate, "1999")),
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.descriptionText, "<p>second description</p>"),
                                                               createPrimitive(DatasetFieldConstant.descriptionDate, "2000-08"))
                                               )));

        assertThat(getField(DatasetFieldConstant.timePeriodCovered, citationBlock)).isEqualTo(
                createMultipleCompound(DatasetFieldConstant.timePeriodCovered,
                                               ImmutableList.of(
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.timePeriodCoveredStart, "1001"),
                                                               createPrimitive(DatasetFieldConstant.timePeriodCoveredEnd, "1002")),
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.timePeriodCoveredStart, "1004"),
                                                               createPrimitive(DatasetFieldConstant.timePeriodCoveredEnd, "1005"))
                                               )));

        assertThat(getField(DatasetFieldConstant.dateOfCollection, citationBlock)).isEqualTo(
                createMultipleCompound(DatasetFieldConstant.dateOfCollection,
                                               ImmutableList.of(
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.dateOfCollectionStart, "1100"),
                                                               createPrimitive(DatasetFieldConstant.dateOfCollectionEnd, "1200")),
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.dateOfCollectionStart, "1205"),
                                                               createPrimitive(DatasetFieldConstant.dateOfCollectionEnd, "1210"))
                                               )));


        assertThat(getField(DatasetFieldConstant.geographicCoverage, geospatialBlock)).isEqualTo(
                createMultipleCompound(DatasetFieldConstant.geographicCoverage,
                                               ImmutableList.of(
                                                       ImmutableSet.of(
                                                               createVocabulary(DatasetFieldConstant.country, "Algeria")),
                                                       ImmutableSet.of(
                                                               createVocabulary(DatasetFieldConstant.country, "United States"),
                                                               createPrimitive(DatasetFieldConstant.otherGeographicCoverage,
                                                                                       "City in Algeria; State in Algeria; Other notes for geo coverage Algeria; Boston; Massachusetts; Other notes for geo coverage USA"))
                                               )));

        assertThat(getField(DatasetFieldConstant.geographicUnit, geospatialBlock)).isEqualTo(
                createMultiplePrimitive(DatasetFieldConstant.geographicUnit, ImmutableList.of("geo unit", "geo unit 2")));

        assertThat(getField(DatasetFieldConstant.geographicBoundingBox, geospatialBlock)).isEqualTo(
                createMultipleCompound(DatasetFieldConstant.geographicBoundingBox,
                                               ImmutableList.of(
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.northLatitude, "12"),
                                                               createPrimitive(DatasetFieldConstant.southLatitude, "-12"),
                                                               createPrimitive(DatasetFieldConstant.eastLongitude, "98"),
                                                               createPrimitive(DatasetFieldConstant.westLongitude, "90"))
                                               )));

        assertThat(getField(DatasetFieldConstant.unitOfAnalysis, socialBlock)).isEqualTo(
                createMultiplePrimitive(DatasetFieldConstant.unitOfAnalysis, ImmutableList.of("Family", "EventOrProcess")));

        assertThat(getField(DatasetFieldConstant.universe, socialBlock)).isEqualTo(
                createMultiplePrimitive(DatasetFieldConstant.universe, ImmutableList.of("social universe 1", "social universe 2")));

        assertThat(getField(DatasetFieldConstant.kindOfData, citationBlock)).isEqualTo(
                createMultiplePrimitive(DatasetFieldConstant.kindOfData, ImmutableList.of("Numeric", "Geospatial")));
    }

    @Test
    void doImport_should_parse_stdyDscr_method_related_metadata_fields() throws XMLStreamException, ImportException, IOException {
        // given
        String ddiXml = UnitTestUtils.readFileToString("xml/export/ddi/dataset-all-ddi-metadata-fields.xml");

        // when
        DatasetDTO datasetDto = importDdiService.doImport(ImportType.HARVEST, ddiXml);

        // then
        DatasetVersionDTO datasetVersionDto = datasetDto.getDatasetVersion();
        MetadataBlockWithFieldsDTO citationBlock = datasetVersionDto.getMetadataBlocks().get("citation");
        MetadataBlockWithFieldsDTO socialBlock = datasetVersionDto.getMetadataBlocks().get("socialscience");

        assertThat(getField(DatasetFieldConstant.timeMethod, socialBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.timeMethod, "Longitudinal, Longitudinal.TrendRepeatedCrossSection, TimeSeries"));

        assertThat(getField(DatasetFieldConstant.dataCollector, socialBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.dataCollector, "Jam jest podmiot zbierający dane"));

        assertThat(getField(DatasetFieldConstant.collectorTraining, socialBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.collectorTraining, "szkolenie podmiotu"));

        assertThat(getField(DatasetFieldConstant.frequencyOfDataCollection, socialBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.frequencyOfDataCollection, "1 sekunda"));

        assertThat(getField(DatasetFieldConstant.samplingProcedure, socialBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.samplingProcedure, "Probability, Probability.SystematicRandom"));

        assertThat(getField(DatasetFieldConstant.targetSampleSize, socialBlock)).isEqualTo(
                createCompound(DatasetFieldConstant.targetSampleSize,
                                       createPrimitive(DatasetFieldConstant.targetSampleActualSize, "100")));

        assertThat(getField(DatasetFieldConstant.deviationsFromSampleDesign, socialBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.deviationsFromSampleDesign, "odchylenie"));

        assertThat(getField(DatasetFieldConstant.collectionMode, socialBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.collectionMode, "Interview.FaceToFace, Interview.FaceToFace.PAPI, Interview.WebBased"));

        assertThat(getField(DatasetFieldConstant.researchInstrument, socialBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.researchInstrument, "Questionnaire, Questionnaire.Unstructured"));

        assertThat(getField(DatasetFieldConstant.dataSources, citationBlock)).isEqualTo(
                createMultiplePrimitive(DatasetFieldConstant.dataSources, ImmutableList.of("data source 1", "data source 2")));

        assertThat(getField(DatasetFieldConstant.originOfSources, citationBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.originOfSources, "sources origin"));

        assertThat(getField(DatasetFieldConstant.characteristicOfSources, citationBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.characteristicOfSources, "sources characteristics text"));

        assertThat(getField(DatasetFieldConstant.dataCollectionSituation, socialBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.dataCollectionSituation, "collecting data situation characteristics"));

        assertThat(getField(DatasetFieldConstant.actionsToMinimizeLoss, socialBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.actionsToMinimizeLoss, "actions to minimize loss"));

        assertThat(getField(DatasetFieldConstant.controlOperations, socialBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.controlOperations, "control operation text"));

        assertThat(getField(DatasetFieldConstant.weighting, socialBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.weighting, "PostStratification, MixedPostStratificationDesign"));

        assertThat(getField(DatasetFieldConstant.cleaningOperations, socialBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.cleaningOperations, "operations cleaning text"));

        assertThat(getField(DatasetFieldConstant.socialScienceNotes, socialBlock)).isEqualTo(
                createCompound(DatasetFieldConstant.socialScienceNotes,
                                       createPrimitive(DatasetFieldConstant.socialScienceNotesSubject, "notes subject (social)"),
                                       createPrimitive(DatasetFieldConstant.socialScienceNotesType, "type of notes (social)"),
                                       createPrimitive(DatasetFieldConstant.socialScienceNotesText, "notes text (social)")));

        assertThat(getField(DatasetFieldConstant.responseRate, socialBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.responseRate, "99.9"));

        assertThat(getField(DatasetFieldConstant.samplingErrorEstimates, socialBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.samplingErrorEstimates, "11"));

        assertThat(getField(DatasetFieldConstant.otherDataAppraisal, socialBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.otherDataAppraisal, "appraisal method text"));

        assertThat(getField(DatasetFieldConstant.notesText, citationBlock)).isEqualTo(
                createPrimitive(DatasetFieldConstant.notesText, "Notes: notes noted;"));
    }

    @Test
    void doImport_should_parse_related_material_metadata_fields() throws XMLStreamException, ImportException, IOException {
        // given
        String ddiXml = UnitTestUtils.readFileToString("xml/export/ddi/dataset-all-ddi-metadata-fields.xml");

        // when
        DatasetDTO datasetDto = importDdiService.doImport(ImportType.HARVEST, ddiXml);

        // then
        DatasetVersionDTO datasetVersionDto = datasetDto.getDatasetVersion();
        MetadataBlockWithFieldsDTO citationBlock = datasetVersionDto.getMetadataBlocks().get("citation");

        assertThat(getField(DatasetFieldConstant.publication, citationBlock)).isEqualTo(
                createMultipleCompound(DatasetFieldConstant.publication,
                                               ImmutableList.of(
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.publicationCitation, "Related publication citation"),
                                                               createVocabulary(DatasetFieldConstant.publicationIDType, "doi"),
                                                               createPrimitive(DatasetFieldConstant.publicationIDNumber, "10.1010/abc"),
                                                               createPrimitive(DatasetFieldConstant.publicationURL, "http://doi.org/10.1010/abc")),
                                                       ImmutableSet.of(
                                                               createPrimitive(DatasetFieldConstant.publicationCitation, "Second related citation"),
                                                               createVocabulary(DatasetFieldConstant.publicationIDType, "arXiv"),
                                                               createPrimitive(DatasetFieldConstant.publicationIDNumber, "123903829"),
                                                               createPrimitive(DatasetFieldConstant.publicationURL, "http://relatedarxiv.com"))
                                               )));

        assertThat(getField(DatasetFieldConstant.otherReferences, citationBlock)).isEqualTo(
                createMultiplePrimitive(DatasetFieldConstant.otherReferences, ImmutableList.of("references to others")));

    }


    @Test
    void doImport_should_parse_files_info_from_otherMat() throws XMLStreamException, ImportException {
        // given
        String ddiXml = "<codeBook>"
                + "<otherMat ID=\"f15\" URI=\"https://doi.org/10.1012/someId\" level=\"datafile\">"
                + "<labl>file.txt</labl>"
                + "<txt>some description</txt>"
                + "<notes level=\"file\" type=\"DATAVERSE:CONTENTTYPE\" subject=\"Content/MIME Type\">text/plain</notes>"
                + "</otherMat>"

                + "<otherMat ID=\"f16\" URI=\"https://doi.org/10.1012/someOtherId\" level=\"datafile\">"
                + "<labl>file2.pdf</labl>"
                + "<txt>some description2</txt>"
                + "<notes level=\"file\" type=\"DATAVERSE:CONTENTTYPE\" subject=\"Content/MIME Type\">application/pdf</notes>"
                + "</otherMat>"
                + "</codeBook>";

        // when
        DatasetDTO datasetDto = importDdiService.doImport(ImportType.HARVEST, ddiXml);

        // then
        DatasetVersionDTO datasetVersionDto = datasetDto.getDatasetVersion();

        assertThat(datasetVersionDto.getFiles()).hasSize(2)
                                                .extracting(
                                                        FileMetadataDTO::getLabel,
                                                        FileMetadataDTO::getDescription,
                                                        f -> f.getDataFile().getContentType(),
                                                        f -> f.getDataFile().getStorageIdentifier())
                                                .contains(
                                                        tuple("file.txt", "some description", "text/plain", "https://doi.org/10.1012/someId"),
                                                        tuple("file2.pdf", "some description2", "application/pdf", "https://doi.org/10.1012/someOtherId"));
    }

    @Test
    void doImport_should_parse_files_info_from_fileDscr() throws XMLStreamException, ImportException {
        // given
        String ddiXml = "<codeBook>"
                + "<fileDscr ID=\"f15\" URI=\"http://localhost:8080/api/access/datafile/15\">"
                + "<fileTxt>"
                + "<fileName>file.txt</fileName>"
                + "<dimensns><caseQnty>100</caseQnty><varQnty>3</varQnty><recPrCas>10</recPrCas></dimensns>"
                + "<fileType>text/plain</fileType>"
                + "</fileTxt></fileDscr>"

                + "<fileDscr ID=\"f16\" URI=\"http://localhost:8080/api/access/datafile/16\">"
                + "<fileTxt>"
                + "<fileName>file2.pdf</fileName>"
                + "<fileType>application/pdf</fileType>"
                + "</fileTxt></fileDscr>"
                + "</codeBook>";
        // when
        DatasetDTO datasetDto = importDdiService.doImport(ImportType.HARVEST, ddiXml);

        // then
        DatasetVersionDTO datasetVersionDto = datasetDto.getDatasetVersion();

        assertThat(datasetVersionDto.getFiles()).hasSize(2)
                                                .extracting(
                                                        FileMetadataDTO::getLabel,
                                                        FileMetadataDTO::getDescription,
                                                        f -> f.getDataFile().getContentType(),
                                                        f -> f.getDataFile().getStorageIdentifier())
                                                .contains(
                                                        tuple("file.txt", null, "data/various-formats", "http://"),
                                                        tuple("file2.pdf", null, "data/various-formats", "http://"));
    }

    @Test
    void doImport_should_include_codebook_id_when_no_other_id_is_present() throws XMLStreamException, ImportException {
        // given
        String ddiXml = "<codeBook ID=\"someId\"></codeBook>";
        // when
        DatasetDTO datasetDto = importDdiService.doImport(ImportType.HARVEST, ddiXml);
        // then
        DatasetVersionDTO datasetVersionDto = datasetDto.getDatasetVersion();
        MetadataBlockWithFieldsDTO citationBlock = datasetVersionDto.getMetadataBlocks().get("citation");

        assertThat(getField(DatasetFieldConstant.otherId, citationBlock)).isEqualTo(
                createMultipleCompound(DatasetFieldConstant.otherId,
                                               ImmutableList.of(
                                                       ImmutableSet.of(createPrimitive(DatasetFieldConstant.otherIdValue, "someId")))));
    }

    @Test
    void doImport_should_assign_cc0_license_to_all_files() throws XMLStreamException, ImportException {
        // given
        String ddiXml = "<codeBook>"
                + "<otherMat><labl>file.txt</labl><txt>desc1</txt></otherMat>"
                + "<otherMat><labl>file2.txt</labl><txt>desc2</txt></otherMat>"
                + "<stdyDscr>"
                + "<dataAccs>"
                + "<notes type=\"DVN:TOU\" level=\"dv\">CC0 Waiver</notes>"
                + "</dataAccs>"
                + "</stdyDscr>"
                + "</codeBook>";
        // when
        DatasetDTO datasetDto = importDdiService.doImport(ImportType.HARVEST, ddiXml);

        // then
        DatasetVersionDTO datasetVersionDto = datasetDto.getDatasetVersion();

        assertThat(datasetVersionDto.getFiles()).hasSize(2)
                                                .allSatisfy(file -> {
                                                    assertThat(file)
                                                            .extracting(FileMetadataDTO::getTermsOfUseType, FileMetadataDTO::getLicenseName)
                                                            .containsExactly(TermsOfUseType.LICENSE_BASED.toString(), License.CCO_LICENSE_NAME);
                                                });

    }

    @Test
    void doImport_should_assign_some_license_to_all_files() throws XMLStreamException, ImportException {
        // given
        String ddiXml = "<codeBook>"
                + "<otherMat><labl>file.txt</labl><txt>desc1</txt></otherMat>"
                + "<otherMat><labl>file2.txt</labl><txt>desc2</txt></otherMat>"
                + "<stdyDscr>"
                + "<dataAccs>"
                + "<notes type=\"DVN:TOU\" level=\"dv\">Text</notes>"
                + "</dataAccs>"
                + "</stdyDscr>"
                + "</codeBook>";

        // when
        DatasetDTO datasetDto = importDdiService.doImport(ImportType.HARVEST, ddiXml);

        // then
        DatasetVersionDTO datasetVersionDto = datasetDto.getDatasetVersion();

        assertThat(datasetVersionDto.getFiles()).hasSize(2)
                                                .allSatisfy(file -> {
                                                    assertThat(file)
                                                            .extracting(FileMetadataDTO::getTermsOfUseType, FileMetadataDTO::getLicenseName)
                                                            .containsExactly(TermsOfUseType.LICENSE_BASED.toString(), "Text");
                                                });

    }

    @Test
    void doImport_should_assign_unknown_terms_to_all_files() throws XMLStreamException, ImportException {
        // given
        String ddiXml = "<codeBook>"
                + "<otherMat><labl>file.txt</labl><txt>desc1</txt></otherMat>"
                + "<otherMat><labl>file2.txt</labl><txt>desc2</txt></otherMat>"
                + "</codeBook>";

        // when
        DatasetDTO datasetDto = importDdiService.doImport(ImportType.HARVEST, ddiXml);

        // then
        DatasetVersionDTO datasetVersionDto = datasetDto.getDatasetVersion();

        assertThat(datasetVersionDto.getFiles()).hasSize(2)
                                                .allSatisfy(file -> {
                                                    assertThat(file)
                                                            .extracting(FileMetadataDTO::getTermsOfUseType)
                                                            .isEqualTo(TermsOfUseType.TERMS_UNKNOWN.toString());
                                                });

    }

    @Test
    void isRelatedMaterial_And_Dataset_ParsedCorrectly() throws IOException, XMLStreamException, ImportException {
        //given
        final String ddiXml = IOUtils.toString(Objects.requireNonNull(HarvestedJsonParserIT.class
                                                                              .getClassLoader()
                                                                              .getResource("xml/imports/modernDdi.xml")), StandardCharsets.UTF_8);

        //when
        DatasetDTO datasetDTO = importDdiService.doImport(ImportType.HARVEST, ddiXml);

        //then
        assertThat(extractValues(datasetDTO, DatasetFieldConstant.relatedMaterial, DatasetFieldConstant.relatedMaterialCitation))
                .isNotNull();
        assertThat(extractValues(datasetDTO, DatasetFieldConstant.relatedMaterial, DatasetFieldConstant.relatedMaterialIDType))
                .isNotNull();
        assertThat(extractValues(datasetDTO, DatasetFieldConstant.relatedMaterial, DatasetFieldConstant.relatedMaterialIDNumber))
                .isNotNull();
        assertThat(extractValues(datasetDTO, DatasetFieldConstant.relatedMaterial, DatasetFieldConstant.relatedMaterialURL))
                .isNotNull();

        assertThat(extractValues(datasetDTO, DatasetFieldConstant.relatedDataset, DatasetFieldConstant.relatedDatasetCitation))
                .isNotNull();
        assertThat(extractValues(datasetDTO, DatasetFieldConstant.relatedDataset, DatasetFieldConstant.relatedDatasetIDType))
                .isNotNull();
        assertThat(extractValues(datasetDTO, DatasetFieldConstant.relatedDataset, DatasetFieldConstant.relatedDatasetIDNumber))
                .isNotNull();
        assertThat(extractValues(datasetDTO, DatasetFieldConstant.relatedDataset, DatasetFieldConstant.relatedDatasetURL))
                .isNotNull();

    }

    @Test
    void isRelatedMaterial_And_Dataset_ParsedCorrectly_for_legacyDDI() throws IOException, XMLStreamException, ImportException {
        //given
        String ddiXml = IOUtils.toString(Objects.requireNonNull(HarvestedJsonParserIT.class
                                                                              .getClassLoader()
                                                                              .getResource("xml/imports/legacyDdi.xml")), StandardCharsets.UTF_8);
        //when
        DatasetDTO datasetDTO = importDdiService.doImport(ImportType.HARVEST, ddiXml);
        List<Map<String, DatasetFieldDTO>> relatedMaterials = (List<Map<String, DatasetFieldDTO>>)
                getField(DatasetFieldConstant.relatedMaterial, datasetDTO.getDatasetVersion().getMetadataBlocks().get("citation"))
                .getValue();

        List<Map<String, DatasetFieldDTO>> relatedDatasets = (List<Map<String, DatasetFieldDTO>>)
                getField(DatasetFieldConstant.relatedDataset, datasetDTO.getDatasetVersion().getMetadataBlocks().get("citation"))
                .getValue();

        //then
        assertThat(relatedMaterials).hasSize(2)
                                       .extracting(e -> e.get(DatasetFieldConstant.relatedMaterialCitation))
                                       .isNotNull();

        assertThat(relatedDatasets).hasSize(2)
                                       .extracting(e -> e.get(DatasetFieldConstant.relatedMaterialCitation))
                                       .isNotNull();

    }

    private DatasetFieldDTO getField(String name, MetadataBlockWithFieldsDTO metadataBlock) {
        return metadataBlock.getFields().stream()
                .filter(f -> name.equals(f.getTypeName()))
                .findFirst()
                .orElse(null);
    }

    private DatasetFieldDTO extractValues(DatasetDTO datasetDTO, String citationParentField, String childField) {
        return ((List<Map<String, DatasetFieldDTO>>) getField(citationParentField,
                datasetDTO.getDatasetVersion().getMetadataBlocks().get("citation")).getValue())
                .get(0)
                .get(childField);
    }

}
