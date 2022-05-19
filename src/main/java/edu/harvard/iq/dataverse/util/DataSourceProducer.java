package edu.harvard.iq.dataverse.util;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.sql.DataSource;

// Find docs here: https://javaee.github.io/javaee-spec/javadocs/javax/annotation/sql/DataSourceDefinition.html
@Singleton
@DataSourceDefinition(
        name = "java:app/jdbc/dataverse",
        // The app server (Payara) deploys a managed pool for this data source for us.
        // We don't need to deal with this on our own.
        //
        // HINT: PGSimpleDataSource would work too, but as we use a connection pool, go with a javax.sql.ConnectionPoolDataSource
        // HINT: PGXADataSource is unnecessary (no distributed transactions used) and breaks ingest.
        className = "org.postgresql.ds.PGConnectionPoolDataSource",
        user = "${MPCONFIG=dataverse.db.user}",
        password = "${MPCONFIG=dataverse.db.password}",
        url = "jdbc:postgresql://${MPCONFIG=dataverse.db.host}:${MPCONFIG=dataverse.db.port}/${MPCONFIG=dataverse.db.name}",
        // If we ever need to change these pool settings, we need to remove this class and create the resource
        // from web.xml. We can use MicroProfile Config in there for these values, impossible to do in the annotation.
        //
        // See also https://blog.payara.fish/an-intro-to-connection-pools-in-payara-server-5
        // Payara DataSourceDefinitionDeployer default value = 8
        minPoolSize = 10,
        // HINT: Payara DataSourceDefinitionDeployer default value = 32
        // HINT: Harvard Dataverse is fine for a while with 64
        maxPoolSize = 100,
        // "The number of seconds that a physical connection should remain unused in the pool before the connection is closed for a connection pool. "
        // Payara DataSourceDefinitionDeployer default value = 300 (seconds)
        maxIdleTime = 300)
// It's possible to add additional properties like this...
//
//properties = {
//    "fish.payara.log-jdbc-calls=true"
//})
//
// ... but at this time we don't think we need any. The full list
// of properties can be found at https://docs.payara.fish/community/docs/5.2021.6/documentation/payara-server/jdbc/advanced-connection-pool-properties.html#full-list-of-properties
//
// All these properties cannot be configured via MPCONFIG as Payara doesn't support this (yet). To be enhanced.
// See also https://github.com/payara/Payara/issues/5024
public class DataSourceProducer {

    @Resource(lookup = "java:app/jdbc/dataverse")
    DataSource ds;

    @Produces
    public DataSource getDatasource() {
        return ds;
    }
}
