package edu.harvard.iq.dataverse.jsf.collections;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import edu.harvard.iq.dataverse.jsf.support.CollectionRef;
import edu.harvard.iq.dataverse.jsf.support.CreateCollectionOptions;
import edu.harvard.iq.dataverse.jsf.support.JsfFixtures;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;

/**
 * Creating a collection: the Metadata Fields and Browse/Search Facets
 * "inherit from parent" checkboxes lock and unlock their option lists, and the
 * collection is created at the chosen alias. The collection is deleted again
 * afterwards by createCollection's cleanup.
 */
public class CreateCollectionJT {

    private static final Pattern DISABLED = Pattern.compile("ui-state-disabled");

    @JsfTest
    @DisplayName("Create a collection")
    public void testCreateCollection(Page page, JsfFixtures fixtures) {
        CollectionRef collection = fixtures.createCollection(new CreateCollectionOptions().setCustomize(form -> {
            fixtures.step("metadata-field checkboxes follow the inherit toggle", () -> {
                Locator inherit = form.locator("[id=\"dataverseForm:metadataRoot\"]");
                Locator blocks = form.locator(
                        "[id=\"dataverseForm:optionBlock\"] div.checkbox label input[type=\"checkbox\"]");
                assertThat(inherit).isChecked();
                int count = blocks.count();
                assertThat(count, greaterThan(0));
                for (int i = 0; i < count; i++) {
                    assertThat(blocks.nth(i)).isDisabled();
                }

                inherit.uncheck();
                // The citation block is mandatory, so it stays locked.
                assertThat(blocks.nth(0)).isDisabled();
                for (int i = 1; i < count; i++) {
                    assertThat(blocks.nth(i)).isEnabled();
                }

                inherit.check();
                Locator confirm = form.locator("button:visible").filter(new Locator.FilterOptions()
                        .setHas(form.locator("span", new Page.LocatorOptions().setHasText("Continue"))));
                confirm.click();
                for (int i = 0; i < count; i++) {
                    assertThat(blocks.nth(i)).isDisabled();
                }
            });

            fixtures.step("facet picker follows the inherit toggle", () -> {
                Locator inherit = form.locator("[id=\"dataverseForm:facetsRoot\"]");
                Locator picker = form.locator("[id=\"dataverseForm:editFacets\"]")
                        .locator("div.ui-picklist-list-wrapper div.ui-widget-header > div")
                        .first();
                assertThat(inherit).isChecked();
                assertThat(picker).hasClass(DISABLED);
                inherit.uncheck();
                assertThat(picker).not().hasClass(DISABLED);
                inherit.check();
                assertThat(picker).hasClass(DISABLED);
            });

            form.locator("[id=\"dataverseForm:description\"]")
                    .fill("Created by the <b>Playwright</b> suite; deleted automatically.");
        }));

        assertThat(page).hasURL(Pattern.compile(collection.path() + "/?([?#]|$)"));
        assertThat(page.locator("a[data-original-title=\"Email Dataverse Contact\"]")).isVisible();
    }
}
