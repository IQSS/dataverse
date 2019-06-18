package edu.harvard.iq.dataverse.datavariable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(indexes = {@Index(columnList = "category_id"), @Index(columnList = "variablemetadata_id")})
public class CategoryMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    VariableCategory category;

    @ManyToOne
    @JoinColumn(nullable = false)
    private VariableMetadata variableMetadata;

    Double wfreq;

    public CategoryMetadata() {
    }

    public CategoryMetadata(VariableMetadata variableMetadata, VariableCategory category) {
        this.variableMetadata = variableMetadata;
        this.category = category;
    }

    public Double getWfreq() {
        return wfreq;
    }

    public void setWfreq(Double wfreq) {
        this.wfreq = wfreq;
    }

    public VariableCategory getCategory() {
        return category;
    }

    public void setCategory(VariableCategory category) {
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setVariableMetadata(VariableMetadata variableMetadata) {
        this.variableMetadata = variableMetadata;
    }

    public VariableMetadata getVariableMetadata() {
        return variableMetadata;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CategoryMetadata)) {
            return false;
        }

        CategoryMetadata other = (CategoryMetadata) object;
        if (this.id != other.id) {
            return this.id != null && this.id.equals(other.id);
        }
        return true;
    }
}
