package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.FileMetadata;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FileUtilTest {

    @Test
    public void testGetCiteDataFileFilename() {

        assertEquals(null, FileUtil.getCiteDataFileFilename(null, null));

        FileMetadata png = new FileMetadata();
        png.setLabel("trees.png");
        assertEquals("trees.png-endnote.xml", FileUtil.getCiteDataFileFilename(png, FileUtil.FileCitationExtension.ENDNOTE));
        assertEquals("trees.png.ris", FileUtil.getCiteDataFileFilename(png, FileUtil.FileCitationExtension.RIS));
        assertEquals("trees.png.bib", FileUtil.getCiteDataFileFilename(png, FileUtil.FileCitationExtension.BIBTEX));
        assertEquals(null, FileUtil.getCiteDataFileFilename(png, null));

        FileMetadata tabular = new FileMetadata();
        tabular.setLabel("50by1000.tab");
        assertEquals("50by1000-endnote.xml", FileUtil.getCiteDataFileFilename(tabular, FileUtil.FileCitationExtension.ENDNOTE));
        assertEquals("50by1000.ris", FileUtil.getCiteDataFileFilename(tabular, FileUtil.FileCitationExtension.RIS));
        assertEquals("50by1000.bib", FileUtil.getCiteDataFileFilename(tabular, FileUtil.FileCitationExtension.BIBTEX));
    }

}
