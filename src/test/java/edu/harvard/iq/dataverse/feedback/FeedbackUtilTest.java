package edu.harvard.iq.dataverse.feedback;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseSession;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class FeedbackUtilTest {

    @Test
    public void testGatherFeedbackOnDataverse() {
        Dataverse dataverse = new Dataverse();
        dataverse.setAlias("dvAlias1");
        DataverseSession dataverseSession = null;
        String messageSubject = "nice dataverse";
        String userMessage = "Let's talk!";
        String systemEmail = "support@librascholar.edu";
        String userEmail = "hsimpson@mailinator.com";
        String baseUrl = "https://demo.dataverse.org";
        Feedback feedback = FeedbackUtil.gatherFeedback(dataverse, dataverseSession, messageSubject, userMessage, systemEmail, userEmail, baseUrl);
        System.out.println("feedback: " + feedback);
        System.out.println("Subject: " + feedback.getSubject());
        System.out.println("Body: " + feedback.getBody());
        assertEquals(messageSubject, feedback.getSubject());
        assertEquals("The message below was sent from the Contact button at https://demo.dataverse.org/dataverse/dvAlias1\n\n" + userMessage, feedback.getBody());
    }

    @Test
    public void testGatherFeedbackOnDataset() {
        Dataset dataset = new Dataset();
        dataset.setProtocol("doi");
        dataset.setAuthority("10.7910/DVN");
        dataset.setIdentifier("TJCLKP");
        Dataverse dataverse = new Dataverse();
        dataset.setOwner(dataverse);
        DataverseSession dataverseSession = null;
        String messageSubject = "nice file";
        String userMessage = "Let's talk!";
        String systemEmail = "support@librascholar.edu";
        String userEmail = "hsimpson@mailinator.com";
        String baseUrl = "https://demo.dataverse.org";
        Feedback feedback = FeedbackUtil.gatherFeedback(dataset, dataverseSession, messageSubject, userMessage, systemEmail, userEmail, baseUrl);
        System.out.println("feedback: " + feedback);
        System.out.println("Subject: " + feedback.getSubject());
        System.out.println("Body: " + feedback.getBody());
        assertEquals(messageSubject, feedback.getSubject());
        assertEquals("The message below was sent from the Contact button at https://demo.dataverse.org/dataset.xhtml?persistentId=doi:10.7910/DVN/TJCLKP\n\n" + userMessage, feedback.getBody());
    }

    @Test
    public void testGatherFeedbackOnFile() {
        DataFile file = new DataFile();
        file.setId(42l);
        DataverseSession dataverseSession = null;
        String messageSubject = "nice file";
        String userMessage = "Let's talk!";
        String systemEmail = "support@librascholar.edu";
        String userEmail = "hsimpson@mailinator.com";
        String baseUrl = "https://demo.dataverse.org";
        Feedback feedback = FeedbackUtil.gatherFeedback(file, dataverseSession, messageSubject, userMessage, systemEmail, userEmail, baseUrl);
        System.out.println("feedback: " + feedback);
        System.out.println("Subject: " + feedback.getSubject());
        System.out.println("Body: " + feedback.getBody());
        assertEquals(messageSubject, feedback.getSubject());
        assertEquals("The message below was sent from the Contact button at https://demo.dataverse.org/file.xhtml?fileId=42\n\n" + userMessage, feedback.getBody());
    }

}
