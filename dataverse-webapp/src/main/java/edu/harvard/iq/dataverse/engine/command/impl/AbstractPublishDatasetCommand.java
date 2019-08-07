package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.workflow.WorkflowContext;

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

    protected WorkflowContext buildContext(Dataset theDataset, WorkflowContext.TriggerType triggerType, boolean datasetExternallyReleased) {
        return new WorkflowContext(getRequest(), theDataset, triggerType, datasetExternallyReleased);
    }

}
