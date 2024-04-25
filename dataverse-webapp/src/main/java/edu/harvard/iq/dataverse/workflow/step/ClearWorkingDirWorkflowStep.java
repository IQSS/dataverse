package edu.harvard.iq.dataverse.workflow.step;

import edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionStepContext;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult.Source;
import org.apache.commons.io.FileUtils;

import java.nio.file.Path;
import java.util.Map;

public class ClearWorkingDirWorkflowStep extends FilesystemAccessingWorkflowStep {

    public static final String STEP_ID = "clear-working-directory";

    // -------------------- CONSTRUCTORS --------------------

    public ClearWorkingDirWorkflowStep(WorkflowStepParams inputParams) {
        super(inputParams);
    }

    // -------------------- LOGIC --------------------

    @Override
    protected Source runInternal(WorkflowExecutionStepContext context, Path workDir) throws Exception {
        FileUtils.deleteDirectory(workDir.toFile());
        return Success.successWith();
    }

    @Override
    public WorkflowStepResult resume(WorkflowExecutionStepContext context, Map<String, String> internalData, String externalData) {
        throw new UnsupportedOperationException("This step does not pause");
    }

    @Override
    public void rollback(WorkflowExecutionStepContext context, Failure reason) {
    }
}
