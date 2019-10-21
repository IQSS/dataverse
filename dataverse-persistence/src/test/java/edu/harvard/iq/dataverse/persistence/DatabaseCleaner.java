package edu.harvard.iq.dataverse.persistence;

import edu.harvard.iq.dataverse.persistence.config.StartupFlywayMigrator;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Class responsible for cleaning database to it's original state
 * 
 * @author kkulik
 * @author madryk
 */
@Stateless
public class DatabaseCleaner {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    @Inject
    private StartupFlywayMigrator startupFlywayMigrator;
    
    
    // -------------------- LOGIC --------------------
    
    /**
     * Cleans up database to it's original state
     * (that is state right after application startup).
     */
    public void cleanupDatabase() {
        dropAllTables();
        startupFlywayMigrator.migrateDatabase();
    }

    // -------------------- PRIVATE --------------------
    
    private void dropAllTables() {
        em.createNativeQuery("DO $$ DECLARE\n" +
                                     "    r RECORD;\n" +
                                     "BEGIN\n" +
                                     "    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = current_schema()) LOOP\n" +
                                     "        EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(r.tablename) || ' CASCADE';\n" +
                                     "    END LOOP;\n" +
                                     "END $$;").executeUpdate();
    }
}
