package edu.harvard.iq.dataverse.settings;

import org.junit.jupiter.api.AfterAll;
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
import org.junit.jupiter.api.Disabled;

@Disabled
class ConfigCheckServiceTest {
    
    @TempDir
    static Path testDir;
    
    private static final String testDirProp = "test.filesDir";
    
    @AfterAll
    static void tearDown() {
        System.clearProperty(testDirProp);
    }
    
    @Nested
    class TestDirNotAbsolute {
        @Test
        void nonAbsolutePathForTestDir() {
            System.setProperty(testDirProp, "foobar");
            ConfigCheckService sut = new ConfigCheckService();
            Assertions.assertFalse(sut.checkSystemDirectories());
        }
    }

    @Nested
    class TestDirNotWritable {
        
        Path notWriteableSubfolder = testDir.resolve("readonly");
        
        @BeforeEach
        void setUp() throws IOException {
            Files.createDirectory(notWriteableSubfolder);
            Files.setPosixFilePermissions(notWriteableSubfolder, Set.of(OWNER_READ, GROUP_READ));
            System.setProperty(testDirProp, notWriteableSubfolder.toString());
        }
        
        @Test
        void writeCheckFails() {
            Assumptions.assumeTrue(Files.exists(notWriteableSubfolder));
            
            ConfigCheckService sut = new ConfigCheckService();
            Assertions.assertFalse(sut.checkSystemDirectories());
        }
    }
    
    @Nested
    class TestDirNotExistent {
        
        Path notExistTestfolder = testDir.resolve("parent-readonly");
        Path notExistConfigSubfolder = notExistTestfolder.resolve("foobar");
        
        @BeforeEach
        void setUp() throws IOException {
            Files.createDirectory(notExistTestfolder);
            // Make test dir not writeable, so the subfolder cannot be created
            Files.setPosixFilePermissions(notExistTestfolder, Set.of(OWNER_READ, GROUP_READ));
            System.setProperty(testDirProp, notExistConfigSubfolder.toString());
        }
        
        @Test
        void mkdirFails() {
            Assumptions.assumeTrue(Files.exists(notExistTestfolder));
            Assumptions.assumeFalse(Files.exists(notExistConfigSubfolder));
            
            ConfigCheckService sut = new ConfigCheckService();
            Assertions.assertFalse(sut.checkSystemDirectories());
        }
    }
    
    @Nested
    class TestDirCreated {

        Path missingToBeCreatedTestfolder = testDir.resolve("create-me");
        Path missingToBeCreatedSubfolder = missingToBeCreatedTestfolder.resolve("foobar");
        
        @BeforeEach
        void setUp() throws IOException {
            Files.createDirectory(missingToBeCreatedTestfolder);
            System.setProperty(testDirProp, missingToBeCreatedSubfolder.toString());
        }
        
        @Test
        void mkdirSucceeds() {
            Assumptions.assumeTrue(Files.exists(missingToBeCreatedTestfolder));
            Assumptions.assumeFalse(Files.exists(missingToBeCreatedSubfolder));
            
            ConfigCheckService sut = new ConfigCheckService();
            Assertions.assertTrue(sut.checkSystemDirectories());
        }
    }
    
}