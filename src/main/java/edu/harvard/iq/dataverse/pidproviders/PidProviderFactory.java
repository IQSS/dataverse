package edu.harvard.iq.dataverse.pidproviders;

public interface PidProviderFactory {

    String getType();

    PidProvider createPidProvider(String id);
}
