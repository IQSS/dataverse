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
    // Has to be using ALIAS for at least Payara 5.2020.3, as MPCONFIG does not seem to read properly
    // from alias store while deploying. See upstream https://github.com/payara/Payara/issues/4487 and https://github.com/payara/Payara/issues/4709
    password = "${ALIAS=dataverse.db.password}",
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
