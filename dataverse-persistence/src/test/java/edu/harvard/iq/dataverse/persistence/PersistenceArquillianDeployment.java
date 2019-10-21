package edu.harvard.iq.dataverse.persistence;

import edu.harvard.iq.dataverse.test.arquillian.ArquillianIntegrationTests;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import java.util.logging.Logger;

@Category(ArquillianIntegrationTests.class)
public class PersistenceArquillianDeployment {

    private static final Logger logger = Logger.getLogger(PersistenceArquillianDeployment.class.getName());

    @Inject
    private SqlScriptRunner sqlScriptRunner;
    @Inject
    private DatabaseCleaner databaseCleaner;
    @Resource
    private UserTransaction transaction;


    @Before
    public void before() throws Throwable {
        sqlScriptRunner.runScriptFromClasspath("/dbinit.sql");
    }

    @After
    public void after() throws Throwable {
        if (transaction.getStatus() == Status.STATUS_NO_TRANSACTION) {
            databaseCleaner.cleanupDatabase();
        }
    }
    
    
    @Deployment
    public static Archive<?> createDeployment() {

        JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class, "dv-persistence.jar")
                .addPackages(true, "edu.harvard.iq.dataverse.persistence")
                .addPackages(true, "edu.harvard.iq.dataverse.common")
                .addAsResource("test-persistence.xml", "META-INF/persistence.xml");
        logger.info(javaArchive.toString(true));

        return javaArchive;
    }
    
}
