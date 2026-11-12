package edu.harvard.iq.dataverse.jsf.support;

import com.microsoft.playwright.Page;
import java.util.function.Consumer;

/** Options for {@link JsfFixtures#createCollection(CreateCollectionOptions)}. */
public class CreateCollectionOptions {

    /** Parent collection alias. Defaults to the root collection. */
    public String parent;
    /** Defaults to a unique "pw-…" alias. */
    public String alias;
    /** Option value of the Category dropdown. Defaults to "DEPARTMENT". */
    public String category;
    /**
     * Fills in extra fields on the create form before it is submitted (e.g. a
     * feature-specific setting). The form is already on screen.
     */
    public Consumer<Page> customize;

    public CreateCollectionOptions setParent(String parent) {
        this.parent = parent;
        return this;
    }

    public CreateCollectionOptions setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    public CreateCollectionOptions setCategory(String category) {
        this.category = category;
        return this;
    }

    public CreateCollectionOptions setCustomize(Consumer<Page> customize) {
        this.customize = customize;
        return this;
    }
}
