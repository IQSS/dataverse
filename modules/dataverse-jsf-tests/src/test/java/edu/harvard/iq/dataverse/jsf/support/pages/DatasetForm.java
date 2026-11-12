package edu.harvard.iq.dataverse.jsf.support.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import edu.harvard.iq.dataverse.jsf.support.Ajax;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The dataset metadata form — used both on the "New Dataset" page and when
 * editing metadata on an existing dataset or a dataset template (all three
 * render Dataverse's shared metadataFragment.xhtml).
 *
 * Fields are located by their field-type name or label, never by position in
 * the form or by JSF-generated ids (j_idt123), so they keep working when an
 * installation enables extra metadata blocks.
 */
public class DatasetForm {

    private final Page page;

    public DatasetForm(Page page) {
        this.page = page;
    }

    /**
     * The role="group" wrapper Dataverse renders around a compound field
     * (author, datasetContact, dsDescription, …), found by its stable
     * #metadata_&lt;fieldName&gt; anchor.
     */
    public Locator compoundField(String fieldName) {
        return page.locator("div.form-group[role=\"group\"]").filter(new Locator.FilterOptions()
                .setHas(page.locator("[id=\"metadata_" + fieldName + "\"]")));
    }

    /** A primitive field (title, subject, …) has no such anchor, so it is found by its exact label. */
    public Locator primitiveField(String label) {
        return page.locator("div.form-group[role=\"group\"]").filter(new Locator.FilterOptions()
                .setHas(page.locator("label.control-label", new Page.LocatorOptions()
                        .setHasText(Pattern.compile("^\\s*" + label + "\\s*$")))));
    }

    // Inputs are matched by the id suffix their label points at
    // (…:inputText, …:description), not by input type: on the template
    // page every field also has a hidden "Custom Instructions" text input.

    public Locator titleInput() {
        return primitiveField("Title").locator("[id$=\":inputText\"]").first();
    }

    public Locator descriptionInput() {
        return compoundField("dsDescription").locator("[id$=\":description\"]").first();
    }

    public Locator authorNameInput() {
        return compoundField("author").getByRole(AriaRole.TEXTBOX, new Locator.GetByRoleOptions()
                .setName(Pattern.compile("^Name\\b")));
    }

    public Locator contactEmailInput() {
        return compoundField("datasetContact").getByLabel(Pattern.compile("e-?mail", Pattern.CASE_INSENSITIVE));
    }

    public void fillTitle(String title) {
        titleInput().fill(title);
    }

    public void fillDescription(String text) {
        descriptionInput().fill(text);
    }

    /** Picks one value in the Subject multi-select (a PrimeFaces selectCheckboxMenu). */
    public void selectSubject(String subject) {
        Locator menu = primitiveField("Subject").locator(".ui-selectcheckboxmenu").first();
        Locator container = menu.locator(".ui-selectcheckboxmenu-multiple-container");
        container.click();

        // PrimeFaces renders the option panel as <menu id>_panel, often
        // appended to <body>, so it can't be found by scoping inside the field.
        String menuId = menu.getAttribute("id");
        Locator panel = page.locator("[id=\"" + menuId + "_panel\"]");
        assertThat(panel).isVisible();
        panel.getByText(subject, new Locator.GetByTextOptions().setExact(true)).click();

        // Close the panel so it doesn't cover the fields below it.
        container.click();
        assertThat(panel).isHidden();
    }

    /**
     * Uploads files via the form's file input and waits until each has a row
     * in the staged-files table. (A .zip is unpacked into several rows, which
     * still satisfies "at least one row per uploaded file".)
     */
    public void uploadFiles(List<Path> paths) {
        page.locator("[id=\"datasetForm:fileUpload_input\"]").setInputFiles(paths.toArray(Path[]::new));
        assertThat(page.locator("[id=\"datasetForm:filesTable:" + (paths.size() - 1) + ":fileName\"]"))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(60_000));
        Ajax.waitForAjaxIdle(page);
    }

    /** Clicks "Save Dataset" on the create page and waits for the dataset page. */
    public void saveNewDataset() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save Dataset")).click();
        assertThat(page.getByText("This dataset has been created."))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(60_000));
    }
}
