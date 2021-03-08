package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

public class StorageIOUtils {

    private static final Logger logger = LoggerFactory.getLogger(StorageIOUtils.class);


    public static File obtainAsLocalFile(StorageIO<DataFile> storageIO, boolean withTemporaryFile) throws IOException {
        storageIO.open();

        if (!withTemporaryFile) {
            return storageIO.getFileSystemPath().toFile();
        }

        File localFile = File.createTempFile("tempLocalFile", ".tmp");
        
        try (
                ReadableByteChannel dataFileChannel = storageIO.getReadChannel();
                FileOutputStream tempIngestSourceOutputStream = new FileOutputStream(localFile);
                FileChannel tempIngestSourceChannel = tempIngestSourceOutputStream.getChannel()) {
            
            tempIngestSourceChannel.transferFrom(dataFileChannel, 0, storageIO.getSize());
            logger.debug("Saved " + storageIO.getSize() + " bytes in a local temp file.");
            return localFile;
        }

    }
    

    public static File obtainAuxAsLocalFile(StorageIO<DataFile> storageIO, String auxTag, boolean withTemporaryFile) throws IOException {
        storageIO.open();

        if (!withTemporaryFile) {
            return storageIO.getAuxObjectAsPath(auxTag).toFile();
        }

        File localFile = File.createTempFile("tempLocalAuxFile", ".tmp");
        
        try (
                ReadableByteChannel dataFileChannel = (ReadableByteChannel)storageIO.openAuxChannel(auxTag);
                FileOutputStream tempIngestSourceOutputStream = new FileOutputStream(localFile);
                FileChannel tempIngestSourceChannel = tempIngestSourceOutputStream.getChannel()) {

            long auxObjectSize = storageIO.getAuxObjectSize(auxTag);
            tempIngestSourceChannel.transferFrom(dataFileChannel, 0, auxObjectSize);
            logger.debug("Saved " + storageIO.getSize() + " bytes in a local temp file.");
            return localFile;
        }

    }
}
