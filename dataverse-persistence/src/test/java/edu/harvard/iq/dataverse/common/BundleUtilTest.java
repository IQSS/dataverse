package edu.harvard.iq.dataverse.common;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
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
    public void getStringFromBundleWithArguments() {
        assertAll(
            () -> assertEquals("You have successfully created your dataverse! To learn more about what you can do with your dataverse, "+
                            "check out the <a href=\"http://guides.dataverse.org/en/4.0/user/dataverse-management.html\" title=\"Dataverse Management - " +
                            "Dataverse User Guide\" target=\"_blank\">User Guide</a>.",
                    BundleUtil.getStringFromBundle("dataverse.create.success", Arrays.asList("http://guides.dataverse.org/en", "4.0"))),
            () -> assertEquals(StringUtils.EMPTY, BundleUtil.getStringFromBundle(null, "")),
            () -> assertEquals(StringUtils.EMPTY, BundleUtil.getStringFromBundle(null, Locale.ENGLISH)),
            () -> assertEquals("Your new dataverse named "
                                 + "dvName (view at dvUrl) "
                                 + "was created in parentDvName (view at parentDvUrl). To learn more "
                                 + "about what you can do with your dataverse, check out "
                                 + "the Dataverse Management - User Guide at "
                                 + "http://guides.dataverse.org/en/4.0/user/dataverse-management.html .",
                    BundleUtil.getStringFromBundle("notification.email.createDataverse",
                            Arrays.asList("dvName", "dvUrl", "parentDvName", "parentDvUrl", "http://guides.dataverse.org/en", "4.0"))),
            () -> assertEquals("Your new dataset named dsName (view at dsUrl ) "
                                 + "was created in parentDvName (view at parentDvUrl ). "
                                 + "To learn more about what you can do with a dataset, "
                                 + "check out the Dataset Management - User Guide at "
                                 + "http://guides.dataverse.org/en/4.0/user/dataset-management.html .",
                         BundleUtil.getStringFromBundle("notification.email.createDataset",
                            Arrays.asList("dsName", "dsUrl", "parentDvName", "parentDvUrl", "http://guides.dataverse.org/en", "4.0"))),
            () -> assertEquals("There are no dataverses, datasets, or files that match your search. "
                                 + "Please try a new search by using other or broader terms. You can also check out "
                                 + "the <a href=\"http://guides.dataverse.org/en/4.2/user/find-use-data.html\" title=\"Finding &amp; Using "
                                 + "Data - Dataverse User Guide\" target=\"_blank\">search guide</a> for tips.",
                         BundleUtil.getStringFromBundle("dataverse.results.empty.zero",
                            Arrays.asList("http://guides.dataverse.org/en", "4.2"))),
            () -> assertEquals("There are no search results based on how you have narrowed your search. You can check out "
                                 + "the <a href=\"http://guides.dataverse.org/en/4.2/user/find-use-data.html\" title=\"Finding &amp; Using "
                                 + "Data - Dataverse User Guide\" target=\"_blank\">search guide</a> for tips.",
                         BundleUtil.getStringFromBundle("dataverse.results.empty.hidden",
                            Arrays.asList("http://guides.dataverse.org/en", "4.2"))),
            () -> assertEquals("The saved search has been successfully linked to "
                                 + "<a href=\"/dataverse/dvAlias\" title=\"DV Name\">DV Name</a>.",
                         BundleUtil.getStringFromBundle("dataverse.saved.search.success",
                                 Collections.singletonList("<a href=\"/dataverse/dvAlias\" title=\"DV Name\">DV Name</a>"))),
            () -> assertEquals("Your institutional log in for TestShib Test IdP matches an email address already being used for a Dataverse "
                                 + "account. By entering your current Dataverse password below, your existing Dataverse account can be "
                                 + "converted to use your institutional log in. After converting, you will only need to use your institutional log in.",
                         BundleUtil.getStringFromBundle("shib.welcomeExistingUserMessage",
                                 Collections.singletonList("TestShib Test IdP"))),
            () -> assertEquals("Your institutional log in for your institution matches an email address already being used for a Dataverse "
                                 + "account. By entering your current Dataverse password below, your existing Dataverse account can be "
                                 + "converted to use your institutional log in. After converting, you will only need to use your institutional log in.",
                         BundleUtil.getStringFromBundle("shib.welcomeExistingUserMessage",
                                 Collections.singletonList(BundleUtil.getStringFromBundle("shib.welcomeExistingUserMessageDefaultInstitution"))))
        );
    }

    @Test
    public void getStringFromPropertyFile() {
        assertEquals("ZIP", BundleUtil.getStringFromPropertyFile("application/zip", "MimeTypeFacets"));
    }

    @Test
    public void getStringFromPropertyFile_expectedEmpty() {
        String stringFromPropertyFile = BundleUtil.getStringFromPropertyFile("FAKE", "MimeTypeFacets");
        assertEquals(StringUtils.EMPTY, stringFromPropertyFile);
    }
}
