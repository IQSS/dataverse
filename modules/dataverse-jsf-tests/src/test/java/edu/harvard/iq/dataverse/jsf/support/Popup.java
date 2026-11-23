package edu.harvard.iq.dataverse.jsf.support;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;

public final class Popup {

    private Popup() {
    }

    /**
     * Opens a dropdown menu or dialog by clicking {@code open} and waits until
     * {@code target} (something inside it) is visible.
     *
     * Right after a page loads, a click can land before Dataverse has wired up
     * its handler: the click "succeeds" but nothing opens, and the next click
     * on the hidden target waits until the test times out. So keep opening
     * until the target shows up — with no fixed delay: when the page is ready,
     * the first click is the only one.
     */
    public static void openPopup(Locator open, Locator target) {
        openPopup(() -> open.click(new Locator.ClickOptions().setTimeout(5_000)), target);
    }

    /** As above, for multi-step openers (e.g. Edit menu → item → confirmation dialog). */
    public static void openPopup(Runnable open, Locator target) {
        Retry.untilPasses(() -> {
            if (!target.isVisible()) {
                open.run();
            }
            assertThat(target).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(2_000));
        }, 30_000);
    }
}
