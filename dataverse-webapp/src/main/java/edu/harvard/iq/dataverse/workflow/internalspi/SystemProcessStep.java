package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.FilesystemAccessingWorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepParams;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static edu.harvard.iq.dataverse.workflow.step.Success.successWith;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.ListUtils.union;

public class SystemProcessStep extends FilesystemAccessingWorkflowStep {

    private static final Logger log = LoggerFactory.getLogger(SystemProcessStep.class);

    public static final String STEP_ID = "system-process";

    /**
     * The binary to run. Used as is, so consider absolute path if necessary.
     */
    public static final String COMMAND_PARAM_NAME = "command";
    /**
     * Pipe (|) separated list of command arguments.
     */
    public static final String ARGUMENTS_PARAM_NAME = "arguments";
    /**
     * Output parameter name of generated, random process identifier.
     */
    public static final String PROCESS_ID_PARAM_NAME = "processId";

    private final String command;
    private final List<String> arguments;

    // -------------------- CONSTRUCTORS --------------------

    public SystemProcessStep(WorkflowStepParams inputParams) {
        super(inputParams);
        command = inputParams.getRequired(COMMAND_PARAM_NAME);
        arguments = inputParams.getList(ARGUMENTS_PARAM_NAME, "\\|");
    }

    // -------------------- LOGIC --------------------

    @Override
    protected WorkflowStepResult.Source runInternal(WorkflowExecutionContext context, Path workDir) throws Exception {
        String processId = UUID.randomUUID().toString();
        log.trace("About to run process {}", processId);
        int exitCode = executeCommand(processId, workDir);
        log.trace("Process {} exited with code {}", processId, exitCode);
        return handleExitCode(processId, exitCode);
    }

    @Override
    public WorkflowStepResult resume(WorkflowExecutionContext context, Map<String, String> internalData, String externalData) {
        throw new UnsupportedOperationException("This step des not pause");
    }

    @Override
    public void rollback(WorkflowExecutionContext context, Failure reason) {
    }

    // -------------------- PRIVATE --------------------

    private int executeCommand(String processId, Path workDir) throws IOException, InterruptedException {
        Path outLog = outLogPath(processId, workDir);
        Path errLog = errLogPath(processId, workDir);

        addFailureArtifacts(
                outLog.getFileName().toString(),
                errLog.getFileName().toString());

        ProcessBuilder process = new ProcessBuilder(union(singletonList(command), arguments))
                .directory(workDir.toFile())
                .redirectOutput(outLog.toFile())
                .redirectError(errLog.toFile());

        return process.start()
                .waitFor();
    }

    Path outLogPath(String processId, Path workDir) {
        return workDir.resolve("out-" + processId + ".log");
    }

    Path errLogPath(String processId, Path workDir) {
        return workDir.resolve("err-" + processId + ".log");
    }

    private WorkflowStepResult.Source handleExitCode(String processId, int exitCode) {
        if (exitCode == 0) {
            return successWith(outputParams ->
                outputParams.put(PROCESS_ID_PARAM_NAME, processId)
            );
        } else {
            return outputParams ->
                    new Failure("Process " + processId + " returned " + exitCode + " exit code",
                            "External program exited with errors", failureArtifacts());
        }
    }
}
