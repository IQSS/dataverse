package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
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
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import edu.harvard.iq.dataverse.util.DateUtil;
import edu.harvard.iq.dataverse.util.json.JsonUtil;

import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author skraffmiller
 */

@NamedQueries({
    @NamedQuery(name = "Template.findByOwnerId",
               query = "select object(o) from Template as o where o.dataverse.id =:ownerId"),
    @NamedQuery(name = "Template.findAll",
               query = "select object(o) from Template as o")
})

@Entity
@Table(indexes = {@Index(columnList="dataverse_id")})
public class Template implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Template() {
    }

    //Constructor for create
    public Template(Dataverse dataverseIn, List<MetadataBlock> systemMDBlocks) {
        dataverse = dataverseIn;
        datasetFields = initDatasetFields();
        initMetadataBlocksForCreate(systemMDBlocks);
    }

    public Long getId() {
        return this.id;
    }

    @NotBlank(message = "{dataset.templatename}")
    @Size(max = 255, message = "{dataset.nameLength}")
    @Column( nullable = false )
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
    @Column( nullable = false )
    private Date createTime;

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getCreateDate() {
        return DateUtil.formatDate(createTime);
    }
    
    @OneToOne(cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST}, orphanRemoval=true)
    @JoinColumn(name = "termsOfUseAndAccess_id")
    private TermsOfUseAndAccess termsOfUseAndAccess;

    public TermsOfUseAndAccess getTermsOfUseAndAccess() {
        return termsOfUseAndAccess;
    }

    public void setTermsOfUseAndAccess(TermsOfUseAndAccess termsOfUseAndAccess) {
        this.termsOfUseAndAccess = termsOfUseAndAccess;
    }

    @OneToMany(mappedBy = "template", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    //@OrderBy("datasetField.displayOrder") 
    private List<DatasetField> datasetFields = new ArrayList<>();

    public List<DatasetField> getDatasetFields() {
        return datasetFields;
    }
    
    @Column(columnDefinition="TEXT", nullable = true )
    private String instructions;

    @Transient
    private Map<String, String> instructionsMap = null;
    
    @Transient
    private TreeMap<MetadataBlock, List<DatasetField>> metadataBlocksForView = new TreeMap<>();
    @Transient
    private TreeMap<MetadataBlock, List<DatasetField>> metadataBlocksForEdit = new TreeMap<>();
    
    @Transient
    private boolean isDefaultForDataverse;

    public boolean isIsDefaultForDataverse() {
        return isDefaultForDataverse;
    }

    public void setIsDefaultForDataverse(boolean isDefaultForDataverse) {
        this.isDefaultForDataverse = isDefaultForDataverse;
    }
    
    @Transient
    private List<Dataverse> dataversesHasAsDefault;

    public List<Dataverse> getDataversesHasAsDefault() {
        return dataversesHasAsDefault;
    }

    public void setDataversesHasAsDefault(List<Dataverse> dataversesHasAsDefault) {
        this.dataversesHasAsDefault = dataversesHasAsDefault;
    }
    

    public TreeMap<MetadataBlock, List<DatasetField>> getMetadataBlocksForView() {
        return metadataBlocksForView;
    }

    public void setMetadataBlocksForView(TreeMap<MetadataBlock, List<DatasetField>> metadataBlocksForView) {
        this.metadataBlocksForView = metadataBlocksForView;
    }

    public TreeMap<MetadataBlock, List<DatasetField>> getMetadataBlocksForEdit() {
        return metadataBlocksForEdit;
    }

    public void setMetadataBlocksForEdit(TreeMap<MetadataBlock, List<DatasetField>> metadataBlocksForEdit) {
        this.metadataBlocksForEdit = metadataBlocksForEdit;
    }

    @ManyToOne
    @JoinColumn(nullable=true)
    private Dataverse dataverse;

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    private List<DatasetField> initDatasetFields() {
        //retList - Return List of values
        List<DatasetField> retList = new ArrayList<>();
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

    private void initMetadataBlocksForCreate(List<MetadataBlock> systemMDBlocks) {
        metadataBlocksForEdit.clear();
        for (MetadataBlock mdb : this.getDataverse().getMetadataBlocks()) {
            if (!systemMDBlocks.contains(mdb)) {
                List<DatasetField> datasetFieldsForEdit = new ArrayList<>();
                for (DatasetField dsf : this.getDatasetFields()) {

                    if (dsf.getDatasetFieldType().getMetadataBlock().equals(mdb)) {
                        datasetFieldsForEdit.add(dsf);
                    }
                }

                if (!datasetFieldsForEdit.isEmpty()) {
                    metadataBlocksForEdit.put(mdb, sortDatasetFields(datasetFieldsForEdit));
                }
            }
        }
    }

    public void setMetadataValueBlocks(List<MetadataBlock> systemMDBlocks) {
        //TODO: A lot of clean up on the logic of this method
        metadataBlocksForView.clear();
        metadataBlocksForEdit.clear();
        List<DatasetField> filledInFields = this.getDatasetFields(); 

        Map<String, String> instructionsMap = getInstructionsMap();
        
        List <MetadataBlock> viewMDB = new ArrayList<>();
        List <MetadataBlock> editMDB=this.getDataverse().getMetadataBlocks(false);
            
        //The metadatablocks in this template include any from the Dataverse it is associated with 
        //plus any others where the template has a displayable field (i.e. from before a block was dropped in the dataverse/collection)
        viewMDB.addAll(this.getDataverse().getMetadataBlocks(true));
        for (DatasetField dsf : filledInFields) {
            if (!dsf.isEmptyForDisplay()) {
                MetadataBlock mdbTest = dsf.getDatasetFieldType().getMetadataBlock();
                if (!viewMDB.contains(mdbTest)) {
                    viewMDB.add(mdbTest);
                }
            }
        }

        for (MetadataBlock mdb : viewMDB) {

            List<DatasetField> datasetFieldsForView = new ArrayList<>();
            for (DatasetField dsf : this.getDatasetFields()) {
                if (dsf.getDatasetFieldType().getMetadataBlock().equals(mdb)) {
                    //For viewing, show the field if it has a value or custom instructions
                    if (!dsf.isEmpty() || instructionsMap.containsKey(dsf.getDatasetFieldType().getName())) {
                        datasetFieldsForView.add(dsf);
                    }
                }
            }

            if (!datasetFieldsForView.isEmpty()) {
                metadataBlocksForView.put(mdb, sortDatasetFields(datasetFieldsForView));
            }

        }
        
        for (MetadataBlock mdb : editMDB) {
            if (!systemMDBlocks.contains(mdb)) {
                List<DatasetField> datasetFieldsForEdit = new ArrayList<>();
                this.setDatasetFields(initDatasetFields());
                for (DatasetField dsf : this.getDatasetFields()) {
                    if (dsf.getDatasetFieldType().getMetadataBlock().equals(mdb)) {
                        datasetFieldsForEdit.add(dsf);
                    }
                }
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
        TermsOfUseAndAccess terms = null;
        if(source.getTermsOfUseAndAccess() != null){
            terms = source.getTermsOfUseAndAccess().copyTermsOfUseAndAccess();
        } else {
            terms = new TermsOfUseAndAccess();
           // terms.setLicense(TermsOfUseAndAccess.defaultLicense);
            terms.setFileAccessRequest(true);
        }
        terms.setTemplate(newTemplate);
        newTemplate.setTermsOfUseAndAccess(terms);
        
        newTemplate.getInstructionsMap().putAll(source.getInstructionsMap());
        newTemplate.updateInstructions();
        return newTemplate;
    }

    public List<DatasetField> copyDatasetFields(List<DatasetField> copyFromList) {
        List<DatasetField> retList = new ArrayList<>();

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
        List<DatasetField> retList = new LinkedList<>();
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
    
    //Cache values in map for reading
    public Map<String, String> getInstructionsMap() {
        if(instructionsMap==null)
            if(instructions != null) {
            instructionsMap = JsonUtil.getJsonObject(instructions).entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(),entry -> ((JsonString)entry.getValue()).getString()));
            } else {
                instructionsMap = new HashMap<String,String>();
        }
        return instructionsMap;
    }

    //Get the cutstom instructions defined for a give fieldType
    public String getInstructionsFor(String fieldType) {
        return getInstructionsMap().get(fieldType);
    }

    /*
    //Add/change or remove (null instructionString) instructions for a given fieldType
    public void setInstructionsFor(String fieldType, String instructionString) {
        if(instructionString==null) {
            getInstructionsMap().remove(fieldType);
        } else {
        getInstructionsMap().put(fieldType, instructionString);
        }
        updateInstructions();
    }
    */
    
    //Keep instructions up-to-date on any change
    public void updateInstructions() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        getInstructionsMap().forEach((key, value) -> {
            if (value != null)
                builder.add(key, value);
        });
        instructions = JsonUtil.prettyPrint(builder.build());
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
