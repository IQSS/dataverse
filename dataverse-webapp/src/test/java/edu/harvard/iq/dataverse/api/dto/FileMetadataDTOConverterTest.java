package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.api.dto.FileMetadataDTO.DataFileDTO;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FileMetadataDTOConverterTest {

    @Test
    void convert() {
        // given
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setLabel("label");
        fileMetadata.setVersion(1L);
        fileMetadata.setTermsOfUse(new FileTermsOfUse());
        fileMetadata.setDatasetVersion(new DatasetVersion());
        DataFile dataFile = new DataFile() {
            public List<String> getTagLabels() {
                return Stream.of("tag1", "tag2").collect(Collectors.toList());
            }
        };
        dataFile.setId(3L);
        dataFile.setFilesize(123L);
        dataFile.setProtocol("doi");
        dataFile.setAuthority("1234");
        dataFile.setIdentifier("id");
        fileMetadata.setDataFile(dataFile);

        // when
        FileMetadataDTO converted = new FileMetadataDTO.Converter().convert(fileMetadata);

        // then
        assertThat(converted)
                .extracting(FileMetadataDTO::getLabel, FileMetadataDTO::getVersion, FileMetadataDTO::getTermsOfUseType)
                .containsExactly("label", 1L, "TERMS_UNKNOWN");
        assertThat(converted.getDataFile())
                .extracting(DataFileDTO::getId, DataFileDTO::getFilesize, DataFileDTO::getPersistentId)
                .containsExactly(3L, 123L, "doi:1234/id");
        assertThat(converted.getDataFile().getTabularTags())
                .containsExactly("tag1", "tag2");
    }
}