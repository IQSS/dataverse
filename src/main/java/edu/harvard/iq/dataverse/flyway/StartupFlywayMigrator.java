package edu.harvard.iq.dataverse.flyway;

import org.flywaydb.core.Flyway;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.sql.DataSource;

@Startup
@Singleton
class StartupFlywayMigrator {

    @Resource(lookup = "jdbc/VDCNetDS")
    private DataSource dataSource;

    @PostConstruct
    void migrateDatabase() {

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
    }
}
