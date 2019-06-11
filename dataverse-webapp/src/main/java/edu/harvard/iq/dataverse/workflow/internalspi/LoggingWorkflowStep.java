package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A step that logs data about the workflow to the server's log.
 * @author michael
 */
public class LoggingWorkflowStep implements WorkflowStep {
    
    private static final Logger logger = Logger.getLogger(LoggingWorkflowStep.class.getName());
    
    private final Map<String,String> params;

    public LoggingWorkflowStep(Map<String, String> paramSet) {
        params = new HashMap<>(paramSet);
    }
    
    @Override
    public WorkflowStepResult run(WorkflowContext context) {
        logger.info("Logging step:");
        logger.log(Level.INFO, "Invocation id {0}", context.getInvocationId());
        logger.log(Level.INFO, "Dataset id:{0}", context.getDataset().getId());
        logger.log(Level.INFO, "Trigger Type {0}", context.getType());
        logger.log(Level.INFO, "Next version:{0}.{1} isMinor:{2}",
                    new Object[]{context.getNextVersionNumber(), context.getNextMinorVersionNumber(), context.isMinorRelease()});
        params.entrySet().forEach(kv->logger.log(Level.INFO, "{0} -> {1}", new Object[]{kv.getKey(), kv.getValue()}) );
        logger.info("/Logging Step");
        
        return WorkflowStepResult.OK;
    }

    @Override
    public WorkflowStepResult resume(WorkflowContext context, Map<String, String> internalData, String externalData) {
        throw new UnsupportedOperationException("Not supported yet."); // This class does not need to resume.
    }

    @Override
    public void rollback(WorkflowContext context, Failure reason) {
        logger.log(Level.INFO, "rolling back workflow invocation {0}", context.getInvocationId());
    }
}
