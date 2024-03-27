package edu.harvard.iq.dataverse.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

public class UrlSignerUtilTest {

    @Test
    public void testSignAndValidate() {

        final String url = "http://localhost:8080/api/test1";
        final String get = "GET";
        final String post = "POST";

        final String user1 = "Alice";
        final String user2 = "Bob";
        final int tooQuickTimeout = -1;
        final int longTimeout = 1000;
        final String key = "abracadabara open sesame";
        final String badkey = "abracadabara open says me";

        Logger.getLogger(UrlSignerUtil.class.getName()).setLevel(Level.FINE);
        
        String signedUrl1 = UrlSignerUtil.signUrl(url, longTimeout, user1, get, key);
        assertTrue(UrlSignerUtil.isValidUrl(signedUrl1, user1, get,  key));
        assertTrue(UrlSignerUtil.isValidUrl(signedUrl1, user1, null, key));
        assertTrue(UrlSignerUtil.isValidUrl(signedUrl1, null, get, key));

        assertFalse(UrlSignerUtil.isValidUrl(signedUrl1, null, get, badkey));
        assertFalse(UrlSignerUtil.isValidUrl(signedUrl1, user2, get, key));
        assertFalse(UrlSignerUtil.isValidUrl(signedUrl1, user1, post, key));
        assertFalse(UrlSignerUtil.isValidUrl(signedUrl1.replace(user1, user2), user1, get, key));
        assertFalse(UrlSignerUtil.isValidUrl(signedUrl1.replace(user1, user2), user2, get, key));
        assertFalse(UrlSignerUtil.isValidUrl(signedUrl1.replace(user1, user2), null, get, key));

        String signedUrl2 = UrlSignerUtil.signUrl(url, null, null, null, key);
        assertTrue(UrlSignerUtil.isValidUrl(signedUrl2, null, null, key));
        assertFalse(UrlSignerUtil.isValidUrl(signedUrl2, null, post, key));
        assertFalse(UrlSignerUtil.isValidUrl(signedUrl2, user1, null, key));

        String signedUrl3 = UrlSignerUtil.signUrl(url, tooQuickTimeout, user1, get, key);

        assertFalse(UrlSignerUtil.isValidUrl(signedUrl3, user1, get, key));
    }
}
