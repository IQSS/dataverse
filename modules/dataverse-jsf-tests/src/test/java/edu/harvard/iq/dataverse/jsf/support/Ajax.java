package edu.harvard.iq.dataverse.jsf.support;

import com.microsoft.playwright.Page;

public final class Ajax {

    private static final String IDLE = """
            () => {
              const w = window;
              const jqueryIdle = typeof w.jQuery === "undefined" || w.jQuery.active === 0;
              const primeIdle =
                typeof w.PrimeFaces === "undefined" ||
                !w.PrimeFaces.ajax?.Queue ||
                w.PrimeFaces.ajax.Queue.isEmpty();
              return jqueryIdle && primeIdle;
            }
            """;

    private Ajax() {
    }

    /**
     * Waits until Dataverse's JSF/PrimeFaces UI has no AJAX requests in flight.
     *
     * Use this instead of a fixed sleep after an action that triggers a
     * PrimeFaces partial update (selecting an autocomplete item, changing a
     * dropdown that re-renders part of the form, etc.). Prefer a web-first
     * assertion on the element you actually care about when there is one —
     * this is for the cases where there isn't.
     */
    public static void waitForAjaxIdle(Page page) {
        waitForAjaxIdle(page, 15_000);
    }

    public static void waitForAjaxIdle(Page page, double timeout) {
        page.waitForFunction(IDLE, null, new Page.WaitForFunctionOptions().setTimeout(timeout));
    }
}
