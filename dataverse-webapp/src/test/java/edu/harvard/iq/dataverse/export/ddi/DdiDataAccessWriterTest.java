package edu.harvard.iq.dataverse.export.ddi;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.DatasetVersionDTO;
import edu.harvard.iq.dataverse.api.dto.FileDTO;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.RestrictType;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

public class DdiDataAccessWriterTest {

    private DdiDataAccessWriter ddiDataAccessWriter = new DdiDataAccessWriter();

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
    public void writeDataAccess_underEmbargo() throws XMLStreamException {
        // given
        DatasetDTO datasetDTO = new DatasetDTO();
        datasetDTO.setEmbargoActive(true);
        datasetDTO.setEmbargoDate("2010-09-21");

        // when
        ddiDataAccessWriter.writeDataAccess(xmlw, datasetDTO);
        xmlw.flush();

        //then
        assertThat(writer.toString()).isEqualTo("<dataAccs>"
                + "<notes type=\"DVN:TOA\" level=\"dv\">Access to all files in this dataset is embargoed.</notes>"
                + "<setAvail>Files in this dataset will be available from 2010-09-21.</setAvail>"
                + "</dataAccs>");
    }

    @Test
    public void writeDataAccess_no_files() throws XMLStreamException {
        // given
        DatasetDTO datasetDTO = createDatasetDTOWithFiles();

        // when
        ddiDataAccessWriter.writeDataAccess(xmlw, datasetDTO);
        xmlw.flush();

        //then
        assertThat(writer.toString()).isEqualTo("<dataAccs/>");
    }

    @Test
    public void writeDataAccess_same_license_for_all_files() throws XMLStreamException {
        // given
        FileDTO fileDTO = createFileDTOWithLicenseTerms("CC0");
        FileDTO fileDTO2 = createFileDTOWithLicenseTerms("CC0");
        DatasetDTO datasetDTO = createDatasetDTOWithFiles(fileDTO, fileDTO2);

        // when
        ddiDataAccessWriter.writeDataAccess(xmlw, datasetDTO);
        xmlw.flush();

        //then
        assertThat(writer.toString()).isEqualTo("<dataAccs>"
                + "<notes type=\"DVN:TOU\" level=\"dv\">CC0</notes>"
                + "</dataAccs>");
    }

    @Test
    public void writeDataAccess_all_rights_reserved_for_all_files() throws XMLStreamException {
        // given
        FileDTO fileDTO = createFileDTOWithAllRightsReservedTerms();
        FileDTO fileDTO2 = createFileDTOWithAllRightsReservedTerms();
        DatasetDTO datasetDTO = createDatasetDTOWithFiles(fileDTO, fileDTO2);

        // when
        ddiDataAccessWriter.writeDataAccess(xmlw, datasetDTO);
        xmlw.flush();

        //then
        assertThat(writer.toString()).isEqualTo("<dataAccs>"
                + "<notes type=\"DVN:TOU\" level=\"dv\">All rights reserved</notes>"
                + "</dataAccs>");
    }

    @Test
    public void writeDataAccess_same_restriction_for_all_files() throws XMLStreamException {
        // given
        FileDTO fileDTO = createFileDTOWithRestrictedTerms(RestrictType.ACADEMIC_PURPOSE, null);
        FileDTO fileDTO2 = createFileDTOWithRestrictedTerms(RestrictType.ACADEMIC_PURPOSE, null);
        DatasetDTO datasetDTO = createDatasetDTOWithFiles(fileDTO, fileDTO2);

        // when
        ddiDataAccessWriter.writeDataAccess(xmlw, datasetDTO);
        xmlw.flush();

        //then
        assertThat(writer.toString()).isEqualTo("<dataAccs>"
                + "<notes type=\"DVN:TOA\" level=\"dv\">Access to all files in this dataset is restricted. For academic purposes only.</notes>"
                + "</dataAccs>");
    }

    @Test
    public void writeDataAccess_same_custom_restriction_for_all_files() throws XMLStreamException {
        // given
        FileDTO fileDTO = createFileDTOWithRestrictedTerms(RestrictType.CUSTOM, "Some condition");
        FileDTO fileDTO2 = createFileDTOWithRestrictedTerms(RestrictType.CUSTOM, "Some condition");
        DatasetDTO datasetDTO = createDatasetDTOWithFiles(fileDTO, fileDTO2);

        // when
        ddiDataAccessWriter.writeDataAccess(xmlw, datasetDTO);
        xmlw.flush();

        //then
        assertThat(writer.toString()).isEqualTo("<dataAccs>"
                + "<notes type=\"DVN:TOA\" level=\"dv\">Access to all files in this dataset is restricted. Some condition</notes>"
                + "</dataAccs>");
    }

    @Test
    public void writeDataAccess_all_restricted_with_different_restrictions() throws XMLStreamException {
        // given
        FileDTO fileDTO = createFileDTOWithRestrictedTerms(RestrictType.ACADEMIC_PURPOSE, null);
        FileDTO fileDTO2 = createFileDTOWithRestrictedTerms(RestrictType.NOT_FOR_REDISTRIBUTION, null);
        DatasetDTO datasetDTO = createDatasetDTOWithFiles(fileDTO, fileDTO2);

        // when
        ddiDataAccessWriter.writeDataAccess(xmlw, datasetDTO);
        xmlw.flush();

        //then
        assertThat(writer.toString()).isEqualTo("<dataAccs>"
                + "<notes type=\"DVN:TOA\" level=\"dv\">Access to all files in this dataset is restricted. Different terms for individual files.</notes>"
                + "</dataAccs>");
    }

    @Test
    public void writeDataAccess_different_licenses() throws XMLStreamException {
        // given
        FileDTO fileDTO = createFileDTOWithLicenseTerms("CCO");
        FileDTO fileDTO2 = createFileDTOWithLicenseTerms("CC1");
        DatasetDTO datasetDTO = createDatasetDTOWithFiles(fileDTO, fileDTO2);

        // when
        ddiDataAccessWriter.writeDataAccess(xmlw, datasetDTO);
        xmlw.flush();

        //then
        assertThat(writer.toString()).isEqualTo("<dataAccs>"
                + "<notes type=\"DVN:TOU\" level=\"dv\">Different licenses or terms for individual files.</notes>"
                + "</dataAccs>");
    }

    @Test
    public void writeDataAccess_different_terms_with_one_restricted() throws XMLStreamException {
        // given
        FileDTO fileDTO = createFileDTOWithLicenseTerms("CCO");
        FileDTO fileDTO2 = createFileDTOWithRestrictedTerms(RestrictType.ACADEMIC_PURPOSE, null);
        DatasetDTO datasetDTO = createDatasetDTOWithFiles(fileDTO, fileDTO2);

        // when
        ddiDataAccessWriter.writeDataAccess(xmlw, datasetDTO);
        xmlw.flush();

        //then
        assertThat(writer.toString()).isEqualTo("<dataAccs>"
                + "<notes type=\"DVN:TOU\" level=\"dv\">Different licenses or terms for individual files.</notes>"
                + "<notes type=\"DVN:TOA\" level=\"dv\">Access to some files in this dataset is restricted.</notes>"
                + "</dataAccs>");
    }

    // -------------------- PRIVATE --------------------

    private FileDTO createFileDTOWithLicenseTerms(String licenseName) {
        FileDTO fileDTO = new FileDTO();
        fileDTO.setTermsOfUseType(TermsOfUseType.LICENSE_BASED.name());
        fileDTO.setLicenseName(licenseName);
        return fileDTO;
    }

    private FileDTO createFileDTOWithAllRightsReservedTerms() {
        FileDTO fileDTO = new FileDTO();
        fileDTO.setTermsOfUseType(TermsOfUseType.ALL_RIGHTS_RESERVED.name());
        return fileDTO;
    }

    private FileDTO createFileDTOWithRestrictedTerms(RestrictType restrictType, String restrictCustomText) {
        FileDTO fileDTO = new FileDTO();
        fileDTO.setTermsOfUseType(TermsOfUseType.RESTRICTED.name());
        fileDTO.setAccessConditions(restrictType.name());
        fileDTO.setAccessConditionsCustomText(restrictCustomText);
        return fileDTO;
    }


    private DatasetDTO createDatasetDTOWithFiles(FileDTO... fileDTOs) {
        DatasetDTO datasetDTO = new DatasetDTO();
        DatasetVersionDTO datasetVersionDTO = new DatasetVersionDTO();
        datasetDTO.setDatasetVersion(datasetVersionDTO);

        datasetVersionDTO.setFiles(Lists.newArrayList(fileDTOs));
        return datasetDTO;
    }
}
