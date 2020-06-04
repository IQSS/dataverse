package edu.harvard.iq.dataverse.persistence;

import edu.harvard.iq.dataverse.test.arquillian.ArquillianIntegrationTests;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
@Category(ArquillianIntegrationTests.class)
public abstract class PersistenceArquillianDeployment {

    protected static final Logger log = LoggerFactory.getLogger(PersistenceArquillianDeployment.class);

    @Inject SqlScriptRunner sqlScriptRunner;
    @Inject DatabaseCleaner databaseCleaner;
    @Resource UserTransaction transaction;

    @Deployment
    public static Archive<?> createDeployment() {
        JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class, "dv-persistence.jar")
                .addPackages(true, "edu.harvard.iq.dataverse.persistence")
                .addPackages(true, "edu.harvard.iq.dataverse.common")
                .addAsResource("test-persistence.xml", "META-INF/persistence.xml");
        log.info(javaArchive.toString(true));
        return javaArchive;
    }

    @Before
    public void before() {
        sqlScriptRunner.runScriptFromClasspath("/dbinit.sql");
    }

    @After
    public void after() throws Exception {
        if (transaction.getStatus() == Status.STATUS_NO_TRANSACTION) {
            databaseCleaner.cleanupDatabase();
        }
    }
}
