package edu.harvard.iq.dataverse.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JhoveFileTypeTest {

    static JhoveFileType jhoveFileType;
    static String baseDirForConfigFiles = "/tmp";
    static File png;
    static File gif;
    static File jpg;
    static File pdf;
    static File zip;
    static File xml;
    static File html;
    static File ico;
    static File ipynb;

    @BeforeAll
    public static void setUpClass() {
        System.setProperty("com.sun.aas.instanceRoot", baseDirForConfigFiles);
        jhoveFileType = new JhoveFileType();
        copyConfigIntoPlace();

        png = new File("src/test/resources/images/coffeeshop.png");
        gif = new File("src/main/webapp/resources/images/ajax-loading.gif");
        jpg = new File("src/main/webapp/resources/images/dataverseproject_logo.jpg");
        pdf = new File("scripts/issues/1380/dvs.pdf");
        zip = new File("src/test/resources/doi-10-5072-fk2hyixmyv1.0.zip");
        xml = new File("pom.xml");
        html = new File("src/main/webapp/mydata_templates/mydata.html");
        ico = new File("src/main/webapp/resources/images/fav/favicon.ico");
        ipynb = new File("src/test/java/edu/harvard/iq/dataverse/util/irc-metrics.ipynb");
    }

    @AfterAll
    public static void tearDownClass() {
        // SiteMapUtilTest relies on com.sun.aas.instanceRoot being null.
        System.clearProperty("com.sun.aas.instanceRoot");
    }

    @Test
    public void testGetFileMimeType() {
        System.out.println("getFileMimeType");
        // GOOD: figured it out. :)
        assertEquals("image/png", jhoveFileType.getFileMimeType(png));
        assertEquals("image/gif", jhoveFileType.getFileMimeType(gif));
        assertEquals("image/jpeg", jhoveFileType.getFileMimeType(jpg));
        assertEquals("application/pdf", jhoveFileType.getFileMimeType(pdf));
        // BAD: couldn't figure it out. :(
        assertEquals("application/octet-stream", jhoveFileType.getFileMimeType(zip));
        assertEquals("application/octet-stream", jhoveFileType.getFileMimeType(ico));
        // BAD: not very specific. :(
        assertEquals("text/plain; charset=US-ASCII", jhoveFileType.getFileMimeType(xml));
        assertEquals("text/plain; charset=US-ASCII", jhoveFileType.getFileMimeType(html));
        assertEquals("text/plain; charset=US-ASCII", jhoveFileType.getFileMimeType(ipynb));
    }

    @Test
    public void testCheckFileType() {
        System.out.println("checkFileType");
        jhoveFileType = new JhoveFileType();
        assertEquals(543938, jhoveFileType.checkFileType(png).getSize());
    }

    @Test
    public void testGetJhoveConfigFile() {
        System.out.println("getJhoveConfigFile");
        assertEquals(baseDirForConfigFiles + "/config/jhove.conf", JhoveFileType.getJhoveConfigFile());
    }

    private static void copyConfigIntoPlace() {
        String testFile1Src = "conf/jhove/jhove.conf";
        String testFile1Tmp = baseDirForConfigFiles + "/config/jhove.conf";
        try {
            FileUtils.copyFile(new File(testFile1Src), new File(testFile1Tmp));
        } catch (IOException ex) {
            Logger.getLogger(JhoveFileTypeTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
