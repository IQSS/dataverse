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
    user = "dataverse",
    password = "${ALIAS=db_password_alias}",
    serverName = "postgresql",
    portNumber = 5432,
    databaseName = "dataverse",
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
