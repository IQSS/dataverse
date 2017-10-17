package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.workflow.WorkflowContext;

/**
 * Base class for commands involved in Dataset publication. Mostly needed for code reuse.
 * 
 * @param <T> Command result type (as usual).
 * @author michael
 */
public abstract class AbstractPublishDatasetCommand<T> extends AbstractCommand<T> {
    
    Dataset theDataset;

    public AbstractPublishDatasetCommand(Dataset datasetIn, DataverseRequest aRequest) {
        super(aRequest, datasetIn);
        theDataset = datasetIn;
    }
    
    protected WorkflowContext buildContext( String doiProvider, WorkflowContext.TriggerType triggerType) {
        return new WorkflowContext(getRequest(), theDataset, doiProvider, triggerType);
    }
    
}
