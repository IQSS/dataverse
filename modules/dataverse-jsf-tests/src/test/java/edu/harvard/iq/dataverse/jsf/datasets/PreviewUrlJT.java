package edu.harvard.iq.dataverse.jsf.datasets;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static edu.harvard.iq.dataverse.jsf.support.Popup.openPopup;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.CreateDatasetOptions;
import edu.harvard.iq.dataverse.jsf.support.JsfFixtures;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import edu.harvard.iq.dataverse.jsf.support.TestData;
import edu.harvard.iq.dataverse.jsf.support.pages.DatasetPage;
import org.junit.jupiter.api.DisplayName;

public class PreviewUrlJT {

    @JsfTest
    @DisplayName("Preview URL: create, open anonymously, disable")
    public void testPreviewUrl(Page page, JsfFixtures fixtures) {
        fixtures.createDataset(new CreateDatasetOptions().setFiles(TestData.SAMPLE_TEXT, TestData.SAMPLE_TEXT_2));
        DatasetPage datasetPage = new DatasetPage(page);
        Locator create = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName("Create General Preview URL"));
        openPopup(() -> datasetPage.openEditMenuItem("privateUrl"), create);
        create.click();
        Locator urlText = page.locator("div.highlight p span");
        assertThat(urlText).containsText("previewurl.xhtml");
        String previewUrl = urlText.innerText().trim();
        assertThat(previewUrl, containsString("token="));

        fixtures.step("the URL works for someone who isn't logged in", () -> {
            // Opening it as the owner just redirects to the dataset, so use a
            // fresh context with no session cookies.
            try (BrowserContext anonymous = fixtures.browser().newContext()) {
                Page preview = anonymous.newPage();
                preview.navigate(previewUrl);
                assertThat(preview.locator("#messagePanel").getByText("Unpublished Dataset Preview URL")).isVisible();
                // The visitor's message (dataset.privateurl.infoMessageReviewer); "Privately
                // share this draft dataset…" is only shown to the dataset's author.
                assertThat(preview.getByText("You are viewing a preview of this unpublished dataset version."))
                        .isVisible();
            }
        });

        fixtures.step("disable it", () -> {
            // The Preview URL dialog is still open on the original page.
            Locator confirm = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                    .setName("Yes, Disable General Preview URL"));
            openPopup(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                    .setName("Disable General Preview URL").setExact(true)), confirm);
            confirm.click();
            assertThat(page.getByText("You have successfully disabled the Preview URL for this unpublished dataset."))
                    .isVisible();
        });
    }
}
