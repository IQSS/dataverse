package edu.harvard.iq.dataverse.workflow.step;

/**
 * Result returned when step execution succeeds.
 */
public class Success implements WorkflowStepResult {

    private final String reason;
    private final String message;

    public Success(String reason) {
        this(reason, null);
    }

    /**
     * Constructs a new success message.
     * 
     * @param reason  Technical comment (for logs etc.).
     * @param message Human readable comment.
     */
    public Success(String reason, String message) {
        this.reason = reason;
        this.message = message;
    }

    /**
     * Holds a technical comment about the success.
     * 
     * @return the technical comment about the processing.
     */
    public String getReason() {
        return reason;
    }

    /**
     * Holds the user-friendly message describing what was successfully done.
     * 
     * @return user-friendly message for the success.
     */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "WorkflowStepResult.Success{" + "reason=" + reason + ", message=" + message + '}';
    }

}