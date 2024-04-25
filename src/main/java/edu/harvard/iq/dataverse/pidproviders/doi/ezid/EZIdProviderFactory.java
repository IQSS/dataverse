package edu.harvard.iq.dataverse.pidproviders.doi.ezid;

import com.google.auto.service.AutoService;

import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidProviderFactory;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;

@AutoService(PidProviderFactory.class)
public class EZIdProviderFactory implements PidProviderFactory {

    @Override
    public PidProvider createPidProvider(String providerId) {
        String providerType = JvmSettings.PID_PROVIDER_TYPE.lookup(providerId);
        if (!providerType.equals(EZIdDOIProvider.TYPE)) {
            // Being asked to create a non-EZId provider
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

        String baseUrl = JvmSettings.EZID_API_URL.lookupOptional(providerId).orElse("https://ezid.cdlib.org");
        String username = JvmSettings.EZID_USERNAME.lookup(providerId);
        String password = JvmSettings.EZID_PASSWORD.lookup(providerId);

        return new EZIdDOIProvider(providerId, providerLabel, providerAuthority, providerShoulder, identifierGenerationStyle, datafilePidFormat,
                managedList, excludedList, baseUrl, username, password);
    }

    public String getType() {
        return EZIdDOIProvider.TYPE;
    }

}
