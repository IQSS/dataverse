package edu.harvard.iq.dataverse.api.imports;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FileDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.api.imports.ImportUtil.ImportType;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.stream.XMLStreamException;

import java.io.IOException;

import static edu.harvard.iq.dataverse.api.dto.FieldDTO.*;
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
        assertThat(datasetDto.getDatasetVersion().getVersionState()).isEqualTo(VersionState.RELEASED);
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
        assertThat(datasetDto.getDatasetVersion().getVersionState()).isEqualTo(VersionState.RELEASED);
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
        assertThat(datasetDto.getDatasetVersion().getVersionState()).isEqualTo(VersionState.RELEASED);
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
        
        MetadataBlockDTO citationBlock = datasetVersionDto.getMetadataBlocks().get("citation");

        assertThat(citationBlock.getField(DatasetFieldConstant.title)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.title, "All metadata Dataset"));

        assertThat(citationBlock.getField(DatasetFieldConstant.subTitle)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.subTitle, "Subtitle"));

        assertThat(citationBlock.getField(DatasetFieldConstant.alternativeTitle)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.alternativeTitle, "variant of title"));

        assertThat(citationBlock.getField(DatasetFieldConstant.otherId)).isEqualTo(
                createMultipleCompoundFieldDTO(DatasetFieldConstant.otherId,
                    ImmutableList.of(
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.otherIdAgency, "Other id agency"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.otherIdValue, "123")),
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.otherIdAgency, "Other id agency 2"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.otherIdValue, "124"))
                        )));
        
        assertThat(citationBlock.getField(DatasetFieldConstant.author)).isEqualTo(
                createMultipleCompoundFieldDTO(DatasetFieldConstant.author,
                    ImmutableList.of(
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.authorName, "First, Author"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.authorAffiliation, "author1 aff")),
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.authorName, "Second, Author"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.authorAffiliation, "author2 Aff"))
                        )));
        
        assertThat(citationBlock.getField(DatasetFieldConstant.producer)).isEqualTo(
                createMultipleCompoundFieldDTO(DatasetFieldConstant.producer,
                    ImmutableList.of(
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.producerName, "Some, Producer"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.producerAbbreviation, "PRODU"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.producerAffiliation, "producer aff"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.producerLogo, "https://example.com/images/producer.jpg"))
                        )));

        assertThat(citationBlock.getField(DatasetFieldConstant.productionDate)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.productionDate, "2001-12-12"));

        assertThat(citationBlock.getField(DatasetFieldConstant.productionPlace)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.productionPlace, "Some production place"));
        
        assertThat(citationBlock.getField(DatasetFieldConstant.software)).isEqualTo(
                createMultipleCompoundFieldDTO(DatasetFieldConstant.software,
                    ImmutableList.of(
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.softwareName, "Software1"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.softwareVersion, "v1")),
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.softwareName, "Software2"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.softwareVersion, "v2"))
                        )));

        assertThat(citationBlock.getField(DatasetFieldConstant.grantNumber)).isEqualTo(
                createMultipleCompoundFieldDTO(DatasetFieldConstant.grantNumber,
                    ImmutableList.of(
                        ImmutableSet.of(
                                createPrimitiveFieldDTO(DatasetFieldConstant.grantNumberAgency, "Grant Agency 1"),
                                createPrimitiveFieldDTO(DatasetFieldConstant.grantNumberValue, "GRANT.1")),
                        ImmutableSet.of(
                                createPrimitiveFieldDTO(DatasetFieldConstant.grantNumberAgency, "Grant Agency 2"),
                                createPrimitiveFieldDTO(DatasetFieldConstant.grantNumberValue, "GRANT.2"))
                        )));
        


        assertThat(citationBlock.getField(DatasetFieldConstant.distributor)).isEqualTo(
                createMultipleCompoundFieldDTO(DatasetFieldConstant.distributor,
                    ImmutableList.of(
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.distributorName, "Some, Distributor1"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.distributorAbbreviation, "DIST1"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.distributorAffiliation, "distributor1 aff"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.distributorURL, "http://distributor1.org")),
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.distributorName, "Some, Distributor2"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.distributorAbbreviation, "DIST2"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.distributorAffiliation, "distributor2 aff"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.distributorURL, "http://distributor2.org"))
                        )));

        assertThat(citationBlock.getField(DatasetFieldConstant.datasetContact)).isEqualTo(
                createMultipleCompoundFieldDTO(DatasetFieldConstant.datasetContact,
                    ImmutableList.of(
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.datasetContactName, "First, Contact"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.datasetContactAffiliation, "contact1 Aff"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.datasetContactEmail, "contact1@example.com")),
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.datasetContactName, "Second, Contact"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.datasetContactAffiliation, "contact2 Aff"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.datasetContactEmail, "contact2@example.com"))
                        )));


        assertThat(citationBlock.getField(DatasetFieldConstant.depositor)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.depositor, "Some, Depositor"));

        assertThat(citationBlock.getField(DatasetFieldConstant.dateOfDeposit)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.dateOfDeposit, "2021-02-15"));

        assertThat(citationBlock.getField(DatasetFieldConstant.distributionDate)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.distributionDate, "1990"));

        assertThat(citationBlock.getField(DatasetFieldConstant.series)).isEqualTo(
                createCompoundFieldDTO(DatasetFieldConstant.series, 
                        createPrimitiveFieldDTO(DatasetFieldConstant.seriesName, "series name"),
                        createPrimitiveFieldDTO(DatasetFieldConstant.seriesInformation, "series info")));

        MetadataBlockDTO socialBlock = datasetVersionDto.getMetadataBlocks().get("socialscience");

        assertThat(socialBlock.getField(DatasetFieldConstant.datasetLevelErrorNotes)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.datasetLevelErrorNotes, "notes on errors in dataset level"));

    }

    @Test
    void doImport_should_parse_stdyDscr_stdyInfo_related_metadata_fields() throws XMLStreamException, ImportException, IOException {
        // given
        String ddiXml = UnitTestUtils.readFileToString("xml/export/ddi/dataset-all-ddi-metadata-fields.xml");
        
        // when
        DatasetDTO datasetDto = importDdiService.doImport(ImportType.HARVEST, ddiXml);
        
        // then
        DatasetVersionDTO datasetVersionDto = datasetDto.getDatasetVersion();
        MetadataBlockDTO citationBlock = datasetVersionDto.getMetadataBlocks().get("citation");
        MetadataBlockDTO geospatialBlock = datasetVersionDto.getMetadataBlocks().get("geospatial");
        MetadataBlockDTO socialBlock = datasetVersionDto.getMetadataBlocks().get("socialscience");
        
        assertThat(citationBlock.getField(DatasetFieldConstant.keyword)).isEqualTo(
                createMultipleCompoundFieldDTO(DatasetFieldConstant.keyword,
                    ImmutableList.of(
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.keywordValue, "Astronomy and Astrophysics")),
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.keywordValue, "Chemistry")),
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.keywordValue, "keyword 1"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.keywordVocab, "LCSH"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.keywordVocabURI, "http://lcsg.com")),
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.keywordValue, "keyword 2"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.keywordVocab, "MeSH"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.keywordVocabURI, "http://mesh.com"))
                        )));

        assertThat(citationBlock.getField(DatasetFieldConstant.topicClassification)).isEqualTo(
                createMultipleCompoundFieldDTO(DatasetFieldConstant.topicClassification,
                    ImmutableList.of(
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.topicClassValue, "topic 1"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.topicClassVocab, "dict"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.topicClassVocabURI, "http://dict.com")),
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.topicClassValue, "topic 2"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.topicClassVocab, "dict 2"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.topicClassVocabURI, "http://dict2.com"))
                        )));

        assertThat(citationBlock.getField(DatasetFieldConstant.description)).isEqualTo(
                createMultipleCompoundFieldDTO(DatasetFieldConstant.description,
                    ImmutableList.of(
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.descriptionText, "<p>some description</p>"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.descriptionDate, "1999")),
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.descriptionText, "<p>second description</p>"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.descriptionDate, "2000-08"))
                        )));

        assertThat(citationBlock.getField(DatasetFieldConstant.timePeriodCovered)).isEqualTo(
                createMultipleCompoundFieldDTO(DatasetFieldConstant.timePeriodCovered,
                    ImmutableList.of(
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.timePeriodCoveredStart, "1001"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.timePeriodCoveredEnd, "1002")),
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.timePeriodCoveredStart, "1004"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.timePeriodCoveredEnd, "1005"))
                        )));

        assertThat(citationBlock.getField(DatasetFieldConstant.dateOfCollection)).isEqualTo(
                createMultipleCompoundFieldDTO(DatasetFieldConstant.dateOfCollection,
                    ImmutableList.of(
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.dateOfCollectionStart, "1100"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.dateOfCollectionEnd, "1200")),
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.dateOfCollectionStart, "1205"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.dateOfCollectionEnd, "1210"))
                        )));
        
        
        assertThat(geospatialBlock.getField(DatasetFieldConstant.geographicCoverage)).isEqualTo(
                createMultipleCompoundFieldDTO(DatasetFieldConstant.geographicCoverage,
                    ImmutableList.of(
                        ImmutableSet.of(
                            createVocabFieldDTO(DatasetFieldConstant.country, "Algeria")),
                        ImmutableSet.of(
                            createVocabFieldDTO(DatasetFieldConstant.country, "United States"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.otherGeographicCoverage,
                                    "City in Algeria; State in Algeria; Other notes for geo coverage Algeria; Boston; Massachusetts; Other notes for geo coverage USA"))
                        )));

        assertThat(geospatialBlock.getField(DatasetFieldConstant.geographicUnit)).isEqualTo(
                createMultiplePrimitiveFieldDTO(DatasetFieldConstant.geographicUnit, ImmutableList.of("geo unit", "geo unit 2")));

        assertThat(geospatialBlock.getField(DatasetFieldConstant.geographicBoundingBox)).isEqualTo(
                createMultipleCompoundFieldDTO(DatasetFieldConstant.geographicBoundingBox,
                    ImmutableList.of(
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.northLatitude, "12"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.southLatitude, "-12"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.eastLongitude, "98"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.westLongitude, "90"))
                        )));

        assertThat(socialBlock.getField(DatasetFieldConstant.unitOfAnalysis)).isEqualTo(
                createMultiplePrimitiveFieldDTO(DatasetFieldConstant.unitOfAnalysis, ImmutableList.of("Family", "EventOrProcess")));

        assertThat(socialBlock.getField(DatasetFieldConstant.universe)).isEqualTo(
                createMultiplePrimitiveFieldDTO(DatasetFieldConstant.universe, ImmutableList.of("social universe 1", "social universe 2")));
        
        assertThat(citationBlock.getField(DatasetFieldConstant.kindOfData)).isEqualTo(
                createMultiplePrimitiveFieldDTO(DatasetFieldConstant.kindOfData, ImmutableList.of("Numeric", "Geospatial")));
    }
    
    @Test
    void doImport_should_parse_stdyDscr_method_related_metadata_fields() throws XMLStreamException, ImportException, IOException {
        // given
        String ddiXml = UnitTestUtils.readFileToString("xml/export/ddi/dataset-all-ddi-metadata-fields.xml");
        
        // when
        DatasetDTO datasetDto = importDdiService.doImport(ImportType.HARVEST, ddiXml);
        
        // then
        DatasetVersionDTO datasetVersionDto = datasetDto.getDatasetVersion();
        MetadataBlockDTO citationBlock = datasetVersionDto.getMetadataBlocks().get("citation");
        MetadataBlockDTO socialBlock = datasetVersionDto.getMetadataBlocks().get("socialscience");

        assertThat(socialBlock.getField(DatasetFieldConstant.timeMethod)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.timeMethod, "Longitudinal, Longitudinal.TrendRepeatedCrossSection, TimeSeries"));

        assertThat(socialBlock.getField(DatasetFieldConstant.dataCollector)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.dataCollector, "Jam jest podmiot zbierający dane"));

        assertThat(socialBlock.getField(DatasetFieldConstant.collectorTraining)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.collectorTraining, "szkolenie podmiotu"));

        assertThat(socialBlock.getField(DatasetFieldConstant.frequencyOfDataCollection)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.frequencyOfDataCollection, "1 sekunda"));

        assertThat(socialBlock.getField(DatasetFieldConstant.samplingProcedure)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.samplingProcedure, "Probability, Probability.SystematicRandom"));

        assertThat(socialBlock.getField(DatasetFieldConstant.targetSampleSize)).isEqualTo(
                createCompoundFieldDTO(DatasetFieldConstant.targetSampleSize, 
                        createPrimitiveFieldDTO(DatasetFieldConstant.targetSampleActualSize, "100")));
        
        assertThat(socialBlock.getField(DatasetFieldConstant.deviationsFromSampleDesign)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.deviationsFromSampleDesign, "odchylenie"));

        assertThat(socialBlock.getField(DatasetFieldConstant.collectionMode)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.collectionMode, "Interview.FaceToFace, Interview.FaceToFace.PAPI, Interview.WebBased"));

        assertThat(socialBlock.getField(DatasetFieldConstant.researchInstrument)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.researchInstrument, "Questionnaire, Questionnaire.Unstructured"));

        assertThat(citationBlock.getField(DatasetFieldConstant.dataSources)).isEqualTo(
                createMultiplePrimitiveFieldDTO(DatasetFieldConstant.dataSources, ImmutableList.of("data source 1", "data source 2")));

        assertThat(citationBlock.getField(DatasetFieldConstant.originOfSources)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.originOfSources, "sources origin"));

        assertThat(citationBlock.getField(DatasetFieldConstant.characteristicOfSources)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.characteristicOfSources, "sources characteristics text"));
        
        assertThat(socialBlock.getField(DatasetFieldConstant.dataCollectionSituation)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.dataCollectionSituation, "collecting data situation characteristics"));
        
        assertThat(socialBlock.getField(DatasetFieldConstant.actionsToMinimizeLoss)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.actionsToMinimizeLoss, "actions to minimize loss"));

        assertThat(socialBlock.getField(DatasetFieldConstant.controlOperations)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.controlOperations, "control operation text"));

        assertThat(socialBlock.getField(DatasetFieldConstant.weighting)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.weighting, "PostStratification, MixedPostStratificationDesign"));
        
        assertThat(socialBlock.getField(DatasetFieldConstant.cleaningOperations)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.cleaningOperations, "operations cleaning text"));

        assertThat(socialBlock.getField(DatasetFieldConstant.socialScienceNotes)).isEqualTo(
                createCompoundFieldDTO(DatasetFieldConstant.socialScienceNotes, 
                        createPrimitiveFieldDTO(DatasetFieldConstant.socialScienceNotesSubject, "notes subject (social)"),
                        createPrimitiveFieldDTO(DatasetFieldConstant.socialScienceNotesType, "type of notes (social)"),
                        createPrimitiveFieldDTO(DatasetFieldConstant.socialScienceNotesText, "notes text (social)")));

        assertThat(socialBlock.getField(DatasetFieldConstant.responseRate)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.responseRate, "99.9"));
        
        assertThat(socialBlock.getField(DatasetFieldConstant.samplingErrorEstimates)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.samplingErrorEstimates, "11"));
        
        assertThat(socialBlock.getField(DatasetFieldConstant.otherDataAppraisal)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.otherDataAppraisal, "appraisal method text"));

        assertThat(citationBlock.getField(DatasetFieldConstant.notesText)).isEqualTo(
                createPrimitiveFieldDTO(DatasetFieldConstant.notesText, "Notes: notes noted;"));
    }
    
    @Test
    void doImport_should_parse_stdyDscr_othrStdyMat_related_metadata_fields() throws XMLStreamException, ImportException, IOException {
        // given
        String ddiXml = UnitTestUtils.readFileToString("xml/export/ddi/dataset-all-ddi-metadata-fields.xml");
        
        // when
        DatasetDTO datasetDto = importDdiService.doImport(ImportType.HARVEST, ddiXml);
        
        // then
        DatasetVersionDTO datasetVersionDto = datasetDto.getDatasetVersion();
        MetadataBlockDTO citationBlock = datasetVersionDto.getMetadataBlocks().get("citation");
        
        assertThat(citationBlock.getField(DatasetFieldConstant.relatedMaterial)).isEqualTo(
                createMultiplePrimitiveFieldDTO(DatasetFieldConstant.relatedMaterial, ImmutableList.of(
                    "<!--  parsed from DDI citation title and holdings -->\n\n" + 
                    "<a href=\"https://doi.org/10.1010/abc33\">https://doi.org/10.1010/abc33</a>")));
        
        assertThat(citationBlock.getField(DatasetFieldConstant.relatedDataset)).isEqualTo(
                createMultiplePrimitiveFieldDTO(DatasetFieldConstant.relatedDataset, ImmutableList.of(
                    "<!--  parsed from DDI citation title and holdings -->\n\n" + 
                    "<a href=\"http://url.com\">http://url.com</a>",

                    "<!--  parsed from DDI citation title and holdings -->\n\n" + 
                    "<a href=\"http://arxivurl.com\">http://arxivurl.com</a>")));

        assertThat(citationBlock.getField(DatasetFieldConstant.publication)).isEqualTo(
                createMultipleCompoundFieldDTO(DatasetFieldConstant.publication,
                    ImmutableList.of(
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.publicationCitation, "Related publication citation"),
                            createVocabFieldDTO(DatasetFieldConstant.publicationIDType, "doi"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.publicationIDNumber, "10.1010/abc"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.publicationURL, "http://doi.org/10.1010/abc")),
                        ImmutableSet.of(
                            createPrimitiveFieldDTO(DatasetFieldConstant.publicationCitation, "Second related citation"),
                            createVocabFieldDTO(DatasetFieldConstant.publicationIDType, "arXiv"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.publicationIDNumber, "123903829"),
                            createPrimitiveFieldDTO(DatasetFieldConstant.publicationURL, "http://relatedarxiv.com"))
                        )));

        assertThat(citationBlock.getField(DatasetFieldConstant.otherReferences)).isEqualTo(
                createMultiplePrimitiveFieldDTO(DatasetFieldConstant.otherReferences, ImmutableList.of("references to others")));

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
                    FileDTO::getLabel,
                    FileDTO::getDescription,
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
                    FileDTO::getLabel,
                    FileDTO::getDescription,
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
        MetadataBlockDTO citationBlock = datasetVersionDto.getMetadataBlocks().get("citation");
        
        assertThat(citationBlock.getField(DatasetFieldConstant.otherId)).isEqualTo(
                createMultipleCompoundFieldDTO(DatasetFieldConstant.otherId,
                    ImmutableList.of(
                        ImmutableSet.of(createPrimitiveFieldDTO(DatasetFieldConstant.otherIdValue, "someId")))));
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
                    .extracting(FileDTO::getTermsOfUseType, FileDTO::getLicenseName)
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
                    .extracting(FileDTO::getTermsOfUseType, FileDTO::getLicenseName)
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
                    .extracting(FileDTO::getTermsOfUseType)
                    .isEqualTo(TermsOfUseType.TERMS_UNKNOWN.toString());
            });
        
    }

}
