package edu.harvard.iq.dataverse.util;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import javax.sql.DataSource;

// Find docs here: https://jakarta.ee/specifications/annotations/2.1/apidocs/jakarta.annotation/jakarta/annotation/sql/datasourcedefinition
@Singleton
@DataSourceDefinition(
        name = "java:app/jdbc/dataverse",
        // The app server (Payara) deploys a managed pool for this data source for us.
        // We don't need to deal with this on our own.
        //
        // HINT: PGSimpleDataSource would work too, but as we use a connection pool, go with a javax.sql.ConnectionPoolDataSource
        // HINT: PGXADataSource is unnecessary (no distributed transactions used) and breaks ingest.
        className = "org.postgresql.ds.PGConnectionPoolDataSource",
        
        // BEWARE: as this resource is created before defaults are read from META-INF/microprofile-config.properties,
        // defaults must be provided in this Payara-proprietary manner.
        user = "${MPCONFIG=dataverse.db.user:dataverse}",
        password = "${MPCONFIG=dataverse.db.password}",
        url = "jdbc:postgresql://${MPCONFIG=dataverse.db.host:localhost}:${MPCONFIG=dataverse.db.port:5432}/${MPCONFIG=dataverse.db.name:dataverse}?${MPCONFIG=dataverse.db.parameters:}",
        
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

        // Set more options via MPCONFIG, including defaults where applicable.
        // TODO: Future versions of Payara might support setting integer properties like pool size,
        //       idle times, etc in a Payara-propietary way. See https://github.com/payara/Payara/pull/5272
        properties = {
            // The following options are documented here:
            // https://docs.payara.fish/community/docs/documentation/payara-server/jdbc/advanced-connection-pool-properties.html
            // VALIDATION
            "fish.payara.is-connection-validation-required=${MPCONFIG=dataverse.db.is-connection-validation-required:false}",
            "fish.payara.connection-validation-method=${MPCONFIG=dataverse.db.connection-validation-method:}",
            "fish.payara.validation-table-name=${MPCONFIG=dataverse.db.validation-table-name:}",
            "fish.payara.validation-classname=${MPCONFIG=dataverse.db.validation-classname:}",
            "fish.payara.validate-atmost-once-period-in-seconds=${MPCONFIG=dataverse.db.validate-atmost-once-period-in-seconds:0}",
            // LEAK DETECTION
            "fish.payara.connection-leak-timeout-in-seconds=${MPCONFIG=dataverse.db.connection-leak-timeout-in-seconds:0}",
            "fish.payara.connection-leak-reclaim=${MPCONFIG=dataverse.db.connection-leak-reclaim:false}",
            "fish.payara.statement-leak-timeout-in-seconds=${MPCONFIG=dataverse.db.statement-leak-timeout-in-seconds:0}",
            "fish.payara.statement-leak-reclaim=${MPCONFIG=dataverse.db.statement-leak-reclaim:false}",
            // LOGGING, SLOWNESS, PERFORMANCE
            "fish.payara.statement-cache-size=${MPCONFIG=dataverse.db.statement-cache-size:50}",
            "fish.payara.statement-cache-type=${MPCONFIG=dataverse.db.statement-cache-type:com.sun.gjc.util.LRUCache}",
            "fish.payara.statement-timeout-in-seconds=${MPCONFIG=dataverse.db.statement-timeout-in-seconds:-1}",
            "fish.payara.slow-query-threshold-in-seconds=${MPCONFIG=dataverse.db.slow-query-threshold-in-seconds:-1}",
            "fish.payara.log-jdbc-calls=${MPCONFIG=dataverse.db.log-jdbc-calls:false}"
        })
public class DataSourceProducer {

    @Resource(lookup = "java:app/jdbc/dataverse")
    DataSource ds;

    @Produces
    public DataSource getDatasource() {
        return ds;
    }
}
