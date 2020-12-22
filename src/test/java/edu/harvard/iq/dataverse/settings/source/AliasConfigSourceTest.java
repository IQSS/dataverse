package edu.harvard.iq.dataverse.settings.source;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class AliasConfigSourceTest {
    
    AliasConfigSource source = new AliasConfigSource();
    
    @Test
    void getValue() {
        // given
        System.setProperty("dataverse.hello.foobar", "test");
        Properties aliases = new Properties();
        aliases.setProperty("dataverse.goodbye.foobar", "dataverse.hello.foobar");
        
        // when
        source.importAliases(aliases);
        
        // then
        assertEquals("test", source.getValue("dataverse.goodbye.foobar"));
    }
    
    @Test
    void readImportTestAliasesFromFile() throws IOException {
        // given
        System.setProperty("dataverse.old.example", "test");
        String filePath = "test-microprofile-aliases.properties";
        
        // when
        Properties aliases = source.readAliases(filePath);
        source.importAliases(aliases);
        
        // then
        assertEquals("test", source.getValue("dataverse.new.example"));
    }
}