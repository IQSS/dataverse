package edu.harvard.iq.dataverse.util.logging;

import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Tracks a streak of consecutive failures and escalates the logging level once a threshold is exceeded.
 * A success resets the streak.
 * <p>
 * Once escalated, only the first failure and every {@code repeatEvery}-th subsequent failure return
 * {@link Level#WARNING}; failures in between are demoted to {@link Level#FINE} to avoid flooding the log
 * (they remain visible at FINE for debugging).
 * <p>
 * If the threshold is set to 0 or a negative value, escalation is deactivated.
 * <p>
 * {@link #recordSuccess()} reports whether the cleared streak had been escalated, so the caller can log
 * a recovery message — otherwise the log would show escalations without ever showing the recovery.
 * <p>
 * Instances are thread-safe and may be shared across concurrent callers and used in other,
 * thread-safe contexts like {@code ConcurrentHashMap}.
 */

public final class FailureEscalation {
    private final AtomicInteger streak = new AtomicInteger();
    private final int threshold;
    private final int repeatEvery;
    
    /**
     * @param threshold consecutive failures required before escalation; 0 or negative deactivates escalation;
     *                  makes escalation repeat every this-many failures
     */
    public FailureEscalation(int threshold) {
        this.threshold = threshold;
        this.repeatEvery = threshold; // we don't care about negative or 0, as escalation is deactivated anyway
    }
    
    /**
     * @param threshold   consecutive failures required before escalation; 0 or negative deactivates escalation
     * @param repeatEvery once escalated, log at WARNING only every this-many failures (minimum 1 = every failure)
     */
    public FailureEscalation(int threshold, int repeatEvery) {
        this.threshold = threshold;
        this.repeatEvery = Math.max(1, repeatEvery);
    }
    
    /**
     * Record a failure and return the level to log it at.
     */
    public Level incrementAndGetLevel() {
        // Deactivated: skip all bookkeeping, no map entries are ever created.
        if (threshold < 1) {
            return Level.FINE;
        }
        // When repeatEvery is smaller than threshold, we must refrain from escalating, as the modulo operation would
        // generate 0 for some failure counts smaller than threshold.
        // Example: (1 - 5) % 4 = 0 (count=1, threshold=5, repeatEvery=4)
        if (streak.incrementAndGet() < threshold) {
            return Level.FINE;
        }
        // Escalated: warn on the first hit and every repeatEvery-th afterwards, demote the rest.
        return (streak.get() - threshold) % repeatEvery == 0 ? Level.WARNING : Level.FINE;
    }
    
    /**
     * Record a success, resetting the streak.
     *
     * @return The length of the just-cleared streak, if it had reached the escalation threshold.
     *         The caller should log a recovery message in that case
     *         (e.g. via {@code recordSuccess().ifPresent(n -> logger.warning(...))}).
     *         Empty otherwise.
     */
    public OptionalInt recordSuccess() {
        int previous = streak.getAndSet(0);
        return (threshold > 0 && previous >= threshold)
            ? OptionalInt.of(previous)
            : OptionalInt.empty();
    }
    
    /**
     * Current streak length; intended for metrics gauges.
     */
    public int currentStreak() {
        return streak.get();
    }
}
