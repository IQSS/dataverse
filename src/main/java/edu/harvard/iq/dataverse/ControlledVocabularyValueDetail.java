package edu.harvard.iq.dataverse;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Column;
import javax.persistence.OneToOne;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(indexes = {@Index(columnList = "controlledvocabularyvalue_id")})
public class ControlledVocabularyValueDetail implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(columnDefinition = "TEXT")
    private String displayFormat;

    public String getDisplayFormat() { return displayFormat; }

    public void setDisplayFormat(String displayFormat) { this.displayFormat = displayFormat; }

    @Column(columnDefinition = "TEXT")
    private String strValue;

    public String getStrValue() {
        return strValue;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;
    }

    @OneToOne
    @JoinColumn(name = "controlledvocabularyvalue_id", unique = true)
    private ControlledVocabularyValue controlledVocabularyValue;

    public ControlledVocabularyValue getControlledVocabularyValue() {
        return controlledVocabularyValue;
    }

    public void setControlledVocabularyValue(ControlledVocabularyValue controlledVocabularyValue) {
        this.controlledVocabularyValue = controlledVocabularyValue;
    }
}
