package edu.harvard.iq.dataverse.settings.source;

import org.eclipse.microprofile.config.spi.ConfigSource;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class DbSettingConfigSource implements ConfigSource {
    
    private static final Logger logger = Logger.getLogger(DbSettingConfigSource.class.getName());
    private static final String PREFIX = "dataverse.settings.fromdb";
    private static final ConcurrentHashMap<String, String> propertiesCache = new ConcurrentHashMap<>();
    private static Instant lastUpdate;
    
    DataSource dataSource;
    
    public DbSettingConfigSource() {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:app/jdbc/dataverse");
            updateProperties();
        } catch (final NamingException e) {
            logger.warning("Could not setup MPCONFIG dataverse setting config source as no DB connection could be found.");
        }
    }
    
    // Test usage (no JNDI context)
    public DbSettingConfigSource(DataSource ds) {
        dataSource = ds;
        updateProperties();
    }
    
    public void updateProperties() {
        // When the DataSource is unavailable (no db connection / ...), do not update to avoid NPEs
        if (dataSource == null)
            return;
        // Do brutal JDBC retrieval over the wire, to be available right from the start of app deployment.
        // Injecting the EntityManager or the SettingsServiceBean is hard, as MPCONFIG sources are POJOs.
        // We would need to be a Startup-Singleton, but this means no values for early retrieval...
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement query = connection.prepareStatement("SELECT name, content FROM Setting WHERE lang IS NULL");
            final ResultSet props = query.executeQuery();
        
            while (props.next()) {
                propertiesCache.put(PREFIX+"."+props.getString(1), props.getString(2));
            }
            lastUpdate = Instant.now();
        
            props.close();
            query.close();
            connection.close();
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public Map<String, String> getProperties() {
        // if the cache is at least XX number of seconds old, update before serving data.
        if (lastUpdate == null || Instant.now().minus(Duration.ofSeconds(60)).isBefore(lastUpdate)) {
            updateProperties();
        }
        return propertiesCache;
    }
    
    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }
    
    @Override
    public int getOrdinal() {
        return 50;
    }
    
    @Override
    public String getValue(String key) {
        return getProperties().getOrDefault(key, null);
    }
    
    @Override
    public String getName() {
        return "DataverseDB";
    }
}
