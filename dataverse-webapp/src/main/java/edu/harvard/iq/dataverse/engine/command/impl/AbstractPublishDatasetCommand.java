package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.workflow.execution.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.execution.WorkflowContext.TriggerType;

/**
 * Base class for commands involved in Dataset publication. Mostly needed for code reuse.
 *
 * @param <T> Command result type (as usual).
 * @author michael
 */
public abstract class AbstractPublishDatasetCommand<T> extends AbstractDatasetCommand<T> {


    public AbstractPublishDatasetCommand(Dataset datasetIn, DataverseRequest aRequest) {
        super(aRequest, datasetIn);
    }

    protected WorkflowContext buildContext(Dataset theDataset, TriggerType triggerType, boolean datasetExternallyReleased) {
        return new WorkflowContext(triggerType, theDataset, getRequest(), datasetExternallyReleased);
    }

}
