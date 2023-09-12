package edu.harvard.iq.dataverse.datavariable;

import jakarta.persistence.Index;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

@Entity
@Table(indexes = {@Index(columnList="category_id"), @Index(columnList="variablemetadata_id")})
public class CategoryMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(nullable=false)
    VariableCategory category;

    @ManyToOne
    @JoinColumn(nullable=false)
    private VariableMetadata variableMetadata;

    Double wfreq;

    public CategoryMetadata() {}

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
        if (this.id != other.id ) {
            if (this.id == null || !this.id.equals(other.id)) {
                return false;
            }
        }
        return true;
    }
}
