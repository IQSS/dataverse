package edu.harvard.iq.dataverse.arquillian.arquillianexamples;

import com.github.sleroy.fakesmtp.core.ServerConfiguration;
import edu.harvard.iq.dataverse.persistence.DatabaseCleaner;
import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.SqlScriptRunner;
import edu.harvard.iq.dataverse.validation.field.FieldValidatorRegistry;
import edu.harvard.iq.dataverse.validation.field.validators.StandardDateValidator;
import edu.harvard.iq.dataverse.validation.field.validators.StandardEmailValidator;
import edu.harvard.iq.dataverse.validation.field.validators.StandardInputValidator;
import edu.harvard.iq.dataverse.validation.field.validators.StandardIntegerValidator;
import edu.harvard.iq.dataverse.validation.field.validators.StandardNumberValidator;
import edu.harvard.iq.dataverse.validation.field.validators.StandardUrlValidator;
import edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionWorker;
import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import java.io.File;
import java.util.logging.Logger;


@ExtendWith({ArquillianExtension.class})
@Tag("ArquillianIntegrationTests")
@ArquillianSuiteDeployment
public class WebappArquillianDeployment {

    private static final Logger logger = Logger.getLogger(WebappArquillianDeployment.class.getName());

    public static FakeSmtpServer smtpServer = new FakeSmtpServer(ServerConfiguration.create()
            .port(2525)
            .relayDomains("gmail.com", "mailinator.com")
            .charset("UTF-8"));

    @Inject
    private SqlScriptRunner sqlScriptRunner;
    @Inject
    private DatabaseCleaner databaseCleaner;
    @Inject
    private FieldValidatorRegistry fieldValidatorRegistry;

    @Resource
    private UserTransaction transaction;

    @BeforeAll
    public static void beforeAllWeb() throws Throwable {
        smtpServer.startServer();
    }

    @AfterAll
    public static void afterAllWeb() throws Exception {
        smtpServer.shutdownServer();
    }

    @BeforeEach
    public void before() {
        sqlScriptRunner.runScriptFromClasspath("/dbinit.sql");

        // Register validators
        fieldValidatorRegistry.register(new StandardDateValidator());
        fieldValidatorRegistry.register(new StandardEmailValidator());
        fieldValidatorRegistry.register(new StandardInputValidator());
        fieldValidatorRegistry.register(new StandardIntegerValidator());
        fieldValidatorRegistry.register(new StandardNumberValidator());
        fieldValidatorRegistry.register(new StandardUrlValidator());
    }

    @AfterEach
    public void after() throws Throwable {
        if (transaction.getStatus() == Status.STATUS_NO_TRANSACTION) {
            databaseCleaner.cleanupDatabase();
        }

        smtpServer.clearMails();
    }


    @Deployment
    public static Archive<?> createDeployment() {

        JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class, "dv-webapp.jar")
                .merge(PersistenceArquillianDeployment.createDeployment())
                .addAsManifestResource(new FileAsset(new File("src/main/webapp/WEB-INF/beans.xml")), "beans.xml")
                .addPackages(true, "edu.harvard.iq.dataverse")
                .deleteClass(WorkflowExecutionWorker.class);

        logger.info(javaArchive.toString(true));

        return javaArchive;
    }

}
