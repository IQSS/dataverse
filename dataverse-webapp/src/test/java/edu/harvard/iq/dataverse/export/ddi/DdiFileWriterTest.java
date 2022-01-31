package edu.harvard.iq.dataverse.export.ddi;

import edu.harvard.iq.dataverse.api.dto.FileMetadataDTO;
import edu.harvard.iq.dataverse.api.dto.FileMetadataDTO.DataFileDTO;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DdiFileWriterTest {

    @InjectMocks
    private DdiFileWriter ddiFileWriter;
    @Mock
    private SystemConfig systemConfig;

    private StringWriter writer;
    private XMLStreamWriter xmlw;


    @BeforeEach
    public void before() throws XMLStreamException {
        writer = new StringWriter();
        xmlw = XMLOutputFactory.newInstance().createXMLStreamWriter(writer);
    }

    @AfterEach
    public void after() throws IOException, XMLStreamException {
        xmlw.close();
        writer.close();
    }

    // -------------------- TESTS --------------------

    @Test
    void writeOtherMatFromFileDto() throws XMLStreamException, IOException {
        // given
        FileMetadataDTO fileDto = new FileMetadataDTO();
        DataFileDTO dataFileDto = new DataFileDTO();
        dataFileDto.setId(10L);
        dataFileDto.setPidURL("https://doi.org/pid");
        dataFileDto.setFilename("file.txt");
        dataFileDto.setDescription("some description");
        dataFileDto.setContentType("plain/text");
        fileDto.setDataFile(dataFileDto);


        // when
        ddiFileWriter.writeOtherMatFromFileDto(xmlw, fileDto);
        xmlw.flush();

        // then
        assertThat(writer.toString()).isEqualTo("<otherMat ID=\"f10\" URI=\"https://doi.org/pid\" level=\"datafile\">"
                + "<labl>file.txt</labl>"
                + "<txt>some description</txt>"
                + "<notes level=\"file\" type=\"DATAVERSE:CONTENTTYPE\" subject=\"Content/MIME Type\">plain/text</notes>"
                + "</otherMat>");
    }
    @Test
    void writeOtherMatFromFileDto_without_pid_url() throws XMLStreamException, IOException {
        // given
        FileMetadataDTO fileDto = new FileMetadataDTO();
        DataFileDTO dataFileDto = new DataFileDTO();
        dataFileDto.setId(10L);
        dataFileDto.setFilename("file.txt");
        fileDto.setDataFile(dataFileDto);

        when(systemConfig.getDataverseSiteUrl()).thenReturn("http://localhost:8080");


        // when
        ddiFileWriter.writeOtherMatFromFileDto(xmlw, fileDto);
        xmlw.flush();

        // then
        assertThat(writer.toString()).contains("URI=\"http://localhost:8080/api/access/datafile/10\"");
    }

    @Test
    void writeOtherMatFromFileMetadata() throws XMLStreamException, IOException {
        // given
        FileMetadata fileMetadata = createFileMetadata();

        // when
        ddiFileWriter.writeOtherMatFromFileMetadata(xmlw, fileMetadata);
        xmlw.flush();

        // then
        assertThat(writer.toString()).isEqualTo("<otherMat ID=\"f15\" URI=\"https://doi.org/10.1012/someId\" level=\"datafile\">"
                + "<labl>file.txt</labl>"
                + "<txt>some description</txt>"
                + "<notes level=\"file\" type=\"DATAVERSE:CONTENTTYPE\" subject=\"Content/MIME Type\">plain/text</notes>"
                + "</otherMat>");
    }

    @Test
    void writeOtherMatFromFileMetadata_file_without_complete_global_id() throws XMLStreamException, IOException {
        // given
        when(systemConfig.getDataverseSiteUrl()).thenReturn("http://localhost:8080");
        FileMetadata fileMetadata = createFileMetadata();
        fileMetadata.getDataFile().setIdentifier(null);

        // when
        ddiFileWriter.writeOtherMatFromFileMetadata(xmlw, fileMetadata);
        xmlw.flush();

        // then
        assertThat(writer.toString()).contains("URI=\"http://localhost:8080/api/access/datafile/15\"");
    }

    @Test
    void writeFileDscr() throws XMLStreamException {
        // given
        when(systemConfig.getDataverseSiteUrl()).thenReturn("http://localhost:8080");
        FileMetadata fileMetadata = createFileMetadataForTabularFile();

        // when
        ddiFileWriter.writeFileDscr(xmlw, fileMetadata);
        xmlw.flush();

        // then
        assertThat(writer.toString()).isEqualTo("<fileDscr ID=\"f15\" URI=\"http://localhost:8080/api/access/datafile/15\">"
                + "<fileTxt>"
                + "<fileName>file.txt</fileName>"
                + "<dimensns>"
                + "<caseQnty>100</caseQnty><varQnty>3</varQnty><recPrCas>10</recPrCas>"
                + "</dimensns><"
                + "fileType>plain/text</fileType>"
                + "</fileTxt></fileDscr>");
    }

    // -------------------- PRIVATE --------------------

    private FileMetadata createFileMetadata() {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setLabel("file.txt");
        fileMetadata.setDescription("some description");

        DataFile dataFile = new DataFile();
        dataFile.setId(15L);
        dataFile.setProtocol("doi");
        dataFile.setAuthority("10.1012");
        dataFile.setIdentifier("someId");
        dataFile.setContentType("plain/text");
        fileMetadata.setDataFile(dataFile);
        return fileMetadata;
    }

    private FileMetadata createFileMetadataForTabularFile() {
        FileMetadata fileMetadata = createFileMetadata();
        DataFile dataFile = fileMetadata.getDataFile();

        DataTable dataTable = new DataTable();
        dataTable.setVarQuantity(3L);
        dataTable.setCaseQuantity(100L);
        dataTable.setRecordsPerCase(10L);
        dataFile.setDataTable(dataTable);

        return fileMetadata;
    }
}
