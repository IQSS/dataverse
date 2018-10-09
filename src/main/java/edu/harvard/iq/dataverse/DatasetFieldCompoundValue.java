/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.MarkupChecker;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author gdurand
 */
@Entity
@Table(indexes = {@Index(columnList="parentdatasetfield_id")})
public class DatasetFieldCompoundValue implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Comparator<DatasetFieldCompoundValue> DisplayOrder = new Comparator<DatasetFieldCompoundValue>() {
        @Override
        public int compare(DatasetFieldCompoundValue o1, DatasetFieldCompoundValue o2) {
            return Integer.compare(o1.getDisplayOrder(), o2.getDisplayOrder());
        }
    };

    public static DatasetFieldCompoundValue createNewEmptyDatasetFieldCompoundValue(DatasetField dsf) {
        DatasetFieldCompoundValue compoundValue = new DatasetFieldCompoundValue();
        compoundValue.setParentDatasetField(dsf);

        for (DatasetFieldType dsfType : dsf.getDatasetFieldType().getChildDatasetFieldTypes()) {
            compoundValue.getChildDatasetFields().add(DatasetField.createNewEmptyChildDatasetField(dsfType, compoundValue));
        }

        return compoundValue;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int displayOrder;

    @ManyToOne(cascade = CascadeType.MERGE)
    private DatasetField parentDatasetField;

    @OneToMany(mappedBy = "parentDatasetFieldCompoundValue", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("datasetFieldType ASC")
    private List<DatasetField> childDatasetFields = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public DatasetField getParentDatasetField() {
        return parentDatasetField;
    }

    public void setParentDatasetField(DatasetField parentDatasetField) {
        this.parentDatasetField = parentDatasetField;
    }

    public List<DatasetField> getChildDatasetFields() {
        return childDatasetFields;
    }

    public void setChildDatasetFields(List<DatasetField> childDatasetFields) {
        this.childDatasetFields = childDatasetFields;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DatasetFieldCompoundValue)) {
            return false;
        }
        DatasetFieldCompoundValue other = (DatasetFieldCompoundValue) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.DatasetFieldCompoundValue[ id=" + id + " ]";
    }

    public DatasetFieldCompoundValue copy(DatasetField parent) {
        DatasetFieldCompoundValue compoundValue = new DatasetFieldCompoundValue();
        compoundValue.setParentDatasetField(parent);
        compoundValue.setDisplayOrder(displayOrder);

        for (DatasetField subField : childDatasetFields) {
            compoundValue.getChildDatasetFields().add(subField.copyChild(compoundValue));
        }

        return compoundValue;
    }

    public Map<DatasetField,String> getDisplayValueMap() {
        // todo - this currently only supports child datasetfields with single values
        // need to determine how we would want to handle multiple
        Map<DatasetField, String> fieldMap = new LinkedHashMap<>();

        for (DatasetField childDatasetField : childDatasetFields) {
            // skip the value if it is empty or N/A
            if (!StringUtils.isBlank(childDatasetField.getValue()) && !DatasetField.NA_VALUE.equals(childDatasetField.getValue())) {
                String format = childDatasetField.getDatasetFieldType().getDisplayFormat();
                if (StringUtils.isBlank(format)) {
                    format = "#VALUE";
                }
                String sanitizedValue = childDatasetField.getDatasetFieldType().isSanitizeHtml() ? MarkupChecker.sanitizeBasicHTML(childDatasetField.getValue()) :  childDatasetField.getValue();
                if (!childDatasetField.getDatasetFieldType().isSanitizeHtml() && childDatasetField.getDatasetFieldType().isEscapeOutputText()){
                    sanitizedValue = MarkupChecker.stripAllTags(sanitizedValue);
                }
                // replace the special values in the format (note: we replace #VALUE last since we don't
                // want any issues if the value itself has #NAME in it)
                String displayValue = format
                        .replace("#NAME", childDatasetField.getDatasetFieldType().getTitle())
                        //todo: this should be handled in more generic way for any other text that can then be internationalized
                        // if we need to use replaceAll for regexp, then make sure to use: java.util.regex.Matcher.quoteReplacement(<target string>)
                        .replace("#EMAIL", BundleUtil.getStringFromBundle("dataset.email.hiddenMessage"))
                        .replace("#VALUE",  sanitizedValue );

                fieldMap.put(childDatasetField,displayValue);
            }
        }

        return fieldMap;
    }
}
