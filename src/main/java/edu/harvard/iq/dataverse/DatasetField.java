/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

/**
 *
 * @author skraffmiller
 */
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.apache.commons.lang3.StringUtils;

@Entity
@ValidateDatasetFieldType
@Table(indexes = {@Index(columnList="datasetfieldtype_id"),@Index(columnList="datasetversion_id"),
    @Index(columnList="parentdatasetfieldcompoundvalue_id"),@Index(columnList="template_id")})
public class DatasetField implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public static final String NA_VALUE = "N/A";

    /**
     * Orders dataset fields by their display order.
     */
    public static final Comparator<DatasetField> DisplayOrder = new Comparator<DatasetField>() {
        @Override
        public int compare(DatasetField o1, DatasetField o2) {
            return Integer.compare( o1.getDatasetFieldType().getDisplayOrder(),
                                    o2.getDatasetFieldType().getDisplayOrder() );
    }};

    public static DatasetField createNewEmptyDatasetField(DatasetFieldType dsfType, DatasetVersion dsv) {
        
        DatasetField dsfv = createNewEmptyDatasetField(dsfType);
        dsfv.setDatasetVersion(dsv);

        return dsfv;
    }

    public static DatasetField createNewEmptyDatasetField(DatasetFieldType dsfType, Template dsv) {
        
        DatasetField dsfv = createNewEmptyDatasetField(dsfType);
        dsfv.setTemplate(dsv);

        return dsfv;
    }

    // originally this was an overloaded method, but we renamed it to get around an issue with Bean Validation
    // (that looked t overloaded methods, when it meant to look at overriden methods
    public static DatasetField createNewEmptyChildDatasetField(DatasetFieldType dsfType, DatasetFieldCompoundValue compoundValue) {
        DatasetField dsfv = createNewEmptyDatasetField(dsfType);
        dsfv.setParentDatasetFieldCompoundValue(compoundValue);
        return dsfv;
    }

    private static DatasetField createNewEmptyDatasetField(DatasetFieldType dsfType) {
        DatasetField dsfv = new DatasetField();
        dsfv.setDatasetFieldType(dsfType);

        if (dsfType.isPrimitive()) {
            if (!dsfType.isControlledVocabulary()) {
                dsfv.getDatasetFieldValues().add(new DatasetFieldValue(dsfv));
            }
        } else { // compound field
            dsfv.getDatasetFieldCompoundValues().add(DatasetFieldCompoundValue.createNewEmptyDatasetFieldCompoundValue(dsfv));
        }

        return dsfv;

    }

    /**
     * Groups a list of fields by the block they belong to.
     *
     * @param fields well, duh.
     * @return a map, mapping each block to the fields that belong to it.
     */
    public static Map<MetadataBlock, List<DatasetField>> groupByBlock(List<DatasetField> fields) {
        Map<MetadataBlock, List<DatasetField>> retVal = new HashMap<>();
        for (DatasetField f : fields) {
            MetadataBlock metadataBlock = f.getDatasetFieldType().getMetadataBlock();
            List<DatasetField> lst = retVal.get(metadataBlock);
            if (lst == null) {
                retVal.put(metadataBlock, new LinkedList<>(Collections.singleton(f)));
            } else {
                lst.add(f);
            }
        }
        return retVal;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne
    @JoinColumn(nullable = false)
    private DatasetFieldType datasetFieldType;

    public DatasetFieldType getDatasetFieldType() {
        return datasetFieldType;
    }

    public void setDatasetFieldType(DatasetFieldType datasetField) {
        this.datasetFieldType = datasetField;
    }

    @ManyToOne
    private DatasetVersion datasetVersion;

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }
    
    @ManyToOne
    private Template template;

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    @ManyToOne(cascade = CascadeType.MERGE)
    private DatasetFieldCompoundValue parentDatasetFieldCompoundValue;

    public DatasetFieldCompoundValue getParentDatasetFieldCompoundValue() {
        return parentDatasetFieldCompoundValue;
    }

    public void setParentDatasetFieldCompoundValue(DatasetFieldCompoundValue parentDatasetFieldCompoundValue) {
        this.parentDatasetFieldCompoundValue = parentDatasetFieldCompoundValue;
    }

    @OneToMany(mappedBy = "parentDatasetField", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder ASC")
    private List<DatasetFieldCompoundValue> datasetFieldCompoundValues = new ArrayList<>();

    public List<DatasetFieldCompoundValue> getDatasetFieldCompoundValues() {
        return datasetFieldCompoundValues;
    }

    public void setDatasetFieldCompoundValues(List<DatasetFieldCompoundValue> datasetFieldCompoundValues) {
        this.datasetFieldCompoundValues = datasetFieldCompoundValues;
    }

    @OneToMany(mappedBy = "datasetField", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder ASC")
    private List<DatasetFieldValue> datasetFieldValues = new ArrayList<>();

    public List<DatasetFieldValue> getDatasetFieldValues() {
        return this.datasetFieldValues;
    }

    public void setDatasetFieldValues(List<DatasetFieldValue> datasetFieldValues) {
        this.datasetFieldValues = datasetFieldValues;
    }

    @ManyToMany(cascade = {CascadeType.MERGE})
    @JoinTable(indexes = {@Index(columnList="datasetfield_id"),@Index(columnList="controlledvocabularyvalues_id")})
    private List<ControlledVocabularyValue> controlledVocabularyValues = new ArrayList<>();

    public List<ControlledVocabularyValue> getControlledVocabularyValues() {
        return controlledVocabularyValues;
    }

    public void setControlledVocabularyValues(List<ControlledVocabularyValue> controlledVocabularyValues) {
        this.controlledVocabularyValues = controlledVocabularyValues;
    }

    // HELPER METHODS
    public DatasetFieldValue getSingleValue() {
        if (!datasetFieldValues.isEmpty()) {
            return datasetFieldValues.get(0);
        } else {
            return new DatasetFieldValue(this);
        }
    }
    
    public void setSingleValue(String value) {
        if (datasetFieldValues.isEmpty()) {
            datasetFieldValues.add(new DatasetFieldValue(this));
        }
        datasetFieldValues.get(0).setValue(value);
    }

    public ControlledVocabularyValue getSingleControlledVocabularyValue() {
        if (!controlledVocabularyValues.isEmpty()) {
            return controlledVocabularyValues.get(0);
        } else {
            return null;
        }
    }

    public void setSingleControlledVocabularyValue(ControlledVocabularyValue cvv) {
        if (!controlledVocabularyValues.isEmpty()) {
            controlledVocabularyValues.set(0, cvv);
        } else {
            controlledVocabularyValues.add(cvv);
        }
    }
    
    public String getValue() {
        if (!datasetFieldValues.isEmpty()) {
            return datasetFieldValues.get(0).getValue();
        } else if (controlledVocabularyValues != null && !controlledVocabularyValues.isEmpty()) {
            if (controlledVocabularyValues.get(0) != null){
                return controlledVocabularyValues.get(0).getLocaleStrValue();                
            }
        }
        return null;
    }

    public String getDisplayValue() {
        return getDisplayValue(null);
    }
    
    public String getDisplayValue(String lang) {
        if(lang!=null && lang.isBlank()) {
            //null will cause the current UI lang to be picked up
            lang=null;
        }
        String returnString = "";
        for (String value : getValues(lang)) {
            if(value == null) {
                value="";
            }
            returnString += (returnString.isEmpty() ? "" : "; ") + value.trim();
        }
        return returnString;
    }
    
    public String getRawValue() {
        String returnString = "";
        for (String value : getRawValuesList()) {
            if(value == null) {
                value="";
            }
            returnString += (returnString.isEmpty() ? "" : "; ") + value.trim();
        }
        return returnString;
    }

    public String getCompoundDisplayValue() {
        String returnString = "";
        for (DatasetFieldCompoundValue dscv : datasetFieldCompoundValues) {
            for (DatasetField dsf : dscv.getChildDatasetFields()) {
                for (String value : dsf.getValues()) {
                    if (!(value == null)) {
                        returnString += (returnString.isEmpty() ? "" : "; ") + value.trim();
                    }
                }
            }
        }
        return returnString;
    }
    
    public String getCompoundRawValue() {
        String returnString = "";
        for (DatasetFieldCompoundValue dscv : datasetFieldCompoundValues) {
            for (DatasetField dsf : dscv.getChildDatasetFields()) {
                for (String value : dsf.getRawValuesList()) {
                    if (!(value == null)) {
                        returnString += (returnString.isEmpty() ? "" : "; ") + value.trim();
                    }
                }
            }
        }
        return returnString;
    }

    /**
     * despite the name, this returns a list of display values; not a list of values
     */
    public List<String> getValues() {
        return getValues(null);
    }

    public List<String> getValues(String langCode) {
        if(langCode!=null && langCode.isBlank()) {
            //null picks up current UI lang
            langCode=null;
        }
        List<String> returnList = new ArrayList<>();
        if (!datasetFieldValues.isEmpty()) {
            for (DatasetFieldValue dsfv : datasetFieldValues) {
                returnList.add(dsfv.getDisplayValue());
            }
        } else {
            for (ControlledVocabularyValue cvv : controlledVocabularyValues) {
                if (cvv != null && cvv.getLocaleStrValue(langCode) != null) {
                    returnList.add(cvv.getLocaleStrValue(langCode));
                }
            }
        }
        return returnList;
    }

    public List<String> getRawValuesList() {
        List<String> returnList = new ArrayList<>();
        if (!datasetFieldValues.isEmpty()) {
            for (DatasetFieldValue dsfv : datasetFieldValues) {
                returnList.add(dsfv.getUnsanitizedDisplayValue());
            }
        } else {
            for (ControlledVocabularyValue cvv : controlledVocabularyValues) {
                if (cvv != null && cvv.getStrValue() != null) {
                    returnList.add(cvv.getStrValue());
                }
            }
        }
        return returnList;
    }
       
    /**
     * list of values (as opposed to display values).
     * used for passing to solr for indexing
     */
    public List<String> getValues_nondisplay()
    {
        List returnList = new ArrayList();
        if (!datasetFieldValues.isEmpty()) {
            for (DatasetFieldValue dsfv : datasetFieldValues) {
                String value = dsfv.getValue();
                if (value != null) {
                    returnList.add(value);
                }
            }
        } else {
            for (ControlledVocabularyValue cvv : controlledVocabularyValues) {
                if (cvv != null && cvv.getStrValue() != null) {
                    returnList.add(cvv.getStrValue());
                }
            }
        }
        return returnList;
    }

    /**
     * appears to be only used for sending info to solr; changed to return values
     * instead of display values
     */
    public List<String> getValuesWithoutNaValues() {
        List<String> returnList = getValues_nondisplay();
        returnList.removeAll(Arrays.asList(NA_VALUE));
        return returnList;
    }
    
    public boolean isEmpty() {
        return isEmpty(false);
    }
    
    public boolean isEmptyForDisplay() {
        return isEmpty(true);
    }


    private boolean isEmpty(boolean forDisplay) {
        if (datasetFieldType.isPrimitive()) { // primitive
        	List<String> values = forDisplay ? getValues() : getValues_nondisplay();
            for (String value : values) {
                if (!StringUtils.isBlank(value) && !(forDisplay && DatasetField.NA_VALUE.equals(value))) {
                    return false;
                }
            }
        } else { // compound
            for (DatasetFieldCompoundValue cv : datasetFieldCompoundValues) {
                for (DatasetField subField : cv.getChildDatasetFields()) {
                    if (!subField.isEmpty(forDisplay)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Transient
    private String validationMessage;

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }
    
    
    // these two booleans are used to wrap around the field type isRequired and isHasChildren methods to also check
    // if the dataverse has overriden the default values
    @Transient 
    private Boolean required;
    @Transient 
    private Boolean hasRequiredChildren;

    public boolean isRequired() {
        if (required == null) {
            required = false;            
            if (this.datasetFieldType.isRequired()) {
                required = true;
            } else {
                // otherwise check if overridden at the dataverse level
                Dataverse dv = getDataverse();
                while (!dv.isMetadataBlockRoot()) {
                    if (dv.getOwner() == null) {
                        break; // we are at the root; which by defintion is metadata blcok root, regarldess of the value
                    }
                    dv = dv.getOwner();
                }
    
                List<DataverseFieldTypeInputLevel> dftilListFirst = dv.getDataverseFieldTypeInputLevels();
                for (DataverseFieldTypeInputLevel dsftil : dftilListFirst) {
                    if (dsftil.getDatasetFieldType().equals(this.datasetFieldType)) {
                        required = dsftil.isRequired();
                    }
                }
            }
            
            // since we don't currently support the use case where the parent is required
            // but no specific children are (the "true/false" case), we override this as false
            if (required && this.datasetFieldType.isCompound() && !isHasRequiredChildren())
            {
                required = false;
            }
            
            //this is needed to enforce required children validation and display on create
            
            if (this.datasetFieldType.isCompound()  && isHasRequiredChildren()){
                required = true;
            }
            
        }
        
        return required;
    }

    public boolean isHasRequiredChildren() {
        if (hasRequiredChildren == null) {
            hasRequiredChildren = false;
        }
        
        if (this.datasetFieldType.isHasRequiredChildren()) {
            hasRequiredChildren = true;
        }        
        
        Dataverse dv = getDataverse();
        while (!dv.isMetadataBlockRoot()) {
            if (dv.getOwner() == null) {
                break; // we are at the root; which by defintion is metadata blcok root, regarldess of the value
            }
            dv = dv.getOwner();
        }        
        
        List<DataverseFieldTypeInputLevel> dftilListFirst = dv.getDataverseFieldTypeInputLevels();

        if (getDatasetFieldType().isHasChildren() && (!dftilListFirst.isEmpty())) {
            for (DatasetFieldType child : getDatasetFieldType().getChildDatasetFieldTypes()) {
                for (DataverseFieldTypeInputLevel dftilTest : dftilListFirst) {
                    if (child.equals(dftilTest.getDatasetFieldType())) {
                        if (dftilTest.isRequired()) {
                            hasRequiredChildren = true;
                        }
                    }
                }
            }
        }        
        return hasRequiredChildren;
    }
    
    public Dataverse getDataverse() {
        if (datasetVersion != null) {
            return datasetVersion.getDataset().getOwner();
        } else if (parentDatasetFieldCompoundValue != null) {
            return parentDatasetFieldCompoundValue.getParentDatasetField().getDataverse();
        } else if (template != null) {
            return template.getDataverse();
        } else {
            throw new IllegalStateException("DatasetField is in an illegal state: no dataset version, compound value, or template is set as its parent.");
        }
    }

    
    @Transient 
    private boolean include;
    
    public void setInclude(boolean include){
        this.include = include;
    }
    
    public boolean isInclude(){
        return this.include;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DatasetField)) {
            return false;
        }
        DatasetField other = (DatasetField) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DatasetField[ id=" + id + " ]";
    }

    public DatasetField copy(DatasetVersion version) {
        return copy(version, null);
    }
    public DatasetField copy(Template template) {
        return copy(template, null);
    }
    
    // originally this was an overloaded method, but we renamed it to get around an issue with Bean Validation
    // (that looked t overloaded methods, when it meant to look at overriden methods
    public DatasetField copyChild(DatasetFieldCompoundValue parent) {
        return copy(null, parent);
    }

    private DatasetField copy(Object versionOrTemplate, DatasetFieldCompoundValue parent) {
        DatasetField dsf = new DatasetField();
        dsf.setDatasetFieldType(datasetFieldType);
        
        if (versionOrTemplate != null) {
            if (versionOrTemplate instanceof DatasetVersion) {
                dsf.setDatasetVersion((DatasetVersion) versionOrTemplate);               
            } else {
                dsf.setTemplate((Template) versionOrTemplate);
            }
        }
        
        dsf.setParentDatasetFieldCompoundValue(parent);
        dsf.setControlledVocabularyValues(controlledVocabularyValues);

        for (DatasetFieldValue dsfv : datasetFieldValues) {
            dsf.getDatasetFieldValues().add(dsfv.copy(dsf));
        }

        for (DatasetFieldCompoundValue compoundValue : datasetFieldCompoundValues) {
            dsf.getDatasetFieldCompoundValues().add(compoundValue.copy(dsf));
        }

        return dsf;
    }

    public boolean removeBlankDatasetFieldValues() {
        if (this.getDatasetFieldType().isPrimitive()) {
            if (!this.getDatasetFieldType().isControlledVocabulary()) {
                Iterator<DatasetFieldValue> dsfvIt = this.getDatasetFieldValues().iterator();
                while (dsfvIt.hasNext()) {
                    DatasetFieldValue dsfv = dsfvIt.next();
                    if (StringUtils.isBlank(dsfv.getValue())) {
                        dsfvIt.remove();
                    }
                }
                if (this.getDatasetFieldValues().isEmpty()) {
                    return true;
                }
            } else { // controlled vocab
                // during harvesting some CVV are put in getDatasetFieldValues. we don't want to remove those
                if (this.getControlledVocabularyValues().isEmpty() && this.getDatasetFieldValues().isEmpty()) {
                    return true;
                }                 
            }
        } else if (this.getDatasetFieldType().isCompound()) {
            Iterator<DatasetFieldCompoundValue> cvIt = this.getDatasetFieldCompoundValues().iterator();
            while (cvIt.hasNext()) {
                DatasetFieldCompoundValue cv = cvIt.next();
                Iterator<DatasetField> dsfIt = cv.getChildDatasetFields().iterator();
                while (dsfIt.hasNext()) {
                    if (dsfIt.next().removeBlankDatasetFieldValues()) {
                        dsfIt.remove();
                    }
                }
                if (cv.getChildDatasetFields().isEmpty()) {
                    cvIt.remove();
                }
            }
            if (this.getDatasetFieldCompoundValues().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void setValueDisplayOrder() {
        if (this.getDatasetFieldType().isPrimitive() && !this.getDatasetFieldType().isControlledVocabulary()) {
            for (int i = 0; i < datasetFieldValues.size(); i++) {
                datasetFieldValues.get(i).setDisplayOrder(i);
            }

        } else if (this.getDatasetFieldType().isCompound()) {
            for (int i = 0; i < datasetFieldCompoundValues.size(); i++) {
                DatasetFieldCompoundValue compoundValue = datasetFieldCompoundValues.get(i);
                compoundValue.setDisplayOrder(i);
                for (DatasetField dsf : compoundValue.getChildDatasetFields()) {
                    dsf.setValueDisplayOrder();
                }
            }
        }
    }
    
    public void trimTrailingSpaces() {
        if (this.getDatasetFieldType().isPrimitive() && !this.getDatasetFieldType().isControlledVocabulary()) {
            for (int i = 0; i < datasetFieldValues.size(); i++) {
                datasetFieldValues.get(i).setValue(datasetFieldValues.get(i).getValue().trim());
            }
        } else if (this.getDatasetFieldType().isCompound()) {
            for (int i = 0; i < datasetFieldCompoundValues.size(); i++) {
                DatasetFieldCompoundValue compoundValue = datasetFieldCompoundValues.get(i);
                for (DatasetField dsf : compoundValue.getChildDatasetFields()) {
                    dsf.trimTrailingSpaces();
                }
            }
        }
    }
     

    public void addDatasetFieldValue(int index) {
        datasetFieldValues.add(index, new DatasetFieldValue(this));
    }

    public void removeDatasetFieldValue(int index) {
        datasetFieldValues.remove(index);
    }

    public void addDatasetFieldCompoundValue(int index) {
        datasetFieldCompoundValues.add(index, DatasetFieldCompoundValue.createNewEmptyDatasetFieldCompoundValue(this));
    }

    public void removeDatasetFieldCompoundValue(int index) {
        datasetFieldCompoundValues.remove(index);
    }

    
    /**
     *  If this is a FieldType.TEXT or FieldType.TEXTBOX, then run it through the markup checker
     * 
     * @return
     */
    public boolean needsTextCleaning(){
  
        
        if (this.getDatasetFieldType() == null || this.getDatasetFieldType().getFieldType() == null){
            return false;
        }
        
        if (this.datasetFieldType.getFieldType().equals(DatasetFieldType.FieldType.TEXT)){
            return true;
        } else if (this.datasetFieldType.getFieldType().equals(DatasetFieldType.FieldType.TEXTBOX)){
            return true;
        }
    
        return false;
        
    } // end: needsTextCleaning

}
