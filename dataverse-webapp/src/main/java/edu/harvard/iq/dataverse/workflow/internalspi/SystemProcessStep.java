package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.FilesystemAccessingWorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static edu.harvard.iq.dataverse.workflow.step.Success.successWith;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.ListUtils.union;

public class SystemProcessStep extends FilesystemAccessingWorkflowStep {

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

    public SystemProcessStep(Map<String, String> inputParams) {
        super(inputParams);
        command = ofNullable(inputParams.get(COMMAND_PARAM_NAME))
                .orElseThrow(() -> new IllegalArgumentException("Command parameter is required"));
        arguments = ofNullable(inputParams.get(ARGUMENTS_PARAM_NAME))
                .map(args -> asList(args.split("\\|")))
                .orElseGet(Collections::emptyList);
    }

    // -------------------- LOGIC --------------------

    @Override
    protected WorkflowStepResult.Source runInternal(WorkflowExecutionContext context, Path workDir) throws Exception {
        String processId = UUID.randomUUID().toString();

        int exitCode = executeCommand(processId, workDir);

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
        Path outLog = outLogPath(workDir, processId);
        Path errLog = errLogPath(workDir, processId);

        ProcessBuilder process = new ProcessBuilder(union(singletonList(command), arguments))
                .directory(workDir.toFile())
                .redirectOutput(outLog.toFile())
                .redirectError(errLog.toFile());

        return process.start()
                .waitFor();
    }

    Path outLogPath(Path workDir, String processId) {
        return workDir.resolve("out-" + processId + ".log");
    }

    Path errLogPath(Path workDir, String processId) {
        return workDir.resolve("err-" + processId + ".log");
    }

    private WorkflowStepResult.Source handleExitCode(String processId, int exitCode) {
        if (exitCode == 0) {
            return successWith(outputParams ->
                outputParams.put(PROCESS_ID_PARAM_NAME, processId)
            );
        } else {
            return outputParams ->
                    new Failure("Process " + processId + "returned " + exitCode + " exit code",
                            "External program exited with errors");
        }
    }
}
