package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;

import java.util.List;
import java.util.stream.Collectors;

public class DataFileListDTO {
    private List<FileMetadataDTO> files;

    // -------------------- GETTERS --------------------

    public List<FileMetadataDTO> getFiles() {
        return files;
    }

    // -------------------- SETTERS --------------------

    public void setFiles(List<FileMetadataDTO> files) {
        this.files = files;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Converter {
        public DataFileListDTO convert(List<DataFile> dataFiles) {
            if (dataFiles == null) {
                throw new NullPointerException("dataFiles cannot be null");
            }
            DataFileListDTO converted = new DataFileListDTO();
            converted.setFiles(new FileMetadataDTO.Converter().convert(dataFiles.stream()
                    .map(DataFile::getFileMetadata)
                    .collect(Collectors.toList())));
            return converted;
        }
    }
}
