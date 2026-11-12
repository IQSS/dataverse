package edu.harvard.iq.dataverse.jsf.support.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** A collection's Dataset Templates page (manage-templates.xhtml). */
public class TemplatesPage {

    private final Page page;

    public TemplatesPage(Page page) {
        this.page = page;
    }

    /** Opens Edit → Dataset Templates for a collection (the root collection if alias is null). */
    public void open(String collectionAlias) {
        CollectionPage collection = new CollectionPage(page);
        collection.open(collectionAlias);
        collection.openEditMenuItem("manageTemplates");
        assertThat(createButton()).isVisible();
    }

    public Locator createButton() {
        return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Create Dataset Template")).first();
    }

    /** The table row for a template, found by its (unique) name. */
    public Locator row(String name) {
        return page.locator("[id=\"manageTemplatesForm:allTemplates_data\"] tr").filter(new Locator.FilterOptions()
                .setHas(page.locator("td[role=\"gridcell\"]", new Page.LocatorOptions().setHasText(name))));
    }

    /** The row's action buttons cell (Make Default, View, Copy, Edit, Delete). */
    public Locator actions(String name) {
        return row(name).locator("td.col-manage-action");
    }

    public void makeDefault(String name) {
        actions(name).getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Make Default").setExact(true))
                .click();
        assertThat(page.getByText("The template has been selected as the default template")).isVisible();
    }

    public void unsetDefault(String name) {
        actions(name).getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Default").setExact(true))
                .click();
        assertThat(actions(name).getByRole(AriaRole.LINK, new Locator.GetByRoleOptions()
                .setName("Make Default").setExact(true))).isVisible();
    }

    public void delete(String name) {
        actions(name).locator("[data-original-title=\"Delete\"]").click();
        page.locator("[id=\"manageTemplatesForm:contDeleteTemplateBtn\"]").click();
        assertThat(row(name)).hasCount(0);
    }
}
