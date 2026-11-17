package edu.harvard.iq.dataverse.jsf.support.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static edu.harvard.iq.dataverse.jsf.support.Popup.openPopup;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.JsfTestConfig;
import java.net.URI;

/** A collection ("dataverse") landing page. */
public class CollectionPage {

    private final Page page;

    public CollectionPage(Page page) {
        this.page = page;
    }

    /** Path of a collection page from its alias, or the configured root collection if null. */
    public static String collectionPath(String alias) {
        return alias != null ? "/dataverse/" + alias : JsfTestConfig.rootCollection();
    }

    /** Opens the configured root collection. */
    public void open() {
        open(null);
    }

    /** Opens a collection by alias; with a null alias, opens the root collection. */
    public void open(String alias) {
        page.navigate(collectionPath(alias));
    }

    public Locator editButton() {
        // The button has no id, so anchor on its menu instead: it is the only
        // dropdown whose menu holds the #dataverse-info-dropdown header
        // (dataverse.xhtml). Matching any button named "Edit" would pick up
        // other content's Edit buttons on a busy collection page.
        return page.locator("div.btn-group")
                .filter(new Locator.FilterOptions().setHas(page.locator("#dataverse-info-dropdown")))
                .locator("> button.dropdown-toggle");
    }

    public Locator successAlert() {
        return page.locator("div.alert.alert-success");
    }

    /**
     * Opens Edit → item. {@code id} is the menu entry's stable JSF id:
     * "themeWidgetsOpts", "managePermissions", "manageGroups",
     * "manageTemplates", "manageGuestbooks", "deleteDataset" (sic — that is
     * the id Dataverse gives "Delete Dataverse").
     */
    public void openEditMenuItem(String id) {
        Locator item = page.locator("[id$=\"" + id + "\"]").first();
        openPopup(editButton(), item);
        item.click();
    }

    public void startNewDataset() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add Data")).click();
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("New Dataset")).click();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save Dataset"))).isVisible();
    }

    public void startNewCollection() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add Data")).click();
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("New Dataverse")).click();
        assertThat(page.locator("[id=\"dataverseForm:identifier\"]")).isVisible();
    }

    public void publish() {
        Locator confirm = page.locator("button:visible").filter(new Locator.FilterOptions()
                .setHas(page.locator("span", new Page.LocatorOptions().setHasText("Continue"))));
        openPopup(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Publish")), confirm);
        confirm.click();
        assertThat(successAlert()).containsText("Your dataverse is now public.");
    }

    /**
     * True when Dataverse offers "Delete Dataverse", which it only does for a
     * collection that contains no datasets or sub-collections.
     */
    public boolean isDeletable() {
        return page.locator("[id$=\"deleteDataset\"]").count() > 0;
    }

    public void delete() {
        String from = URI.create(page.url()).getPath();
        Locator confirm = page.locator("[id$=\"deleteDvConfirm\"]")
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Continue"));
        openPopup(() -> openEditMenuItem("deleteDataset"), confirm);
        confirm.click();
        // Dataverse redirects to the parent collection once the delete succeeds.
        page.waitForURL(url -> !URI.create(url).getPath().equals(from));
    }
}
