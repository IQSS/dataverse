package edu.harvard.iq.dataverse.workflow.step;

import java.util.HashMap;
import java.util.Map;

/**
 * Result returned when step execution fails.
 *
 * @author michael
 */
public class Failure implements WorkflowStepResult {

    public static final String REASON_PARAM_NAME = "reason";
    public static final String MESSAGE_PARAM_NAME = "message";

    private final String reason;
    private final String message;

    public Failure(String reason) {
        this(reason, reason);
    }

    /**
     * Constructs a new failure message.
     *
     * @param reason  Technical reason (for logs etc.).
     * @param message Human readable reason.
     */
    public Failure(String reason, String message) {
        this.reason = reason;
        this.message = message;
    }

    /**
     * Holds the technical reason for the failure, useful for debugging the problem.
     *
     * @return the technical reason for the problem.
     */
    public String getReason() {
        return reason;
    }

    /**
     * Holds the user-friendly message explaining the failure.
     *
     * @return user-friendly message for the failure.
     */
    public String getMessage() {
        return message;
    }

    @Override
    public Map<String, String> getData() {
        Map<String, String> data = new HashMap<>();
        data.put(REASON_PARAM_NAME, reason);
        data.put(MESSAGE_PARAM_NAME, message);
        return data;
    }

    @Override
    public String toString() {
        return "WorkflowStepResult.Failure{" + "reason=" + reason + ", message=" + message + '}';
    }
}
