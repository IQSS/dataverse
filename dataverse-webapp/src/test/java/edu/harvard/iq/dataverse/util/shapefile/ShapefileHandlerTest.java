package edu.harvard.iq.dataverse.util.shapefile;

import edu.harvard.iq.dataverse.util.ShapefileHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static edu.harvard.iq.dataverse.util.ShapefileHandler.SHP_XML_EXTENSION;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author raprasad
 */
public class ShapefileHandlerTest {


    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();


    @Test
    public void testCreateZippedNonShapefile() throws IOException {

        List<String> file_names = Arrays.asList("not-quite-a-shape.shp", "not-quite-a-shape.shx", "not-quite-a-shape.dbf", "not-quite-a-shape.pdf"); //, "prj");
        File zipfile_obj = createAndZipFiles(file_names, "not-quite-a-shape.zip");

        ShapefileHandler shp_handler = new ShapefileHandler(zipfile_obj);
        shp_handler.DEBUG = true;

        assertThat(shp_handler.containsShapefile()).isFalse();

        assertThat(shp_handler.getFileGroups()).isNotEmpty();
        assertThat(shp_handler.getFileGroups()).containsOnlyKeys("not-quite-a-shape");
        assertThat(shp_handler.getFileGroups()).extractingByKey("not-quite-a-shape").asList()
                .containsExactly("shp", "shx", "dbf", "pdf");
    }


    @Test
    public void testZippedTwoShapefiles() throws IOException {

        List<String> file_names = Arrays.asList("shape1.shp", "shape1.shx", "shape1.dbf", "shape1.prj", "shape1.fbn", "shape1.fbx", // 1st shapefile
                                                "shape2.shp", "shape2.shx", "shape2.dbf", "shape2.prj",     // 2nd shapefile
                                                "shape2.txt", "shape2.pdf", "shape2"                  // single files, same basename as 2nd shapefile
                                                );

        File zipfile_obj = createAndZipFiles(file_names, "two-shapes.zip");

        ShapefileHandler shp_handler = new ShapefileHandler(zipfile_obj);
        shp_handler.DEBUG = true;

        assertThat(shp_handler.containsShapefile()).isTrue();
        assertThat(shp_handler.errorFound).isFalse();

        assertThat(shp_handler.getFileGroups()).isNotEmpty();
        assertThat(shp_handler.getFileGroups()).containsOnlyKeys("shape1", "shape2");
        assertThat(shp_handler.getFileGroups()).extractingByKey("shape1").asList()
            .containsExactly("shp", "shx", "dbf", "prj", "fbn", "fbx");
        assertThat(shp_handler.getFileGroups()).extractingByKey("shape2").asList()
            .containsExactly("shp", "shx", "dbf", "prj", "txt", "pdf", ShapefileHandler.BLANK_EXTENSION);
    }

    @Test
    public void testZippedTwoShapefiles_reshape() throws IOException {
        List<String> file_names = Arrays.asList("shape1.shp", "shape1.shx", "shape1.dbf", "shape1.prj", "shape1.fbn", "shape1.fbx", // 1st shapefile
                "shape2.shp", "shape2.shx", "shape2.dbf", "shape2.prj",     // 2nd shapefile
                "shape2.txt", "shape2.pdf", "shape2"                  // single files, same basename as 2nd shapefile
                );

        File zipfile_obj = createAndZipFiles(file_names, "two-shapes.zip");
        File test_unzip_folder = this.tempFolder.newFolder("test_unzip").getAbsoluteFile();


        ShapefileHandler shp_handler = new ShapefileHandler(zipfile_obj);
        shp_handler.rezipShapefileSets(test_unzip_folder);

        assertThat(test_unzip_folder.list()).containsOnly("shape1.zip", "shape2.zip", "shape2.txt", "shape2.pdf", "shape2");
    }
    

    @Test
    public void testZippedShapefileWithExtraFiles() throws IOException {

        List<String> file_names = Arrays.asList("shape1.shp", "shape1.shx", "shape1.dbf", "shape1.prj", "shape1.pdf", "shape1.cpg", "shape1." + SHP_XML_EXTENSION, "README.md", "shape_notes.txt");
        File zipfile_obj = createAndZipFiles(file_names, "shape-plus.zip");

        ShapefileHandler shp_handler = new ShapefileHandler(zipfile_obj);
        shp_handler.DEBUG = true;

        assertThat(shp_handler.containsShapefile()).isTrue();

        assertThat(shp_handler.getFileGroups()).isNotEmpty();
        assertThat(shp_handler.getFileGroups()).containsOnlyKeys("shape1", "README", "shape_notes");

        assertThat(shp_handler.getFileGroups()).extractingByKey("shape1").asList()
            .containsExactly("shp", "shx", "dbf", "prj", "pdf", "cpg", SHP_XML_EXTENSION);
        
        assertThat(shp_handler.getFileGroups()).extractingByKey("README").asList()
            .containsExactly("md");
        assertThat(shp_handler.getFileGroups()).extractingByKey("shape_notes").asList()
            .containsExactly("txt");
    }

    @Test
    public void testZippedShapefileWithExtraFiles_reshape() throws IOException {

        List<String> file_names = Arrays.asList("shape1.shp", "shape1.shx", "shape1.dbf", "shape1.prj", "shape1.pdf", "shape1.cpg", "shape1." + SHP_XML_EXTENSION, "README.md", "shape_notes.txt");
        File zipfile_obj = createAndZipFiles(file_names, "shape-plus.zip");
        File unzip2Folder = this.tempFolder.newFolder("test_unzip2").getAbsoluteFile();
        
        ShapefileHandler shp_handler = new ShapefileHandler(zipfile_obj);
        shp_handler.rezipShapefileSets(unzip2Folder);

        assertThat(unzip2Folder.list()).containsOnly("shape1.zip", "shape1.pdf", "README.md", "shape_notes.txt");
    }

    /**
     * Convenience class to create .zip file and return a FileInputStream
     * 
     * @param fileNamesToZip - List of filenames to add to .zip.  These names will be used to create 0 length files
     * @param zipFileName - Name of .zip file to create
    */
    private File createAndZipFiles(List<String> fileNamesToZip, String zipFileName) throws IOException {

        File zipFileObj = this.tempFolder.newFile(zipFileName);
        
        try (ZipOutputStream zip_stream = new ZipOutputStream(Files.newOutputStream(zipFileObj.toPath()))) {

            for (String fileName : fileNamesToZip) {
                this.addToZipFile(fileName, "".getBytes(StandardCharsets.UTF_8), zip_stream);
            }
        }

        return zipFileObj;

    }

    private void addToZipFile(String fileName, byte[] fileAsBytesToZip, ZipOutputStream zipOutputStream) throws IOException {

        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOutputStream.putNextEntry(zipEntry);
        zipOutputStream.write(fileAsBytesToZip);
        zipOutputStream.closeEntry();
    }

}
    


