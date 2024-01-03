package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.settings.JvmSettings;

class DataCiteProviderFactory extends PidProviderFactory {
    
    public PidProvider createProvider(String providerName) {
            String providerType=JvmSettings.PID_PROVIDER_TYPE.lookup(providerName);
            if(!providerType.equals(DataCiteDOIProvider.TYPE)) {
                //Being asked to create a non-DataCite provider
                return null;
            }
            String providerAuthority=JvmSettings.PID_PROVIDER_AUTHORITY.lookup(providerName);
            String providerShoulder=JvmSettings.PID_PROVIDER_SHOULDER.lookup(providerName);
            String mdsUrl=JvmSettings.DATACITE_MDS_API_URL.lookup(providerName);
            String apiUrl = JvmSettings.DATACITE_REST_API_URL.lookup(providerName);
            String username = JvmSettings.DATACITE_USERNAME.lookup(providerName);
            String password = JvmSettings.DATACITE_PASSWORD.lookup(providerName);
            String[] whitelist = JvmSettings.PID_PROVIDER_WHITELIST.lookup(providerName).split(",");
            
            return new DataCiteDOIProvider(providerAuthority, providerShoulder, mdsUrl, apiUrl, username, password);
    }

    public String getType() {
        return DataCiteDOIProvider.TYPE;
    }
}
