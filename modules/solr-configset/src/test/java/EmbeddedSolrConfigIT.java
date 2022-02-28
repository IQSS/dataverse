import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.CoreStatus;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.core.NodeConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag(Constants.TAG_CONFIG)
@DisplayName("Embedded Solr Configuration Integration Test")
public class EmbeddedSolrConfigIT {
    
    final static String buildDirectory = System.getProperty("buildDirectory");
    final static String[] solrHomeSubPath = {"solr"};
    
    static Path solrHome;
    static Path solrConfigSets;
    
    final static String collectionName = "collection1";
    final static String configSetName = "dataverse";
    static SolrClient solrClient;
    
    @BeforeAll
    public static void setUp() throws IOException {
        checkAndSetupSolrDirectories();
        
        // build a node config, so we can start an embedded server with it
        final NodeConfig config = new NodeConfig.NodeConfigBuilder("embeddedSolrServerNode", solrHome)
            .setConfigSetBaseDirectory(solrConfigSets.toString())
            .build();
    
        // create the server
        final EmbeddedSolrServer embeddedSolrServer = new EmbeddedSolrServer(config, collectionName);
        solrClient = embeddedSolrServer;
    }
    
    @AfterAll
    public static void tearDown() throws IOException {
        // delete the solr home (so we can run the test again without mvn clean)
        try (Stream<Path> walk = Files.walk(solrHome)) {
            walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                //.peek(System.out::println)
                .forEach(File::delete);
        }
    }
    
    @Test
    @DisplayName("Deploy Solr Core from Dataverse ConfigSet")
    public void deployDataverseCore() throws SolrServerException, IOException {
        assumeTrue(solrClient != null);
    
        // create the core from our configset
        final CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName(collectionName);
        createRequest.setConfigSet(configSetName);
        createRequest.process(solrClient);
        
        // get the core status
        final CoreStatus coreStatus = CoreAdminRequest.getCoreStatus(collectionName, solrClient);
        assertNotNull(coreStatus);
        assertTrue(coreStatus.getCoreStartTime().before(Calendar.getInstance().getTime()));
        
        // check ping
        final SolrPing ping = new SolrPing();
        SolrPingResponse pingResponse = ping.process(solrClient);
        assertNotNull(pingResponse);
        assertTrue(pingResponse.toString().contains("status=OK"));
    }
    
    
    private static void checkAndSetupSolrDirectories() throws IOException {
        assertNotNull(buildDirectory);
        final Path buildDir = Path.of(buildDirectory);
        assertTrue(buildDir.isAbsolute() &&
            Files.exists(buildDir) &&
            Files.isDirectory(buildDir) &&
            Files.isReadable(buildDir) &&
            Files.isWritable(buildDir));
    
        // create the solr home (might be replaced with a memory fs)
        solrHome = Path.of(buildDirectory, solrHomeSubPath);
        Files.createDirectories(solrHome);
        
        // we simply reuse the parent directory from the build as our configsets source directory
        solrConfigSets = buildDir;
    }
}
