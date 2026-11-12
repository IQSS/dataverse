package edu.harvard.iq.dataverse.jsf.collections;

import com.microsoft.playwright.Page;
import edu.harvard.iq.dataverse.jsf.support.JsfFixtures;
import edu.harvard.iq.dataverse.jsf.support.JsfTest;
import edu.harvard.iq.dataverse.jsf.support.pages.CollectionPage;
import org.junit.jupiter.api.DisplayName;

public class PublishCollectionJT {

    @JsfTest
    @DisplayName("Publish a collection")
    public void testPublishCollection(Page page, JsfFixtures fixtures) {
        // Published collections can still be deleted while they are empty, so
        // createCollection's cleanup removes this one too.
        fixtures.createCollection();
        new CollectionPage(page).publish();
    }
}
