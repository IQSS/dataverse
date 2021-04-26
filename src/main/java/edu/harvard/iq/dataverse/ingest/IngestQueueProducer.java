package edu.harvard.iq.dataverse.ingest;

import javax.annotation.Resource;
// https://www.baeldung.com/jee-cdi-vs-ejb-singleton
import javax.inject.Singleton;
import javax.enterprise.inject.Produces;
import javax.jms.JMSConnectionFactoryDefinition;
import javax.jms.JMSDestinationDefinition;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;

@JMSConnectionFactoryDefinition(
    description = "Dataverse Ingest Queue Factory",
    name = "java:app/jms/factory/ingest",
    resourceAdapter = "jmsra",
    interfaceName = "javax.jms.QueueConnectionFactory",
    maxPoolSize = 250,
    minPoolSize = 1,
    properties = {
        "org.glassfish.connector-connection-pool.max-wait-time-in-millis=60000",
        "org.glassfish.connector-connection-pool.pool-resize-quantity=2"
    }
)
@JMSDestinationDefinition(
    description = "Dataverse Ingest Queue",
    name = "java:app/jms/queue/ingest",
    resourceAdapter = "jmsra",
    interfaceName="javax.jms.Queue",
    destinationName = "DataverseIngest"
)
@Singleton
public class IngestQueueProducer {
    
    @Resource(lookup="java:app/jms/queue/ingest")
    Queue ingestQueue;
    
    @Resource(lookup="java:app/jms/factory/ingest")
    QueueConnectionFactory ingestQueueFactory;
    
    @Produces
    public Queue getIngestQueue() {
        return ingestQueue;
    }
    
    @Produces
    public QueueConnectionFactory getIngestQueueFactory() {
        return ingestQueueFactory;
    }
    
}
