package edu.harvard.iq.dataverse.util.bagit;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author adaybujeda
 */
public class BagValidation {

    private final Optional<String> errorMessage;
    private final Map<Path, FileValidationResult> fileResults;

    public BagValidation(Optional<String> errorMessage) {
        this.errorMessage = errorMessage == null ? Optional.empty() : errorMessage;
        this.fileResults = new LinkedHashMap<>();
    }

    public FileValidationResult addFileResult(Path filePath) {
        FileValidationResult fileResult = new FileValidationResult(filePath);
        fileResults.put(filePath, fileResult);
        return fileResult;
    }

    public Optional<String> getErrorMessage() {
        return errorMessage;
    }

    public Map<Path, FileValidationResult> getFileResults() {
        return Collections.unmodifiableMap(fileResults);
    }

    public List<String> getAllErrors() {
        Stream<String> mainError = getErrorMessage().stream();
        Stream<String> fileErrors = getFileResults().values().stream().filter(result -> result.isError()).map(result -> result.getMessage());
        return Stream.concat(mainError, fileErrors).collect(Collectors.toList());
    }

    public long errors() {
        return fileResults.values().stream().filter(result -> result.isError()).count();
    }

    public boolean success() {
        return errorMessage.isEmpty() && fileResults.values().stream().allMatch(result -> result.isSuccess());
    }

    public String report() {
        long fileResultsPending = fileResults.values().stream().filter(result -> result.isPending()).count();
        long fileResultsSuccess = fileResults.values().stream().filter(result -> result.isSuccess()).count();
        long fileResultsError = fileResults.values().stream().filter(result -> result.isError()).count();
        return String.format("BagValidation{success=%s, errorMessage=%s, fileResultsItems=%s, fileResultsSuccess=%s, fileResultsPending=%s, fileResultsError=%s}", success(), errorMessage, fileResults.size(), fileResultsSuccess, fileResultsPending, fileResultsError);
    }

    @Override
    public String toString() {
        return String.format("BagValidation{errorMessage=%s, fileResultsItems=%s}", errorMessage, fileResults.size());
    }

    public static class FileValidationResult {
        public static enum Status {
            PENDING, SUCCESS, ERROR;
        }

        private final Path filePath;
        private Status status;
        private String message;

        public FileValidationResult(Path filePath) {
            this.filePath = filePath;
            this.status = Status.PENDING;
        }

        public Path getFilePath() {
            return filePath;
        }

        public void setSuccess() {
            this.status = Status.SUCCESS;
        }

        public void setError(String message) {
            this.status = Status.ERROR;
            this.message = message;
        }

        public boolean isPending() {
            return status.equals(Status.PENDING);
        }

        public boolean isSuccess() {
            return status.equals(Status.SUCCESS);
        }

        public boolean isError() {
            return status.equals(Status.ERROR);
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format("FileValidationResult{filePath=%s, status=%s, message=%s}", filePath, status, message);
        }
    }

}
