package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;

class FakeProviderFactory implements PidProviderFactory {

    @Override
    public PidProvider createPidProvider(String providerId) {
        String providerType = JvmSettings.PID_PROVIDER_TYPE.lookup(providerId);
        if (!providerType.equals(FakeDOIProvider.TYPE)) {
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
        String managedList = JvmSettings.PID_PROVIDER_MANAGED_LIST.lookup(providerId);
        String excludedList = JvmSettings.PID_PROVIDER_EXCLUDED_LIST.lookup(providerId);

        return new FakeDOIProvider(providerId, providerLabel, providerAuthority, providerShoulder, identifierGenerationStyle,
                datafilePidFormat, managedList, excludedList);
    }

    public String getType() {
        return FakeDOIProvider.TYPE;
    }

}
