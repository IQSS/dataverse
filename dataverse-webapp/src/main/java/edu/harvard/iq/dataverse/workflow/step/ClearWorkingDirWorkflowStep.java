package edu.harvard.iq.dataverse.workflow.step;

import edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionStepContext;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult.Source;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class ClearWorkingDirWorkflowStep extends FilesystemAccessingWorkflowStep {

    public static final String STEP_ID = "clear-working-directory";
    private static final Logger logger = LoggerFactory.getLogger(ClearWorkingDirWorkflowStep.class);

    // -------------------- CONSTRUCTORS --------------------

    public ClearWorkingDirWorkflowStep(WorkflowStepParams inputParams) {
        super(inputParams);
    }

    // -------------------- LOGIC --------------------

    @Override
    protected Source runInternal(WorkflowExecutionStepContext context, Path workDir) throws Exception {
        FileUtils.deleteDirectory(workDir.toFile());
        getBaseWorkDir()
                .filter(workDir::startsWith)
                .ifPresent(baseWorkDir -> cleanupEmptyParentsUpTo(baseWorkDir, workDir));
        return Success.successWith();
    }

    @Override
    public WorkflowStepResult resume(WorkflowExecutionStepContext context, Map<String, String> internalData, String externalData) {
        throw new UnsupportedOperationException("This step does not pause");
    }

    @Override
    public void rollback(WorkflowExecutionStepContext context, Failure reason) {
    }

    // -------------------- PRIVATE --------------------

    private void cleanupEmptyParentsUpTo(Path baseWorkDir, Path child) {
        Path parent = child.getParent();
        try {
            if (parent.startsWith(baseWorkDir) && !parent.equals(baseWorkDir) && PathUtils.isEmptyDirectory(parent)) {
                PathUtils.deleteDirectory(parent);
                cleanupEmptyParentsUpTo(baseWorkDir, parent);
            }
        } catch (IOException e) {
            throw new RuntimeException("Couldn't clean-up directory: " + parent, e);
        }
    }
}
