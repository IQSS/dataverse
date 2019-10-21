package edu.harvard.iq.dataverse.persistence;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Runner of sql scripts
 * <p>
 * Based on MyBatis2 ScriptRunner
 * 
 * @see https://github.com/mybatis/ibatis-2/blob/master/src/main/java/com/ibatis/common/jdbc/ScriptRunner.java
 */
@Stateless
public class SqlScriptRunner {
    private static final Logger LOG = Logger.getLogger(SqlScriptRunner.class.getCanonicalName());
    private static final String DELIMITER = ";";


    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager entityManager;


    // -------------------- LOGIC --------------------

    /**
     * Runs an SQL script from classpath
     */
    public void runScriptFromClasspath(String classpath) {
        try (Reader reader = new StringReader(IOUtils.resourceToString(classpath, StandardCharsets.UTF_8))) {
            LOG.info("Run script from classpath: " + classpath);
            runScript(reader);
        } catch (IOException e) {
            throw new RuntimeException("Encountered error while reading sql file", e);
        }
    }

    // -------------------- PRIVATE --------------------

    /**
     * Runs an SQL script (read in using the Reader parameter)
     * @throws IOException 
     */
    private void runScript(Reader reader) throws IOException {
        StringBuffer command = new StringBuffer();
        LineNumberReader lineReader = new LineNumberReader(reader);

        try {
            String line = null;
            while ((line = lineReader.readLine()) != null) {
                
                String trimmedLine = line.trim();
                if (trimmedLine.length() == 0 || trimmedLine.startsWith("//") || trimmedLine.startsWith("--")) {
                    continue;
                }
                if (!trimmedLine.endsWith(DELIMITER)) {
                    command.append(line);
                    command.append(" ");
                    continue;
                }


                command.append(line.substring(0, line.lastIndexOf(DELIMITER)));
                command.append(" ");

                executeCommand(command.toString());

                command.delete(0, command.length());
            }

        } finally {
            entityManager.flush();
        }
    }

    private void executeCommand(String commandString) {
        LOG.finest("Executing command: " + commandString);

        Query query = entityManager.createNativeQuery(commandString);

        if (StringUtils.startsWithIgnoreCase(commandString, "select")) {
            query.getResultList();
        } else {
            query.executeUpdate();
        }
    }
}