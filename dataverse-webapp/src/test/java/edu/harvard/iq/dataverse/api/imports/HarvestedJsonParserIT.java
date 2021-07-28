package edu.harvard.iq.dataverse.api.imports;

import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

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

        Assertions.assertThat(mxrdrFile).extracting(FileTermsOfUse::isAllRightsReserved).isEqualTo(true);

        Assertions.assertThat(repodFile).extracting(FileTermsOfUse::getTermsOfUseType).isEqualTo(FileTermsOfUse.TermsOfUseType.LICENSE_BASED);
        Assertions.assertThat(repodFile).extracting(FileTermsOfUse::getLicense).extracting(License::getName)
        .isEqualTo("CC0 Creative Commons Zero 1.0 Waiver");

        Assertions.assertThat(rorDataFile).extracting(FileTermsOfUse::getTermsOfUseType).isEqualTo(FileTermsOfUse.TermsOfUseType.RESTRICTED);
        Assertions.assertThat(rorDataFile).extracting(FileTermsOfUse::getRestrictType).isEqualTo(FileTermsOfUse.RestrictType.CUSTOM);
        Assertions.assertThat(rorDataFile).extracting(FileTermsOfUse::getRestrictCustomText).isEqualTo("terms desc");

        Assertions.assertThat(sampleFile).extracting(FileTermsOfUse::getTermsOfUseType).isEqualTo(FileTermsOfUse.TermsOfUseType.RESTRICTED);
        Assertions.assertThat(sampleFile).extracting(FileTermsOfUse::getRestrictType).isEqualTo(FileTermsOfUse.RestrictType.ACADEMIC_PURPOSE);

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
        Assertions.assertThat(dataset.getFiles())
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
        Assertions.assertThat(dataset.getFiles())
                  .extracting(dataFile -> dataFile.getFileMetadata().getTermsOfUse().getTermsOfUseType())
                  .containsOnly(FileTermsOfUse.TermsOfUseType.TERMS_UNKNOWN);


    }

    private FileMetadata retrieveDatafileByName(List<DataFile> dataFileList, String nameToFind) {
        return dataFileList.stream()
                           .map(DataFile::getFileMetadata)
                           .filter(fileMetadata -> fileMetadata.getLabel().equals(nameToFind))
                           .findFirst().get();
    }
}