package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.config.EntityCustomizer;
import edu.harvard.iq.dataverse.persistence.config.annotations.CustomizeSelectionQuery;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.eclipse.persistence.annotations.Customizer;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author skraffmiller
 */
@Entity
@Table(indexes = {@Index(columnList = "dataverse_id")})
@Customizer(EntityCustomizer.class)
public class Template implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "{dataset.templatename}")
    @Size(max = 255, message = "{dataset.nameLength}")
    @Column(nullable = false)
    private String name;

    private Long usageCount;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date createTime;

    @OneToMany(mappedBy = "template", orphanRemoval = true,
            cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @CustomizeSelectionQuery(EntityCustomizer.Customizations.DATASET_FIELDS_WITH_PRIMARY_SOURCE)
    private List<DatasetField> datasetFields = new ArrayList<>();

    @ManyToOne
    @JoinColumn(nullable = true)
    private Dataverse dataverse;

    // -------------------- CONSTRUCTORS --------------------

    public Template() { }

    public Template(Dataverse dataverseIn) {
        dataverse = dataverseIn;
    }

    // -------------------- GETTERS --------------------

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return name;
    }

    public Long getUsageCount() {
        return usageCount;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public String getCreateDate() {
        return new SimpleDateFormat("MMMM d, yyyy").format(createTime);
    }

    public List<DatasetField> getDatasetFields() {
        return datasetFields;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    // -------------------- LOGIC --------------------

    public Template cloneNewTemplate(Template source) {
        Template newTemplate = new Template();

        // if the latest version has values get them copied over
        if (source.getDatasetFields() != null && !source.getDatasetFields().isEmpty()) {
            newTemplate.setDatasetFields(DatasetFieldUtil.copyDatasetFields(source.getDatasetFields()));
        }
        newTemplate.setDataverse(dataverse);

        return newTemplate;
    }

    public Template cloneNewTemplate() {
        return cloneNewTemplate(this);
    }

    // -------------------- SETTERS --------------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUsageCount(Long usageCount) {
        this.usageCount = usageCount;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public void setDatasetFields(List<DatasetField> datasetFields) {
        for (DatasetField dsf : datasetFields) {
            dsf.setTemplate(this);
        }
        this.datasetFields = datasetFields;
    }

    // -------------------- hashCode & equals --------------------

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Template)) {
            return false;
        }
        Template other = (Template) object;
        return Objects.equals(this.id, other.id);
    }

}
