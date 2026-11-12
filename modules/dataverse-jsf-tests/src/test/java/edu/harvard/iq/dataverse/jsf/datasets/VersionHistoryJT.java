package edu.harvard.iq.dataverse.jsf.datasets;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.JsfFixtures;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import edu.harvard.iq.dataverse.jsf.support.pages.DatasetPage;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;

/**
 * Needs a published version 1.0, so — like the lifecycle test — this leaves
 * one published dataset behind in the root collection per run.
 */
public class VersionHistoryJT {

    @JsfTest
    @DisplayName("View a dataset's version history")
    public void testVersionHistory(Page page, JsfFixtures fixtures) {
        fixtures.createDataset();
        DatasetPage dataset = new DatasetPage(page);
        dataset.publish();

        dataset.tab(DatasetPage.Tab.VERSIONS).click();
        Locator versions = page.locator("[id=\"datasetForm:tabView:versionsTab\"]");
        versions.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("1.0").setExact(true)).click();
        assertThat(page).hasURL(Pattern.compile("version=1\\.0"));
    }
}
