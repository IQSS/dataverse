package edu.harvard.iq.dataverse.workflow.listener;

import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.FinalizeDatasetPublicationCommand;
import edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionContext;

import javax.ejb.Singleton;
import javax.inject.Inject;

import static edu.harvard.iq.dataverse.workflow.execution.WorkflowContext.TriggerType.PrePublishDataset;

@Singleton
public class FinalizeDatasetPublicationListener implements WorkflowExecutionListener {

    private final EjbDataverseEngine engine;

    @Inject
    public FinalizeDatasetPublicationListener(EjbDataverseEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onSuccess(WorkflowExecutionContext ctx) {
        if (PrePublishDataset == ctx.getType()) {
            engine.submit(new FinalizeDatasetPublicationCommand(
                    ctx.getDataset(), ctx.getRequest(), ctx.isDatasetExternallyReleased()));
        }
    }
}
