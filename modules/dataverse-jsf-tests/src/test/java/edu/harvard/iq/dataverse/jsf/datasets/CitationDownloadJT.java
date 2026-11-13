package edu.harvard.iq.dataverse.jsf.datasets;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import edu.harvard.iq.dataverse.jsf.support.JsfFixtures;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.DisplayName;

public class CitationDownloadJT {

    @JsfTest
    @DisplayName("Citation: DOI shown, EndNote/RIS/BibTeX downloads")
    public void testCitationDownloads(Page page, JsfFixtures fixtures) {
        assumeFalse(fixtures.browserName().equals("webkit"), "WebKit opens XML/RIS inline instead of downloading");

        fixtures.createDataset();
        assertThat(page.getByText("https://doi.org/")).isVisible();

        fixtures.step("EndNote XML", () -> {
            String xml = downloadCitation(page, "endNoteLink");
            assertThat(xml, containsString("<?xml"));
            assertThat(xml, containsString("<records>"));
            assertThat(xml, containsString("<record>"));
            assertThat(xml, containsString("doi"));
        });

        fixtures.step("RIS", () -> {
            String ris = downloadCitation(page, "risLink");
            assertThat(ris, containsString("TY  - DATA"));
            assertThat(ris, containsString("DO  - doi:"));
            assertThat(ris, containsString("ER  -"));
        });

        fixtures.step("BibTeX (opens in a new tab)", () -> {
            openCiteMenu(page, "bibLink");
            Page tab = page.context().waitForPage(() -> page.locator("[id=\"datasetForm:bibLink\"]").click());
            tab.waitForLoadState(LoadState.DOMCONTENTLOADED);
            String bib = tab.content();
            tab.close();
            assertThat(bib, containsString("@data"));
            assertThat(bib, containsString("doi = {"));
            assertThat(bib, containsString("url = {https://doi.org/"));
        });
    }

    /** Opens the Cite Dataset menu and waits for a given entry to be clickable. */
    private static void openCiteMenu(Page page, String entryId) {
        page.locator("button.downloadCitation").click();
        assertThat(page.locator("[id=\"datasetForm:" + entryId + "\"]")).isVisible();
    }

    private static String downloadCitation(Page page, String entryId) {
        openCiteMenu(page, entryId);
        Download download = page.waitForDownload(() -> page.locator("[id=\"datasetForm:" + entryId + "\"]").click());
        try {
            return Files.readString(download.path(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
