package edu.harvard.iq.dataverse.pidproviders.perma;

import com.google.auto.service.AutoService;

import edu.harvard.iq.dataverse.pidproviders.PidProvider;
import edu.harvard.iq.dataverse.pidproviders.PidProviderFactory;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;

@AutoService(PidProviderFactory.class)
public class PermaLinkProviderFactory implements PidProviderFactory {
    
    @Override
    public PidProvider createPidProvider(String providerId) {
        String providerType = JvmSettings.PID_PROVIDER_TYPE.lookup(providerId);
        if (!providerType.equals(PermaLinkPidProvider.TYPE)) {
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

        String baseUrl = JvmSettings.PERMALINK_BASE_URL.lookupOptional(providerId)
                .orElse(SystemConfig.getDataverseSiteUrlStatic() + "/citation?persistentId=" + PermaLinkPidProvider.PERMA_PROTOCOL + ":");
        ;
        String separator = JvmSettings.PERMALINK_SEPARATOR.lookupOptional(providerId).orElse("");

        return new PermaLinkPidProvider(providerId, providerLabel, providerAuthority, providerShoulder, identifierGenerationStyle,
                datafilePidFormat, managedList, excludedList, baseUrl, separator);
    }

    public String getType() {
        return PermaLinkPidProvider.TYPE;
    }

}
