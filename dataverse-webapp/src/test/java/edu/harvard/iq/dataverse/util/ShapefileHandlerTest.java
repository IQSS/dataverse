package edu.harvard.iq.dataverse.util;

import com.google.common.collect.ImmutableMap;
import edu.harvard.iq.dataverse.persistence.datafile.ingest.IngestException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static edu.harvard.iq.dataverse.util.ShapefileHandler.SHP_XML_EXTENSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * @author raprasad
 */
public class ShapefileHandlerTest {


    @TempDir
    public Path tempFolder;


    @Test
    public void testCreateZippedNonShapefile() throws IOException {

        List<String> file_names = Arrays.asList("not-quite-a-shape.shp", "not-quite-a-shape.shx", "not-quite-a-shape.dbf", "not-quite-a-shape.pdf"); //, "prj");
        File zipfile_obj = createAndZipFiles(file_names, "not-quite-a-shape.zip");

        ShapefileHandler shp_handler = newShapeFileHandler(zipfile_obj);

        assertThat(shp_handler.containsShapefile()).isFalse();

        assertThat(shp_handler.getBaseNameExtensions()).isNotEmpty();
        assertThat(shp_handler.getBaseNameExtensions()).containsOnlyKeys("not-quite-a-shape");
        assertThat(shp_handler.getBaseNameExtensions()).extractingByKey("not-quite-a-shape").asList()
                .containsExactly("shp", "shx", "dbf", "pdf");
    }


    @Test
    public void testZippedTwoShapefiles() throws IOException {

        List<String> file_names = Arrays.asList("shape1.shp", "shape1.shx", "shape1.dbf", "shape1.prj", "shape1.fbn", "shape1.fbx", // 1st shapefile
                                                "shape2.shp", "shape2.shx", "shape2.dbf", "shape2.prj",     // 2nd shapefile
                                                "shape2.txt", "shape2.pdf", "shape2"                  // single files, same basename as 2nd shapefile
                                                );

        File zipfile_obj = createAndZipFiles(file_names, "two-shapes.zip");

        ShapefileHandler shp_handler = newShapeFileHandler(zipfile_obj);

        assertThat(shp_handler.containsShapefile()).isTrue();

        assertThat(shp_handler.getBaseNameExtensions()).isNotEmpty();
        assertThat(shp_handler.getBaseNameExtensions()).containsOnlyKeys("shape1", "shape2");
        assertThat(shp_handler.getBaseNameExtensions()).extractingByKey("shape1").asList()
            .containsExactly("shp", "shx", "dbf", "prj", "fbn", "fbx");
        assertThat(shp_handler.getBaseNameExtensions()).extractingByKey("shape2").asList()
            .containsExactly("shp", "shx", "dbf", "prj", "txt", "pdf", ShapefileHandler.BLANK_EXTENSION);
    }

    @Test
    public void testZipped__duplicate() throws IOException {

        List<String> file_names = Arrays.asList("shape2.shp", "shape2.shx", "shape2.dbf", "shape2.prj",     // 2nd shapefile
                "shape2.txt", "shape2.pdf", "shape2", "folder/shape2.pdf" // duplicate
        );

        File zipfile_obj = createAndZipFiles(file_names, "duplicate_file.zip");

        assertThatThrownBy(() -> newShapeFileHandler(zipfile_obj))
                .hasMessage("Found file-name collision: shape2.pdf");
    }

    @Test
    public void testZippedTwoShapefiles_reshape() throws IOException {
        List<String> file_names = Arrays.asList("shape1.shp", "shape1.shx", "shape1.dbf", "shape1.prj", "shape1.fbn", "shape1.fbx", // 1st shapefile
                "shape2.shp", "shape2.shx", "shape2.dbf", "shape2.prj",     // 2nd shapefile
                "shape2.txt", "shape2.pdf", "shape2"                  // single files, same basename as 2nd shapefile
                );

        File zipfile_obj = createAndZipFiles(file_names, "two-shapes.zip");
        File test_unzip_folder = Files.createDirectories(this.tempFolder.resolve("test_unzip")).toFile().getAbsoluteFile();
        File test_rezip_folder = Files.createDirectories(this.tempFolder.resolve("test_rezip")).toFile().getAbsoluteFile();


        ShapefileHandler shp_handler = newShapeFileHandler(zipfile_obj);
        shp_handler.reZipShapefileSets(test_unzip_folder, test_rezip_folder);

        assertThat(test_unzip_folder.list().length).isEqualTo(0);
        assertThat(test_rezip_folder.list())
                .containsOnly("shape1.zip", "shape2.zip", "shape2.txt", "shape2.pdf", "shape2");
    }
    

    @Test
    public void testZippedShapefileWithExtraFiles() throws IOException {

        List<String> file_names = Arrays.asList("shape1.shp", "shape1.shx", "shape1.dbf", "shape1.prj", "shape1.pdf", "shape1.cpg", "shape1." + SHP_XML_EXTENSION, "README.md", "shape_notes.txt");
        File zipfile_obj = createAndZipFiles(file_names, "shape-plus.zip");

        ShapefileHandler shp_handler = newShapeFileHandler(zipfile_obj);

        assertThat(shp_handler.containsShapefile()).isTrue();

        assertThat(shp_handler.getBaseNameExtensions()).isNotEmpty();
        assertThat(shp_handler.getBaseNameExtensions()).containsOnlyKeys("shape1", "README", "shape_notes");

        assertThat(shp_handler.getBaseNameExtensions()).extractingByKey("shape1").asList()
            .containsExactly("shp", "shx", "dbf", "prj", "pdf", "cpg", SHP_XML_EXTENSION);
        
        assertThat(shp_handler.getBaseNameExtensions()).extractingByKey("README").asList()
            .containsExactly("md");
        assertThat(shp_handler.getBaseNameExtensions()).extractingByKey("shape_notes").asList()
            .containsExactly("txt");
    }

    @Test
    public void testZippedShapefile__too_many_files() throws IOException {

        List<String> file_names = Arrays.asList("shape1.shp", "shape1.shx", "shape1.dbf", "shape1.prj", "shape1.pdf",
                "shape1.cpg", "shape1.shp.xml", "README.md", "shape_notes.txt");
        File zipfile_obj = createAndZipFiles(file_names, "shape-plus.zip");

        assertThatThrownBy(() -> new ShapefileHandler(zipfile_obj, 1024L, 8L))
                .isInstanceOf(IngestException.class)
                .hasMessage("There was a problem during ingest. Passing error key UNZIP_FILE_LIMIT_FAIL to report.");
    }

    @Test
    public void testZippedShapefile__too_big_files() throws IOException {
        Map<String, String> files = ImmutableMap.<String, String>builder().put("shape1.shp", "")
                .put("shape1.shx", "")
                .put("shape1.dbf", "")
                .put("shape1.prj", "")
                .put("shape1.pdf", RandomStringUtils.randomAlphanumeric(2048))
                .put("shape1.cpg", "")
                .put("shape1.shp.xml", "")
                .put("README.md", "")
                .put("shape_notes.txt", "")
                .build();
        File zipfile_obj = createAndZipFiles(files, "shape-plus.zip");

        assertThatThrownBy(() -> new ShapefileHandler(zipfile_obj, 1024L, 100L))
                .isInstanceOf(IngestException.class)
                .hasMessage("There was a problem during ingest. Passing error key UNZIP_SIZE_FAIL to report.");
    }

    @Test
    public void testZippedShapefileWithExtraFiles_reshape() throws IOException {

        List<String> file_names = Arrays.asList("shape1.shp", "shape1.shx", "shape1.dbf", "shape1.prj", "shape1.pdf", "shape1.cpg", "shape1." + SHP_XML_EXTENSION, "README.md", "shape_notes.txt");
        File zipfile_obj = createAndZipFiles(file_names, "shape-plus.zip");
        File unzip2Folder = Files.createDirectories(this.tempFolder.resolve("test_unzip2")).toFile().getAbsoluteFile();
        File rezip2Folder = Files.createDirectories(this.tempFolder.resolve("test_rezip2")).toFile().getAbsoluteFile();

        ShapefileHandler shp_handler = newShapeFileHandler(zipfile_obj);
        shp_handler.reZipShapefileSets(unzip2Folder, rezip2Folder);

        assertThat(unzip2Folder.list()).isEmpty();
        assertThat(rezip2Folder.list())
                .containsOnly("shape1.zip", "shape1.pdf", "README.md", "shape_notes.txt");
    }

    /**
     * Convenience class to create .zip file and return a FileInputStream
     * 
     * @param fileNamesToZip - List of filenames to add to .zip.  These names will be used to create 0 length files
     * @param zipFileName - Name of .zip file to create
    */
    private File createAndZipFiles(List<String> fileNamesToZip, String zipFileName) throws IOException {
        Map<String, String> filesWithContent = fileNamesToZip.stream().collect(LinkedHashMap::new,
                (map, fileName) -> map.put(fileName, ""), Map::putAll);
        return createAndZipFiles(filesWithContent, zipFileName);
    }

    private File createAndZipFiles(Map<String, String> filesToZip, String zipFileName) throws IOException {
        File zipFileObj = this.tempFolder.resolve(zipFileName).toFile();
        zipFileObj.createNewFile();

        try (ZipOutputStream zip_stream = new ZipOutputStream(Files.newOutputStream(zipFileObj.toPath()))) {
            for (Map.Entry<String, String> entry : filesToZip.entrySet()) {
                this.addToZipFile(entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8), zip_stream);
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

    private ShapefileHandler newShapeFileHandler(File zipfile_obj) {
        return new ShapefileHandler(zipfile_obj, 1024L, 100L);
    }
}
    


