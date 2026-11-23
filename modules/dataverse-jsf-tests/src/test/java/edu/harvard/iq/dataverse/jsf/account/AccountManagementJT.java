package edu.harvard.iq.dataverse.jsf.account;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static edu.harvard.iq.dataverse.jsf.support.Popup.openPopup;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.Cleanup;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import java.time.Year;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;

/**
 * Exercises My Data, Notifications, Account Information, and API Token from
 * the authenticated user dropdown.
 */
public class AccountManagementJT {

    @JsfTest
    @DisplayName("Account menu: My Data, Notifications, Account Information, API Token")
    public void testAccountMenu(Page page, Cleanup cleanup) {
        page.navigate("/");

        Locator userMenuTrigger = page.locator("span#userDisplayInfoTitle");
        Locator dropdownMenu = page.locator("ul.dropdown-menu").filter(new Locator.FilterOptions()
                .setHas(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Log Out"))));

        // 1. My Data
        openPopup(userMenuTrigger, dropdownMenu);
        dropdownMenu.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("My Data")).click();
        assertThat(page).hasURL(Pattern.compile("dataverseuser\\.xhtml\\?selectTab=dataRelatedToMe"));

        // In a freshly-provisioned CI environment the user has no data yet at
        // this point in the run, so #div-card-results may be hidden. The URL
        // check above is sufficient; if there happen to be results we
        // additionally verify at least one card.
        Locator cardResults = page.locator("#resultsTable #div-card-results");
        if (cardResults.isVisible()) {
            assertThat(cardResults.locator(":scope > div").count(), greaterThan(0));
        }

        // 2. Notifications
        page.navigate("/");
        openPopup(userMenuTrigger, dropdownMenu);
        dropdownMenu.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Notifications")).click();
        assertThat(page).hasURL(Pattern.compile("dataverseuser\\.xhtml\\?selectTab=notifications"));
        assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Notifications")).first())
                .isVisible();

        // 3. Account Information
        page.navigate("/");
        openPopup(userMenuTrigger, dropdownMenu);
        dropdownMenu.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Account Information")).click();
        assertThat(page).hasURL(Pattern.compile("dataverseuser\\.xhtml\\?selectTab=accountInfo"));

        Locator metadataTable = page.locator("table.metadata");
        assertThat(metadataTable).isVisible();
        for (String heading : new String[]{"Username", "Given Name", "Family Name", "Email"}) {
            assertThat(metadataTable.getByRole(AriaRole.ROWHEADER, new Locator.GetByRoleOptions().setName(heading)))
                    .isVisible();
        }
        assertThat(page.getByText(Pattern.compile("Verified", Pattern.CASE_INSENSITIVE)).first()).isVisible();

        // 4. API Token
        page.navigate("/");
        openPopup(userMenuTrigger, dropdownMenu);
        dropdownMenu.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("API Token")).click();
        assertThat(page).hasURL(Pattern.compile("dataverseuser\\.xhtml\\?selectTab=apiTokenTab"));

        Locator createTokenButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create Token"));
        assertThat(createTokenButton).isVisible();

        // If the test dies before "Revoke Token" below, revoke it anyway: a
        // leftover token hides "Create Token" and breaks the next run.
        cleanup.add("revoke API token", p -> {
            p.navigate("/dataverseuser.xhtml?selectTab=apiTokenTab");
            Locator revoke = p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Revoke Token"));
            if (revoke.isVisible()) {
                revoke.click();
                assertThat(p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create Token")))
                        .isVisible();
            }
        });
        createTokenButton.click();

        Locator tokenCode = page.locator("#apiToken pre code");
        assertThat(tokenCode).isVisible();
        String tokenText = tokenCode.textContent();
        assertTrue(Pattern.compile("[a-zA-Z]").matcher(tokenText).find(), "token has a letter: " + tokenText);
        assertTrue(Pattern.compile("[0-9]").matcher(tokenText).find(), "token has a digit: " + tokenText);
        assertThat(tokenText, containsString("-"));

        assertThat(page.getByText(Pattern.compile("Expiration Date", Pattern.CASE_INSENSITIVE)).first()).isVisible();
        Locator expirationCell = page.locator("table.metadata tbody tr")
                .filter(new Locator.FilterOptions().setHas(page.locator("th", new Page.LocatorOptions()
                        .setHasText("Expiration Date"))))
                .locator("td");
        assertThat(expirationCell.textContent(), containsString(String.valueOf(Year.now().getValue() + 1)));

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Revoke Token")).click();
        assertThat(createTokenButton).isVisible();

        page.navigate("/");
    }
}
