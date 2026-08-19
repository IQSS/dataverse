package edu.harvard.iq.dataverse.util;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public final class SecureTempFiles {
    
    private SecureTempFiles() {
    }
    
    @SuppressWarnings("java:S5443") // Make SonarQube stop warning about "raw" temp file generator on Windows.
    public static Path createOwnerOnlyTempFile(String prefix, String suffix) throws IOException {
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            // POSIX (Linux, macOS): owner read/write only -> "rw-------" (0600)
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
            FileAttribute<Set<PosixFilePermission>> attr =
                PosixFilePermissions.asFileAttribute(perms);
            return Files.createTempFile(prefix, suffix, attr);
        } else {
            // Windows: the per-user temp directory (%TEMP%) is already
            // ACL-protected so only the owner (and admins) can access it.
            return Files.createTempFile(prefix, suffix);
        }
    }
}
