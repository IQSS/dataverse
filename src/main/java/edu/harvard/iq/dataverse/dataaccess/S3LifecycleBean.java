package edu.harvard.iq.dataverse.dataaccess;

import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

@Singleton
@Startup
public class S3LifecycleBean {

    @PreDestroy
    public void cleanup() {
        S3AccessIO.closeAll();
    }
}
