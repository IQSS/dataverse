package edu.harvard.iq.dataverse.jsf.datasets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.CreateDatasetOptions;
import edu.harvard.iq.dataverse.jsf.support.JsfFixtures;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import edu.harvard.iq.dataverse.jsf.support.TestData;
import edu.harvard.iq.dataverse.jsf.support.pages.DatasetPage;
import org.junit.jupiter.api.DisplayName;

public class DownloadFilesJT {

    @JsfTest
    @DisplayName("Download all of a dataset's files")
    public void testDownloadAllFiles(Page page, JsfFixtures fixtures) {
        // Two files: with two or more selected, Dataverse offers a single zip
        // "Download" button.
        fixtures.createDataset(new CreateDatasetOptions().setFiles(TestData.SAMPLE_TEXT, TestData.SAMPLE_TEXT_2));
        new DatasetPage(page).selectAllFiles();

        Download download = page.waitForDownload(
                () -> page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Download")).click());
        assertThat(download.suggestedFilename(), not(emptyOrNullString()));
    }
}
