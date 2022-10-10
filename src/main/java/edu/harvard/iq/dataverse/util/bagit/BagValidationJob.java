package edu.harvard.iq.dataverse.util.bagit;
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author qqmyers@hotmail.com
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.zip.ZipException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFile.ChecksumType;

import org.apache.commons.compress.utils.IOUtils;

/**
 * @author Jim
 *
 */
public class BagValidationJob implements Runnable {

    private static final Logger log = Logger.getLogger(BagValidationJob.class.getCanonicalName());

    private static ZipFile zf = null;
    private static BagGenerator bagGenerator = null;

    private String hash;
    private String name;
    private String basePath;
    private static ChecksumType hashtype;

    public BagValidationJob(String bagName, String value, String key) throws IllegalStateException {
        if (zf == null || bagGenerator == null) {
            throw new IllegalStateException(
                    "Static Zipfile and BagGenerator must be set before creating ValidationJobs");
        }
        basePath=bagName;
        hash = value;
        name = key;

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {

        String realHash = generateFileHash(basePath + "/" + name, zf);
        if (hash.equals(realHash)) {
            log.fine("Valid hash for " + name);
        } else {
            log.severe("Invalid " + bagGenerator.getHashtype().name() + " for " + name);
            log.fine("As sent: " + hash);
            log.fine("As calculated: " + realHash);
        }
    }

    private String generateFileHash(String name, ZipFile zf) {

        String realHash = null;
        
        ZipArchiveEntry archiveEntry1 = zf.getEntry(name);
        
        if(archiveEntry1 != null) {
        // Error check - add file sizes to compare against supplied stats
        log.fine("Getting stream for " + name);
        long start = System.currentTimeMillis();
        InputStream inputStream = null;
        
        try {
            inputStream = zf.getInputStream(archiveEntry1);
            if (hashtype.equals(DataFile.ChecksumType.SHA1)) {
                realHash = DigestUtils.sha1Hex(inputStream);
            } else if (hashtype.equals(DataFile.ChecksumType.SHA256)) {
                realHash = DigestUtils.sha256Hex(inputStream);
            } else if (hashtype.equals(DataFile.ChecksumType.SHA512)) {
                realHash = DigestUtils.sha512Hex(inputStream);
            } else if (hashtype.equals(DataFile.ChecksumType.MD5)) {
                realHash = DigestUtils.md5Hex(inputStream);
            } else {
                log.warning("Unknown hash type: " + hashtype.name());
            }

        } catch (ZipException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        log.fine("Retrieve/compute time = " + (System.currentTimeMillis() - start) + " ms");
        // Error check - add file sizes to compare against supplied stats
        bagGenerator.incrementTotalDataSize(archiveEntry1.getSize());
        } else {
            log.warning("Entry " + name + " not found in zipped bag: not validated");
        }
        return realHash;
    }

    public static void setZipFile(ZipFile zf) {
        BagValidationJob.zf = zf;
    }

    public static void setBagGenerator(BagGenerator bg) {
        bagGenerator = bg;
        hashtype = bagGenerator.getHashtype();
        if (hashtype == null) {
            log.warning("Null hashtype. Validation will not occur");
        }

    }

}
