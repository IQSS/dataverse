package edu.harvard.iq.dataverse.jsf.datasets;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.CreateDatasetOptions;
import edu.harvard.iq.dataverse.jsf.support.JsfFixtures;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import edu.harvard.iq.dataverse.jsf.support.TestData;
import edu.harvard.iq.dataverse.jsf.support.pages.DatasetPage;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;

/**
 * Upload a batch of files while creating a dataset and check every one of
 * them is listed afterwards. Tabular-ingest formats get their own test so an
 * ingest failure can't mask a plain-upload failure (or vice versa).
 */
public class FileUploadJT {

    @JsfTest
    @DisplayName("File upload: non-ingest formats")
    public void testNonIngestFormats(Page page, JsfFixtures fixtures) {
        uploadAndCheck(page, fixtures,
                List.of(TestData.CSV, TestData.ZIP, TestData.PDF, TestData.R_SCRIPT, TestData.RO_CRATE),
                // demo-archive.zip is unpacked on upload into readme.txt and data.csv.
                List.of("sample-data.csv", "readme.txt", "data.csv", "demo-document.pdf", "demo-code.R",
                        "ro-crate-metadata.json"));
    }

    @JsfTest
    @DisplayName("File upload: tabular ingest formats")
    public void testTabularIngestFormats(Page page, JsfFixtures fixtures) {
        uploadAndCheck(page, fixtures,
                List.of(TestData.STATA, TestData.R_DATA, TestData.SPSS, TestData.EXCEL),
                List.of("demo-data.dta", "demo-data.RData", "demo-data.sav", "demo-data.xlsx"));
    }

    private static void uploadAndCheck(Page page, JsfFixtures fixtures, List<Path> upload, List<String> expected) {
        fixtures.createDataset(new CreateDatasetOptions().setFiles(upload));
        Locator filesTable = new DatasetPage(page).filesTable();
        for (String filename : expected) {
            assertThat(filesTable.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions()
                    .setName(filename).setExact(true))).isVisible();
        }
    }
}
