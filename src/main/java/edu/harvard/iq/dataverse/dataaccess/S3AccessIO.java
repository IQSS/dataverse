package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 *
 * @author Matthew Dunlap
 * @param <T> what it stores
 */
/* 
    Experimental Amazon AWS S3 driver
 */
public class S3AccessIO<T extends DvObject> extends DataFileIO<T> {

    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.dataaccess.S3AccessIO");

    //FIXME: Empty
    public S3AccessIO() {
        this(null);
    }

    //FIXME: Empty
    public S3AccessIO(T dvObject) {
        this(dvObject, null);
    }
    
    //FIXME: Empty
    public S3AccessIO(T dvObject, DataAccessRequest req) {
        super(dvObject, req);
        this.setIsLocalFile(false);
    }

    //FIXME: Delete vars? 
    private boolean isReadAccess = false;
    private boolean isWriteAccess = false;


    //FIXME: Copied, change?
    @Override
    public boolean canRead() {
        return isReadAccess;
    }

    //FIXME: Copied, change?
    @Override
    public boolean canWrite() {
        return isWriteAccess;
    }

    
//    @Override
//    public DvObjectType getDvObjectType() {
//     
//    }
    
    //FIXME: Empty
    @Override
    public void open(DataAccessOption... options) throws IOException {
    }

    // DataFileIO method for copying a local Path (for ex., a temp file), into this DataAccess location:

    //FIXME: Empty
    @Override
    public void savePath(Path fileSystemPath) throws IOException {
    }
    
    //FIXME: Empty
    @Override
    public void saveInputStream(InputStream inputStream) throws IOException {
    }
    
    //FIXME: Empty
    @Override
    public void delete() throws IOException {
    }

    //FIXME: Empty
    @Override
    public Channel openAuxChannel(String auxItemTag, DataAccessOption... options) throws IOException {
        return null;
    }

    //FIXME: Empty
    @Override
    public boolean isAuxObjectCached(String auxItemTag) throws IOException {
        return false;
    }
    
    //FIXME: Empty
    @Override
    public long getAuxObjectSize(String auxItemTag) throws IOException {
        return 0;
    }
    
    //FIXME: Empty
    @Override 
    public Path getAuxObjectAsPath(String auxItemTag) throws IOException {
        return null;
    }

    //FIXME: Empty
    @Override
    public void backupAsAux(String auxItemTag) throws IOException {
    }

    //FIXME: Empty
    @Override
    // this method copies a local filesystem Path into this DataAccess Auxiliary location:
    public void savePathAsAux(Path fileSystemPath, String auxItemTag) throws IOException {
    }
    
    //FIXME: Empty
    // this method copies a local InputStream into this DataAccess Auxiliary location:
    @Override
    public void saveInputStreamAsAux(InputStream inputStream, String auxItemTag) throws IOException {
    }
    
    //FIXME: Empty
    @Override
    public List<String>listAuxObjects() throws IOException {
        return null;
    }
    
    //FIXME: Empty
    @Override
    public void deleteAuxObject(String auxItemTag) throws IOException {
    }
    
    //FIXME: Empty
    @Override
    public void deleteAllAuxObjects() throws IOException {
    }
    
    //FIXME: Empty
    @Override
    public String getStorageLocation() {
        return null;
    }

    //FIXME: Empty
    @Override
    public Path getFileSystemPath() throws IOException {
        return null;
    }
    
    //FIXME: Empty
    @Override
    public boolean exists() throws IOException {
        return false;
    }

    //FIXME: Empty
    @Override
    public WritableByteChannel getWriteChannel() throws IOException {
        return null;
    }
    
    //FIXME: Empty
    @Override  
    public OutputStream getOutputStream() throws IOException {
        return null;
    }
    
    @Override
    public InputStream getAuxFileAsInputStream(String auxItemTag) throws IOException {
        return null;
    }

    @Override
    public String getSwiftContainerName() {
        return null;
    }
    
    // Auxilary helper methods, S3-specific:
    // // FIXME: Refer to swift implementation while implementing S3
    

     
}
