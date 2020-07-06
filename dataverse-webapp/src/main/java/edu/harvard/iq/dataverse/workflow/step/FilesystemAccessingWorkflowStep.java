package edu.harvard.iq.dataverse.workflow.step;

import edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static java.util.UUID.randomUUID;

/**
 * Base class for {@link WorkflowStep}'s operating on a local filesystem.
 * <p>
 * Handles creation of a local working directory to be used for all file storage by the step.
 * Also takes care for passing through that working directory to the next step of the same type.
 * This way you can easily have multiple steps working in the same directory one after another,
 * as long as each of them extend this class. To override this simply set {@value WORK_DIR_PARAM_NAME}
 * input parameter to a different path you need.
 */
public abstract class FilesystemAccessingWorkflowStep implements WorkflowStep {

    private static final Logger log = LoggerFactory.getLogger(FilesystemAccessingWorkflowStep.class);

    /**
     * Directory to run execute the step in.
     * If not defined a temporary directory will be created for that purpose.
     */
    public static final String WORK_DIR_PARAM_NAME = "workDir";

    private final String workDirParam;

    // -------------------- CONSTRUCTORS --------------------

    public FilesystemAccessingWorkflowStep(WorkflowStepParams inputParams) {
        this.workDirParam = inputParams.get(WORK_DIR_PARAM_NAME);
    }

    // -------------------- LOGIC --------------------

    @Override
    public final WorkflowStepResult run(WorkflowExecutionContext context) {
        try {
            Path workDir = createWorkDir(context);

            WorkflowStepResult.Source resultSupplier = runInternal(context, workDir);

            return resultSupplier.apply(defaultOutputParams(workDir));
        } catch (Exception e) {
            return handleError(e);
        }
    }

    protected abstract WorkflowStepResult.Source runInternal(WorkflowExecutionContext context, Path workDir) throws Exception;

    protected WorkflowStepResult handleError(Exception e) {
        log.error("Failed workflow step", e);
        return new Failure(e.getMessage());
    }

    // -------------------- PRIVATE --------------------

    private Path createWorkDir(WorkflowExecutionContext context) throws IOException {
        return Files.createDirectories(resolveWorkDir(context));
    }

    private Path resolveWorkDir(WorkflowExecutionContext context) {
        if (workDirParam == null) {
            return Paths.get(System.getProperty("java.io.tmpdir"),
                    "dataverse",
                    Long.toString(context.getDataset().getId()),
                    Long.toString(context.getNextVersionNumber()),
                    Long.toString(context.getNextMinorVersionNumber()),
                    randomUUID().toString());
        } else {
            return Paths.get(workDirParam);
        }
    }

    private HashMap<String, String> defaultOutputParams(Path workDir) {
        HashMap<String, String> outputParams = new HashMap<>();
        outputParams.put(WORK_DIR_PARAM_NAME, workDir.toAbsolutePath().toString());
        return outputParams;
    }
}
