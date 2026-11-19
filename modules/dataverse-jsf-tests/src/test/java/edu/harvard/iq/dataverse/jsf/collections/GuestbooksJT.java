package edu.harvard.iq.dataverse.jsf.collections;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static edu.harvard.iq.dataverse.jsf.support.Popup.openPopup;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.JsfFixtures;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import edu.harvard.iq.dataverse.jsf.support.Naming;
import edu.harvard.iq.dataverse.jsf.support.pages.CollectionPage;
import org.junit.jupiter.api.DisplayName;

public class GuestbooksJT {

    @JsfTest
    @DisplayName("Guestbook: create, download responses, delete")
    public void testGuestbook(Page page, JsfFixtures fixtures) {
        assumeFalse(fixtures.browserName().equals("webkit"), "WebKit opens the CSV inline instead of downloading it");

        // A throwaway collection keeps the guestbook out of the root collection.
        String alias = fixtures.createCollection().alias();
        String guestbookName = Naming.uniqueName("Playwright Guestbook");

        // Only matters if the test fails before its own delete step.
        fixtures.cleanup().add("delete guestbook \"" + guestbookName + "\"", p -> {
            openGuestbooks(p, alias);
            if (guestbookRow(p, guestbookName).count() > 0) {
                deleteGuestbook(p, guestbookName);
            }
        });

        openGuestbooks(page, alias);

        fixtures.step("create the guestbook", () -> {
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Create Dataset Guestbook")).click();
            page.locator("[id=\"guestbookForm:guestbookName\"]").fill(guestbookName);
            for (String field : new String[]{"name", "email", "institution", "position"}) {
                page.locator("[id=\"guestbookForm:" + field + "RequiredCb\"]").check();
            }

            Locator question = page.locator("[id=\"guestbookForm:customQuestions\"]");
            Locator options = question.locator("[id$=\":responseText\"]");
            // Switching to multiple choice re-renders the question via AJAX.
            question.locator("[id$=\":questionOptions\"]").first().selectOption("options");
            assertThat(options.first()).isVisible();
            question.locator("[id$=\":questionText\"]").first().fill("How did you find this dataset?");
            options.nth(0).fill("Web Search");
            // .nolabel-field-btn is the option-level Add (the question-level one is .compound-field-btn).
            question.locator(".nolabel-field-btn[data-original-title=\"Add\"]").first().click();
            assertThat(options).hasCount(2);
            options.nth(1).fill("Colleague Recommendation");

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create Dataset Guestbook")).click();
        });

        Locator row = guestbookRow(page, guestbookName);
        assertThat(row).isVisible();

        fixtures.step("download its responses", () -> {
            Download download = page.waitForDownload(
                    () -> row.locator("[id$=\"downloadResponsesByDvAndGuestbook\"]").click());
            assertThat(download.suggestedFilename(), not(emptyOrNullString()));
        });

        fixtures.step("delete it", () -> deleteGuestbook(page, guestbookName));
    }

    private static Locator guestbookRow(Page page, String guestbookName) {
        return page.locator("[id=\"manageGuestbooksForm:allGuestbooks_data\"] tr").filter(new Locator.FilterOptions()
                .setHas(page.locator("td[role=\"gridcell\"]", new Page.LocatorOptions().setHasText(guestbookName))));
    }

    private static void deleteGuestbook(Page page, String guestbookName) {
        Locator confirm = page.locator("[id=\"manageGuestbooksForm:deleteGuestbookConfirm\"]")
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Continue"));
        openPopup(guestbookRow(page, guestbookName).locator("[data-original-title=\"Delete\"]"), confirm);
        confirm.click();
        assertThat(guestbookRow(page, guestbookName)).hasCount(0);
    }

    private static void openGuestbooks(Page page, String alias) {
        CollectionPage collection = new CollectionPage(page);
        collection.open(alias);
        collection.openEditMenuItem("manageGuestbooks");
    }
}
