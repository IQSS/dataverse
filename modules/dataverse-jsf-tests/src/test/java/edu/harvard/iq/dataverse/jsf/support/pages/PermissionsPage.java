package edu.harvard.iq.dataverse.jsf.support.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static edu.harvard.iq.dataverse.jsf.support.Popup.openPopup;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.Ajax;
import java.util.regex.Pattern;

/**
 * permissions-manage.xhtml — the same page serves collections and datasets.
 *
 * Users and groups are identified the way Dataverse shows them in the ID
 * column: "@username" for a user, ":authenticated-users" for a built-in group.
 */
public class PermissionsPage {

    private final Page page;

    public PermissionsPage(Page page) {
        this.page = page;
    }

    public Locator assignButton() {
        return page.locator("[id=\"rolesPermissionsForm:userGroupsAdd\"]");
    }

    public Locator assignedRoles() {
        return page.locator("[id=\"rolesPermissionsForm:assignedRoles\"]");
    }

    /** The assigned-roles row for one user/group + role pair. */
    public Locator assignment(String assignee, String role) {
        return assignedRoles().locator("tbody tr")
                .filter(new Locator.FilterOptions().setHasText(assignee))
                .filter(new Locator.FilterOptions().setHasText(role));
    }

    /**
     * The "Users/Groups" panel starts collapsed on a collection's permissions
     * page (it is expanded on a dataset's), so open it if necessary.
     */
    public void showUsersAndGroups() {
        Locator panel = page.locator("#panelCollapseUsersGroups");
        if (!panel.isVisible()) {
            page.locator("[data-target=\"#panelCollapseUsersGroups\"]").click();
        }
        assertThat(assignButton()).isVisible();
    }

    public void assignRole(String assignee, String role) {
        showUsersAndGroups();
        Locator dialog = page.locator("[id=\"rolesPermissionsForm:userGroupDialog\"]");
        Locator input = dialog.locator("input[id*='userGroupAutoComplete_input']");
        openPopup(assignButton(), input);
        input.pressSequentially(assignee, new Locator.PressSequentiallyOptions().setDelay(50));

        Locator suggestion = page.locator(".ui-autocomplete-item").first();
        assertThat(suggestion).isVisible();
        suggestion.click();
        Ajax.waitForAjaxIdle(page);

        // PrimeFaces radios: the real <input> is visually hidden, so click the
        // styled box that belongs to the label with the role's name.
        Locator label = dialog.locator("label", new Locator.LocatorOptions()
                .setHasText(Pattern.compile("^\\s*" + role + "\\s*$")));
        String inputId = label.getAttribute("for");
        page.locator("[id=\"" + inputId + "\"]")
                .locator("xpath=ancestor::div[contains(@class,'ui-radiobutton')][1]")
                .locator(".ui-radiobutton-box")
                .click();

        dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save Changes")).click();
        assertThat(dialog).isHidden();
        assertThat(assignment(assignee, role)).isVisible();
    }

    public void removeRole(String assignee, String role) {
        showUsersAndGroups();
        Locator confirm = page.locator("[id=\"rolesPermissionsForm:accessRemoveConfirm\"]")
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Continue"));
        openPopup(assignment(assignee, role).getByRole(AriaRole.LINK, new Locator.GetByRoleOptions()
                .setName(Pattern.compile("Remove"))), confirm);
        confirm.click();
        assertThat(assignment(assignee, role)).hasCount(0);
    }
}
