package edu.harvard.iq.dataverse.settings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;

class ConfigCheckServiceTest {

    @Nested
    class TestDirNotWritable {
        @TempDir
        Path testDir;
        
        private String oldUploadDirSetting;
        
        @BeforeEach
        void setUp() throws IOException {
            Files.setPosixFilePermissions(this.testDir, Set.of(OWNER_READ, GROUP_READ));
            
            // TODO: This is a workaround until PR #9273 is merged, providing the ability to lookup values for
            //       @JvmSetting from static methods. Should be deleted.
            this.oldUploadDirSetting = System.getProperty(JvmSettings.UPLOADS_DIRECTORY.getScopedKey());
            System.setProperty(JvmSettings.UPLOADS_DIRECTORY.getScopedKey(), this.testDir.toString());
        }
        
        @AfterEach
        void tearDown() {
            // TODO: This is a workaround until PR #9273 is merged, providing the ability to lookup values for
            //       @JvmSetting from static methods. Should be deleted.
            if (this.oldUploadDirSetting != null)
                System.setProperty(JvmSettings.UPLOADS_DIRECTORY.getScopedKey(), this.oldUploadDirSetting);
        }
        
        @Test
        void writeCheckFails() {
            Assumptions.assumeTrue(Files.exists(this.testDir));
            
            ConfigCheckService sut = new ConfigCheckService();
            Assertions.assertFalse(sut.checkSystemDirectories());
        }
    }
    
    @Nested
    class TestDirNotExistent {
        @TempDir
        Path testDir;
        String subFolder = "foobar";
        
        String oldUploadDirSetting;
        
        @BeforeEach
        void setUp() throws IOException {
            // Make test dir not writeable, so the subfolder cannot be created
            Files.setPosixFilePermissions(this.testDir, Set.of(OWNER_READ, GROUP_READ));
            
            // TODO: This is a workaround until PR #9273 is merged, providing the ability to lookup values for
            //       @JvmSetting from static methods. Should be deleted.
            oldUploadDirSetting = System.getProperty(JvmSettings.UPLOADS_DIRECTORY.getScopedKey());
            System.setProperty(JvmSettings.UPLOADS_DIRECTORY.getScopedKey(), this.testDir.resolve(this.subFolder).toString());
        }
        
        @AfterEach
        void tearDown() {
            // TODO: This is a workaround until PR #9273 is merged, providing the ability to lookup values for
            //       @JvmSetting from static methods. Should be deleted.
            if (this.oldUploadDirSetting != null)
                System.setProperty(JvmSettings.UPLOADS_DIRECTORY.getScopedKey(), this.oldUploadDirSetting);
        }
        
        @Test
        void mkdirFails() {
            Assumptions.assumeTrue(Files.exists(this.testDir));
            Assumptions.assumeFalse(Files.exists(this.testDir.resolve(this.subFolder)));
            
            ConfigCheckService sut = new ConfigCheckService();
            Assertions.assertFalse(sut.checkSystemDirectories());
        }
    }
    
}