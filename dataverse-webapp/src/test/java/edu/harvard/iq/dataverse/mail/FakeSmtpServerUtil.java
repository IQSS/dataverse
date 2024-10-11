package edu.harvard.iq.dataverse.mail;

import com.github.sleroy.fakesmtp.model.EmailModel;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.FakeSmtpServer;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

/**
 * @author madryk
 */
public class FakeSmtpServerUtil {
    
    
    // -------------------- LOGIC --------------------
    
    /**
     * Waits until email is sent to the address given as parameter
     * and returns it.
     */
    public static EmailModel waitForEmailSentTo(FakeSmtpServer smtpServer, String emailTo) {
        await()
            .atMost(Duration.ofSeconds(3L))
            .until(() -> smtpServer.getMails().stream()
                            .anyMatch((email) -> StringUtils.equals(email.getTo(), emailTo))
            );
        
        return smtpServer.getMails().stream()
            .filter((email) -> StringUtils.equals(email.getTo(), emailTo))
            .findFirst()
            .get();
    }
    
    
}
