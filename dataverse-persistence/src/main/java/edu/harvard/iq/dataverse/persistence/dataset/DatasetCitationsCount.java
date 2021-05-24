package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

/**
 * Entity that stores number of citations for a dataset.
 * <p>
 * Number of citations are currently obtained using DataCite
 * service.
 * 
 * @author madryk
 */
@Entity
public class DatasetCitationsCount implements JpaEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @JoinColumn(nullable = false)
    @ManyToOne
    private Dataset dataset;
    
    private int citationsCount;

    // -------------------- GETTERS --------------------

    public Long getId() {
        return id;
    }

    public Dataset getDataset() {
        return dataset;
    }

    /**
     * Returns number of citations for dataset referenced by {@link #getDataset()}
     * For example if method will return number 6 this
     * means that dataset is cited by 6 unique works (articles, other datasets, etc.)
     */
    public int getCitationsCount() {
        return citationsCount;
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public void setCitationsCount(int citationsCount) {
        this.citationsCount = citationsCount;
    }
}
