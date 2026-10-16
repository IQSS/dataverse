package edu.harvard.iq.dataverse.jsf.support.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.util.regex.Pattern;

/** The "New Dataverse" (create collection) form. */
public class CollectionForm {

    private final Page page;

    public CollectionForm(Page page) {
        this.page = page;
    }

    public Locator field(String id) {
        return page.locator("[id=\"dataverseForm:" + id + "\"]");
    }

    public void fillIdentifier(String alias) {
        field("identifier").fill(alias);
    }

    /** Category option value, e.g. "DEPARTMENT", "LABORATORY", "RESEARCH_PROJECTS". */
    public void selectCategory(String category) {
        field("dataverseCategory").selectOption(category);
    }

    public void create(String alias) {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create Dataverse")).click();
        // Dataverse redirects to /dataverse/<alias>/ (note the trailing slash).
        assertThat(page).hasURL(Pattern.compile("/dataverse/" + alias + "/?([?#]|$)"));
        assertThat(page.getByText("You have successfully created your dataverse!")).isVisible();
    }
}
