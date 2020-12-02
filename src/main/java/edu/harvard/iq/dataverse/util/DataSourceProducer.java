package edu.harvard.iq.dataverse.util;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.sql.DataSource;

@Singleton
@DataSourceDefinition(
    name = "java:app/jdbc/dataverse",
    //
    // The app server (Payara) deploys a managed pool for this data source for us.
    // We don't need to deal with this on our own.
    //
    // HINT: PGSimpleDataSource would work too, but as we use a connection pool, go with a javax.sql.ConnectionPoolDataSource
    // HINT: PGXADataSource is unnecessary (no distributed transactions used) and breaks ingest.
    className = "org.postgresql.ds.PGConnectionPoolDataSource",
    user = "${MPCONFIG=dataverse.db.user}",
    password = "${MPCONFIG=dataverse.db.password}",
    url = "jdbc:postgresql://${MPCONFIG=dataverse.db.host}:${MPCONFIG=dataverse.db.port}/${MPCONFIG=dataverse.db.name}",
    minPoolSize = 10,
    maxPoolSize = 200)
public class DataSourceProducer {
    
    @Resource(lookup="java:app/jdbc/dataverse")
    DataSource ds;
    
    @Produces
    public DataSource getDatasource() {
        return ds;
    }
}
