package edu.harvard.iq.dataverse.workflow.step;

import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifactSource;
import edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newInputStream;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

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
    
    public static final String BASE_WORK_DIR_PARAM_NAME = "baseWorkDir";
    
    /**
     * Semicolon (;) separated list of file artifacts to save in case of failure.
     * Can be used i.e. to save command input files for later debug.
     */
    public static final String FAILURE_ARTIFACTS_PARAM_NAME = "failureArtifacts";

    private final String workDirParam;
    private final String baseWorkDirParam;
    private final Set<String> failureArtifacts;

    private Path workDir;

    // -------------------- CONSTRUCTORS --------------------

    public FilesystemAccessingWorkflowStep(WorkflowStepParams inputParams) {
        workDirParam = inputParams.get(WORK_DIR_PARAM_NAME);
        baseWorkDirParam = inputParams.get(BASE_WORK_DIR_PARAM_NAME);
        failureArtifacts = new HashSet<>(inputParams.getList(FAILURE_ARTIFACTS_PARAM_NAME, ";"));
    }

    // -------------------- LOGIC --------------------

    @Override
    public final WorkflowStepResult run(WorkflowExecutionContext context) {
        try {
            workDir = createWorkDir(context);

            WorkflowStepResult.Source resultSupplier = runInternal(context, workDir);

            return resultSupplier.apply(defaultOutputParams());
        } catch (Exception e) {
            return handleError(e);
        }
    }

    protected abstract WorkflowStepResult.Source runInternal(WorkflowExecutionContext context, Path workDir) throws Exception;

    protected WorkflowStepResult handleError(Exception e) {
        log.error("Failed workflow step", e);
        return new Failure(e.getMessage(), failureArtifacts());
    }

    protected void addFailureArtifacts(String...fileNames) {
        failureArtifacts.addAll(asList(fileNames));
    }

    protected List<WorkflowArtifactSource> failureArtifacts() {
        return workDirArtifacts(failureArtifacts, UTF_8);
    }

    protected List<WorkflowArtifactSource> workDirArtifacts(Collection<String> fileNames, Charset encoding) {
        return fileNames.stream()
                .map(fileName -> workDirArtifactOf(fileName, encoding))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
    }

    protected Optional<WorkflowArtifactSource> workDirArtifactOf(String fileName, Charset encoding) {
        return workDirArtifactOf(fileName, fileName, encoding);
    }

    protected Optional<WorkflowArtifactSource> workDirArtifactOf(String name, String fileName, Charset encoding) {
        return Optional.of(workDir.resolve(fileName))
                .filter(Files::exists)
                .map(path -> new WorkflowArtifactSource(name, encoding.name(), () -> newInputStream(path)));
    }

    protected Path resolveWorkDir(WorkflowExecutionContext context) {
        if (workDirParam == null) {
            Path basePath = baseWorkDirParam != null ?
                    Paths.get(baseWorkDirParam)
                    : Paths.get(System.getProperty("java.io.tmpdir"), "dataverse");
            Path subPath = Paths.get(
                    Long.toString(context.getDatasetId()),
                    Long.toString(context.getVersionNumber()),
                    Long.toString(context.getMinorVersionNumber()),
                    randomUUID().toString());
            
            return basePath.resolve(subPath);
        } else {
            return Paths.get(workDirParam);
        }
    }
    
    // -------------------- PRIVATE --------------------

    private Path createWorkDir(WorkflowExecutionContext context) throws IOException {
        return Files.createDirectories(resolveWorkDir(context));
    }

    private HashMap<String, String> defaultOutputParams() {
        HashMap<String, String> outputParams = new HashMap<>();
        outputParams.put(WORK_DIR_PARAM_NAME, workDir.toAbsolutePath().toString());
        return outputParams;
    }
}
