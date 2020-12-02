package edu.harvard.iq.dataverse.util;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.sql.DataSource;

@Singleton
@DataSourceDefinition(
    // Find docs here: https://javaee.github.io/javaee-spec/javadocs/javax/annotation/sql/DataSourceDefinition.html
    
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
    maxIdleTime = 300,
    
    // These are documented at https://docs.payara.fish/community/docs/5.2020.6/documentation/payara-server/jdbc/advanced-connection-pool-properties.html#full-list-of-properties
    properties = {
        "fish.payara.pool-resize-quantity=${MPCONFIG=dataverse.db.pool-resize-quantity}",
        "fish.payara.is-connection-validation-required=${MPCONFIG=dataverse.db.is-connection-validation-required}",
        "fish.payara.connection-validation-method=${MPCONFIG=dataverse.db.connection-validation-method}",
        "fish.payara.validation-table-name=${MPCONFIG=dataverse.db.validation-table-name}",
        "fish.payara.validation-classname=${MPCONFIG=dataverse.db.validation-classname}",
        "fish.payara.connection-leak-timeout-in-seconds=${MPCONFIG=dataverse.db.connection-leak-timeout-in-seconds}",
        "fish.payara.connection-leak-reclaim=${MPCONFIG=dataverse.db.connection-leak-reclaim}",
        "fish.payara.statement-timeout-in-seconds=${MPCONFIG=dataverse.db.statement-timeout-in-seconds}",
        "fish.payara.statement-leak-timeout-in-seconds=${MPCONFIG=dataverse.db.statement-leak-timeout-in-seconds}",
        "fish.payara.statement-leak-reclaim=${MPCONFIG=dataverse.db.statement-leak-reclaim}",
        "fish.payara.slow-query-threshold-in-seconds=${MPCONFIG=dataverse.db.slow-query-threshold-in-seconds}",
        "fish.payara.log-jdbc-calls=${MPCONFIG=dataverse.db.log-jdbc-calls}"
    })
public class DataSourceProducer {
    
    @Resource(lookup="java:app/jdbc/dataverse")
    DataSource ds;
    
    @Produces
    public DataSource getDatasource() {
        return ds;
    }
}
