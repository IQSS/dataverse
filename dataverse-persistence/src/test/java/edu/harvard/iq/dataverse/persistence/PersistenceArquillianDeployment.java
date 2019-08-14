package edu.harvard.iq.dataverse.persistence;

import edu.harvard.iq.dataverse.test.arquillian.ArquillianIntegrationTests;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.experimental.categories.Category;

import java.util.logging.Logger;

@Category(ArquillianIntegrationTests.class)
public class PersistenceArquillianDeployment {

    private static final Logger logger = Logger.getLogger(PersistenceArquillianDeployment.class.getName());

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
