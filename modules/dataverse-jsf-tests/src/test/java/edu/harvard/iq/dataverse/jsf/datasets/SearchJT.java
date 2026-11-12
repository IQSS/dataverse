package edu.harvard.iq.dataverse.jsf.datasets;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.DatasetRef;
import edu.harvard.iq.dataverse.jsf.support.JsfFixtures;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import edu.harvard.iq.dataverse.jsf.support.Retry;
import edu.harvard.iq.dataverse.jsf.support.pages.CollectionPage;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;

public class SearchJT {

    @JsfTest
    @DisplayName("Search for a dataset by title, then run an advanced search")
    public void testSearch(Page page, JsfFixtures fixtures) {
        DatasetRef dataset = fixtures.createDataset();
        CollectionPage collection = new CollectionPage(page);
        // Each search result links to the dataset twice (title and thumbnail).
        Locator result = page.locator("#resultsTable").getByRole(AriaRole.LINK,
                new Locator.GetByRoleOptions().setName(dataset.title()).setExact(true)).first();

        fixtures.step("basic search finds the dataset", () -> {
            // Indexing is asynchronous, so retry the search until Solr has caught up.
            Retry.untilPasses(() -> {
                collection.open();
                page.getByPlaceholder("Search this dataverse").fill("\"" + dataset.title() + "\"");
                page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Find")).click();
                assertThat(result).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(5_000));
            }, 60_000);
        });

        fixtures.step("advanced search reflects the query in the URL", () -> {
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Advanced Search")).click();
            String[] words = dataset.title().split(" ");
            String keyword = words[words.length - 1];
            page.locator("[id=\"advancedSearchForm:dvFieldName\"]").fill(keyword);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Find")).last().click();
            assertThat(page).hasURL(Pattern.compile(keyword));
        });
    }
}
