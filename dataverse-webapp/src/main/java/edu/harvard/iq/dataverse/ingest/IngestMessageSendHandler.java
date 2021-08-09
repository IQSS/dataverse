package edu.harvard.iq.dataverse.ingest;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import java.util.logging.Logger;

/**
 * Handler of {@link IngestMessageSendEvent} events.
 * It assures that ingest will not start before
 * completion of backend tasks.
 *
 * @author dbojanek
 */
@ApplicationScoped
public class IngestMessageSendHandler {

    private static final Logger logger = Logger.getLogger(IngestMessageSendHandler.class.getCanonicalName());

    @Resource(mappedName = "jms/DataverseIngest")
    Queue queue;
    @Resource(mappedName = "jms/IngestQueueConnectionFactory")
    QueueConnectionFactory factory;

    // -------------------- LOGIC --------------------

    public void sendIngestMessage(@Observes(during = TransactionPhase.AFTER_SUCCESS) IngestMessageSendEvent ingestEvent) {
        QueueConnection conn = null;
        QueueSession session = null;
        QueueSender sender = null;

        try {
            conn = factory.createQueueConnection();
            session = conn.createQueueSession(false, 0);
            sender = session.createSender(queue);

            Message queueMessage = session.createObjectMessage(ingestEvent.getIngestMessage());

            sender.send(queueMessage);

            logger.fine("Ingest message has been sent to jms. Message was: " + queueMessage.toString());

        } catch (JMSException ex) {
            ex.printStackTrace();
            logger.warning("Caught exception trying to close connections after starting a " +
                    "(re)ingest job in the JMS queue! Stack trace below.");
        } finally {
            try {
                if (sender != null) {
                    sender.close();
                }
                if (session != null) {
                    session.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
                logger.warning("Caught exception trying to close connections after starting a " +
                        "(re)ingest job in the JMS queue! Stack trace below.");
                ex.printStackTrace();
            }
        }
    }
}
