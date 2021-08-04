package edu.harvard.iq.dataverse.api.imports;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.*;
import org.junit.runner.*;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class HarvestedJsonParserIT extends WebappArquillianDeployment {

    @Inject
    private HarvestedJsonParser harvestedJsonParser;

    @Test
    public void parseDataset_WithLicenseCheck() throws IOException, JsonParseException {
        //given
        final String harvestedDataset = IOUtils.toString(Objects.requireNonNull(HarvestedJsonParserIT.class
                                                                                        .getClassLoader()
                                                                                        .getResource("json/import/harvestedDataset.json")), StandardCharsets.UTF_8);
        //when
        final Dataset dataset = harvestedJsonParser.parseDataset(harvestedDataset);

        //then
        final FileTermsOfUse mxrdrFile = retrieveDatafileByName(dataset.getFiles(), "mxrdr-resized.png").getTermsOfUse();
        final FileTermsOfUse repodFile = retrieveDatafileByName(dataset.getFiles(), "repod-resized.png").getTermsOfUse();
        final FileTermsOfUse rorDataFile = retrieveDatafileByName(dataset.getFiles(), "rorData.json").getTermsOfUse();
        final FileTermsOfUse sampleFile = retrieveDatafileByName(dataset.getFiles(), "sample.pdf").getTermsOfUse();

        assertThat(mxrdrFile).extracting(FileTermsOfUse::isAllRightsReserved).isEqualTo(true);

        assertThat(repodFile).extracting(FileTermsOfUse::getTermsOfUseType).isEqualTo(FileTermsOfUse.TermsOfUseType.LICENSE_BASED);
        assertThat(repodFile).extracting(FileTermsOfUse::getLicense).extracting(License::getName)
                .isEqualTo("CC0 Creative Commons Zero 1.0 Waiver");

        assertThat(rorDataFile).extracting(FileTermsOfUse::getTermsOfUseType).isEqualTo(FileTermsOfUse.TermsOfUseType.RESTRICTED);
        assertThat(rorDataFile).extracting(FileTermsOfUse::getRestrictType).isEqualTo(FileTermsOfUse.RestrictType.CUSTOM);
        assertThat(rorDataFile).extracting(FileTermsOfUse::getRestrictCustomText).isEqualTo("terms desc");

        assertThat(sampleFile).extracting(FileTermsOfUse::getTermsOfUseType).isEqualTo(FileTermsOfUse.TermsOfUseType.RESTRICTED);
        assertThat(sampleFile).extracting(FileTermsOfUse::getRestrictType).isEqualTo(FileTermsOfUse.RestrictType.ACADEMIC_PURPOSE);

    }

    @Test
    public void parseDataset_WithLegacyJsonLicenseCheck() throws IOException, JsonParseException {
        //given
        final String legacyHarvestedDataset = IOUtils.toString(Objects.requireNonNull(HarvestedJsonParserIT.class
                                                                                        .getClassLoader()
                                                                                        .getResource("json/import/legacyHarvestedDataset.json")), StandardCharsets.UTF_8);

        //when
        final Dataset dataset = harvestedJsonParser.parseDataset(legacyHarvestedDataset);

        //then
        assertThat(dataset.getFiles())
                  .extracting(dataFile -> dataFile.getFileMetadata().getTermsOfUse().getLicense().getName())
                  .containsOnly("CC0 Creative Commons Zero 1.0 Waiver");


    }

    @Test
    public void parseDataset_WithLegacyJsonLicenseCheck_WithoutLicense() throws IOException, JsonParseException {
        //given
        final String legacyHarvestedDataset = IOUtils.toString(Objects.requireNonNull(HarvestedJsonParserIT.class
                                                                                        .getClassLoader()
                                                                                        .getResource("json/import/legacyHarvestedDatasetNoLicense.json")), StandardCharsets.UTF_8);

        //when
        final Dataset dataset = harvestedJsonParser.parseDataset(legacyHarvestedDataset);

        //then
        assertThat(dataset.getFiles())
                  .extracting(dataFile -> dataFile.getFileMetadata().getTermsOfUse().getTermsOfUseType())
                  .containsOnly(FileTermsOfUse.TermsOfUseType.TERMS_UNKNOWN);


    }

    private FileMetadata retrieveDatafileByName(List<DataFile> dataFileList, String nameToFind) {
        return dataFileList.stream()
                .map(DataFile::getFileMetadata)
                .filter(fileMetadata -> fileMetadata.getLabel().equals(nameToFind))
                .findFirst().get();
    }

    @Test
    public void parseDataset_WithCompoundDatasetFieldsMismatch() throws IOException, JsonParseException {
        //given
        final String legacyHarvestedDataset = IOUtils.toString(Objects.requireNonNull(HarvestedJsonParserIT.class
                .getClassLoader()
                .getResource("json/import/harvestedDataset_mismatchedCompounds.json")), StandardCharsets.UTF_8);

        //when
        final Dataset dataset = harvestedJsonParser.parseDataset(legacyHarvestedDataset);

        //then
        List<DatasetField> importedDatasetFields = dataset.getVersions().get(0).getDatasetFields();
        assertEquals(2, importedDatasetFields.size());
        assertThat(importedDatasetFields)
                .extracting(datasetField -> datasetField.getDatasetFieldType().getName())
                .containsExactlyInAnyOrder("author", "datasetContact");

        List<DatasetField> authorFieldChildren = importedDatasetFields
                .stream().filter(field -> field.getDatasetFieldType().getName().equals("author"))
                .collect(Collectors.toList());

        assertEquals(1, authorFieldChildren.size());
        assertThat(authorFieldChildren.get(0).getDatasetFieldsChildren())
                .extracting(datasetField -> datasetField.getDatasetFieldType().getName())
                .containsExactlyInAnyOrder("authorName", "authorAffiliation");
        assertThat(authorFieldChildren.get(0).getDatasetFieldsChildren())
                .extracting(datasetField -> datasetField.getFieldValue().getOrElse(StringUtils.EMPTY))
                .containsExactlyInAnyOrder("Author Value 1", "Affiliation as single value");

        List<DatasetField> datasetContactFieldChildren = importedDatasetFields
                .stream().filter(field -> field.getDatasetFieldType().getName().equals("datasetContact"))
                .collect(Collectors.toList());

        assertEquals(1, datasetContactFieldChildren.size());
        assertThat(datasetContactFieldChildren.get(0).getDatasetFieldsChildren())
                .extracting(datasetField -> datasetField.getDatasetFieldType().getName())
                .containsExactlyInAnyOrder("datasetContactName", "datasetContactAffiliation", "datasetContactEmail");
        assertThat(datasetContactFieldChildren.get(0).getDatasetFieldsChildren())
                .extracting(datasetField -> datasetField.getFieldValue().getOrElse(StringUtils.EMPTY))
                .containsExactlyInAnyOrder("Admin, Dataverse", "Dataverse.org", "dataverse@mailinator.com");
    }

    @Test
    public void parseDataset_WithControlledVocabularyDatasetFieldsMismatch() throws IOException, JsonParseException {
        //given
        final String legacyHarvestedDataset = IOUtils.toString(Objects.requireNonNull(HarvestedJsonParserIT.class
                .getClassLoader()
                .getResource("json/import/harvestedDataset_mismatchedControlledVocab.json")), StandardCharsets.UTF_8);

        //when
        final Dataset dataset = harvestedJsonParser.parseDataset(legacyHarvestedDataset);

        //then
        List<DatasetField> importedDatasetFields = dataset.getVersions().get(0).getDatasetFields();
        assertEquals(5, importedDatasetFields.size());
        assertThat(importedDatasetFields)
                .extracting(datasetField -> datasetField.getDatasetFieldType().getName())
                .containsExactlyInAnyOrder("subject", "kindOfData", "journalArticleType", "author", "publication");

        List<DatasetField> authorFieldChildren = importedDatasetFields
                .stream().filter(field -> field.getDatasetFieldType().getName().equals("author"))
                .collect(Collectors.toList());

        assertEquals(1, authorFieldChildren.size());
        assertEquals(1, authorFieldChildren.get(0).getDatasetFieldsChildren().size());
        assertThat(authorFieldChildren.get(0).getDatasetFieldsChildren())
                .extracting(datasetField -> datasetField.getDatasetFieldType().getName())
                .containsExactly("authorIdentifierScheme");
        assertThat(authorFieldChildren.get(0).getDatasetFieldsChildren().get(0).getControlledVocabularyValues().size())
                .isEqualTo(1);
        assertThat(authorFieldChildren.get(0).getDatasetFieldsChildren())
                .extracting(datasetField -> datasetField.getControlledVocabularyValues().get(0).getStrValue())
                .containsExactlyInAnyOrder("ORCID");

        List<DatasetField> publicationFieldChildren = importedDatasetFields
                .stream().filter(field -> field.getDatasetFieldType().getName().equals("publication"))
                .collect(Collectors.toList());

        assertEquals(1, publicationFieldChildren.size());
        assertEquals(1, publicationFieldChildren.get(0).getDatasetFieldsChildren().size());
        assertThat(publicationFieldChildren.get(0).getDatasetFieldsChildren())
                .extracting(datasetField -> datasetField.getDatasetFieldType().getName())
                .containsExactly("publicationIDType");
        assertThat(publicationFieldChildren.get(0).getDatasetFieldsChildren().get(0).getControlledVocabularyValues().size())
                .isEqualTo(1);
        assertThat(publicationFieldChildren.get(0).getDatasetFieldsChildren())
                .extracting(datasetField -> datasetField.getControlledVocabularyValues().get(0).getStrValue())
                .isSubsetOf(new String[]{"eissn", "lissn", "arXiv"})
                .doesNotContain("non-existing value");

        List<DatasetField> subjectFieldChildren = importedDatasetFields
                .stream().filter(field -> field.getDatasetFieldType().getName().equals("subject"))
                .collect(Collectors.toList());

        assertEquals(1, subjectFieldChildren.size());
        assertEquals(3, subjectFieldChildren.get(0).getControlledVocabularyValues().size());
        assertThat(subjectFieldChildren.get(0).getControlledVocabularyValues())
                .extracting(ControlledVocabularyValue::getStrValue)
                .containsExactlyInAnyOrder("Agricultural Sciences", "Other", "Engineering")
                .doesNotContain("Non-existing value");

        List<DatasetField> kindOfDataFieldChildren = importedDatasetFields
                .stream().filter(field -> field.getDatasetFieldType().getName().equals("kindOfData"))
                .collect(Collectors.toList());

        assertEquals(1, kindOfDataFieldChildren.size());
        assertEquals(1, kindOfDataFieldChildren.get(0).getControlledVocabularyValues().size());
        assertThat(kindOfDataFieldChildren.get(0).getControlledVocabularyValues())
                .extracting(ControlledVocabularyValue::getStrValue)
                .containsExactly("StillImage");

        List<DatasetField> journalArticleTypeFieldChildren = importedDatasetFields
                .stream().filter(field -> field.getDatasetFieldType().getName().equals("journalArticleType"))
                .collect(Collectors.toList());

        assertEquals(1, journalArticleTypeFieldChildren.size());
        assertEquals(1, journalArticleTypeFieldChildren.get(0).getControlledVocabularyValues().size());
        assertThat(journalArticleTypeFieldChildren.get(0).getControlledVocabularyValues())
                .extracting(ControlledVocabularyValue::getStrValue)
                .containsExactly("correction");
    }

    @Test
    public void parseDataset_WithPrimitiveDatasetFieldsMismatch() throws IOException, JsonParseException {
        //given
        final String legacyHarvestedDataset = IOUtils.toString(Objects.requireNonNull(HarvestedJsonParserIT.class
                .getClassLoader()
                .getResource("json/import/harvestedDataset_mismatchedPrimitives.json")), StandardCharsets.UTF_8);

        //when
        final Dataset dataset = harvestedJsonParser.parseDataset(legacyHarvestedDataset);

        //then
        List<DatasetField> importedDatasetFields = dataset.getVersions().get(0).getDatasetFields();
        assertEquals(6, importedDatasetFields.size());
        assertThat(importedDatasetFields)
                .extracting(datasetField -> datasetField.getDatasetFieldType().getName())
                .containsExactlyInAnyOrder("title", "otherReferences", "otherReferences",
                        "otherReferences", "dataSources", "collectorTraining");

        List<DatasetField> titleField = importedDatasetFields
                .stream().filter(field -> field.getDatasetFieldType().getName().equals("title"))
                .collect(Collectors.toList());

        assertEquals(1, titleField.size());
        assertThat(titleField.get(0).getValue())
                .isEqualTo("geo");

        List<DatasetField> otherReferencesFields = importedDatasetFields
                .stream().filter(field -> field.getDatasetFieldType().getName().equals("otherReferences"))
                .collect(Collectors.toList());

        assertEquals(3, otherReferencesFields.size());
        assertThat(otherReferencesFields)
                .extracting(DatasetField::getValue)
                .containsExactlyInAnyOrder("value 1", "value 2", "value 3");

        List<DatasetField> dataSourcesField = importedDatasetFields
                .stream().filter(field -> field.getDatasetFieldType().getName().equals("dataSources"))
                .collect(Collectors.toList());

        assertEquals(1, dataSourcesField.size());
        assertThat(dataSourcesField)
                .extracting(DatasetField::getValue)
                .containsExactlyInAnyOrder("multi value field passed as single value");

        List<DatasetField> collectorTrainingField = importedDatasetFields
                .stream().filter(field -> field.getDatasetFieldType().getName().equals("collectorTraining"))
                .collect(Collectors.toList());

        assertEquals(1, collectorTrainingField.size());
        assertThat(collectorTrainingField)
                .extracting(DatasetField::getValue)
                .containsExactlyInAnyOrder("collectorTraining single value passed as multi");
    }
}