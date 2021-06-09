package edu.harvard.iq.dataverse.persistence.ror;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class RorLabel implements Serializable {

    @Column
    private String label;

    @Column
    private String code;

    // -------------------- CONSTRUCTORS --------------------

    public RorLabel() { }

    public RorLabel(String label, String code) {
        this.label = label;
        this.code = code;
    }

    // -------------------- GETTERS --------------------

    public String getLabel() {
        return label;
    }

    public String getCode() {
        return code;
    }

    // -------------------- SETTERS --------------------

    public void setLabel(String label) {
        this.label = label;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
