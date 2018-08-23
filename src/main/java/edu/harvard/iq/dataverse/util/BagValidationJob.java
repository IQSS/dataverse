package edu.harvard.iq.dataverse.util;
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
import java.util.zip.ZipException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.log4j.Logger;

import org.apache.commons.compress.utils.IOUtils;

/**
 * @author Jim
 *
 */
public class BagValidationJob implements Runnable {

    private static final Logger log = Logger.getLogger(BagValidationJob.class);

    private static ZipFile zf = null;
    private static BagGenerator bagGenerator = null;

    private String hash;
    private String name;

    public BagValidationJob(String value, String key) throws IllegalStateException {
        if (zf == null || bagGenerator == null) {
            throw new IllegalStateException(
                    "Static Zipfile and BagGenerator must be set before creating ValidationJobs");
        }
        hash = value;
        name = key;

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {

        String realHash = generateFileHash(name, zf);
        if (hash.equals(realHash)) {
            log.debug("Valid hash for " + name);
        } else {
            log.error("Invalid " + bagGenerator.getHashtype() + " for " + name);
            log.debug("As sent: " + hash);
            log.debug("As calculated: " + realHash);
        }
    }

    private String generateFileHash(String name, ZipFile zf) {

        ZipArchiveEntry archiveEntry1 = zf.getEntry(name);
        // Error check - add file sizes to compare against supplied stats

        long start = System.currentTimeMillis();
        InputStream inputStream = null;
        String realHash = null;
        try {
            inputStream = zf.getInputStream(archiveEntry1);
            String hashtype = bagGenerator.getHashtype();
            if (hashtype != null) {
                if (hashtype.equals("SHA1 Hash")) {
                    realHash = DigestUtils.sha1Hex(inputStream);
                } else if (hashtype.equals("SHA512 Hash")) {
                    realHash = DigestUtils.sha512Hex(inputStream);
                }
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
        log.debug("Retrieve/compute time = "
                + (System.currentTimeMillis() - start) + " ms");
        // Error check - add file sizes to compare against supplied stats
        bagGenerator.incrementTotalDataSize(archiveEntry1.getSize());
        return realHash;
    }

    public static void setZipFile(ZipFile zf) {
        BagValidationJob.zf = zf;
    }

    public static void setBagGenerator(BagGenerator bg) {
        bagGenerator = bg;
    }

}
