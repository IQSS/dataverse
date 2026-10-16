package edu.harvard.iq.dataverse.jsf.datasets;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.Ajax;
import edu.harvard.iq.dataverse.jsf.support.CreateCollectionOptions;
import edu.harvard.iq.dataverse.jsf.support.CreateDatasetOptions;
import edu.harvard.iq.dataverse.jsf.support.JsfFixtures;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import edu.harvard.iq.dataverse.jsf.support.JsfTestConfig;
import edu.harvard.iq.dataverse.jsf.support.TestData;
import edu.harvard.iq.dataverse.jsf.support.pages.DatasetPage;
import org.junit.jupiter.api.DisplayName;

public class LocallyFairDownloadJT {

    @JsfTest
    @DisplayName("Download a dataset's files in a Locally FAIR collection")
    public void testLocallyFairDownload(Page page, JsfFixtures fixtures) {
        assumeTrue(JsfTestConfig.locallyFairEnabled(),
                "Set -Ddataverse.test.jsf.locallyfair=true (LOCALLY_FAIR_ENABLED=true) to run");

        String alias = fixtures.createCollection(new CreateCollectionOptions().setCustomize(form -> {
            // Locally FAIR adds a contact autocomplete to the collection form.
            Locator contact = form.locator("[id=\"dataverseForm:userGroupNameAssign:userGroupAutoComplete_input\"]");
            contact.pressSequentially(JsfTestConfig.credentials().username(),
                    new Locator.PressSequentiallyOptions().setDelay(50));
            Locator suggestion = form.locator(".ui-autocomplete-item").first();
            assertThat(suggestion).isVisible();
            suggestion.click();
            Ajax.waitForAjaxIdle(form);
        })).alias();

        fixtures.createDataset(new CreateDatasetOptions()
                .setCollection(alias)
                .setFiles(TestData.SAMPLE_TEXT, TestData.SAMPLE_TEXT_2));
        new DatasetPage(page).selectAllFiles();

        Download download = page.waitForDownload(
                () -> page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Download")).click());
        assertThat(download.suggestedFilename(), not(emptyOrNullString()));
    }
}
