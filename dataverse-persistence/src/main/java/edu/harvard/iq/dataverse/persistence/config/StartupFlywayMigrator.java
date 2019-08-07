package edu.harvard.iq.dataverse.persistence.config;

import org.flywaydb.core.Flyway;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.sql.DataSource;

@Startup
@Singleton
public class StartupFlywayMigrator {

    @Resource(lookup = "jdbc/VDCNetDS")
    private DataSource dataSource;

    @PostConstruct
    void migrateDatabase() {

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .validateOnMigrate(false)
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
    }
}
