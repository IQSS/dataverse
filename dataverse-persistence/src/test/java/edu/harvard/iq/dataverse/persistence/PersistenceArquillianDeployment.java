package edu.harvard.iq.dataverse.persistence;

import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

@ExtendWith(ArquillianExtension.class)
@Transactional(TransactionMode.ROLLBACK)
@Tag("ArquillianIntegrationTests")
@ArquillianSuiteDeployment
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

    @BeforeEach
    public void before() {
        sqlScriptRunner.runScriptFromClasspath("/dbinit.sql");
    }

    @AfterEach
    public void after() throws Exception {
        if (transaction.getStatus() == Status.STATUS_NO_TRANSACTION) {
            databaseCleaner.cleanupDatabase();
        }
    }
}
