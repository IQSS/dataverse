package edu.harvard.iq.dataverse.pidproviders.handle;

import com.google.auto.service.AutoService;

import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidProviderFactory;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;

@AutoService(PidProviderFactory.class)
public class HandleProviderFactory implements PidProviderFactory {
    
    @Override
    public PidProvider createPidProvider(String providerId) {
        String providerType = JvmSettings.PID_PROVIDER_TYPE.lookup(providerId);
        if (!providerType.equals(HandlePidProvider.TYPE)) {
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

        int index = JvmSettings.HANDLENET_INDEX.lookupOptional(Integer.class, providerId).orElse(300);
        boolean independentHandleService = JvmSettings.HANDLENET_INDEPENDENT_SERVICE
                .lookupOptional(Boolean.class, providerId).orElse(false);
        String handleAuthHandle = JvmSettings.HANDLENET_AUTH_HANDLE.lookup(providerId);
        String path = JvmSettings.HANDLENET_KEY_PATH.lookup(providerId);
        String passphrase = JvmSettings.HANDLENET_KEY_PASSPHRASE.lookup(providerId);
        return new HandlePidProvider(providerId, providerLabel, providerAuthority, providerShoulder, identifierGenerationStyle,
                datafilePidFormat, managedList, excludedList, index, independentHandleService, handleAuthHandle, path,
                passphrase);
    }

    public String getType() {
        return HandlePidProvider.TYPE;
    }

}
