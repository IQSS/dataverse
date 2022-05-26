package edu.harvard.iq.dataverse.util.bagit.data;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author adaybujeda
 */
public interface FileDataProvider extends Closeable {

    public String getName();
    public List<Path> getFilePaths();
    public Optional<InputStreamProvider> getInputStreamProvider(Path filePath);

    public static interface InputStreamProvider {
        public InputStream getInputStream() throws IOException;
    }
}
