package edu.harvard.iq.dataverse.dataset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FileLabelInfo implements Comparable<FileLabelInfo> {
    private long id;
    private String label;
    private String labelAfterChange;

    @JsonIgnore
    private boolean affected;

    // -------------------- CONSTRUCTORS --------------------

    public FileLabelInfo(long id, String label, boolean affected) {
        this(id, label, null, affected);
    }

    public FileLabelInfo(long id, String label, String labelAfterChange, boolean affected) {
        this.id = id;
        this.label = label;
        this.labelAfterChange = labelAfterChange;
        this.affected = affected;
    }

    // -------------------- LOGIC --------------------

    @Override
    public int compareTo(FileLabelInfo other) {
        return Long.compare(id, other.getId());
    }

    // -------------------- GETTERS --------------------

    public long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getLabelAfterChange() {
        return labelAfterChange;
    }

    public boolean isAffected() {
        return affected;
    }
}
