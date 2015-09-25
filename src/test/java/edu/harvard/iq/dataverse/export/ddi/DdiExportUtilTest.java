package edu.harvard.iq.dataverse.export.ddi;

import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DdiExportUtilTest {

    private static final Logger logger = Logger.getLogger(DdiExportUtilTest.class.getCanonicalName());

    @Test
    public void testJson2ddi() throws Exception {
        File datasetVersionJson = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/datasetversion-finch1.json");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));
        File ddiFile = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.xml");
        String datasetAsDdi = XmlPrinter.prettyPrintXml(new String(Files.readAllBytes(Paths.get(ddiFile.getAbsolutePath()))));
        logger.fine(datasetAsDdi);
        String result = DdiExportUtil.json2ddi(datasetVersionAsJson);
        logger.fine(result);
        boolean doneWithIssue2579 = false;
        if (doneWithIssue2579) {
            assertEquals(datasetAsDdi, result);
        }
    }

}
