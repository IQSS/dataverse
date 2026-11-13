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
                // Flyway uses its own LogFactory to resolve a logging framework at runtime,
                // picking SLF4J by default when found on the classpath. However, in the
                // Dataverse/Payara environment, Flyway's SLF4J logging does not surface in
                // the server log.
                // Using JavaUtilLogCreator bypasses SLF4J and logs directly through JUL,
                // which Payara routes to the server log.
                // Note: "jul" is NOT a valid alias in Flyway's .loggers() config — it must be
                // the fully-qualified class name. If this breaks on a Flyway upgrade, check
                // org.flywaydb.core.internal.logging for the replacement class.
                .loggers("org.flywaydb.core.internal.logging.javautil.JavaUtilLogCreator")
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
