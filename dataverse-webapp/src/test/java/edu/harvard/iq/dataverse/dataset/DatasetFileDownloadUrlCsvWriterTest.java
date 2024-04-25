package edu.harvard.iq.dataverse.dataset;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DatasetFileDownloadUrlCsvWriterTest {

    @Mock
    private SystemConfig systemConfig;

    @InjectMocks
    private DatasetFileDownloadUrlCsvWriter csvWriter;

    // -------------------- TESTS --------------------

    @Test
    public void write() throws IOException {
        // given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        List<FileMetadata> files = Lists.newArrayList(
                newFileMetadata("normal.txt", false, 1),
                newFileMetadata("normal2.pdf", false, 2),
                newFileMetadata("restricted.txt", true, 3),
                newFileMetadata("special,char.txt", false, 4),
                newFileMetadata("special\",\"char.txt", false, 5)
        );
        when(systemConfig.getDataverseSiteUrl()).thenReturn("http://test.org");


        // when
        csvWriter.write(outputStream, files);

        // then
        String[] csv = outputStream.toString().split("\r\n");
        assertEquals(6, csv.length);
        assertEquals("Filename,openAccess,url", csv[0]);
        assertEquals("normal.txt,true,http://test.org/api/access/datafile/1", csv[1]);
        assertEquals("normal2.pdf,true,http://test.org/api/access/datafile/2", csv[2]);
        assertEquals("restricted.txt,false,", csv[3]);
        assertEquals("\"special,char.txt\",true,http://test.org/api/access/datafile/4", csv[4]);
        assertEquals("\"special\"\",\"\"char.txt\",true,http://test.org/api/access/datafile/5", csv[5]);
    }

    // -------------------- PRIVATE --------------------

    private static FileMetadata newFileMetadata(String fileName, boolean restricted, int dataFileId) {
        FileMetadata file = MocksFactory.makeFileMetadata(123l, fileName, 0);
        file.getDataFile().setId(Long.valueOf(dataFileId));
        if (restricted) {
            file.getTermsOfUse().setLicense(null);
            file.getTermsOfUse().setRestrictType(FileTermsOfUse.RestrictType.NOT_FOR_REDISTRIBUTION);
        }
        return file;
    }
}
