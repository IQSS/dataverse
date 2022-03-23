package edu.harvard.iq.dataverse.flyway;

import org.flywaydb.core.Flyway;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import javax.sql.DataSource;

@Startup
@Singleton
@TransactionManagement(value = TransactionManagementType.BEAN)
public class StartupFlywayMigrator {

    @Resource(lookup = "java:app/jdbc/dataverse")
    private DataSource dataSource;

    @PostConstruct
    void migrateDatabase() {

        if (dataSource == null){
            throw new NullPointerException("Failed to migrate, cannot connect to database");
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
    }
}
