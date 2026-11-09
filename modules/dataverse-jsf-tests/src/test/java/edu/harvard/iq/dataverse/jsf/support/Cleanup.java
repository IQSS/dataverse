package edu.harvard.iq.dataverse.jsf.support;

import com.microsoft.playwright.Page;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Teardown work for one test. It runs after the test — pass or fail — each
 * task in a fresh tab, newest first, so things created inside a collection
 * are removed before the collection itself.
 */
public final class Cleanup {

    private record Task(String label, Consumer<Page> run) {
    }

    private final List<Task> tasks = new ArrayList<>();

    /** Queue teardown work. Tasks run newest-first, so children go before parents. */
    public void add(String label, Consumer<Page> run) {
        tasks.add(new Task(label, run));
    }

    /** Runs every task; one failing task never stops the rest. Returns the failures. */
    List<String> runAll(Supplier<Page> newPage) {
        List<String> failures = new ArrayList<>();
        for (Task task : tasks.reversed()) {
            Page page = null;
            try {
                page = newPage.get();
                task.run().accept(page);
            } catch (RuntimeException | AssertionError e) {
                String message = e.getMessage() != null ? e.getMessage() : e.toString();
                failures.add(task.label() + ": " + message.lines().findFirst().orElse(""));
            } finally {
                if (page != null) {
                    page.close();
                }
            }
        }
        return failures;
    }
}
