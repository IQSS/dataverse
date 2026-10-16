package edu.harvard.iq.dataverse.jsf.support.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static edu.harvard.iq.dataverse.jsf.support.Popup.openPopup;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.Retry;
import java.util.regex.Pattern;

/** The dataset view page (dataset.xhtml). */
public class DatasetPage {

    /** Tabs on the dataset page, by their stable JSF ids. */
    public enum Tab {
        DATA_FILES("dataFilesTab"),
        METADATA("metadataMapTab"),
        TERMS("termsTab"),
        VERSIONS("versionsTab");

        private final String id;

        Tab(String id) {
            this.id = id;
        }
    }

    /** What Edit Dataset → Permissions manages. */
    public enum PermissionsScope {
        DATASET("manageDatasetPermissions", "permissions-manage\\.xhtml"),
        FILE("manageFilePermissions", "permissions-manage-files\\.xhtml");

        private final String menuId;
        private final Pattern url;

        PermissionsScope(String menuId, String url) {
            this.menuId = menuId;
            this.url = Pattern.compile(url);
        }
    }

    private static final Pattern TICKED = Pattern.compile("ui-state-active");

    private final Page page;

    public DatasetPage(Page page) {
        this.page = page;
    }

    public void open(String url) {
        page.navigate(url);
        assertThat(editButton()).isVisible();
    }

    public Locator editButton() {
        return page.locator("[id=\"editDataSet\"]").first();
    }

    public Locator filesTable() {
        return page.locator("[id=\"datasetForm:tabView:filesTable\"]");
    }

    /** A tab header on the dataset page. */
    public Locator tab(Tab tab) {
        return page.locator("a[href=\"#datasetForm:tabView:" + tab.id + "\"]");
    }

    /** Opens Edit Dataset → item, where item is the menu entry's link id. */
    public void openEditMenuItem(String id) {
        Locator item = page.locator("[id=\"datasetForm:" + id + "\"]");
        openPopup(editButton(), item);
        item.click();
    }

    /** Edit Dataset → Permissions → Dataset (or File). */
    public void openPermissions(PermissionsScope scope) {
        Locator permissions = page.locator("li.dropdown-submenu a", new Page.LocatorOptions()
                .setHasText("Permissions")).first();
        openPopup(editButton(), permissions);
        permissions.hover();
        page.locator("[id=\"datasetForm:" + scope.menuId + "\"]").click();
        page.waitForURL(scope.url);
    }

    /**
     * Ticks the "select all files" checkbox in the files table. Under load the
     * click can land before the table's handler is wired and not register, so
     * click again until the box shows as ticked (never clicking a ticked box,
     * which would untick it).
     */
    public void selectAllFiles() {
        Locator box = filesTable().locator(".ui-chkbox-all .ui-chkbox-box").first();
        Retry.untilPasses(() -> {
            String classes = box.getAttribute("class");
            if (classes == null || !TICKED.matcher(classes).find()) {
                box.click(new Locator.ClickOptions().setTimeout(5_000));
            }
            assertThat(box).hasClass(TICKED, new LocatorAssertions.HasClassOptions().setTimeout(2_000));
        }, 30_000);
    }

    public void publish() {
        Locator release = page.locator("[id=\"datasetForm:releaseDatasetButton\"]");
        openPopup(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Publish Dataset")), release);
        release.click();
        // Dataverse locks the dataset while it finalizes the publish, then
        // reloads the page. The Publish link vanishes as soon as the lock starts,
        // so wait for the released-version label ("Version 1.0") instead.
        assertThat(page.locator("#datasetVersionBlock .label-default", new Page.LocatorOptions()
                .setHasText(Pattern.compile("^\\s*Version \\d+\\.\\d+\\s*$"))))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(60_000));
        assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Publish Dataset"))).isHidden();
    }

    /**
     * True when Dataverse offers "Delete Dataset" — i.e. the dataset has never
     * been published. (The menu items are in the DOM even while it's closed.)
     */
    public boolean isDeletable() {
        return page.locator("[id=\"datasetForm:deleteDataset\"]").count() > 0;
    }

    public void delete() {
        Locator confirm = page.locator("[id=\"datasetForm:deleteConfirmation\"]")
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Continue"));
        openPopup(() -> openEditMenuItem("deleteDataset"), confirm);
        confirm.click();
        page.waitForURL(url -> !url.contains("dataset.xhtml"));
    }
}
