package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.buttonaction;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyZeroInteractions;

@ExtendWith(MockitoExtension.class)
public class AddReplicationTextActionHandlerTest {

    private AddReplicationTextActionHandler addReplicationTextActionHandler = new AddReplicationTextActionHandler();
    
    @Mock
    private List<DatasetFieldsByType> allBlockFields;
    
    // -------------------- TESTS --------------------
    
    @Test
    public void handleAction() {
        // given
        DatasetField datasetField = new DatasetField();
        datasetField.setFieldValue("before action");
        
        // when
        addReplicationTextActionHandler.handleAction(datasetField, allBlockFields);
        
        // then
        assertEquals("Replication Data for: before action", datasetField.getFieldValue().get());
        verifyZeroInteractions(allBlockFields);
    }
}
