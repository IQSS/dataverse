package edu.harvard.iq.dataverse.util;

import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class BundleUtilTest {

    @Test
    public void testGetStringFromBundle() {
        assertEquals(null, BundleUtil.getStringFromBundle(null));
        assertEquals(null, BundleUtil.getStringFromBundle(""));
        assertEquals(null, BundleUtil.getStringFromBundle("junkKeyWeDoNotExpectToFind"));
        assertEquals("Search", BundleUtil.getStringFromBundle("search"));
        assertEquals("Error validating the username, email address, or password. Please try again. If the problem persists, contact an administrator.", BundleUtil.getStringFromBundle("login.error"));
    }

    @Test
    public void testGetStringFromBundleWithArguments() {
        assertEquals(null, BundleUtil.getStringFromBundle(null, null));
        String actual = BundleUtil.getStringFromBundle("dataverse.create.success", Arrays.asList("http://guides.dataverse.org/en", "4.0"));
        String expected = "You have successfully created your dataverse! To learn more about what you can do with your dataverse, check out the <a href=\"http://guides.dataverse.org/en/4.0/user/dataverse-management.html\" title=\"Dataverse Management - Dataverse User Guide\" target=\"_blank\">User Guide</a>.";
        assertEquals(expected, actual);
        assertEquals("Your new dataverse named "
                + "dvName (view at dvUrl ) "
                + "was created in parentDvName (view at parentDvUrl ). To learn more "
                + "about what you can do with your dataverse, check out "
                + "the Dataverse Management - User Guide at "
                + "http://guides.dataverse.org/en/4.0/user/dataverse-management.html .",
                BundleUtil.getStringFromBundle("notification.email.createDataverse",
                        Arrays.asList("dvName", "dvUrl", "parentDvName", "parentDvUrl", "http://guides.dataverse.org/en", "4.0")));
        assertEquals("Your new dataset named dsName (view at dsUrl ) "
                + "was created in parentDvName (view at parentDvUrl ). "
                + "To learn more about what you can do with a dataset, "
                + "check out the Dataset Management - User Guide at "
                + "http://guides.dataverse.org/en/4.0/user/dataset-management.html .",
                BundleUtil.getStringFromBundle("notification.email.createDataset",
                        Arrays.asList("dsName", "dsUrl", "parentDvName", "parentDvUrl", "http://guides.dataverse.org/en", "4.0")));
        assertEquals("There are no dataverses, datasets, or files that match your search. "
                + "Please try a new search by using other or broader terms. You can also check out "
                + "the <a href=\"http://guides.dataverse.org/en/4.2/user/find-use-data.html\" title=\"Finding &amp; Using "
                + "Data - Dataverse User Guide\" target=\"_blank\">search guide</a> for tips.",
                BundleUtil.getStringFromBundle("dataverse.results.empty.zero",
                        Arrays.asList("http://guides.dataverse.org/en", "4.2")));
        assertEquals("There are no search results based on how you have narrowed your search. You can check out "
                + "the <a href=\"http://guides.dataverse.org/en/4.2/user/find-use-data.html\" title=\"Finding &amp; Using "
                + "Data - Dataverse User Guide\" target=\"_blank\">search guide</a> for tips.",
                BundleUtil.getStringFromBundle("dataverse.results.empty.hidden",
                        Arrays.asList("http://guides.dataverse.org/en", "4.2")));
        assertEquals("The saved search has been successfully linked to "
                + "<a href=\"/dataverse/dvAlias\" title=\"DV Name\">DV Name</a>.",
                BundleUtil.getStringFromBundle("dataverse.saved.search.success",
                        Arrays.asList("<a href=\"/dataverse/dvAlias\" title=\"DV Name\">DV Name</a>")));
        assertEquals("Your institutional log in for TestShib Test IdP matches an email address already being used for a Dataverse "
                + "account. By entering your current Dataverse password below, your existing Dataverse account can be "
                + "converted to use your institutional log in. After converting, you will only need to use your institutional log in.",
                BundleUtil.getStringFromBundle("shib.welcomeExistingUserMessage",
                        Arrays.asList("TestShib Test IdP")));
        assertEquals("Your institutional log in for your institution matches an email address already being used for a Dataverse "
                + "account. By entering your current Dataverse password below, your existing Dataverse account can be "
                + "converted to use your institutional log in. After converting, you will only need to use your institutional log in.",
                BundleUtil.getStringFromBundle("shib.welcomeExistingUserMessage",
                        Arrays.asList(BundleUtil.getStringFromBundle("shib.welcomeExistingUserMessageDefaultInstitution"))));
    }

    @Test
    public void testGetStringFromBundleWithArgumentsAndSpecificBundle() {
        assertEquals(null, BundleUtil.getStringFromBundle(null, null, null));
        assertEquals("Search", BundleUtil.getStringFromBundle("search", null, ResourceBundle.getBundle("Bundle", Locale.US)));
    }

}
