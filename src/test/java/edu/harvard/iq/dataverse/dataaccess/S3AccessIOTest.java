package edu.harvard.iq.dataverse.dataaccess;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.junit.Ignore;

public class S3AccessIOTest {

    /**
     * FIXME: How can we test System.getProperty from JUnit?
     */
    @Ignore
    @Test
    public void testEncryptFile() {
        System.out.println("encryptFile");
        String key = S3AccessIO.encryptOnUploadKey;
        System.setProperty(key, null);
        assertEquals(true, S3AccessIO.encryptFile());
        System.setProperty(key, "true");
        assertEquals(true, S3AccessIO.encryptFile());
        System.setProperty(key, "nonBooleanValue");
        assertEquals(true, S3AccessIO.encryptFile());
        System.setProperty(key, "false");
        assertEquals(false, S3AccessIO.encryptFile());
    }

}
