package edu.harvard.iq.dataverse.persistence;

import org.eclipse.persistence.queries.DataReadQuery;
import org.eclipse.persistence.sequencing.Sequence;
import org.eclipse.persistence.sequencing.TableSequence;
import org.eclipse.persistence.sessions.DatabaseSession;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.sessions.SessionEvent;
import org.eclipse.persistence.sessions.SessionEventAdapter;
import org.eclipse.persistence.tools.schemaframework.DefaultTableGenerator;
import org.eclipse.persistence.tools.schemaframework.SchemaManager;
import org.eclipse.persistence.tools.schemaframework.TableCreator;
import org.eclipse.persistence.tools.schemaframework.TableDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Creates missing entity tables right after EclipseLink logs in to the database, but only when
 * needed. Registered in {@code META-INF/persistence.xml} via the
 * {@code eclipselink.session-event-listener} property.
 *
 * <p>This replaces the former {@code eclipselink.ddl-generation=create-tables} setting, which made
 * EclipseLink issue CREATE statements for every entity on every single deployment - a significant
 * part of (re)deployment time. The semantics are preserved: on first boot against an empty
 * database (or when a new entity has been added) the missing tables, indexes and sequences are
 * created by the very same EclipseLink schema framework that {@code create-tables} uses, at the
 * same point in the application lifecycle (after session login, before the application is used).
 * When all entity tables already exist - the common case - table creation is skipped entirely at
 * the cost of a single catalog query.
 *
 * <p>Note that just like {@code create-tables}, this never alters existing tables. Incremental
 * schema changes are managed by Flyway, see
 * {@link edu.harvard.iq.dataverse.flyway.StartupFlywayMigrator} and
 * {@code src/main/resources/db/migration}.
 */
public class ConditionalSchemaCreator extends SessionEventAdapter {

    private static final Logger logger = Logger.getLogger(ConditionalSchemaCreator.class.getCanonicalName());

    @Override
    public void postLogin(SessionEvent event) {
        Session session = event.getSession();

        Set<String> missingTables = getExpectedTables(session);
        missingTables.removeAll(getExistingTables(session));

        if (missingTables.isEmpty()) {
            logger.fine("All expected tables present in the database, skipping DDL generation.");
            return;
        }

        logger.info("Found " + missingTables.size() + " table(s) missing from the database "
            + "(empty database or newly added entities/mappings). Creating missing tables, indexes and sequences...");
        SchemaManager schemaManager = new SchemaManager((DatabaseSession) session);
        schemaManager.createDefaultTables(true);
        logger.info("Schema creation done.");
    }

    /**
     * All table names (lowercased) the schema framework would create for this persistence unit:
     * entity (and secondary) tables, relation tables of {@code @ManyToMany} mappings, collection
     * tables of {@code @ElementCollection} mappings, and the sequence table (used by
     * {@code GenerationType.AUTO}). Computed with the same {@link DefaultTableGenerator} that
     * {@link SchemaManager#createDefaultTables} uses, so detection and creation stay consistent.
     */
    private Set<String> getExpectedTables(Session session) {
        Set<String> expectedTables = new HashSet<>();
        TableCreator tableCreator =
            new DefaultTableGenerator(session.getProject(), true).generateDefaultTableCreator();
        for (TableDefinition tableDefinition : tableCreator.getTableDefinitions()) {
            expectedTables.add(tableDefinition.getName().toLowerCase(Locale.ROOT));
        }
        // The sequence table is not part of the table definitions above - createDefaultTables
        // creates it separately (see TableCreator.createTables), so expect it separately as well.
        if (session.getProject().usesSequencing()) {
            Sequence sequence = session.getLogin().getDefaultSequence();
            if (sequence instanceof TableSequence tableSequence) {
                expectedTables.add(tableSequence.getTableName().toLowerCase(Locale.ROOT));
            }
        }
        return expectedTables;
    }

    /**
     * All table names (lowercased) present in the current schema of the database we logged in to.
     */
    private Set<String> getExistingTables(Session session) {
        DataReadQuery query = new DataReadQuery(
            "SELECT lower(table_name) AS table_name FROM information_schema.tables WHERE table_schema = current_schema()");
        List<?> rows = (List<?>) session.executeQuery(query);
        Set<String> existingTables = new HashSet<>();
        for (Object row : rows) {
            existingTables.add(String.valueOf(((Map<?, ?>) row).get("table_name")));
        }
        return existingTables;
    }
}
