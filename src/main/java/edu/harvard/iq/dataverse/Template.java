package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.Size;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author skraffmiller
 */
@Entity
public class Template implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Template() {

    }

    //Constructor for create
    public Template(Dataverse dataverseIn) {
        dataverse = dataverseIn;
        datasetFields = initDatasetFields();
        initMetadataBlocksForCreate();
    }

    public Long getId() {
        return this.id;
    }

    @NotBlank(message = "Please enter a name.")
    @Size(max = 255, message = "Name must be at most 255 characters.")
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

    @OneToMany(mappedBy = "template", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    //@OrderBy("datasetField.displayOrder") 
    private List<DatasetField> datasetFields = new ArrayList();

    public List<DatasetField> getDatasetFields() {
        return datasetFields;
    }

    @Transient
    private Map<MetadataBlock, List<DatasetField>> metadataBlocksForView = new HashMap();
    @Transient
    private Map<MetadataBlock, List<DatasetField>> metadataBlocksForEdit = new HashMap();

    public Map<MetadataBlock, List<DatasetField>> getMetadataBlocksForView() {
        return metadataBlocksForView;
    }

    public void setMetadataBlocksForView(Map<MetadataBlock, List<DatasetField>> metadataBlocksForView) {
        this.metadataBlocksForView = metadataBlocksForView;
    }

    public Map<MetadataBlock, List<DatasetField>> getMetadataBlocksForEdit() {
        return metadataBlocksForEdit;
    }

    public void setMetadataBlocksForEdit(Map<MetadataBlock, List<DatasetField>> metadataBlocksForEdit) {
        this.metadataBlocksForEdit = metadataBlocksForEdit;
    }

    @Transient
    private Dataverse dataverse;

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    private List<DatasetField> initDatasetFields() {
        //retList - Return List of values
        List<DatasetField> retList = new ArrayList();
        for (DatasetField dsf : this.getDatasetFields()) {
            retList.add(initDatasetField(dsf));
        }

        //Test to see that there are values for 
        // all fields in this dataset via metadata blocks
        //only add if not added above
        for (MetadataBlock mdb : this.getDataverse().getMetadataBlocks()) {
            for (DatasetFieldType dsfType : mdb.getDatasetFieldTypes()) {
                if (!dsfType.isSubField()) {
                    boolean add = true;
                    //don't add if already added as a val
                    for (DatasetField dsf : retList) {
                        if (dsfType.equals(dsf.getDatasetFieldType())) {
                            add = false;
                            break;
                        }
                    }

                    if (add) {
                        retList.add(DatasetField.createNewEmptyDatasetField(dsfType, this));
                    }
                }
            }
        }

        //sort via display order on dataset field
        Collections.sort(retList, new Comparator<DatasetField>() {
            public int compare(DatasetField d1, DatasetField d2) {
                int a = d1.getDatasetFieldType().getDisplayOrder();
                int b = d2.getDatasetFieldType().getDisplayOrder();
                return Integer.valueOf(a).compareTo(Integer.valueOf(b));
            }
        });

        return sortDatasetFields(retList);
    }

    private List<DatasetField> sortDatasetFields(List<DatasetField> dsfList) {
        Collections.sort(dsfList, new Comparator<DatasetField>() {
            public int compare(DatasetField d1, DatasetField d2) {
                int a = d1.getDatasetFieldType().getDisplayOrder();
                int b = d2.getDatasetFieldType().getDisplayOrder();
                return Integer.valueOf(a).compareTo(Integer.valueOf(b));
            }
        });
        return dsfList;
    }

    private void initMetadataBlocksForCreate() {
        metadataBlocksForView.clear();
        metadataBlocksForEdit.clear();
        for (MetadataBlock mdb : this.getDataverse().getMetadataBlocks()) {
            List<DatasetField> datasetFieldsForView = new ArrayList();
            List<DatasetField> datasetFieldsForEdit = new ArrayList();
            for (DatasetField dsf : this.getDatasetFields()) {

                if (dsf.getDatasetFieldType().getMetadataBlock().equals(mdb)) {
                    datasetFieldsForEdit.add(dsf);
                }
            }

            if (!datasetFieldsForView.isEmpty()) {
                metadataBlocksForView.put(mdb, sortDatasetFields(datasetFieldsForView));
            }
            if (!datasetFieldsForEdit.isEmpty()) {
                metadataBlocksForEdit.put(mdb, sortDatasetFields(datasetFieldsForEdit));
            }
        }
    }

    public void setMetadataValueBlocks() {
        //TODO: A lot of clean up on the logic of this method
        metadataBlocksForView.clear();
        metadataBlocksForEdit.clear();
        for (MetadataBlock mdb : this.getDataverse().getMetadataBlocks()) {
            List<DatasetField> datasetFieldsForView = new ArrayList();
            List<DatasetField> datasetFieldsForEdit = new ArrayList();
            for (DatasetField dsf : this.getDatasetFields()) {
                if (dsf.getDatasetFieldType().getMetadataBlock().equals(mdb)) {
                    datasetFieldsForEdit.add(dsf);
                    if (!dsf.isEmpty()) {
                        datasetFieldsForView.add(dsf);
                    }
                }
            }

            if (!datasetFieldsForView.isEmpty()) {
                metadataBlocksForView.put(mdb, sortDatasetFields(datasetFieldsForView));
            }
            if (!datasetFieldsForEdit.isEmpty()) {
                metadataBlocksForEdit.put(mdb, sortDatasetFields(datasetFieldsForEdit));
            }
        }
    }

    // TODO: clean up init methods and get them to work, cascading all the way down.
    // right now, only work for one level of compound objects
    private DatasetField initDatasetField(DatasetField dsf) {
        if (dsf.getDatasetFieldType().isCompound()) {
            for (DatasetFieldCompoundValue cv : dsf.getDatasetFieldCompoundValues()) {
                // for each compound value; check the datasetfieldTypes associated with its type
                for (DatasetFieldType dsfType : dsf.getDatasetFieldType().getChildDatasetFieldTypes()) {
                    boolean add = true;
                    for (DatasetField subfield : cv.getChildDatasetFields()) {
                        if (dsfType.equals(subfield.getDatasetFieldType())) {
                            add = false;
                            break;
                        }
                    }

                    if (add) {
                        cv.getChildDatasetFields().add(DatasetField.createNewEmptyChildDatasetField(dsfType, cv));
                    }
                }

                sortDatasetFields(cv.getChildDatasetFields());
            }
        }

        return dsf;
    }

    public Template cloneNewTemplate(Template source) {
        Template newTemplate = new Template();
        Template latestVersion = source;
        //if the latest version has values get them copied over
        if (latestVersion.getDatasetFields() != null && !latestVersion.getDatasetFields().isEmpty()) {
            newTemplate.setDatasetFields(newTemplate.copyDatasetFields(source.getDatasetFields()));
        }
        return newTemplate;
    }

    public List<DatasetField> copyDatasetFields(List<DatasetField> copyFromList) {
        List<DatasetField> retList = new ArrayList();

        for (DatasetField sourceDsf : copyFromList) {
            //the copy needs to have the current version
            retList.add(sourceDsf.copy(this));
        }

        return retList;
    }

    public void setDatasetFields(List<DatasetField> datasetFields) {
        for (DatasetField dsf : datasetFields) {
            dsf.setTemplate(this);
        }
        this.datasetFields = datasetFields;
    }
    
    public List<DatasetField> getFlatDatasetFields() {
        return getFlatDatasetFields(getDatasetFields());
    }

    private List<DatasetField> getFlatDatasetFields(List<DatasetField> dsfList) {
        List<DatasetField> retList = new LinkedList();
        for (DatasetField dsf : dsfList) {
            retList.add(dsf);
            if (dsf.getDatasetFieldType().isCompound()) {
                for (DatasetFieldCompoundValue compoundValue : dsf.getDatasetFieldCompoundValues()) {
                    retList.addAll(getFlatDatasetFields(compoundValue.getChildDatasetFields()));
                }

            }
        }
        return retList;
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
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        return true;
    } 

}
