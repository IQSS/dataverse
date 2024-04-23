package edu.harvard.iq.dataverse.util.bagit;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author adaybujeda
 */
public enum BagChecksumType {
    MD5("manifest-md5.txt", inputStream -> DigestUtils.md5Hex(inputStream)),
    SHA1("manifest-sha1.txt", inputStream -> DigestUtils.sha1Hex(inputStream)),
    SHA256("manifest-sha256.txt", inputStream -> DigestUtils.sha256Hex(inputStream)),
    SHA512("manifest-sha512.txt", inputStream -> DigestUtils.sha512Hex(inputStream));

    private final String fileName;
    private final InputStreamDigester inputStreamDigester;

    private BagChecksumType(String fileName, InputStreamDigester inputStreamDigester) {
        this.fileName = fileName;
        this.inputStreamDigester = inputStreamDigester;
    }

    public static List<BagChecksumType> asList() {
        return Arrays.asList(BagChecksumType.values());
    }

    public String getFileName() {
        return fileName;
    }

    public InputStreamDigester getInputStreamDigester() {
        return inputStreamDigester;
    }

    public static interface InputStreamDigester {
        public String digest(InputStream inputStream) throws Exception;
    }
}
