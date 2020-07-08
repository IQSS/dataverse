package edu.harvard.iq.dataverse.workflow.artifacts;

import com.google.common.io.InputSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * Allows storing binary data in form of records in <code>db_storage</code> table.
 */
@Singleton
public class DatabaseWorkflowArtifactStorage implements WorkflowArtifactStorage {

    private static final Logger log = LoggerFactory.getLogger(DatabaseWorkflowArtifactStorage.class);

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;

    // -------------------- LOGIC --------------------

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void delete(String location) {
        em.createNativeQuery("DELETE FROM db_storage WHERE id = ?")
                .setParameter(1, UUID.fromString(location))
                .executeUpdate();
    }

    @Override
    public String write(InputSupplier<InputStream> data) throws IOException {
        UUID id = UUID.randomUUID();
        try (InputStream input = data.getInput()) {
            PreparedStatement insert = prepareStatement("INSERT INTO db_storage VALUES (?, ?)");
            insert.setObject(1, id);
            insert.setBinaryStream(2, input);
            insert.execute();
        } catch (SQLException ex) {
            log.error("Exception while storing artifact in database: {}", ex.getMessage());
            throw new IOException(ex);
        }
        return id.toString();
    }

    @Override
    public Optional<InputSupplier<InputStream>> read(String location) {
        UUID id = UUID.fromString(location);
        try {
            PreparedStatement query = prepareStatement("SELECT stored_data FROM db_storage WHERE id = ?");
            query.setObject(1, id);
            ResultSet result = query.executeQuery();
            return result.next()
                    ? Optional.of(supplyDataStream(result))
                    : Optional.empty();
        } catch (SQLException se) {
            log.error("Exception while retrieving stored artifact with id={}. Message is: {}.", location, se.getMessage());
            throw new RuntimeException(se);
        }
    }

    // -------------------- PRIVATE --------------------

    private InputSupplier<InputStream> supplyDataStream(ResultSet result) {
        return () -> {
            try {
                return result.getBinaryStream(1);
            } catch (SQLException se) {
                log.error("Exception while accessing data stream: {}", se.getMessage());
                throw new IOException(se);
            }
        };
    }

    private PreparedStatement prepareStatement(String query) throws SQLException {
        Connection connection = em.unwrap(Connection.class);
        return connection.prepareStatement(query);
    }
}
