package edu.harvard.iq.dataverse.util;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * A utility class for creating secure temporary files with restricted permissions.
 * This class provides an API to generate temporary files that restrict access to only the file owner.
 * The implementation ensures that file permissions are secure and appropriate based on the operating system.
 * <ul>
 * <li>On POSIX-compliant systems (e.g., Linux, macOS), created files will have permissions set to "rw-------" (owner read/write only).</li>
 * <li>On Windows, the method relies on the operating system's ACL protections for files created in the user’s temporary directory.</li>
 * </ul>
 */
public final class SecureTempFiles {
    
    private SecureTempFiles() {
        // Intentionally left blank and hidden as best practice for utility classes.
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
            // Windows: the per-user temp directory (%TEMP%) is already ACL-protected so only the owner (and admins) can access it.
            return Files.createTempFile(prefix, suffix);
        }
    }
}
