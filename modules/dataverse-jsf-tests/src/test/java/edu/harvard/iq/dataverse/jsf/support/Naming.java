package edu.harvard.iq.dataverse.jsf.support;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unique names for everything a test creates.
 *
 * Every collection, dataset, template, and guestbook a test creates must be
 * uniquely named so that (a) tests never collide with each other or with a
 * previous run's leftovers, and (b) a test can always find its own object on
 * a page instead of picking "the first row".
 */
public final class Naming {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private Naming() {
    }

    /** Short, sortable, collision-resistant suffix, e.g. "m1x9k2a7-3f". */
    public static String uniqueSuffix() {
        String random = Integer.toString(36 * 36 + ThreadLocalRandom.current().nextInt(36 * 36), 36).substring(1);
        return Long.toString(System.currentTimeMillis(), 36)
                + Integer.toString(COUNTER.incrementAndGet(), 36)
                + "-" + random;
    }

    /** Human-readable unique name, e.g. "Playwright Dataset m1x9k2a7-3f". */
    public static String uniqueName(String prefix) {
        return prefix + " " + uniqueSuffix();
    }

    /** URL-safe unique collection alias, e.g. "pw-m1x9k2a7-3f". */
    public static String uniqueAlias() {
        return uniqueAlias("pw");
    }

    public static String uniqueAlias(String prefix) {
        return (prefix + "-" + uniqueSuffix()).toLowerCase().replaceAll("[^a-z0-9-]", "-");
    }
}
