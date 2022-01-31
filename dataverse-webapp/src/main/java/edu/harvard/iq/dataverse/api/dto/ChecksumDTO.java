package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;

public class ChecksumDTO {
    private String type;
    private String value;

    // -------------------- GETTERS --------------------

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    // -------------------- SETTERS --------------------

    public void setType(String type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    // -------------------- INNER CLASSES --------------------

    public static class Creator {

        // -------------------- LOGIC --------------------

        public ChecksumDTO create(SolrSearchResult result) {
            if (result.getFileChecksumType() == null) {
                return null;
            }
            ChecksumDTO checksum = new ChecksumDTO();
            checksum.setType(result.getFileChecksumType().toString());
            checksum.setValue(result.getFileChecksumValue());
            return checksum;
        }

        public ChecksumDTO create(DataFile dataFile) {
            if (dataFile.getChecksumType() == null) {
                return null;
            }
            ChecksumDTO checksum = new ChecksumDTO();
            checksum.setType(dataFile.getChecksumType().toString());
            checksum.setValue(dataFile.getChecksumValue());
            return checksum;
        }
    }
}
