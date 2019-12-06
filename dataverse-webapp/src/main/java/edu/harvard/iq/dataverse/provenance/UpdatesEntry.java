package edu.harvard.iq.dataverse.provenance;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import io.vavr.control.Option;

/**
 *For storing datafile and provjson in a map value.
 *
 * Note: I'm only certain that provJson can be nullable, other values must be checked.
 */
public class UpdatesEntry {
    private Option<String> provJson;
    private DataFile dataFile;
    private String provFreeform;
    private Boolean deleteJson;

    // -------------------- CONSTRUCTORS --------------------

    public UpdatesEntry(DataFile dataFile, String provJson, Boolean deleteJson, String provFreeform) {

        this.provJson = Option.of(provJson);
        this.dataFile = dataFile;
        this.provFreeform = provFreeform;
        this.deleteJson = deleteJson;
    }

    public UpdatesEntry(DataFile dataFile, Option<String> provJson, Boolean deleteJson, String provFreeform) {
        this.provJson = provJson;
        this.dataFile = dataFile;
        this.provFreeform = provFreeform;
        this.deleteJson = deleteJson;
    }

    // -------------------- GETTERS --------------------

    public Option<String> getProvJson() {
        return provJson;
    }

    public DataFile getDataFile() {
        return dataFile;
    }

    public String getProvFreeform() {
        return provFreeform;
    }

    public Boolean getDeleteJson() {
        return deleteJson;
    }

    // -------------------- SETTERS --------------------

    public void setProvJson(Option<String> provJson) {
        this.provJson = provJson;
    }

    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile;
    }

    public void setProvFreeform(String provFreeform) {
        this.provFreeform = provFreeform;
    }

    public void setDeleteJson(Boolean deleteJson) {
        this.deleteJson = deleteJson;
    }
}
