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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

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

    // configurations for link creation
    private static final Map<String, Pair<String, String>> linkComponents = Map.of(
      "author", new ImmutablePair<>("authorIdentifierScheme", "authorIdentifier")
    );

    // field for handling links. Annotation '@Transient' prevents these fields to be saved in DB
    @Transient
    private Map<DatasetField, Boolean> linkMap = new LinkedHashMap<>();
    @Transient
    private String linkScheme = null;
    @Transient
    private String linkValue = null;

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

    public Map<DatasetField, String> getDisplayValueMap() {
        // todo - this currently only supports child datasetfields with single values
        // need to determine how we would want to handle multiple
        Map<DatasetField, String> fieldMap = new LinkedHashMap<>();
        linkMap.clear();
        boolean fixTrailingComma = false;
        Pair<String, String> linkComponents = getLinkComponents();
        linkScheme = null;
        linkValue = null;
        for (DatasetField childDatasetField : childDatasetFields) {
            fixTrailingComma = false;
            // skip the value if it is empty or N/A
            if (!StringUtils.isBlank(childDatasetField.getValue()) && !DatasetField.NA_VALUE.equals(childDatasetField.getValue())) {
                if (linkComponents != null) {
                    if (fieldNameEquals(childDatasetField, linkComponents.getKey())) {
                        linkScheme = childDatasetField.getValue();
                    } else if (fieldNameEquals(childDatasetField, linkComponents.getValue())) {
                        linkValue = childDatasetField.getValue();
                        if (StringUtils.isNotBlank(linkScheme) && StringUtils.isNotBlank(linkValue))
                            linkMap.put(childDatasetField, true);
                    }
                }

                String format = childDatasetField.getDatasetFieldType().getDisplayFormat();
                if (StringUtils.isBlank(format)) {
                    format = "#VALUE";
                }
                String sanitizedValue = childDatasetField.getDatasetFieldType().isSanitizeHtml() ? MarkupChecker.sanitizeBasicHTML(childDatasetField.getValue()) :  childDatasetField.getValue();
                if (!childDatasetField.getDatasetFieldType().isSanitizeHtml() && childDatasetField.getDatasetFieldType().isEscapeOutputText()){
                    sanitizedValue = MarkupChecker.stripAllTags(sanitizedValue);
                }
                //if a series of child values is comma delimited we want to strip off the final entry's comma
                if (format.equals("#VALUE, ")) fixTrailingComma = true;
                
                // replace the special values in the format (note: we replace #VALUE last since we don't
                // want any issues if the value itself has #NAME in it)

                String displayValue = format
                        .replace("#NAME", childDatasetField.getDatasetFieldType().getTitle())
                        //todo: this should be handled in more generic way for any other text that can then be internationalized
                        // if we need to use replaceAll for regexp, then make sure to use: java.util.regex.Matcher.quoteReplacement(<target string>)
                        .replace("#EMAIL", BundleUtil.getStringFromBundle("dataset.email.hiddenMessage"))
                        .replace("#VALUE",  sanitizedValue);
                fieldMap.put(childDatasetField, displayValue);
            }
        }
        
        if (fixTrailingComma) {
            return (removeLastComma(fieldMap));
        }

        return fieldMap;
    }

    public String getLink() {
        return DatasetAuthor.getIdentifierAsUrl(linkScheme, linkValue);
    }

    public boolean isLink(DatasetField datasetField) {
        return linkMap.containsKey(datasetField) && linkMap.get(datasetField) == true && getLink() != null;
    }

    private boolean fieldNameEquals(DatasetField datasetField, String linkTypeComponent) {
        return datasetField.getDatasetFieldType().getName().equals(linkTypeComponent);
    }

    public boolean isLinkableField() {
        return linkComponents.containsKey(parentDatasetField.getDatasetFieldType().getName());
    }

    public Pair<String, String> getLinkComponents() {
        if (!isLinkableField())
            return null;
        return linkComponents.get(parentDatasetField.getDatasetFieldType().getName());
    }

    public boolean hasChildOfType(String name) {
        for (DatasetField child : childDatasetFields) {
            if (child.getDatasetFieldType().getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private Map<DatasetField, String> removeLastComma(Map<DatasetField, String> mapIn) {

        Iterator<Map.Entry<DatasetField, String>> itr = mapIn.entrySet().iterator();
        Map.Entry<DatasetField, String> entry = null;
        DatasetField keyVal = null;
        String oldValue = null;

        while (itr.hasNext()) {
            entry = itr.next();
            keyVal = entry.getKey();
            oldValue = entry.getValue();
        }

        if (keyVal != null && oldValue != null && oldValue.length() >= 2) {
            String newValue = oldValue.substring(0, oldValue.length() - 2);
            mapIn.replace(keyVal, oldValue, newValue);
        }

        return mapIn;
    }
}
