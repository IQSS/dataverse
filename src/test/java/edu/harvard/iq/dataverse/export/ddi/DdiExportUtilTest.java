package edu.harvard.iq.dataverse.export.ddi;

import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DdiExportUtilTest {

    private static final Logger logger = Logger.getLogger(DdiExportUtilTest.class.getCanonicalName());

    @Test
    public void testJson2DdiNoFiles() throws Exception {
        File datasetVersionJson = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.json");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));
        File ddiFile = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.xml");
        String datasetAsDdi = XmlPrinter.prettyPrintXml(new String(Files.readAllBytes(Paths.get(ddiFile.getAbsolutePath()))));
        logger.info(datasetAsDdi);
        String result = DdiExportUtil.datasetDtoAsJson2ddi(datasetVersionAsJson);
        logger.info(result);
        assertEquals(datasetAsDdi, result);
    }

    @Test
    public void testJson2ddiHasFiles() throws Exception {
        /**
         * Note that `cat dataset-spruce1.json | jq
         * .datasetVersion.files[0].datafile.description` yields an empty string
         * but datasets created in the GUI sometimes don't have a description
         * field at all.
         */
        File datasetVersionJson = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-spruce1.json");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));
        File ddiFile = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-spruce1.xml");
        String datasetAsDdi = XmlPrinter.prettyPrintXml(new String(Files.readAllBytes(Paths.get(ddiFile.getAbsolutePath()))));
        logger.info(datasetAsDdi);
        String result = DdiExportUtil.datasetDtoAsJson2ddi(datasetVersionAsJson);
        logger.info(result);
        boolean filesMinimallySupported = true;
        if (filesMinimallySupported) {
            assertEquals(datasetAsDdi, result);
        }
    }

}
