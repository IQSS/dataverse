package edu.harvard.iq.dataverse.jsf.support;

import com.microsoft.playwright.PlaywrightException;

/**
 * The Java counterpart of Playwright Test's {@code expect(async () => {...}).toPass()}:
 * re-runs a block of actions and assertions until it completes without
 * throwing, or the timeout is reached (then the last failure is rethrown).
 *
 * Use it for things that only become true after something asynchronous on the
 * server, such as search indexing — not as a substitute for a web-first
 * assertion on a single element.
 */
public final class Retry {

    /** Same back-off as toPass(): 100, 250, 500, then every 1000 ms. */
    private static final long[] INTERVALS = {100, 250, 500, 1000};

    private Retry() {
    }

    public static void untilPasses(Runnable block, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        for (int attempt = 0;; attempt++) {
            try {
                block.run();
                return;
            } catch (AssertionError | PlaywrightException e) {
                long pause = INTERVALS[Math.min(attempt, INTERVALS.length - 1)];
                if (System.currentTimeMillis() + pause > deadline) {
                    throw new AssertionError("Still failing after " + timeoutMillis + " ms", e);
                }
                try {
                    Thread.sleep(pause);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }
}
