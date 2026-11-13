package edu.harvard.iq.dataverse.jsf.datasets;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.DatasetRef;
import edu.harvard.iq.dataverse.jsf.support.JsfFixtures;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import edu.harvard.iq.dataverse.jsf.support.JsfTestConfig;
import edu.harvard.iq.dataverse.jsf.support.Naming;
import edu.harvard.iq.dataverse.jsf.support.TestData;
import edu.harvard.iq.dataverse.jsf.support.pages.CollectionPage;
import edu.harvard.iq.dataverse.jsf.support.pages.DatasetForm;
import edu.harvard.iq.dataverse.jsf.support.pages.DatasetPage;
import edu.harvard.iq.dataverse.jsf.support.pages.TemplatesPage;
import org.junit.jupiter.api.DisplayName;

public class DefaultCustomLicenseJT {

    @JsfTest
    @DisplayName("New datasets inherit custom terms from the collection's default template")
    public void testDefaultTemplateCustomTerms(Page page, JsfFixtures fixtures) {
        assumeTrue(JsfTestConfig.customLicenseEnabled(),
                "Set -Ddataverse.test.jsf.customlicense=true (CUSTOM_LICENSE_ENABLED=true) to run");

        // Everything happens in a throwaway collection, so the default template
        // never affects the root collection.
        String alias = fixtures.createCollection().alias();
        String templateName = Naming.uniqueName("Playwright License Template");
        String templateTitle = templateName + " title";
        String customTerms = "All Rights Reserved";
        TemplatesPage templates = new TemplatesPage(page);
        DatasetForm form = new DatasetForm(page);

        fixtures.cleanup().add("delete template \"" + templateName + "\"", p -> {
            TemplatesPage leftovers = new TemplatesPage(p);
            leftovers.open(alias);
            if (leftovers.row(templateName).count() > 0) {
                leftovers.delete(templateName);
            }
        });

        fixtures.step("create a template with custom terms", () -> {
            templates.open(alias);
            templates.createButton().click();
            page.locator("[id$=\":templateName\"]").fill(templateName);
            form.fillTitle(templateTitle);
            form.authorNameInput().fill("Playwright Tester");
            form.contactEmailInput().fill("playwright@example.org");
            form.fillDescription(TestData.DEFAULT_DESCRIPTION);
            form.selectSubject(TestData.DEFAULT_SUBJECT);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save + Add Terms")).click();
            assertThat(page.getByText("Template has been created.")).isVisible();

            page.locator("[id=\"templateForm:licenses_label\"]").click();
            page.locator("[id=\"templateForm:licenses_items\"]")
                    .getByText("Custom Dataset Terms", new Locator.GetByTextOptions().setExact(true))
                    .click();
            page.locator("[id=\"templateForm:dlTermsdOfUse\"]").fill(customTerms);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save Dataset Template")).click();
            assertThat(page.getByText("Template has been edited and saved.")).isVisible();
        });

        templates.makeDefault(templateName);

        fixtures.step("a new dataset picks the terms up", () -> {
            CollectionPage collection = new CollectionPage(page);
            collection.open(alias);
            collection.startNewDataset();
            form.saveNewDataset();
            fixtures.trackDataset(new DatasetRef(templateTitle, page.url()));

            assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions()
                    .setName(templateTitle).setExact(true))).isVisible();
            new DatasetPage(page).tab(DatasetPage.Tab.TERMS).click();
            Locator terms = page.locator("[id=\"datasetForm:tabView:touFragment\"]");
            assertThat(terms.getByText("Custom Dataset Terms")).isVisible();
            assertThat(terms.getByText(customTerms, new Locator.GetByTextOptions().setExact(true))).isVisible();
        });

        fixtures.step("unset the default", () -> {
            templates.open(alias);
            templates.unsetDefault(templateName);
        });
    }
}
