package edu.harvard.iq.dataverse.dataset.difference;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.util.List;

/**
 * Class that contains old and new value of {@link DatasetField}
 * that is different between two {@link DatasetVersion}s
 *
 * @author madryk
 */
public class DatasetFieldDiff extends MultipleItemDiff<DatasetField> {

    private DatasetFieldType fieldType;

    // -------------------- CONSTRUCTORS --------------------

    public DatasetFieldDiff(List<DatasetField> oldValue, List<DatasetField> newValue, DatasetFieldType fieldType) {
        super(oldValue, newValue);
        this.fieldType = fieldType;
    }

    public DatasetFieldDiff(DatasetField oldValue, DatasetField newValue, DatasetFieldType fieldType) {
        super(Lists.newArrayList(oldValue), Lists.newArrayList(newValue));
        this.fieldType = fieldType;
    }

// -------------------- GETTERS --------------------

    public DatasetFieldType getFieldType() {
        return fieldType;
    }

    // -------------------- LOGIC --------------------

    /**
     * Generates DatasetFields joined value pair's so you can compare old and new values.
     */
    public Tuple2<String, String> generatePairOfJoinedValues() {
        return Tuple.of(DatasetFieldUtil.joinAllValues(getOldValue()), DatasetFieldUtil.joinAllValues(getNewValue()));
    }
}