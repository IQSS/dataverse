package edu.harvard.iq.dataverse.pidproviders;

public interface PidProviderFactory {
    
    public String getType();
    
    public PidProvider createPidProvider(String id);

}
