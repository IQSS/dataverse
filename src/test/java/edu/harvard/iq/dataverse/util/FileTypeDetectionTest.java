package edu.harvard.iq.dataverse.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileTypeDetectionTest {

    static String baseDirForConfigFiles = "/tmp";

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("com.sun.aas.instanceRoot", baseDirForConfigFiles);
        String testFile1Src = "conf/jhove/jhove.conf";
        String testFile1Tmp = baseDirForConfigFiles + "/config/jhove.conf";
        try {
            FileUtils.copyFile(new File(testFile1Src), new File(testFile1Tmp));
        } catch (IOException ex) {
            Logger.getLogger(JhoveFileTypeTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @AfterClass
    public static void tearDownClass() {
        // SiteMapUtilTest relies on com.sun.aas.instanceRoot being null.
        System.clearProperty("com.sun.aas.instanceRoot");
    }

    @Test
    public void testDetermineFileTypeJupyterNoteboook() throws Exception {
        File file = new File("src/test/java/edu/harvard/iq/dataverse/util/irc-metrics.ipynb");
        // https://jupyter.readthedocs.io/en/latest/reference/mimetype.html
        assertEquals("application/x-ipynb+json", FileTypeDetection.determineFileType(file));
    }

}
