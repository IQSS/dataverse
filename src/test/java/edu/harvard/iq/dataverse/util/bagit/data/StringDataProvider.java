package edu.harvard.iq.dataverse.util.bagit.data;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author adaybujeda
 */
public class StringDataProvider implements FileDataProvider {
    private final Boolean withDelay;
    private final List<Path> items;

    public StringDataProvider(Boolean withDelay, List<Path> items) {
        this.withDelay = withDelay;
        this.items = items;
    }

    @Override
    public String getName() {
        return "StringDataProvider";
    }

    @Override
    public List<Path> getFilePaths() {
        return List.copyOf(items);
    }

    @Override
    public Optional<InputStreamProvider> getInputStreamProvider(Path filePath) {
        return items.stream().filter(item -> item.equals(filePath)).findFirst().map(item -> () -> {
            if (withDelay) {
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (Exception e) {
                }
            }
            return IOUtils.toInputStream(item.toString(), "UTF-8");
        });
    }

    @Override
    public void close() throws IOException {
    }
}
