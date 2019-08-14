package edu.harvard.iq.dataverse.arquillian.arquillianexamples;

import com.github.sleroy.fakesmtp.core.ServerConfiguration;
import com.github.sleroy.junit.mail.server.test.FakeSmtpRule;
import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import edu.harvard.iq.dataverse.test.arquillian.ArquillianIntegrationTests;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Rule;
import org.junit.experimental.categories.Category;

import java.util.logging.Logger;


@Category(ArquillianIntegrationTests.class)
public class WebappArquillianDeployment {

    private static final Logger logger = Logger.getLogger(WebappArquillianDeployment.class.getName());

    @Rule
    public FakeSmtpRule smtpServer = new FakeSmtpRule(ServerConfiguration.create()
                                                              .port(2525)
                                                              .relayDomains("gmail.com")
                                                              .charset("UTF-8"));

    @Deployment
    public static Archive<?> createDeployment() {

        JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class, "dv-webapp.jar")
                .merge(PersistenceArquillianDeployment.createDeployment())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackages(true, "edu.harvard.iq.dataverse");
        
        logger.info(javaArchive.toString(true));

        return javaArchive;
    }

}
