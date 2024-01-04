package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;

class DataCiteProviderFactory implements PidProviderFactory {

    @Override
    public PidProvider createPidProvider(String providerName) {
            String providerType=JvmSettings.PID_PROVIDER_TYPE.lookup(providerName);
            if(!providerType.equals(DataCiteDOIProvider.TYPE)) {
                //Being asked to create a non-DataCite provider
                return null;
            }
            String providerAuthority=JvmSettings.PID_PROVIDER_AUTHORITY.lookup(providerName);
            String providerShoulder=JvmSettings.PID_PROVIDER_SHOULDER.lookupOptional(providerName).orElse("");
            String identifierGenerationStyle=JvmSettings.PID_PROVIDER_IDENTIFIER_GENERATION_STYLE.lookupOptional(providerName).orElse("randomString");
            String datafilePidFormat=JvmSettings.PID_PROVIDER_DATAFILE_PID_FORMAT.lookupOptional(providerName).orElse(SystemConfig.DataFilePIDFormat.DEPENDENT.toString());
            String managedList = JvmSettings.PID_PROVIDER_MANAGED_LIST.lookup(providerName);
            String excludedList = JvmSettings.PID_PROVIDER_EXCLUDED_LIST.lookup(providerName);
            
            String mdsUrl=JvmSettings.DATACITE_MDS_API_URL.lookup(providerName);
            String apiUrl = JvmSettings.DATACITE_REST_API_URL.lookup(providerName);
            String username = JvmSettings.DATACITE_USERNAME.lookup(providerName);
            String password = JvmSettings.DATACITE_PASSWORD.lookup(providerName);

            
            return new DataCiteDOIProvider(providerAuthority, providerShoulder, identifierGenerationStyle, datafilePidFormat, managedList, excludedList, mdsUrl, apiUrl, username, password);
    }

    public String getType() {
        return DataCiteDOIProvider.TYPE;
    }

}
