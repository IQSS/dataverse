package edu.harvard.iq.dataverse.pidproviders.doi.datacite;

import com.google.auto.service.AutoService;

import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidProviderFactory;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;

@AutoService(PidProviderFactory.class)
public class DataCiteProviderFactory implements PidProviderFactory {

    @Override
    public PidProvider createPidProvider(String providerId) {
        String providerType = JvmSettings.PID_PROVIDER_TYPE.lookup(providerId);
        if (!providerType.equals(DataCiteDOIProvider.TYPE)) {
            // Being asked to create a non-DataCite provider
            return null;
        }
        String providerLabel = JvmSettings.PID_PROVIDER_LABEL.lookup(providerId);
        String providerAuthority = JvmSettings.PID_PROVIDER_AUTHORITY.lookup(providerId);
        String providerShoulder = JvmSettings.PID_PROVIDER_SHOULDER.lookupOptional(providerId).orElse("");
        String identifierGenerationStyle = JvmSettings.PID_PROVIDER_IDENTIFIER_GENERATION_STYLE
                .lookupOptional(providerId).orElse("randomString");
        String datafilePidFormat = JvmSettings.PID_PROVIDER_DATAFILE_PID_FORMAT.lookupOptional(providerId)
                .orElse(SystemConfig.DataFilePIDFormat.DEPENDENT.toString());
        String managedList = JvmSettings.PID_PROVIDER_MANAGED_LIST.lookupOptional(providerId).orElse("");
        String excludedList = JvmSettings.PID_PROVIDER_EXCLUDED_LIST.lookupOptional(providerId).orElse("");

        String mdsUrl = JvmSettings.DATACITE_MDS_API_URL.lookupOptional(providerId).orElse("https://mds.test.datacite.org");
        String apiUrl = JvmSettings.DATACITE_REST_API_URL.lookupOptional(providerId).orElse("https://api.test.datacite.org");
        String username = JvmSettings.DATACITE_USERNAME.lookup(providerId);
        String password = JvmSettings.DATACITE_PASSWORD.lookup(providerId);

        return new DataCiteDOIProvider(providerId, providerLabel, providerAuthority, providerShoulder, identifierGenerationStyle,
                datafilePidFormat, managedList, excludedList, mdsUrl, apiUrl, username, password);
    }

    public String getType() {
        return DataCiteDOIProvider.TYPE;
    }

}
