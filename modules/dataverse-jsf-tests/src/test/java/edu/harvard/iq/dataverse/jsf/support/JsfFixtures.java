package edu.harvard.iq.dataverse.jsf.support;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import edu.harvard.iq.dataverse.jsf.support.pages.CollectionForm;
import edu.harvard.iq.dataverse.jsf.support.pages.CollectionPage;
import edu.harvard.iq.dataverse.jsf.support.pages.DatasetForm;
import edu.harvard.iq.dataverse.jsf.support.pages.DatasetPage;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * What a {@link JsfTest} method gets on top of its {@link Page}:
 *
 * <pre>
 *   cleanup()           Register teardown work. It runs after the test —
 *                       pass or fail — in a fresh tab, newest first.
 *   createCollection()  Create a uniquely-named child collection. It is
 *                       deleted automatically after the test.
 *   createDataset()     Create a uniquely-named draft dataset. It is deleted
 *                       automatically after the test unless it was published
 *                       (Dataverse offers no UI to delete a published dataset).
 *   trackDataset()      Hand a dataset the test itself created (because
 *                       creation is what it's testing) to the same cleanup.
 *   step()              Group actions under a name in the trace and the log.
 * </pre>
 *
 * Tests must never rely on data another test left behind, and must never pick
 * "the first row" of a listing: create what you need with these methods and
 * refer to it by the name/URL they return.
 */
public final class JsfFixtures {

    private static final Logger logger = Logger.getLogger(JsfFixtures.class.getCanonicalName());

    private final Page page;
    private final Browser browser;
    private final String browserName;
    private final Cleanup cleanup;

    JsfFixtures(Page page, Browser browser, String browserName, Cleanup cleanup) {
        this.page = page;
        this.browser = browser;
        this.browserName = browserName;
        this.cleanup = cleanup;
    }

    public Page page() {
        return page;
    }

    /** The shared browser, e.g. to open a separate, logged-out context. */
    public Browser browser() {
        return browser;
    }

    /** "chromium", "firefox" or "webkit". */
    public String browserName() {
        return browserName;
    }

    public Cleanup cleanup() {
        return cleanup;
    }

    /** Runs {@code body} as a named step: a group in the trace, and a line in the log. */
    public void step(String title, Runnable body) {
        logger.info("step: " + title);
        page.context().tracing().group(title);
        try {
            body.run();
        } finally {
            page.context().tracing().groupEnd();
        }
    }

    public CollectionRef createCollection() {
        return createCollection(new CreateCollectionOptions());
    }

    public CollectionRef createCollection(CreateCollectionOptions options) {
        String alias = options.alias != null ? options.alias : Naming.uniqueAlias();
        CollectionPage collection = new CollectionPage(page);
        collection.open(options.parent);
        collection.startNewCollection();

        CollectionForm form = new CollectionForm(page);
        form.fillIdentifier(alias);
        form.selectCategory(options.category != null ? options.category : "DEPARTMENT");
        if (options.customize != null) {
            options.customize.accept(page);
        }
        // Registered before submitting: if anything fails after Dataverse has
        // created the collection (e.g. the redirect), it is still deleted.
        cleanup.add("delete collection \"" + alias + "\"", p -> deleteCollection(p, alias));
        form.create(alias);

        return new CollectionRef(alias, "/dataverse/" + alias);
    }

    public DatasetRef createDataset() {
        return createDataset(new CreateDatasetOptions());
    }

    public DatasetRef createDataset(CreateDatasetOptions options) {
        String title = options.title != null ? options.title : Naming.uniqueName("Playwright Dataset");
        CollectionPage collection = new CollectionPage(page);
        collection.open(options.collection);
        collection.startNewDataset();

        DatasetForm form = new DatasetForm(page);
        form.fillTitle(title);
        form.fillDescription(options.description != null ? options.description : TestData.DEFAULT_DESCRIPTION);
        form.selectSubject(options.subject != null ? options.subject : TestData.DEFAULT_SUBJECT);
        if (!options.files.isEmpty()) {
            form.uploadFiles(options.files);
        }
        form.saveNewDataset();

        DatasetRef dataset = new DatasetRef(title, withoutVersion(page.url()));
        trackDataset(dataset);
        return dataset;
    }

    public void trackDataset(DatasetRef dataset) {
        cleanup.add("delete dataset \"" + dataset.title() + "\"",
                p -> deleteDatasetIfDraft(p, withoutVersion(dataset.url())));
    }

    /**
     * Drops the version= parameter Dataverse adds after saving
     * (…&amp;version=DRAFT), so the URL keeps working after a publish.
     */
    public static String withoutVersion(String url) {
        int hash = url.indexOf('#');
        String fragment = hash >= 0 ? url.substring(hash) : "";
        String rest = hash >= 0 ? url.substring(0, hash) : url;
        int question = rest.indexOf('?');
        if (question < 0) {
            return url;
        }
        String query = Arrays.stream(rest.substring(question + 1).split("&"))
                .filter(p -> !p.isEmpty() && !p.equals("version") && !p.startsWith("version="))
                .collect(Collectors.joining("&"));
        return rest.substring(0, question) + (query.isEmpty() ? "" : "?" + query) + fragment;
    }

    /** Deletes a dataset if it is still an unpublished draft. */
    private static void deleteDatasetIfDraft(Page page, String url) {
        DatasetPage dataset = new DatasetPage(page);
        dataset.open(url);
        if (dataset.isDeletable()) {
            dataset.delete();
        } else {
            logger.info("cleanup: leaving published dataset in place: " + url);
        }
    }

    private static void deleteCollection(Page page, String alias) {
        Response response = page.navigate(CollectionPage.collectionPath(alias));
        if (response != null && response.status() == 404) {
            logger.info("cleanup: collection \"" + alias + "\" was never created");
            return;
        }
        CollectionPage collection = new CollectionPage(page);
        if (!collection.isDeletable()) {
            throw new IllegalStateException("collection \"" + alias + "\" still contains data (probably a "
                    + "published dataset), so Dataverse won't delete it");
        }
        collection.delete();
    }
}
