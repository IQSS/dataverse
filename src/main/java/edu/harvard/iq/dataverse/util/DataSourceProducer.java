package edu.harvard.iq.dataverse.util;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.sql.DataSource;

@Singleton
@DataSourceDefinition(
    name = "java:global/jdbc/dataverse",
    // Using PGXADataSource instead of deprecated PGPoolingDataSource
    className = "org.postgresql.xa.PGXADataSource",
    user = "${MPCONFIG=dataverse.db.user}",
    password = "${MPCONFIG=dataverse.db.password}",
    serverName = "${MPCONFIG=dataverse.db.host}",
    url = "jdbc:postgresql://${MPCONFIG=dataverse.db.host}:${MPCONFIG=dataverse.db.port}/${MPCONFIG=dataverse.db.name}",
    minPoolSize = 10,
    maxPoolSize = 200)
public class DataSourceProducer {
    
    @Resource(lookup="java:global/jdbc/dataverse")
    DataSource ds;
    
    @Produces
    public DataSource getDatasource() {
        return ds;
    }
}
