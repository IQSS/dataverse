package edu.harvard.iq.dataverse.pidproviders;

public interface PidProviderFactory {
    
    public String getType();
    
    public PidProvider createPidProvider(String id);
    
    /**
     * Get ordinal value to ensure loading order of provider factories is coherent
     */
    default int getOrdinal() {
        return 100;
    }
}
