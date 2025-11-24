package edu.harvard.iq.dataverse.flyway;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Flyway callback that runs after all migrations and removes any settings
 * whose "name" column does not correspond to a SettingsServiceBean.Key.
 *
 * This enforces that the settings table contains only keys known to the
 * current application version.
 */
public class SettingsCleanupCallback implements Callback {

    private static final Logger logger = Logger.getLogger(SettingsCleanupCallback.class.getName());

    @Override
    public boolean supports(Event event, Context context) {
        // Only run after all migrations have completed successfully.
        return event == Event.AFTER_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        // Prefer to run inside the same transaction
        return true;
    }

    @Override
    public void handle(Event event, Context context) {
        if (event != Event.AFTER_MIGRATE) {
            return;
        }

        logger.info("Starting settings cleanup: removing entries with unknown keys");

        try {
            cleanupInvalidSettings(context.getConnection());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error while cleaning up settings table", e);
            throw new FlywayException("Failed to clean up invalid settings", e);
        }

        logger.info("Finished cleaning up settings");
    }
    
    @Override
    public String getCallbackName() {
        return "SettingsCleanup";
    }
    
    private void cleanupInvalidSettings(Connection connection) throws SQLException {
        // Collect IDs of rows to delete
        List<Long> idsToDelete = new ArrayList<>();

        String selectSql = "SELECT id, name FROM setting";
        try (PreparedStatement ps = connection.prepareStatement(selectSql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long id = rs.getLong("id");
                String name = rs.getString("name");

                // We expect names like ":KeyName". Anything that does not parse
                // to a SettingsServiceBean.Key is considered invalid and will be removed.
                SettingsServiceBean.Key key = SettingsServiceBean.Key.parse(name);
                if (key == null) {
                    idsToDelete.add(id);
                }
            }
        }

        if (idsToDelete.isEmpty()) {
            logger.fine("Settings cleanup: no invalid settings found");
            return;
        }

        logger.info(() -> "Settings cleanup: found " + idsToDelete.size()
                + " invalid settings; deleting them");

        String deleteSql = "DELETE FROM setting WHERE id = ?";
        try (PreparedStatement delete = connection.prepareStatement(deleteSql)) {
            for (Long id : idsToDelete) {
                delete.setLong(1, id);
                delete.addBatch();
            }
            int[] counts = delete.executeBatch();
            logger.info(() -> "Settings cleanup: deleted " + counts.length + " rows with invalid keys");
        }
    }
}
