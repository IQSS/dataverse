package edu.harvard.iq.dataverse.arquillian.arquillianexamples;

import com.github.sleroy.fakesmtp.core.ServerConfiguration;
import com.github.sleroy.junit.mail.server.test.FakeSmtpRule;
import edu.harvard.iq.dataverse.persistence.DatabaseCleaner;
import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.SqlScriptRunner;
import edu.harvard.iq.dataverse.test.arquillian.ArquillianIntegrationTests;
import edu.harvard.iq.dataverse.validation.datasetfield.FieldValidatorRegistry;
import edu.harvard.iq.dataverse.validation.datasetfield.validators.StandardDateValidator;
import edu.harvard.iq.dataverse.validation.datasetfield.validators.StandardEmailValidator;
import edu.harvard.iq.dataverse.validation.datasetfield.validators.StandardInputValidator;
import edu.harvard.iq.dataverse.validation.datasetfield.validators.StandardIntegerValidator;
import edu.harvard.iq.dataverse.validation.datasetfield.validators.StandardNumberValidator;
import edu.harvard.iq.dataverse.validation.datasetfield.validators.StandardUrlValidator;
import edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionWorker;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.categories.Category;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import java.io.File;
import java.util.logging.Logger;


@Category(ArquillianIntegrationTests.class)
public class WebappArquillianDeployment {

    private static final Logger logger = Logger.getLogger(WebappArquillianDeployment.class.getName());

    @Rule
    public FakeSmtpRule smtpServer = new FakeSmtpRule(ServerConfiguration.create()
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


    @Before
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

    @After
    public void after() throws Throwable {
        if (transaction.getStatus() == Status.STATUS_NO_TRANSACTION) {
            databaseCleaner.cleanupDatabase();
        }
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
