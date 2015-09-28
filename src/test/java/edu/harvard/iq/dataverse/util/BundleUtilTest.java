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
                + "the Dataverse Management - Dataverse User Guide at "
                + "http://guides.dataverse.org/en/4.0/user/dataverse-management.html .",
                BundleUtil.getStringFromBundle("notification.email.createDataverse",
                        Arrays.asList("dvName", "dvUrl", "parentDvName", "parentDvUrl", "http://guides.dataverse.org/en", "4.0")));
        assertEquals("Your new dataset named dsName (view at dsUrl ) "
                + "was created in parentDvName (view at parentDvUrl ). "
                + "To learn more about what you can do with a dataset, "
                + "check out the Dataset Management - Dataset User Guide at "
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
    }

    @Test
    public void testGetStringFromBundleWithArgumentsAndSpecificBundle() {
        assertEquals(null, BundleUtil.getStringFromBundle(null, null, null));
        assertEquals("Search", BundleUtil.getStringFromBundle("search", null, ResourceBundle.getBundle("Bundle", Locale.US)));
    }

}
