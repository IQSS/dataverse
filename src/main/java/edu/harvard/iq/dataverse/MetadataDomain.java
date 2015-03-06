package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

/**
 *
 * @author skraffmiller
 */
@Entity
public class MetadataDomain implements Serializable {
        private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column( nullable = false )
    private String name;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
        public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    @ManyToMany(cascade={CascadeType.PERSIST } )
    private Collection<DatasetFieldType> datasetFields;
    
    public Collection<DatasetFieldType> getDatasetFields() {
        return datasetFields;
    }

    public void setDatasetFields(Collection<DatasetFieldType> datasetFields) {
        this.datasetFields = datasetFields;
    }
    
}
