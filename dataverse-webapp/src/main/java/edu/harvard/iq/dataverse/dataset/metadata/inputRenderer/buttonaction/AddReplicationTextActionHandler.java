package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.buttonaction;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Stateless;

import java.util.List;

@Stateless
public class AddReplicationTextActionHandler implements FieldButtonActionHandler {

    public static final String ADD_REPLICATION_TEXT_ACTION_NAME = "AddReplicationTextActionHandler";
    
    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns {@link #ADD_REPLICATION_TEXT_ACTION_NAME}
     */
    @Override
    public String getName() {
        return ADD_REPLICATION_TEXT_ACTION_NAME;
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * This implementation adds translated text: "dataset.replicationDataFor"
     * at the beginning of the dataset field.
     * It does nothing with allBlockFields param.
     */
    @Override
    public void handleAction(DatasetField datasetField, List<DatasetFieldsByType> allBlockFields) {
        
        String newValue = BundleUtil.getStringFromBundle("dataset.replicationDataFor") + " "
                + datasetField.getFieldValue().getOrElse(StringUtils.EMPTY);

        datasetField.setFieldValue(newValue);
    }
}
