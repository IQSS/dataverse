package edu.harvard.iq.dataverse.util.file;

import edu.harvard.iq.dataverse.DataFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author adaybujeda
 */
public class BagItFileHandlerPostProcessor {

    private static final Logger logger = Logger.getLogger(BagItFileHandlerPostProcessor.class.getCanonicalName());

    public static final List<String> FILES_TO_IGNORE = Arrays.asList("__", "._", ".DS_Store");

    public List<DataFile> process(List<DataFile> items) {
        if(items == null) {
            return null;
        }

        List<DataFile> filteredItems = new ArrayList<>(items.size());

        for(DataFile item: items) {
            String fileName = item.getCurrentName();
            if(fileName == null || fileName.isEmpty()) {
                continue;
            }

            if(FILES_TO_IGNORE.stream().anyMatch(prefix -> fileName.startsWith(prefix))) {
                logger.fine(String.format("action=BagItFileHandlerPostProcessor result=ignore-entry file=%s", fileName));
                continue;
            }

            filteredItems.add(item);
        }

        return filteredItems;
    }
}
