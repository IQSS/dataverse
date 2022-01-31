package edu.harvard.iq.dataverse.export.ddi;

import edu.harvard.iq.dataverse.api.dto.DatasetDTO;
import edu.harvard.iq.dataverse.api.dto.FileMetadataDTO;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.util.xml.XmlAttribute;
import io.vavr.Tuple2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.util.List;

import static edu.harvard.iq.dataverse.util.xml.XmlStreamWriterUtils.writeFullElement;
import static edu.harvard.iq.dataverse.util.xml.XmlStreamWriterUtils.writeFullElementWithAttributes;

public class DdiDataAccessWriter {

    // -------------------- LOGIC --------------------

    public void writeDataAccess(XMLStreamWriter xmlw, DatasetDTO datasetDTO) throws XMLStreamException {
        Tuple2<String, String> termsOfUseAndAccess = obtainTermsOfUseAndAccess(datasetDTO);

        xmlw.writeStartElement("dataAccs");

        writeFullElementWithAttributes(xmlw, "notes", termsOfUseAndAccess._1(),
                XmlAttribute.of("type", DdiConstants.NOTE_TYPE_TERMS_OF_USE),
                XmlAttribute.of("level", "dv"));
        writeFullElementWithAttributes(xmlw, "notes", termsOfUseAndAccess._2(),
                XmlAttribute.of("type", DdiConstants.NOTE_TYPE_TERMS_OF_ACCESS),
                XmlAttribute.of("level", "dv"));
        writeFullElement(xmlw, "setAvail", datasetDTO.getEmbargoActive() ?
                "Files in this dataset will be available from " + datasetDTO.getEmbargoDate() + "." : StringUtils.EMPTY);
        xmlw.writeEndElement();
    }

    // -------------------- PRIVATE --------------------

    private Tuple2<String, String> obtainTermsOfUseAndAccess(DatasetDTO datasetDTO) {
        if (datasetDTO.getEmbargoActive()) {
            return new Tuple2<>(StringUtils.EMPTY, "Access to all files in this dataset is embargoed.");
        }
        List<FileMetadataDTO> files = datasetDTO.getDatasetVersion().getFiles();
        if (CollectionUtils.isEmpty(files)) {
            return new Tuple2<>(StringUtils.EMPTY, StringUtils.EMPTY);
        }

        if (areAllFilesHaveSameTermsOfUseTypeOrLicense(files)) {
            FileMetadataDTO firstFile = files.get(0);
            if (isOfTermsOfUseType(firstFile, TermsOfUseType.ALL_RIGHTS_RESERVED)) {
                return new Tuple2<>("All rights reserved", StringUtils.EMPTY);
            }
            if (isOfTermsOfUseType(firstFile, TermsOfUseType.LICENSE_BASED)) {
                return new Tuple2<>(firstFile.getLicenseName(), StringUtils.EMPTY);
            }

            return new Tuple2<>(StringUtils.EMPTY, obtainTermsOfAccessForAllRestrictedFiles(files));
        } else {
            return new Tuple2<>("Different licenses or terms for individual files.",
                                hasRestrictedFile(files) ? "Access to some files in this dataset is restricted." : "");
        }
    }

    private String obtainTermsOfAccessForAllRestrictedFiles(List<FileMetadataDTO> files) {
        if (areAllFilesHaveSameRestrictedType(files)) {
            return "Access to all files in this dataset is restricted. " + obtainRestrictedTypeText(files.get(0));
        } else {
            return "Access to all files in this dataset is restricted. Different terms for individual files.";
        }
    }

    private String obtainRestrictedTypeText(FileMetadataDTO file) {
        if (StringUtils.equals(FileTermsOfUse.RestrictType.CUSTOM.name(), file.getAccessConditions())) {
            return file.getAccessConditionsCustomText();
        }
        if (StringUtils.equals(FileTermsOfUse.RestrictType.ACADEMIC_PURPOSE.name(), file.getAccessConditions())) {
            return "For academic purposes only.";
        }
        if (StringUtils.equals(FileTermsOfUse.RestrictType.ACADEMIC_PURPOSE_AND_NOT_FOR_REDISTRIBUTION.name(), file.getAccessConditions())) {
            return "For academic purposes only, not for redistribution.";
        }
        if (StringUtils.equals(FileTermsOfUse.RestrictType.NOT_FOR_REDISTRIBUTION.name(), file.getAccessConditions())) {
            return "Not for redistribution.";
        }
        return StringUtils.EMPTY;
    }


    private boolean areAllFilesHaveSameTermsOfUseTypeOrLicense(List<FileMetadataDTO> files) {
        FileMetadataDTO firstFile = files.get(0);

        return files.stream().allMatch(fileDTO ->
                StringUtils.equals(firstFile.getLicenseName(), fileDTO.getLicenseName())
                && StringUtils.equals(firstFile.getTermsOfUseType(), fileDTO.getTermsOfUseType()));
    }

    private boolean areAllFilesHaveSameRestrictedType(List<FileMetadataDTO> files) {
        FileMetadataDTO firstFile = files.get(0);

        return files.stream().allMatch(fileDTO ->
                StringUtils.equals(firstFile.getAccessConditions(), fileDTO.getAccessConditions())
                && StringUtils.equals(firstFile.getAccessConditionsCustomText(), fileDTO.getAccessConditionsCustomText()));
    }

    private boolean hasRestrictedFile(List<FileMetadataDTO> files) {
        return files
                .stream()
                .anyMatch(fileDTO -> isOfTermsOfUseType(fileDTO, FileTermsOfUse.TermsOfUseType.RESTRICTED));
    }


    private boolean isOfTermsOfUseType(FileMetadataDTO fileDTO, FileTermsOfUse.TermsOfUseType termsOfUseType) {
        return fileDTO.getTermsOfUseType().equals(termsOfUseType.toString());
    }
}
