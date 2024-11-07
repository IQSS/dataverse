package edu.harvard.iq.dataverse.common;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Collections.enumeration;
import static javax.persistence.Persistence.createEntityManagerFactory;
import static org.eclipse.persistence.sessions.DatabaseLogin.TRANSACTION_READ_UNCOMMITTED;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.eclipse.persistence.internal.jpa.EntityManagerImpl;
import org.eclipse.persistence.sessions.DatabaseLogin;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.postgresql.ds.PGSimpleDataSource;

public class DBItegrationTest {

    // -------------------------------------------------------------------------
    private final EntityManager entityManager = createEntityManager();

    private static final Path persistenceXMLPath = Paths.get("").resolve("src")
            .resolve("test").resolve("resources")
            .resolve("dbintegration_test_persistence.xml");

    // -------------------------------------------------------------------------
    static {
        try {
            initDB();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    protected EntityManager getEntityManager() {

        return this.entityManager;
    }

    // -------------------------------------------------------------------------
    // @BeforeAll
    public static void initDB() throws Exception {

        final DataSource datadource = createDataSource();
        try (final Connection c = datadource.getConnection()) {
            dropAllTables(c);
            dropAllSequences(c);
        }

        Flyway.configure().dataSource(datadource).validateOnMigrate(false)
                .baselineOnMigrate(true).load().migrate();

        insertInitialData(datadource);
    }

    // -------------------------------------------------------------------------
    @BeforeEach
    public void beginTransaction() throws SQLException {

        this.entityManager.getTransaction().begin();
    }

    // -------------------------------------------------------------------------
    @AfterEach
    public void clearDB() {

        this.entityManager.getTransaction().rollback();
        this.entityManager.close();
    }

    // -------------------------------------------------------------------------
    private static DataSource createDataSource() {

        final PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setServerNames(new String[] { "localhost" });
        ds.setPortNumbers(new int[] { 5432 });
        ds.setDatabaseName("dvndb-test");
        ds.setUser("dvnapp");
        ds.setPassword("secret");

        return ds;
    }

    // -------------------------------------------------------------------------
    public EntityManager createEntityManager() {

        try {
            final Map<String, Object> props = new HashMap<String, Object>();
            final String persistenceXmlPathString = persistenceXMLPath.toString();
            final URL persistenceXmlURL = persistenceXMLPath.toAbsolutePath().toUri()
                    .toURL();

            props.put("eclipselink.persistencexml", persistenceXmlPathString);
            props.put("javax.persistence.nonJtaDataSource", createDataSource());

            // trick eclipselink to use different persistence.xml
            currentThread().setContextClassLoader(new ClassLoader() {
                @Override
                public Enumeration<URL> getResources(final String name)
                        throws IOException {

                    if (name.equals(persistenceXmlPathString)) {
                        return enumeration(asList(persistenceXmlURL));
                    } else {
                        return super.getResources(name);
                    }
                }
            });

            final EntityManagerImpl em = (EntityManagerImpl) createEntityManagerFactory(
                    "dvndb-test", props).createEntityManager();

            DatabaseLogin databaseLogin = (DatabaseLogin) em.getSession()
                    .getDatasourceLogin();
            databaseLogin.setTransactionIsolation(TRANSACTION_READ_UNCOMMITTED);

            return em;
        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    private static void dropAllTables(final Connection connection)
            throws SQLException {

        try (final Statement s = connection.createStatement()) {
            s.execute("DO $$ DECLARE\n" + "    r RECORD;\n" + "BEGIN\n"
                    + "    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = current_schema()) LOOP\n"
                    + "        EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(r.tablename) || ' CASCADE';\n"
                    + "    END LOOP;\n" + "END $$;");
        }
    }

    // -------------------------------------------------------------------------
    private static void dropAllSequences(final Connection connection)
            throws SQLException {

        try (final Statement s = connection.createStatement()) {
            s.execute("DO $$ DECLARE\n" + "    r RECORD;\n" + "BEGIN\n"
                    + "    FOR r IN (SELECT relname FROM pg_class c WHERE (c.relkind = 'S')) LOOP\n"
                    + "        EXECUTE 'DROP SEQUENCE ' || quote_ident(r.relname);\n"
                    + "    END LOOP;\n" + "END $$;");
        }
    }

    // -------------------------------------------------------------------------
    private static void insertInitialData(final DataSource datadource)
            throws Exception {

        try (final BufferedReader lineReader = openInitDbSql()) {
            final StringBuilder command = new StringBuilder();

            try (final Connection c = datadource.getConnection()) {
                String line = null;
                while ((line = lineReader.readLine()) != null) {

                    String trimmedLine = line.trim();
                    if (trimmedLine.length() == 0 || trimmedLine.startsWith("//")
                            || trimmedLine.startsWith("--")) {
                        continue;
                    }
                    if (!trimmedLine.endsWith(";")) {
                        command.append(line);
                        command.append(" ");
                        continue;
                    }

                    command.append(line.substring(0, line.lastIndexOf(";")));
                    command.append(" ");

                    try (final Statement s = c.createStatement()) {
                        s.execute(command.toString());
                    }

                    command.setLength(0);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    private static BufferedReader openInitDbSql() throws UnsupportedEncodingException {

        return new BufferedReader(new InputStreamReader(
                DBItegrationTest.class.getResourceAsStream("/dbinit.sql"), "UTF-8"));
    }

    // -------------------------------------------------------------------------
    // clears database
    public static void main(String[] args) throws Exception {

        final DataSource datadource = createDataSource();
        try (final Connection c = datadource.getConnection()) {
            dropAllTables(c);
            dropAllSequences(c);
        }
    }
}
