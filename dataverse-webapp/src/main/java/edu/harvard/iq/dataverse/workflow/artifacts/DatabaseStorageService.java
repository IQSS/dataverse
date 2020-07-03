package edu.harvard.iq.dataverse.workflow.artifacts;

import org.omnifaces.cdi.Startup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
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
import java.util.function.Supplier;

/**
 * Implementation of {@link StorageService} for {@link StorageType#DATABASE}.
 * The data is stored inside db_storage table.
 */

@Startup
@Singleton
@DependsOn("WorkflowArtifactServiceBean")
public class DatabaseStorageService implements StorageService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseStorageService.class);

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;

    private WorkflowArtifactServiceBean serviceBean;

    // -------------------- CONSTRUCTORS --------------------

    public DatabaseStorageService() { }

    @Inject
    public DatabaseStorageService(WorkflowArtifactServiceBean serviceBean) {
        this.serviceBean = serviceBean;
    }

    // -------------------- LOGIC --------------------

    @PostConstruct
    public void register() {
        serviceBean.register(this);
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.DATABASE;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void delete(String location) {
        em.createNativeQuery("DELETE FROM db_storage WHERE id = ?")
                .setParameter(1, UUID.fromString(location))
                .executeUpdate();
    }

    @Override
    public String save(Supplier<InputStream> inputStreamSupplier) {
        UUID id = UUID.randomUUID();
        try (InputStream input = inputStreamSupplier.get()) {
            PreparedStatement insert = prepareStatement("INSERT INTO db_storage VALUES (?, ?)");
            insert.setObject(1, id);
            insert.setBinaryStream(2, input);
            insert.execute();
        } catch (IOException | SQLException ex) {
            logger.error("Exception while storing artifact in database: {}", ex.getMessage());
            throw new RuntimeException(ex);
        }
        return id.toString();
    }

    @Override
    public Optional<Supplier<InputStream>> readAsStream(String location) {
        UUID id = UUID.fromString(location);
        try {
            PreparedStatement query = prepareStatement("SELECT stored_data FROM db_storage WHERE id = ?");
            query.setObject(1, id);
            ResultSet result = query.executeQuery();
            return result.next()
                    ? Optional.of(supplyDataStream(result))
                    : Optional.empty();
        } catch (SQLException se) {
            logger.error("Exception while retrieving stored artifact with id={}. Message is: {}.", location, se.getMessage());
            throw new RuntimeException(se);
        }
    }

    // -------------------- PRIVATE --------------------

    private Supplier<InputStream> supplyDataStream(ResultSet result) {
        return () -> {
            try {
                return result.getBinaryStream(1);
            } catch (SQLException se) {
                logger.error("Exception while accessing data stream: {}", se.getMessage());
                throw new RuntimeException(se);
            }
        };
    }

    private PreparedStatement prepareStatement(String query) throws SQLException {
        Connection connection = em.unwrap(Connection.class);
        return connection.prepareStatement(query);
    }
}
