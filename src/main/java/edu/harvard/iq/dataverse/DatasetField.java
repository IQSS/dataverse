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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import org.apache.commons.lang.StringUtils;

@Entity
public class DatasetField implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Comparator<DatasetField> orderByTypeName = new Comparator<DatasetField>() {
        @Override
        public int compare(DatasetField o1, DatasetField o2) {
            return o1.getDatasetFieldType().getDisplayOrder() - o2.getDatasetFieldType().getDisplayOrder();
        }
    };

    public static DatasetField createNewEmptyDatasetField(DatasetFieldType dsfType, DatasetVersion dsv) {
        DatasetField dsfv = createNewEmptyDatasetField(dsfType);
        dsfv.setDatasetVersion(dsv);
        return dsfv;
    }

    public static DatasetField createNewEmptyDatasetField(DatasetFieldType dsfType, DatasetFieldCompoundValue compoundValue) {
        DatasetField dsfv = createNewEmptyDatasetField(dsfType);
        dsfv.setParentDatasetFieldCompoundValue(compoundValue);
        return dsfv;
    }

    public static DatasetField createNewEmptyDatasetField(DatasetFieldType dsfType) {
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
    private List<DatasetFieldCompoundValue> datasetFieldCompoundValues = new ArrayList();

    public List<DatasetFieldCompoundValue> getDatasetFieldCompoundValues() {
        return datasetFieldCompoundValues;
    }

    public void setDatasetFieldCompoundValues(List<DatasetFieldCompoundValue> datasetFieldCompoundValues) {
        this.datasetFieldCompoundValues = datasetFieldCompoundValues;
    }

    @OneToMany(mappedBy = "datasetField", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder ASC")
    private List<DatasetFieldValue> datasetFieldValues = new ArrayList();

    public List<DatasetFieldValue> getDatasetFieldValues() {
        return this.datasetFieldValues;
    }

    public void setDatasetFieldValues(List<DatasetFieldValue> datasetFieldValues) {
        this.datasetFieldValues = datasetFieldValues;
    }

    @OneToMany(cascade = {CascadeType.MERGE})
    private List<ControlledVocabularyValue> controlledVocabularyValues = new ArrayList();

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
        } else if (!controlledVocabularyValues.isEmpty()) {
            return controlledVocabularyValues.get(0).getStrValue();
        }
        return null;
    }

    public String getDisplayValue() {
        String returnString = "";
        for (String value : getValues()) {
            returnString += (returnString.equals("") ? "" : "; ") + value;
        }
        return returnString;
    }

    public List<String> getValues() {
        List returnList = new ArrayList();
        if (!datasetFieldValues.isEmpty()) {
            for (DatasetFieldValue dsfv : datasetFieldValues) {
                returnList.add(dsfv.getValue());
            }
        } else {
            for (ControlledVocabularyValue cvv : controlledVocabularyValues) {
                returnList.add(cvv.getStrValue());
            }
        }
        return returnList;
    }

    public boolean isEmpty() {
        if (datasetFieldType.isPrimitive()) { // primitive
            for (String value : getValues()) {
                if (value != null && value.trim() != "") {
                    return false;
                }
            }
        } else { // compound
            for (DatasetFieldCompoundValue cv : datasetFieldCompoundValues) {
                for (DatasetField subField : cv.getChildDatasetFields()) {
                    if (!subField.isEmpty()) {
                        return false;
                    }
                }
            }
        }

        return true;
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
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DatasetField[ id=" + id + " ]";
    }

    public DatasetField copy(DatasetVersion version) {
        return copy(version, null);
    }

    public DatasetField copy(DatasetFieldCompoundValue parent) {
        return copy(null, parent);
    }

    private DatasetField copy(DatasetVersion version, DatasetFieldCompoundValue parent) {
        DatasetField dsf = new DatasetField();
        dsf.setDatasetFieldType(datasetFieldType);

        dsf.setDatasetVersion(version);
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
        if (this.getDatasetFieldType().isPrimitive() && !this.getDatasetFieldType().isControlledVocabulary()) {
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
        int displayOrder = 0;
        if (this.getDatasetFieldType().isPrimitive() && !this.getDatasetFieldType().isControlledVocabulary()) {
            Iterator<DatasetFieldValue> dsfvIt = this.getDatasetFieldValues().iterator();
            displayOrder = 0;
            while (dsfvIt.hasNext()) {
                DatasetFieldValue dsfv = dsfvIt.next();
                dsfv.setDisplayOrder(displayOrder);
                displayOrder++;
            }
        } else if (this.getDatasetFieldType().isCompound()) {
            Iterator<DatasetFieldCompoundValue> cvIt = this.getDatasetFieldCompoundValues().iterator();
            displayOrder = 0;
            while (cvIt.hasNext()) {
                DatasetFieldCompoundValue cv = cvIt.next();
                cv.setDisplayOrder(displayOrder);
                displayOrder++;
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

}
