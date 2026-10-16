package edu.harvard.iq.dataverse.jsf.datasets;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.CreateDatasetOptions;
import edu.harvard.iq.dataverse.jsf.support.DatasetRef;
import edu.harvard.iq.dataverse.jsf.support.JsfFixtures;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import edu.harvard.iq.dataverse.jsf.support.Retry;
import edu.harvard.iq.dataverse.jsf.support.pages.CollectionPage;
import edu.harvard.iq.dataverse.jsf.support.pages.DatasetPage;
import org.junit.jupiter.api.DisplayName;

public class BrowseJT {

    @JsfTest
    @DisplayName("Browse to a dataset from its collection and open its metadata")
    public void testBrowseToDataset(Page page, JsfFixtures fixtures) {
        // Browsing a dedicated collection means the listing contains only what
        // this test created — no dependence on whatever else is on the instance.
        String alias = fixtures.createCollection().alias();
        DatasetRef dataset = fixtures.createDataset(new CreateDatasetOptions().setCollection(alias));

        CollectionPage collection = new CollectionPage(page);
        // Each search result links to the dataset twice (title and thumbnail).
        Locator listing = page.locator("#resultsTable").getByRole(AriaRole.LINK,
                new Locator.GetByRoleOptions().setName(dataset.title()).setExact(true)).first();

        // Collection listings come from the search index, which updates
        // asynchronously, so retry until the new dataset shows up.
        Retry.untilPasses(() -> {
            collection.open(alias);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sort")).click();
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Name")).first().click();
            page.locator("a.facetTypeChBox[href*=\"types=datasets\"]").click();
            assertThat(listing).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(5_000));
        }, 60_000);

        listing.click();
        assertThat(page.locator("#title")).hasText(dataset.title());

        new DatasetPage(page).tab(DatasetPage.Tab.METADATA).click();
        assertThat(page.locator("[id=\"datasetForm:tabView:metadataMapTab\"]")).isVisible();
    }
}
