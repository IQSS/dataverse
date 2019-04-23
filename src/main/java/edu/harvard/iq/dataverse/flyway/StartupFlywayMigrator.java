package edu.harvard.iq.dataverse.flyway;

import org.flywaydb.core.Flyway;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

@Startup
@Singleton
@TransactionManagement(value = TransactionManagementType.BEAN)
class StartupFlywayMigrator {

    @Resource(lookup = "jdbc/VDCNetDS")
    private DataSource dataSource;

    @PostConstruct
    void migrateDatabase() {

        if (dataSource == null){
            throw new NullPointerException("Failed to migrate, cannot connect to database");
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .outOfOrder(true)
                .load();

        flyway.migrate();
    }
}
