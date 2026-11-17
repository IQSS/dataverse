package edu.harvard.iq.dataverse.jsf.collections;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.JsfFixtures;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import edu.harvard.iq.dataverse.jsf.support.Naming;
import edu.harvard.iq.dataverse.jsf.support.TestData;
import edu.harvard.iq.dataverse.jsf.support.pages.DatasetForm;
import edu.harvard.iq.dataverse.jsf.support.pages.TemplatesPage;
import org.junit.jupiter.api.DisplayName;

public class TemplatesJT {

    @JsfTest
    @DisplayName("Dataset template: create, rename, make default, copy, delete")
    public void testTemplateLifecycle(Page page, JsfFixtures fixtures) {
        String alias = fixtures.createCollection().alias();
        String name = Naming.uniqueName("Playwright Template");
        String renamed = Naming.uniqueName("Renamed Template");
        String copy = "Copy of " + renamed;

        // The test deletes its templates itself; this only matters if it fails
        // part-way. (Registered after the collection, so it runs before the
        // collection is deleted.)
        fixtures.cleanup().add("delete leftover templates", p -> {
            TemplatesPage leftovers = new TemplatesPage(p);
            leftovers.open(alias);
            for (String t : new String[]{copy, renamed, name}) {
                if (leftovers.row(t).count() > 0) {
                    leftovers.delete(t);
                }
            }
        });

        TemplatesPage templates = new TemplatesPage(page);
        templates.open(alias);

        fixtures.step("create", () -> {
            templates.createButton().click();
            DatasetForm form = new DatasetForm(page);
            page.locator("[id$=\":templateName\"]").fill(name);
            form.fillTitle(name + " title");
            form.authorNameInput().fill("Playwright Tester");
            form.contactEmailInput().fill("playwright@example.org");
            form.fillDescription(TestData.DEFAULT_DESCRIPTION);
            form.selectSubject(TestData.DEFAULT_SUBJECT);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save + Add Terms")).click();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save Dataset Template")).click();
            assertThat(templates.row(name)).isVisible();
        });

        fixtures.step("rename", () -> {
            templates.actions(name).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions()
                    .setName("Edit Template")).click();
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Metadata")).click();
            page.locator("[id$=\":templateName\"]").fill(renamed);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save Changes")).click();
            assertThat(templates.row(renamed)).isVisible();
        });

        templates.makeDefault(renamed);

        fixtures.step("copy", () -> {
            templates.actions(renamed).locator("[data-original-title=\"Copy\"]").click();
            // Copy opens the edit form — save it unchanged.
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save Changes")).click();
            assertThat(page.getByText("Template has been edited and saved")).isVisible();
            assertThat(templates.row(copy)).isVisible();
        });

        fixtures.step("delete both", () -> {
            templates.delete(copy);
            templates.delete(renamed);
        });
    }
}
