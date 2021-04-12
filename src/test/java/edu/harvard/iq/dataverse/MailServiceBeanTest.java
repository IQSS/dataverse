package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.MailUtil;
import org.junit.jupiter.api.Test;

import javax.mail.internet.InternetAddress;

import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.*;

class MailServiceBeanTest {

    @Test
    void setContactDelegation_withName() {
        InternetAddress fromAddress = MailUtil.parseSystemAddress("Foo Bar <foo@bar.org>");
        MailServiceBean mailServiceBean = new MailServiceBean();
        try {
            mailServiceBean.setContactDelegation("user@example.edu", fromAddress);
            assertEquals(
                "Foo Bar on behalf of user@example.edu",
                fromAddress.getPersonal()
            );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Test
    void setContactDelegation_withoutName() {
        InternetAddress fromAddress = MailUtil.parseSystemAddress("dataverse@dataverse.org");
        MailServiceBean mailServiceBean = new MailServiceBean();
        try {
            mailServiceBean.setContactDelegation("user@example.edu", fromAddress);
            assertEquals(
                "Dataverse administrator on behalf of user@example.edu",
                fromAddress.getPersonal()
            );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}