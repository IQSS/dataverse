package edu.harvard.iq.dataverse.export.ddi;

import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

public class DdiExportUtilTest {

    private static final Logger logger = Logger.getLogger(DdiExportUtilTest.class.getCanonicalName());

    @Test
    public void testJson2DdiNoFiles() throws Exception {
        String datasetAsDdi = XmlPrinter.prettyPrintXml(new String(Files
                .readAllBytes(Paths.get(getClass().getClassLoader()
                    .getResource("xml/export/ddi/dataset-finch1.xml").toURI()))));
        logger.info(datasetAsDdi);
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader()
                .getResource("json/export/ddi/dataset-finch1.json").toURI())));

        String result = DdiExportUtil.datasetDtoAsJson2ddi(datasetVersionAsJson, "https://localhost:8080");

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


        String datasetAsDdi = XmlPrinter.prettyPrintXml(new String(Files
                .readAllBytes(Paths.get(getClass().getClassLoader()
                        .getResource("xml/export/ddi/dataset-spruce1.xml").toURI()))));
        logger.info(datasetAsDdi);
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader()
                .getResource("json/export/ddi/dataset-spruce1.json").toURI())));
        String result = DdiExportUtil.datasetDtoAsJson2ddi(datasetVersionAsJson, "https://localhost:8080");

        logger.info(result);
        boolean filesMinimallySupported = false;
        // TODO: 
        // setting "filesMinimallySupported" to false here, thus disabling the test;
        // before we can reenable it again, we'll need to figure out what to do 
        // with the access URLs, that are now included in the fileDscr and otherMat
        // sections. So a) we'll need to add something like URI=http://localhost/api/access/datafile/12 to 
        // dataset-spruce1.xml, above; and modify the DDI export util so that 
        // it can be instructed to use "localhost" for the API urls (otherwise 
        // it will use the real hostname). -- L.A. 4.5
        if (filesMinimallySupported) {
            assertEquals(datasetAsDdi, result);
        }
    }

}
