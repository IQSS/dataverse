package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.GlobalIdServiceBean;

public interface PidProviderFactory {
    
    public String getType();
    
    public GlobalIdServiceBean createPidProvider(String name);

}
