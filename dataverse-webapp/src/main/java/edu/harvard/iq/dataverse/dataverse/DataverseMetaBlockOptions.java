package edu.harvard.iq.dataverse.dataverse;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DataverseMetaBlockOptions implements Serializable {

    private Map<Long, MetadataBlockViewOptions> mdbViewOptions = new HashMap<>();
    private Map<Long, DatasetFieldViewOptions> datasetFieldViewOptions = new HashMap<>();
    private boolean inheritMetaBlocksFromParent = true;

    // -------------------- GETTERS --------------------

    /**
     * Retrives metadata block view options.
     */
    public Map<Long, MetadataBlockViewOptions> getMdbViewOptions() {
        return mdbViewOptions;
    }

    /**
     * Retrives dataset fields view options.
     */
    public Map<Long, DatasetFieldViewOptions> getDatasetFieldViewOptions() {
        return datasetFieldViewOptions;
    }

    /**
     * Indicates if metadata blocks are inherited from parent dataverse or if they are being edited by user.
     */
    public boolean isInheritMetaBlocksFromParent() {
        return inheritMetaBlocksFromParent;
    }
    // -------------------- LOGIC --------------------

    /**
     * Indicates if dataset field should be shown.
     *
     * @return true/false or false if null.
     */
    public boolean isShowDatasetFieldTypes(Long mdbId) {

        return mdbViewOptions.containsKey(mdbId) && mdbViewOptions.get(mdbId).isShowDatasetFieldTypes();
    }

    /**
     * Indicates if dataset field should be editable.
     *
     * @return true/false or false if null.
     */
    public boolean isEditableDatasetFieldTypes(Long mdbId) {
        return mdbViewOptions.containsKey(mdbId) && mdbViewOptions.get(mdbId).isEditableDatasetFieldTypes();
    }

    /**
     * Indicates if dataset field is included (required/optional).
     *
     * @return true/false or false if null.
     */
    public boolean isDsftIncludedField(Long dsftId) {
        return datasetFieldViewOptions.containsKey(dsftId) && datasetFieldViewOptions.get(dsftId).isIncluded();
    }

    /**
     * Indicates if metadata block is selected.
     *
     * @return true/false or false if null.
     */
    public boolean isMetaBlockSelected(Long mdbId) {
        return mdbViewOptions.containsKey(mdbId) && mdbViewOptions.get(mdbId).isSelected();
    }

    /**
     * Makes a deep copy using serialization/deserialization.
     * If top performance is really required, it would be advisable to implement different deep copy mechanism.
     */
    public DataverseMetaBlockOptions deepCopy() {
        return SerializationUtils.clone(this);
    }

    // -------------------- SETTERS --------------------

    public void setInheritMetaBlocksFromParent(boolean inheritMetaBlocksFromParent) {
        this.inheritMetaBlocksFromParent = inheritMetaBlocksFromParent;
    }
}
