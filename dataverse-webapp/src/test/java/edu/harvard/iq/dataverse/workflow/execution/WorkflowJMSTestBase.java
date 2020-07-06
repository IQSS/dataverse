package edu.harvard.iq.dataverse.workflow.execution;

import org.apache.activemq.jndi.ActiveMQInitialContextFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionScheduler.JMS_CONNECTION_FACTORY_RESOURCE_NAME;
import static edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionScheduler.JMS_QUEUE_RESOURCE_NAME;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static javax.naming.Context.INITIAL_CONTEXT_FACTORY;
import static javax.naming.Context.PROVIDER_URL;
import static org.awaitility.Awaitility.await;

public abstract class WorkflowJMSTestBase {

    static final MessageListener NO_OP_MESSAGE_LISTENER = m -> {};

    static {
        // http://activemq.apache.org/objectmessage.html
        System.setProperty("org.apache.activemq.SERIALIZABLE_PACKAGES", "*");
    }

    // https://activemq.apache.org/jndi-support
    // https://activemq.apache.org/vm-transport-reference
    // https://activemq.apache.org/broker-uri
    InitialContext jndi = new InitialContext(new Properties() {{
        setProperty(INITIAL_CONTEXT_FACTORY, ActiveMQInitialContextFactory.class.getCanonicalName());
        setProperty(PROVIDER_URL, "vm://localhost?broker.persistent=false&broker.useJmx=false&broker.useShutdownHook=false");
        setProperty("connectionFactoryNames", JMS_CONNECTION_FACTORY_RESOURCE_NAME);
        setProperty("queue." + JMS_QUEUE_RESOURCE_NAME, "dataverseWorkflow");
    }});

    protected QueueConnectionFactory factory = (QueueConnectionFactory) jndi.lookup(JMS_CONNECTION_FACTORY_RESOURCE_NAME);
    protected Queue queue = (Queue) jndi.lookup(JMS_QUEUE_RESOURCE_NAME);

    protected WorkflowJMSTestBase() throws NamingException { }

    protected ListeningMessageConsumer givenMessageConsumer(MessageListener listener) {
        return new ListeningMessageConsumer(listener);
    }

    protected ListeningMessageConsumer callProducer(Runnable producer) {
        return givenMessageConsumer(NO_OP_MESSAGE_LISTENER).callProducer(producer);
    }

    protected class ListeningMessageConsumer {

        private final CollectingListener collector = new CollectingListener();
        private final MessageListener listener;
        private Runnable producer;

        ListeningMessageConsumer(MessageListener listener) {
            this.listener = listener;
        }

        public ListeningMessageConsumer callProducer(Runnable producer) {
            this.producer = producer;
            return this;
        }

        public <T> T andAwaitOneMessageWithBody() throws JMSException  {
            andAwaitMessages(1);
            return collector.getFirstMessageBody();
        }

        public void andAwaitMessages(int expectedCount) throws JMSException {
            andAwaitMessages(expectedCount, ofSeconds(1));
        }

        public void andAwaitMessages(int expectedCount, Duration timeout) throws JMSException {
            try (
                    Connection connection = createAndStartConnection();
                    Session session = connection.createSession(false, AUTO_ACKNOWLEDGE);
                    MessageConsumer consumer = createAndSubscribeConsumer(session)
            ) {
                producer.run();
                collector.awaitMessageCount(expectedCount, timeout);
            }
        }

        private Connection createAndStartConnection() throws JMSException {
            Connection connection = factory.createConnection();
            connection.start();
            return connection;
        }

        private MessageConsumer createAndSubscribeConsumer(Session session) throws JMSException {
            MessageConsumer consumer = session.createConsumer(queue);
            // collector at the end, to count messages only after all logic was executed
            consumer.setMessageListener(new CompositeListener(listener, collector));
            return consumer;
        }
    }

    static class CollectingListener implements MessageListener {

        final List<Message> received = new ArrayList<>();

        @Override
        public void onMessage(Message message) {
            received.add(message);
        }

        void awaitMessageCount(int expectedCount, Duration timeout) {
            await().pollInterval(ofMillis(100)).atMost(timeout)
                    .until(() -> received.size() >= expectedCount);
        }

        @SuppressWarnings("unchecked")
        <T> T getFirstMessageBody() throws JMSException {
            return (T) ((ObjectMessage) received.get(0)).getObject();
        }
    }

    static class CompositeListener implements MessageListener {

        private final MessageListener[] delegates;

        public CompositeListener(MessageListener...delegates) {
            this.delegates = delegates;
        }

        @Override
        public void onMessage(Message message) {
            for (MessageListener delegate : delegates) {
                delegate.onMessage(message);
            }
        }
    }
}
