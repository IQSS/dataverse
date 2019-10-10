package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author skraffmiller
 */
@Entity
@Table(indexes = {@Index(columnList = "dataverse_id")})
public class Template implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Template() {

    }
    public Template(Dataverse dataverseIn) {
        dataverse = dataverseIn;
    }

    public Long getId() {
        return this.id;
    }

    @NotBlank(message = "{dataset.templatename}")
    @Size(max = 255, message = "{dataset.nameLength}")
    @Column(nullable = false)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private Long usageCount;

    public Long getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Long usageCount) {
        this.usageCount = usageCount;
    }

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date createTime;

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getCreateDate() {
        return new SimpleDateFormat("MMMM d, yyyy").format(createTime);
    }

    @OneToOne(cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST}, orphanRemoval = true)
    @JoinColumn(name = "termsOfUseAndAccess_id")
    private TermsOfUseAndAccess termsOfUseAndAccess;

    public TermsOfUseAndAccess getTermsOfUseAndAccess() {
        return termsOfUseAndAccess;
    }

    public void setTermsOfUseAndAccess(TermsOfUseAndAccess termsOfUseAndAccess) {
        this.termsOfUseAndAccess = termsOfUseAndAccess;
    }

    @OneToMany(mappedBy = "template", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetField> datasetFields = new ArrayList<>();

    @ManyToOne
    @JoinColumn(nullable = true)
    private Dataverse dataverse;

    public List<DatasetField> getDatasetFields() {
        return datasetFields;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public Template cloneNewTemplate(Template source) {
        Template newTemplate = new Template();
        Template latestVersion = source;
        //if the latest version has values get them copied over
        if (latestVersion.getDatasetFields() != null && !latestVersion.getDatasetFields().isEmpty()) {
            newTemplate.setDatasetFields(DatasetFieldUtil.copyDatasetFields(source.getDatasetFields()));
        }
        TermsOfUseAndAccess terms;
        if (source.getTermsOfUseAndAccess() != null) {
            terms = source.getTermsOfUseAndAccess().copyTermsOfUseAndAccess();
        } else {
            terms = new TermsOfUseAndAccess();
            terms.setLicense(TermsOfUseAndAccess.defaultLicense);
        }
        newTemplate.setTermsOfUseAndAccess(terms);
        newTemplate.setDataverse(dataverse);

        return newTemplate;
    }

    public void setDatasetFields(List<DatasetField> datasetFields) {
        for (DatasetField dsf : datasetFields) {
            dsf.setTemplate(this);
        }
        this.datasetFields = datasetFields;
    }

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
        return this.id == other.id || (this.id != null && this.id.equals(other.id));
    }

}
