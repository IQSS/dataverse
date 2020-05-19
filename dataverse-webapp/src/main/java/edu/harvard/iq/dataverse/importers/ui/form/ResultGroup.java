package edu.harvard.iq.dataverse.importers.ui.form;

import java.util.ArrayList;
import java.util.List;

public class ResultGroup {
    private String localizedName;
    private ProcessingType processingType;
    private List<ResultItem> items = new ArrayList<>();

    // -------------------- GETTERS --------------------

    public String getLocalizedName() {
        return localizedName;
    }

    public ProcessingType getProcessingType() {
        return processingType;
    }

    public List<ResultItem> getItems() {
        return items;
    }

    // -------------------- SETTERS --------------------

    public void setProcessingType(ProcessingType processingType) {
        this.processingType = processingType;
    }

    // -------------------- NON-JavaBean SETTERES --------------------

    public ResultGroup setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
        return this;
    }
}
