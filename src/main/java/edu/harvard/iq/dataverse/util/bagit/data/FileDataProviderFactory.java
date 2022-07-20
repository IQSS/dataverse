package edu.harvard.iq.dataverse.util.bagit.data;

import edu.harvard.iq.dataverse.DataFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 *
 * @author adaybujeda
 */
public class FileDataProviderFactory {

    public FileDataProvider getFileDataProvider(File file) throws IOException {
        return new ZipFileDataProvider(file.getName(), file);
    }

    public FileDataProvider getFileDataProvider(Path folderLocation) {
        return new FolderDataProvider(folderLocation);
    }

    public FileDataProvider getFileDataProvider(String name, List<DataFile> datafiles) {
        return new DataFileDataProvider(name, datafiles);
    }

}
