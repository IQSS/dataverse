package edu.harvard.iq.dataverse.persistence.dataset;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * @author ellenk
 */
@Entity
@Table(indexes = {@Index(columnList = "controlledvocabularyvalue_id"), @Index(columnList = "datasetfieldtype_id")})
public class ControlledVocabAlternate implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(columnDefinition = "TEXT", nullable = false)
    private String strValue;

    public String getStrValue() {
        return strValue;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;

    }

    @ManyToOne
    @JoinColumn(nullable = false)
    private DatasetFieldType datasetFieldType;

    public DatasetFieldType getDatasetFieldType() {
        return datasetFieldType;
    }

    public void setDatasetFieldType(DatasetFieldType datasetFieldType) {
        this.datasetFieldType = datasetFieldType;
    }

    @ManyToOne
    @JoinColumn(nullable = false)
    private ControlledVocabularyValue controlledVocabularyValue;

    public ControlledVocabularyValue getControlledVocabularyValue() {
        return controlledVocabularyValue;
    }

    public void setControlledVocabularyValue(ControlledVocabularyValue controlledVocabularyValue) {
        this.controlledVocabularyValue = controlledVocabularyValue;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ControlledVocabAlternate other = (ControlledVocabAlternate) obj;
        return Objects.equals(this.id, other.id);
    }


}
