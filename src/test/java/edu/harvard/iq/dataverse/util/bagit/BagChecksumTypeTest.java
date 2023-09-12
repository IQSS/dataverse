package edu.harvard.iq.dataverse.util.bagit;

import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 *
 * @author adaybujeda
 */
public class BagChecksumTypeTest {

    @Test
    public void should_validate_all_digesters() throws Exception {
        Map<BagChecksumType, String> expectedResults = Map.of(
                BagChecksumType.MD5, "098f6bcd4621d373cade4e832627b4f6",
                BagChecksumType.SHA1, "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3",
                BagChecksumType.SHA256, "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                BagChecksumType.SHA512, "ee26b0dd4af7e749aa1a8ee3c10ae9923f618980772e473f8819a5d4940e0db27ac185f8a0e1d5f84f88bc887fd67b143732c304cc5fa9ad8e6f57f50028a8ff"
        );

        for(BagChecksumType type: BagChecksumType.values()) {
            String expectedDigestForTestString = expectedResults.get(type);
            // Ensure that any new types get added here
            MatcherAssert.assertThat(expectedDigestForTestString, Matchers.notNullValue());
            String calculatedDigest = type.getInputStreamDigester().digest(IOUtils.toInputStream("test", "UTF-8"));
            MatcherAssert.assertThat(calculatedDigest, Matchers.is(expectedDigestForTestString));
        }
    }

    @Test
    public void should_validate_all_manifest_filenames() throws Exception {
        Map<BagChecksumType, String> expectedResults = Map.of(
                BagChecksumType.MD5, "manifest-md5.txt",
                BagChecksumType.SHA1, "manifest-sha1.txt",
                BagChecksumType.SHA256, "manifest-sha256.txt",
                BagChecksumType.SHA512, "manifest-sha512.txt"
        );

        for(BagChecksumType type: BagChecksumType.values()) {
            String expectedFilename = expectedResults.get(type);
            // Ensure that any new types get added here
            MatcherAssert.assertThat(expectedFilename, Matchers.notNullValue());
            MatcherAssert.assertThat(type.getFileName(), Matchers.is(expectedFilename));
        }
    }
}