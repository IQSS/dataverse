package edu.harvard.iq.dataverse.common;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BundleUtilTest {

    @Test
    public void getStringFromBundle() {
        assertAll(
            () -> assertEquals(StringUtils.EMPTY, BundleUtil.getStringFromBundle(null)),
            () -> assertEquals(StringUtils.EMPTY, BundleUtil.getStringFromBundle("")),
            () -> assertEquals(StringUtils.EMPTY, BundleUtil.getStringFromBundle("junkKeyWeDoNotExpectToFind")),
            () -> assertEquals("Search", BundleUtil.getStringFromBundle("search")),
            () -> assertEquals("Error validating the username, email address, or password. Please try again. If the problem persists, contact an administrator.", BundleUtil.getStringFromBundle("login.error"))
        );
    }

    @Test
    public void getStringFromBundleWithLocale() {
        assertAll(
            () -> assertEquals(StringUtils.EMPTY, BundleUtil.getStringFromBundleWithLocale(null, Locale.ENGLISH)),
            () -> assertEquals("Search", BundleUtil.getStringFromBundleWithLocale("search", Locale.ENGLISH)),
            () -> assertEquals("Szukaj", BundleUtil.getStringFromBundleWithLocale("search", new Locale("pl")))
        );
    }
    
    @Test
    public void getStringFromBundleWithArguments() {
        assertAll(
            () -> assertEquals("You have successfully created your dataverse! To learn more about what you can do with your dataverse, "+
                            "check out the <a href=\"http://guides.dataverse.org/en/4.0/user/dataverse-management.html\" title=\"Dataverse Management - " +
                            "Dataverse User Guide\" target=\"_blank\">User Guide</a>.",
                    BundleUtil.getStringFromBundle("dataverse.create.success", "http://guides.dataverse.org/en", "4.0")),
            () -> assertEquals("Your new dataverse named "
                                 + "dvName (view at dvUrl) "
                                 + "was created in parentDvName (view at parentDvUrl). To learn more "
                                 + "about what you can do with your dataverse, check out "
                                 + "the Dataverse Management - User Guide at "
                                 + "http://guides.dataverse.org/en/4.0/user/dataverse-management.html .",
                    BundleUtil.getStringFromBundle("notification.email.createDataverse",
                            "dvName", "dvUrl", "parentDvName", "parentDvUrl", "http://guides.dataverse.org/en", "4.0")),
            () -> assertEquals("Your new dataset named dsName (view at dsUrl ) "
                                 + "was created in parentDvName (view at parentDvUrl ). "
                                 + "To learn more about what you can do with a dataset, "
                                 + "check out the Dataset Management - User Guide at "
                                 + "http://guides.dataverse.org/en/4.0/user/dataset-management.html .",
                         BundleUtil.getStringFromBundle("notification.email.createDataset",
                            "dsName", "dsUrl", "parentDvName", "parentDvUrl", "http://guides.dataverse.org/en", "4.0")),
            () -> assertEquals("There are no dataverses, datasets, or files that match your search. "
                                 + "Please try a new search by using other or broader terms. You can also check out "
                                 + "the <a href=\"http://guides.dataverse.org/en/4.2/user/find-use-data.html\" title=\"Finding &amp; Using "
                                 + "Data - Dataverse User Guide\" target=\"_blank\">search guide</a> for tips.",
                         BundleUtil.getStringFromBundle("dataverse.results.empty.zero",
                            "http://guides.dataverse.org/en", "4.2")),
            () -> assertEquals("There are no search results based on how you have narrowed your search. You can check out "
                                 + "the <a href=\"http://guides.dataverse.org/en/4.2/user/find-use-data.html\" title=\"Finding &amp; Using "
                                 + "Data - Dataverse User Guide\" target=\"_blank\">search guide</a> for tips.",
                         BundleUtil.getStringFromBundle("dataverse.results.empty.hidden",
                            "http://guides.dataverse.org/en", "4.2")),
            () -> assertEquals("The saved search has been successfully linked to "
                                 + "<a href=\"/dataverse/dvAlias\" title=\"DV Name\">DV Name</a>.",
                         BundleUtil.getStringFromBundle("dataverse.saved.search.success",
                                 "<a href=\"/dataverse/dvAlias\" title=\"DV Name\">DV Name</a>")),
            () -> assertEquals("Your institutional log in for TestShib Test IdP matches an email address already being used for a Dataverse "
                                 + "account. By entering your current Dataverse password below, your existing Dataverse account can be "
                                 + "converted to use your institutional log in. After converting, you will only need to use your institutional log in.",
                         BundleUtil.getStringFromBundle("shib.welcomeExistingUserMessage",
                                 "TestShib Test IdP")),
            () -> assertEquals("Your institutional log in for your institution matches an email address already being used for a Dataverse "
                                 + "account. By entering your current Dataverse password below, your existing Dataverse account can be "
                                 + "converted to use your institutional log in. After converting, you will only need to use your institutional log in.",
                         BundleUtil.getStringFromBundle("shib.welcomeExistingUserMessage",
                                 BundleUtil.getStringFromBundle("shib.welcomeExistingUserMessageDefaultInstitution")))
        );
    }

    @Test
    public void getStringFromNonDefaultBundle() {
        assertEquals("ZIP", BundleUtil.getStringFromNonDefaultBundle("application/zip", "MimeTypeFacets"));
    }

    @Test
    public void getStringFromNonDefaultBundle_expectedEmpty() {
        String stringFromPropertyFile = BundleUtil.getStringFromNonDefaultBundle("FAKE", "MimeTypeFacets");
        assertEquals(StringUtils.EMPTY, stringFromPropertyFile);
    }
}
