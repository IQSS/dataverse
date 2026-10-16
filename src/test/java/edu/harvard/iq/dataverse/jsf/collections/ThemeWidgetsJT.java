package edu.harvard.iq.dataverse.jsf.collections;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.Ajax;
import edu.harvard.iq.dataverse.jsf.support.JsfFixtures;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import edu.harvard.iq.dataverse.jsf.support.TestData;
import edu.harvard.iq.dataverse.jsf.support.pages.CollectionPage;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;

public class ThemeWidgetsJT {

    private static final String THEME_SAVED = "You have successfully updated the theme for this dataverse!";

    @JsfTest
    @DisplayName("Theme: upload images and set tagline and website")
    public void testThemeImagesTaglineWebsite(Page page, JsfFixtures fixtures) {
        String alias = fixtures.createCollection().alias();
        openThemeEditor(page, alias);

        uploadAllImages(page);
        themeInput(page, "tagline").fill("Created by the Playwright suite");
        themeInput(page, "website").fill("https://dataverse.org");
        saveTheme(page);
    }

    @JsfTest
    @DisplayName("Theme: remove and replace the thumbnail image")
    public void testRemoveAndReplaceThumbnail(Page page, JsfFixtures fixtures) {
        String alias = fixtures.createCollection().alias();
        openThemeEditor(page, alias);
        uploadAllImages(page);
        saveTheme(page);

        fixtures.step("remove the thumbnail", () -> {
            openThemeEditor(page, alias);
            Locator thumbnailSection = page.locator(".form-group").filter(new Locator.FilterOptions()
                    .setHas(page.locator("label[for=\"thumbnailFormat\"]")));
            thumbnailSection.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Remove")).click();
            assertThat(themeInput(page, "uploadlogoThumbnail_input")).isAttached();
            saveTheme(page);
        });

        fixtures.step("upload a replacement thumbnail", () -> {
            openThemeEditor(page, alias);
            uploadImage(page, "uploadlogoThumbnail", TestData.LOGO_IMAGE);
            saveTheme(page);
        });
    }

    private static Locator themeInput(Page page, String id) {
        return page.locator("[id=\"themeWidgetsForm:themeWidgetsTabView:" + id + "\"]");
    }

    private static void openThemeEditor(Page page, String alias) {
        CollectionPage collection = new CollectionPage(page);
        collection.open(alias);
        collection.openEditMenuItem("themeWidgetsOpts");
        assertThat(themeInput(page, "tagline")).isVisible();
    }

    /** Each image upload re-renders the tab via AJAX; let it settle before the next one. */
    private static void uploadImage(Page page, String inputId, Path file) {
        themeInput(page, inputId + "_input").setInputFiles(file);
        Ajax.waitForAjaxIdle(page);
    }

    private static void saveTheme(Page page) {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save Changes")).click();
        assertThat(new CollectionPage(page).successAlert()).containsText(THEME_SAVED);
    }

    private static void uploadAllImages(Page page) {
        uploadImage(page, "uploadlogo", TestData.LOGO_IMAGE);
        uploadImage(page, "uploadlogoThumbnail", TestData.THUMBNAIL_IMAGE);
        uploadImage(page, "uploadlogoFooter", TestData.FOOTER_IMAGE);
    }
}
