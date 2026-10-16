package edu.harvard.iq.dataverse.jsf.datasets;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.CreateDatasetOptions;
import edu.harvard.iq.dataverse.jsf.support.DatasetRef;
import edu.harvard.iq.dataverse.jsf.support.JsfFixtures;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import edu.harvard.iq.dataverse.jsf.support.TestData;
import edu.harvard.iq.dataverse.jsf.support.pages.DatasetForm;
import edu.harvard.iq.dataverse.jsf.support.pages.DatasetPage;
import org.junit.jupiter.api.DisplayName;

/**
 * Create → edit metadata → edit file metadata → replace a file → publish.
 *
 * The dataset ends up published, which Dataverse's UI cannot delete, so this
 * test leaves one published dataset behind in the root collection per run.
 */
public class DatasetLifecycleJT {

    @JsfTest
    @DisplayName("Dataset lifecycle: create, edit, replace a file, publish")
    public void testDatasetLifecycle(Page page, JsfFixtures fixtures) {
        DatasetRef dataset = fixtures.createDataset(new CreateDatasetOptions()
                .setFiles(TestData.SAMPLE_TEXT, TestData.SAMPLE_TEXT_2));
        DatasetPage datasetPage = new DatasetPage(page);
        DatasetForm form = new DatasetForm(page);
        String newTitle = dataset.title() + " (edited)";

        fixtures.step("edit dataset metadata", () -> {
            datasetPage.openEditMenuItem("editMetadata");
            form.fillTitle(newTitle);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save Changes")).last().click();
            assertThat(page.locator("#title")).hasText(newTitle);
        });

        fixtures.step("edit file metadata", () -> {
            datasetPage.selectAllFiles();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Edit Files")).first().click();
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Metadata")).last().click();
            page.locator("[name=\"datasetForm:filesTable:0:fileDescription\"]").fill("An edited file description.");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save Changes")).last().click();
            assertThat(datasetPage.filesTable()).isVisible();
        });

        fixtures.step("replace a file", () -> {
            datasetPage.filesTable().getByRole(AriaRole.LINK, new Locator.GetByRoleOptions()
                    .setName("sample-dataset-file.txt").setExact(true)).click();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Edit File")).first().click();
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Replace")).click();
            page.locator("[id=\"datasetForm:fileUpload_input\"]").setInputFiles(TestData.REPLACEMENT_TEXT);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save Changes")).last().click();
        });

        fixtures.step("publish", () -> {
            datasetPage.open(dataset.url());
            assertThat(datasetPage.filesTable().getByRole(AriaRole.LINK, new Locator.GetByRoleOptions()
                    .setName("replaced-sample-dataset-file.txt").setExact(true))).isVisible();
            datasetPage.publish();
        });
    }
}
