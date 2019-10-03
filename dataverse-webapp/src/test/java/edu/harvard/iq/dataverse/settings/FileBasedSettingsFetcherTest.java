package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.settings.FileSettingLocations.SettingLocationType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileBasedSettingsFetcherTest {
    
    @Test
    public void loadSettings__PROPERTIES_FROM_CLASSPATH() {
        
        // given
        FileSettingLocations settingLocations = new FileSettingLocations();
        settingLocations.addLocation(SettingLocationType.CLASSPATH, "/test1.properties", false);
        
        FileBasedSettingsFetcher settingsFetcher = new FileBasedSettingsFetcher(settingLocations);
        
        // when
        settingsFetcher.loadSettings();
        
        // then
        assertEquals("keyval1", settingsFetcher.getSetting("BuiltinUsers.KEY"));
        assertEquals("someval1", settingsFetcher.getSetting(":SomeSetting"));
        assertEquals("testval1", settingsFetcher.getSetting(":Test1Setting"));
        assertThat(settingsFetcher.getAllSettings().keySet(),
                containsInAnyOrder("BuiltinUsers.KEY", ":SomeSetting", ":Test1Setting"));
    }
    
    @Test
    public void loadSettings__NOT_EXISTING_PROPERTIES_FROM_CLASSPATH() {
        
        // given
        FileSettingLocations settingLocations = new FileSettingLocations();
        settingLocations.addLocation(SettingLocationType.CLASSPATH, "/notexisting.properties", false);
        
        FileBasedSettingsFetcher settingsFetcher = new FileBasedSettingsFetcher(settingLocations);
        
        // when
        Executable loadSettingsOperation = settingsFetcher::loadSettings;
        
        // then
        assertThrows(RuntimeException.class, loadSettingsOperation);
    }
    
    @Test
    public void loadSettings__NOT_EXISTING_OPTIONAL_PROPERTIES_FROM_CLASSPATH() {
        
        // given
        FileSettingLocations settingLocations = new FileSettingLocations();
        settingLocations.addLocation(SettingLocationType.CLASSPATH, "/notexisting.properties", true);
        
        FileBasedSettingsFetcher settingsFetcher = new FileBasedSettingsFetcher(settingLocations);
        
        // when
        settingsFetcher.loadSettings();
        
        // then
        assertEquals(0, settingsFetcher.getAllSettings().size());
    }
    
    
    @Test
    public void loadSettings__PROPERTIES_FROM_FILESYSTEM(@TempDir Path tempDir) throws IOException {
        
        // given
        byte[] propertiesBytes = IOUtils.resourceToByteArray("/test1.properties");
        File filesystemPropertiesFile = new File(tempDir.toFile(), "filesystem.properties");
        FileUtils.writeByteArrayToFile(filesystemPropertiesFile, propertiesBytes);
        
        FileSettingLocations settingLocations = new FileSettingLocations();
        settingLocations.addLocation(SettingLocationType.FILESYSTEM, filesystemPropertiesFile.getAbsolutePath(), false);
        
        FileBasedSettingsFetcher settingsFetcher = new FileBasedSettingsFetcher(settingLocations);
        
        // when
        settingsFetcher.loadSettings();
        
        // then
        assertEquals("keyval1", settingsFetcher.getSetting("BuiltinUsers.KEY"));
        assertEquals("someval1", settingsFetcher.getSetting(":SomeSetting"));
        assertEquals("testval1", settingsFetcher.getSetting(":Test1Setting"));
        assertThat(settingsFetcher.getAllSettings().keySet(),
                containsInAnyOrder("BuiltinUsers.KEY", ":SomeSetting", ":Test1Setting"));
    }
    
    @Test
    public void loadSettings__NOT_EXISTING_PROPERTIES_FROM_FILESYSTEM() {
        
        // given
        FileSettingLocations settingLocations = new FileSettingLocations();
        settingLocations.addLocation(SettingLocationType.FILESYSTEM, "notexisting.properties", false);
        
        FileBasedSettingsFetcher settingsFetcher = new FileBasedSettingsFetcher(settingLocations);
        
        // when
        Executable loadSettingsOperation = settingsFetcher::loadSettings;
        
        // then
        assertThrows(RuntimeException.class, loadSettingsOperation);
    }
    
    @Test
    public void loadSettings__NOT_EXISTING_OPTIONAL_PROPERTIES_FROM_FILESYSTEM() {
        
        // given
        FileSettingLocations settingLocations = new FileSettingLocations();
        settingLocations.addLocation(SettingLocationType.FILESYSTEM, "notexisting.properties", true);
        
        FileBasedSettingsFetcher settingsFetcher = new FileBasedSettingsFetcher(settingLocations);
        
        // when
        settingsFetcher.loadSettings();
        
        // then
        assertEquals(0, settingsFetcher.getAllSettings().size());
    }
    
    @Test
    public void loadSettings__MULTIPLE_PROPERTIES() {
        
        // given
        FileSettingLocations settingLocations = new FileSettingLocations();
        settingLocations.addLocation(SettingLocationType.CLASSPATH, "/test1.properties", false);
        settingLocations.addLocation(SettingLocationType.CLASSPATH, "/test2.properties", false);
        settingLocations.addLocation(SettingLocationType.CLASSPATH, "/test3.properties", true);
        
        FileBasedSettingsFetcher settingsFetcher = new FileBasedSettingsFetcher(settingLocations);
        
        // when
        settingsFetcher.loadSettings();
        
        // then
        assertEquals("keyval2", settingsFetcher.getSetting("BuiltinUsers.KEY"));
        assertEquals("someval2", settingsFetcher.getSetting(":SomeSetting"));
        assertEquals("testval1", settingsFetcher.getSetting(":Test1Setting"));
        assertEquals("testval2", settingsFetcher.getSetting(":Test2Setting"));
        assertThat(settingsFetcher.getAllSettings().keySet(),
                containsInAnyOrder("BuiltinUsers.KEY", ":SomeSetting", ":Test1Setting", ":Test2Setting"));
    }
    
    
}
