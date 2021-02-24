package edu.harvard.iq.dataverse.workflow.step;

/**
 * Result returned when step execution succeeds.
 */
public class Success implements WorkflowStepResult {
    
    private final String reason;
    private final String message;
    
    public Success( String reason ) {
        this(reason, null);
    }
    
    /**
     * Constructs a new success message.
     * @param reason Technical reason (for logs etc.).
     * @param message Human readable reason.
     */
    public Success(String reason, String message) {
        this.reason = reason;
        this.message = message;
    }
    
    /**
     * Holds the technical reason for the success, useful for debugging the problem.
     * @return the technical reason for the problem.
     */
    public String getReason() {
        return reason;
    }

    /**
     * Holds the user-friendly message explaining the failure.
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
