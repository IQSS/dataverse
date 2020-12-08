package edu.harvard.iq.dataverse.settings.source;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Tag("testcontainers")
@Testcontainers
class DbSettingConfigSourceIT {

    @Container
    static JdbcDatabaseContainer dbContainer = new PostgreSQLContainer("postgres:"+System.getProperty("postgresql.server.version", "9.6"))
                                                        .withInitScript("sql/dbsetting-config-source-testdata.sql")
                                                        .withDatabaseName("dataverse");
    
    static DbSettingConfigSource dbSource;
    
    @BeforeAll
    static void setUp() {
        // create the datasource
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(dbContainer.getJdbcUrl());
        ds.setUser(dbContainer.getUsername());
        ds.setPassword(dbContainer.getPassword());
        
        // create the config source
        dbSource = new DbSettingConfigSource(ds);
    }
    
    @Test
    void testDataRetrieval() {
        assertEquals("foobar@example.org", dbSource.getValue("dataverse.settings.fromdb.SystemEmail"));
    }

}