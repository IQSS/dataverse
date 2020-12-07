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

public class DbSettingConfigSource implements ConfigSource {
    
    static final String PREFIX = "dataverse.settings.fromdb";
    static final ConcurrentHashMap<String, String> propertiesCache = new ConcurrentHashMap<>();
    static Instant lastUpdate;
    
    DataSource dataSource;
    
    public DbSettingConfigSource() {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:app/jdbc/dataverse");
            updateProperties();
        } catch (final NamingException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public void updateProperties() {
        // Do brutal JDBC retrieval over the wire, to be available right from the start of app deployment.
        // Injecting the EntityManager or the SettingsServiceBean is hard, as MPCONFIG sources are POJOs.
        // We would need to be a Startup-Singleton, but this means no values for early retrieval...
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement query = connection.prepareStatement("SELECT name, content FROM Setting WHERE lang IS NULL");
            final ResultSet props = query.executeQuery();
        
            while (props.next()) {
                propertiesCache.put(PREFIX+"."+props.getString(0), props.getString(1));
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
        return 0;
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
