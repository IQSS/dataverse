package edu.harvard.iq.dataverse.api.dto;

import java.util.HashSet;
import java.util.Set;

public class UningestRequestDTO {

    private Set<Long> dataFileIds = new HashSet<>();

    // -------------------- GETTERS --------------------

    public Set<Long> getDataFileIds() {
        return dataFileIds;
    }

    // -------------------- LOGIC --------------------

    public UningestRequestDTO addDataFileId(Long id) {
        this.dataFileIds.add(id);
        return this;
    }
}
