package edu.harvard.iq.dataverse.confirmemail;

import org.junit.Test;
import static org.junit.Assert.*;

public class ConfirmEmailUtilTest {
    
    ConfirmEmailUtil confirmEmailUtil;
    
    @Test
    public void testFriendlyExpirationTime() {
        System.out.println("1440 Minutes: " + confirmEmailUtil.friendlyExpirationTime(1440));
        assertEquals("24 hours", ConfirmEmailUtil.friendlyExpirationTime(1440));
        System.out.println("60 Minutes: " + confirmEmailUtil.friendlyExpirationTime(60));
        assertEquals("1 hour", ConfirmEmailUtil.friendlyExpirationTime(60));
        System.out.println("30 Minutes: " + confirmEmailUtil.friendlyExpirationTime(30));
        assertEquals("30 minutes", ConfirmEmailUtil.friendlyExpirationTime(30));
    }
    
}
