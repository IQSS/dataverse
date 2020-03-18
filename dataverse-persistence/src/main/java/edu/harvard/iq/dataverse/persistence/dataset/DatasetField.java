package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.MarkupChecker;
import io.vavr.control.Option;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author skraffmiller
 */
@Entity
@ValidateDatasetFieldType
@Table(indexes = {@Index(columnList = "datasetfieldtype_id"), @Index(columnList = "datasetversion_id"),
        @Index(columnList = "parentdatasetfieldcompoundvalue_id"), @Index(columnList = "template_id")})
public class DatasetField implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String NA_VALUE = "N/A";

    /**
     * Orders dataset fields by their display order.
     */
    public static final Comparator<DatasetField> DisplayOrder = new Comparator<DatasetField>() {
        @Override
        public int compare(DatasetField o1, DatasetField o2) {
            return Integer.compare(o1.getDatasetFieldType().getDisplayOrder(),
                                   o2.getDatasetFieldType().getDisplayOrder());
        }
    };

    public static DatasetField createNewEmptyDatasetField(DatasetFieldType dsfType, Object dsv) {

        DatasetField dsfv = createNewEmptyDatasetField(dsfType);
        //TODO - a better way to handle this?
        if (dsv instanceof DatasetVersion) {
            dsfv.setDatasetVersion((DatasetVersion) dsv);
        } else if (dsv instanceof Template) {
            dsfv.setTemplate((Template) dsv);
        }

        return dsfv;
    }

    // originally this was an overloaded method, but we renamed it to get around an issue with Bean Validation
    // (that looked t overloaded methods, when it meant to look at overriden methods
    public static DatasetField createNewEmptyChildDatasetField(DatasetFieldType dsfType, DatasetField compoundValue) {
        DatasetField dsfv = createNewEmptyDatasetField(dsfType);
        dsfv.setDatasetFieldParent(compoundValue);
        return dsfv;
    }

    private static DatasetField createNewEmptyDatasetField(DatasetFieldType dsfType) {
        DatasetField dsf = new DatasetField();
        dsf.setDatasetFieldType(dsfType);

        if (dsfType.isCompound() || dsfType.isControlledVocabulary()) {
            for (DatasetFieldType childDatasetFieldType : dsfType.getChildDatasetFieldTypes()) {
                dsf.getDatasetFieldsChildren().add(createNewEmptyDatasetFieldCompoundValue(dsf, childDatasetFieldType));
            }
        }

        return dsf;

    }

    public static DatasetField createNewEmptyDatasetFieldCompoundValue(DatasetField dsf, DatasetFieldType datasetFieldType) {
        DatasetField compoundValue = new DatasetField();
        compoundValue.setDatasetFieldType(datasetFieldType);
        compoundValue.setDatasetFieldParent(dsf);

        return compoundValue;
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

    public DatasetField setDatasetFieldType(DatasetFieldType datasetField) {
        this.datasetFieldType = datasetField;
        return this;
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

    @OneToMany(mappedBy = "datasetFieldParent", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder ASC")
    private List<DatasetField> datasetFieldsChildren = new ArrayList<>();

    public List<DatasetField> getDatasetFieldsChildren() {
        return datasetFieldsChildren;
    }

    public void setDatasetFieldsChildren(List<DatasetField> compundDatasetFields) {
        this.datasetFieldsChildren = compundDatasetFields;
    }

    @ManyToOne
    @JoinColumn
    private DatasetField datasetFieldParent;

    public Option<DatasetField> getDatasetFieldParent() {
        return Option.of(datasetFieldParent);
    }

    public DatasetField setDatasetFieldParent(DatasetField datasetFieldParent) {
        this.datasetFieldParent = datasetFieldParent;
        return this;
    }

    @Column(columnDefinition = "TEXT")
    private String fieldValue;

    public Option<String> getFieldValue() {
        return Option.of(fieldValue);
    }

    public DatasetField setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
        return this;
    }

    private int displayOrder;

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    @ManyToMany(cascade = {CascadeType.MERGE})
    @JoinTable(indexes = {@Index(columnList = "datasetfield_id"), @Index(columnList = "controlledvocabularyvalues_id")})
    private List<ControlledVocabularyValue> controlledVocabularyValues = new ArrayList<>();

    public List<ControlledVocabularyValue> getControlledVocabularyValues() {
        return controlledVocabularyValues;
    }

    public void setControlledVocabularyValues(List<ControlledVocabularyValue> controlledVocabularyValues) {
        this.controlledVocabularyValues = controlledVocabularyValues;
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
        if (!getFieldValue().isEmpty()) {
            return fieldValue;
        } else if (controlledVocabularyValues != null && !controlledVocabularyValues.isEmpty()) {
            if (controlledVocabularyValues.get(0) != null) {
                return controlledVocabularyValues.get(0).getStrValue();
            }
        }
        return null;
    }

    public void setValue(String value) {
        this.fieldValue = value;
    }

    public String getDisplayValue() {
        String returnString = "";
        for (String value : getValues()) {
            if (value == null) {
                value = "";
            }
            returnString += (returnString.isEmpty() ? "" : "; ") + value.trim();
        }
        return returnString;
    }

    public String getRawValue() {
        String returnString = "";
        for (String value : getRawValuesList()) {
            if (value == null) {
                value = "";
            }
            returnString += (returnString.isEmpty() ? "" : "; ") + value.trim();
        }
        return returnString;
    }

    public String getCompoundDisplayValue() {

        return datasetFieldsChildren.stream()
                .filter(datasetField -> datasetField.getFieldValue().isDefined())
                .map(datasetField -> datasetField.fieldValue)
                .map(String::trim)
                .collect(Collectors.joining("; "));
    }

    public String getCompoundRawValue() {
        String returnString = "";
        for (DatasetField dsf : getDatasetFieldsChildren()) {
            for (String value : dsf.getRawValuesList()) {
                if (!(value == null)) {
                    returnString += (returnString.isEmpty() ? "" : "; ") + value.trim();
                }
            }
        }
        return returnString;
    }

    /**
     * despite the name, this returns a list of display values; not a list of values
     */
    public List<String> getValues() {
        List<String> returnList = new ArrayList<>();
        if (!getFieldValue().isEmpty()) {
            returnList.add(getFieldDisplayValue(fieldValue, datasetFieldType));
        } else {
            for (ControlledVocabularyValue cvv : controlledVocabularyValues) {
                if (cvv != null && cvv.getLocaleStrValue() != null) {
                    returnList.add(getFieldDisplayValue(cvv.getLocaleStrValue(), datasetFieldType));
                }
            }
        }
        return returnList;
    }

    public List<String> getRawValuesList() {
        List<String> returnList = new ArrayList<>();
        if (getFieldValue().isDefined()) {
            returnList.add(getUnsanitizedDisplayValue());
        }

        if (!getDatasetFieldsChildren().isEmpty()) {
            for (DatasetField dsf : getDatasetFieldsChildren()) {
                returnList.add(dsf.getUnsanitizedDisplayValue());
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
    public List<String> getValues_nondisplay() {
        List returnList = new ArrayList();

        if (getFieldValue().isDefined()) {
            returnList.add(fieldValue);
        }

        if (!getDatasetFieldsChildren().isEmpty()) {
            for (DatasetField dsf : getDatasetFieldsChildren()) {
                String value = dsf.getValue();
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

    private String getFieldDisplayValue(String value, DatasetFieldType fieldType) {
        String retVal = "";
        if (!StringUtils.isBlank(value) && !DatasetField.NA_VALUE.equals(value)) {
            String format = fieldType.getDisplayFormat();
            if (StringUtils.isBlank(format)) {
                format = "#VALUE";
            }
            String sanitizedValue = !fieldType.isSanitizeHtml() ?
                    value :
                    MarkupChecker.sanitizeBasicHTML(value);

            if (!fieldType.isSanitizeHtml() && fieldType.isEscapeOutputText()) {
                sanitizedValue = MarkupChecker.stripAllTags(sanitizedValue);
            }

            // replace the special values in the format (note: we replace #VALUE last since we don't
            // want any issues if the value itself has #NAME in it)
            String displayValue = format
                    .replace("#NAME", fieldType.getLocaleTitle() == null ? "" : fieldType.getLocaleTitle())
                    .replace("#EMAIL", BundleUtil.getStringFromBundle("dataset.email.hiddenMessage"))
                    .replace("#VALUE", sanitizedValue);
            retVal = displayValue;
        }

        return retVal;
    }

    public String getUnsanitizedDisplayValue() {
        String retVal = "";
        if (!StringUtils.isBlank(this.getValue()) && !DatasetField.NA_VALUE.equals(this.getValue())) {
            String format = getDatasetFieldType().getDisplayFormat();
            if (StringUtils.isBlank(format)) {
                format = "#VALUE";
            }
            String value = this.getValue();
            String displayValue = format
                    .replace("#NAME", getDatasetFieldType().getTitle() == null ? "" : getDatasetFieldType().getTitle())
                    .replace("#EMAIL", BundleUtil.getStringFromBundle("dataset.email.hiddenMessage"))
                    .replace("#VALUE", value);
            retVal = displayValue;
        }
        return retVal;
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
            for (DatasetField subField : getDatasetFieldsChildren()) {
                if (!subField.isEmpty(forDisplay)) {
                    return false;
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
        return "DatasetField[ id=" + id + " ]";
    }

    public DatasetField copy() {
        return DatasetFieldUtil.copyDatasetField(this);
    }

    public void trimTrailingSpaces() {
        datasetFieldsChildren.stream()
            .forEach(child -> child.trimTrailingSpaces());
        fieldValue = StringUtils.trim(fieldValue);
    }


    /**
     * If this is a FieldType.TEXT or FieldType.TEXTBOX, then run it through the markup checker
     *
     * @return
     */
    public boolean needsTextCleaning() {


        if (this.getDatasetFieldType() == null || this.getDatasetFieldType().getFieldType() == null) {
            return false;
        }

        if (this.datasetFieldType.getFieldType().equals(FieldType.TEXT)) {
            return true;
        } else {
            return this.datasetFieldType.getFieldType().equals(FieldType.TEXTBOX);
        }

    } // end: needsTextCleaning


}
