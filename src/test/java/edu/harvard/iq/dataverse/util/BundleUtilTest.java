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
    }

    @Test
    public void testGetStringFromBundleWithArgumentsAndSpecificBundle() {
        assertEquals(null, BundleUtil.getStringFromBundle(null, null, null));
        assertEquals("Search", BundleUtil.getStringFromBundle("search", null, ResourceBundle.getBundle("Bundle", Locale.US)));
    }

}
