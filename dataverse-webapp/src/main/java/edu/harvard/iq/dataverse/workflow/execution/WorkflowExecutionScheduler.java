package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Success;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;

import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static javax.jms.Message.DEFAULT_TIME_TO_LIVE;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;

/**
 * Responsible for composing {@link WorkflowExecutionMessage}'s and sending them to the message broker queue.
 *
 * @author kaczynskid
 */
@Singleton
public class WorkflowExecutionScheduler {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionScheduler.class);

    /**
     * We don't want to start executing new workflows until we finish all already running.
     */
    private static final int FIRST_STEP_PRIORITY = 0;
    /**
     * We want to complete the workflow before we start the next one, but not before we need to roll something back.
     */
    private static final int NEXT_STEP_PRIORITY = 5;
    /**
     * Rolling back the workflow is the most important thing, to get back to consistent state as soon as posible.
     */
    private static final int ROLLBACK_STEP_PRIORITY = 9;

    static final String JMS_CONNECTION_FACTORY_RESOURCE_NAME = "jms/activemqConnection";

    static final String JMS_QUEUE_RESOURCE_NAME = "jms/queue/dataverseWorkflow";

    private QueueConnectionFactory factory;

    private Queue queue;

    // -------------------- CONSTRUCTORS --------------------

    @Resource(mappedName = JMS_CONNECTION_FACTORY_RESOURCE_NAME)
    public void setFactory(QueueConnectionFactory factory) {
        this.factory = factory;
    }

    @Resource(mappedName = JMS_QUEUE_RESOURCE_NAME)
    public void setQueue(Queue queue) {
        this.queue = queue;
    }

    @PostConstruct
    public void init() {
        requireNonNull(factory, "JMS connection factory is required");
        requireNonNull(factory, "JMS queue is required");
    }

    // -------------------- LOGIC --------------------

    /**
     * Schedules execution of the first step of the workflow.
     * @param context workflow execution context to compose the message from.
     */
    public void executeFirstWorkflowStep(WorkflowExecutionContext context) {
        sendWorkflowExecutionMessage(executeWorkflowMessageOf(context, new Success()), FIRST_STEP_PRIORITY);
    }

    /**
     * Schedules execution of each following step of the workflow, except of the first one scheduled above.
     * @param context workflow execution context to compose the message from.
     * @param lastStepResult previous step result to be passed on to the next step.
     */
    public void executeNextWorkflowStep(WorkflowExecutionContext context, Success lastStepResult) {
        sendWorkflowExecutionMessage(executeWorkflowMessageOf(context, lastStepResult), NEXT_STEP_PRIORITY);
    }

    /**
     * Schedules resuming of execution of given workflow step.
     * @param context workflow execution context to compose the message from.
     * @param externalData external data to be passed on for resuming.
     */
    public void resumePausedWorkflowStep(WorkflowExecutionContext context, String externalData) {
        sendWorkflowExecutionMessage(resumeWorkflowMessageOf(context, externalData), NEXT_STEP_PRIORITY);
    }

    /**
     * Schedules rolling back of each following workflow step in reverse order.
     * @param context workflow execution context to compose the message from.
     * @param failure failure that caused the roll back.
     */
    public void rollbackNextWorkflowStep(WorkflowExecutionContext context, Failure failure) {
        sendWorkflowExecutionMessage(rollbackWorkflowMessageOf(context, failure), ROLLBACK_STEP_PRIORITY);
    }

    // -------------------- PRIVATE --------------------

    private void sendWorkflowExecutionMessage(WorkflowExecutionMessage messageBody, int priority) {
        try (
                Connection connection = factory.createConnection();
                Session session = connection.createSession(false, AUTO_ACKNOWLEDGE);
                MessageProducer producer = session.createProducer(queue);
        ) {
            ObjectMessage message = session.createObjectMessage(messageBody);
            producer.send(message, DeliveryMode.PERSISTENT, priority, DEFAULT_TIME_TO_LIVE);
        } catch (JMSException e) {
            log.error("Failed sending message: {}", messageBody);
            throw new RuntimeException("Unexpected error sending a workflow execution message", e);
        }
    }

    private static WorkflowExecutionMessage executeWorkflowMessageOf(WorkflowExecutionContext executionContext,
                                                                    Success lastStepResult) {
        return workflowExecutionMessageOf(executionContext, lastStepResult, null);
    }

    private static WorkflowExecutionMessage resumeWorkflowMessageOf(WorkflowExecutionContext executionContext,
                                                                   String externalData) {
        return workflowExecutionMessageOf(executionContext, new Success(), externalData);
    }

    private static WorkflowExecutionMessage rollbackWorkflowMessageOf(WorkflowExecutionContext executionContext,
                                                                     Failure lastStepResult) {
        return workflowExecutionMessageOf(executionContext, lastStepResult, null);
    }

    private static WorkflowExecutionMessage workflowExecutionMessageOf(WorkflowExecutionContext executionContext,
                                                                       WorkflowStepResult lastStepResult,
                                                                       String externalData) {
        return new WorkflowExecutionMessage(
                executionContext.getType().name(),
                executionContext.getDataset().getId(),
                executionContext.getNextVersionNumber(),
                executionContext.getNextMinorVersionNumber(),
                executionContext.getRequest().getUser().getIdentifier(),
                executionContext.getRequest().getSourceAddress().toString(),
                executionContext.isDatasetExternallyReleased(),
                executionContext.getWorkflow().getId(),
                executionContext.getExecution().getId(),
                lastStepResult,
                externalData
        );
    }
}
