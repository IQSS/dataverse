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
                .locations(
                    // Path where to find normal SQL migrations
                    "classpath:db/migration",
                    // Path where to find compiled Java migrations
                    "classpath:edu/harvard/iq/dataverse/flyway"
                )
                // Java-based callbacks are not auto-discovered (unlike migrations)
                .callbacks(new SettingsCleanupCallback())
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
    }
}
