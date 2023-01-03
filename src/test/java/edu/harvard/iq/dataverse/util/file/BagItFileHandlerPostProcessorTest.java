package edu.harvard.iq.dataverse.util.file;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author adaybujeda
 */
public class BagItFileHandlerPostProcessorTest {

    private BagItFileHandlerPostProcessor target = new BagItFileHandlerPostProcessor();

    @Test
    public void should_return_null_when_datafiles_are_null() throws Exception {
        List<DataFile> result = target.process(null);
        MatcherAssert.assertThat(result, Matchers.nullValue());
    }

    @Test
    public void should_ignore_mac_control_files() throws Exception {
        String bagEntry = UUID.randomUUID().toString();
        String macFile03 = ".DS_Store";
        String macFile04 = "._.DS_Store";
        List<DataFile> dataFiles = createDataFiles(bagEntry, macFile03, macFile04);

        List<DataFile> result = target.process(dataFiles);
        MatcherAssert.assertThat(result.size(), Matchers.is(1));
        MatcherAssert.assertThat(result.get(0).getCurrentName(), Matchers.is(bagEntry));
    }

    @Test
    public void should_ignore_empty_files() throws Exception {
        String bagEntry = UUID.randomUUID().toString();
        String fileToIgnore = "";
        List<DataFile> dataFiles = createDataFiles(bagEntry, fileToIgnore);

        List<DataFile> result = target.process(dataFiles);
        MatcherAssert.assertThat(result.size(), Matchers.is(1));
        MatcherAssert.assertThat(result.get(0).getCurrentName(), Matchers.is(bagEntry));
    }

    @Test
    public void should_ignore_files_that_start_with_dot_underscore() throws Exception {
        String bagEntry = UUID.randomUUID().toString();
        String fileToIgnore = "._FileNameToIgnore";
        List<DataFile> dataFiles = createDataFiles(bagEntry, fileToIgnore);

        List<DataFile> result = target.process(dataFiles);
        MatcherAssert.assertThat(result.size(), Matchers.is(1));
        MatcherAssert.assertThat(result.get(0).getCurrentName(), Matchers.is(bagEntry));
    }

    @Test
    public void should_ignore_files_that_start_with_double_underscore() throws Exception {
        String bagEntry = UUID.randomUUID().toString();
        String fileToIgnore = "__FileNameToIgnore";
        String validFile = "validName";
        List<DataFile> dataFiles = createDataFiles(bagEntry, fileToIgnore);

        List<DataFile> result = target.process(dataFiles);
        MatcherAssert.assertThat(result.size(), Matchers.is(1));
        MatcherAssert.assertThat(result.get(0).getCurrentName(), Matchers.is(bagEntry));
    }

    private List<DataFile> createDataFiles(String... filePathItems) throws Exception {
        List<DataFile> dataFiles = new ArrayList<>(filePathItems.length);

        for(String filePath:  filePathItems) {
            String fileName = Path.of(filePath).getFileName().toString();
            DataFile dataFile = new DataFile();
            dataFile.setId(MocksFactory.nextId());
            dataFile.getFileMetadatas().add(new FileMetadata());
            dataFile.getLatestFileMetadata().setLabel(fileName);
            dataFiles.add(dataFile);
        }

        return dataFiles;
    }

}