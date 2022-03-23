package edu.harvard.iq.dataverse.locality;

import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.List;
import jakarta.json.JsonObject;

public class StorageSiteUtil {

    public static StorageSite parse(JsonObject jsonObject) throws Exception {
        StorageSite storageSite = new StorageSite();
        storageSite.setHostname(getRequiredString(jsonObject, StorageSite.HOSTNAME));
        storageSite.setName(getRequiredString(jsonObject, StorageSite.NAME));
        try {
            storageSite.setPrimaryStorage(jsonObject.getBoolean(StorageSite.PRIMARY_STORAGE));
        } catch (Exception ex) {
            throw new IllegalArgumentException(StorageSite.PRIMARY_STORAGE + " must be true or false.");
        }
        storageSite.setTransferProtocols(parseTransferProtocolsString(jsonObject));
        return storageSite;
    }

    private static String parseTransferProtocolsString(JsonObject jsonObject) {
        String commaSeparatedInput = getRequiredString(jsonObject, StorageSite.TRANSFER_PROTOCOLS);
        String[] strings = commaSeparatedInput.split(",");
        for (String string : strings) {
            try {
                SystemConfig.TransferProtocols.fromString(string);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Exception processing the string '" + string + "' in comma-separated '" + StorageSite.TRANSFER_PROTOCOLS + "' list: " + ex.getLocalizedMessage());
            }
        }
        return commaSeparatedInput;
    }

    private static String getRequiredString(JsonObject jsonObject, String key) {
        try {
            String value = jsonObject.getString(key);
            return value;
        } catch (NullPointerException ex) {
            throw new IllegalArgumentException("String " + key + " is required!");
        }
    }

    public static void ensureOnlyOnePrimary(StorageSite storageSite, List<StorageSite> exitingSites) throws Exception {
        if (storageSite.isPrimaryStorage()) {
            for (StorageSite exitingSite : exitingSites) {
                if (exitingSite.isPrimaryStorage()) {
                    // obligatory Highlander reference
                    throw new Exception("Storage site " + exitingSite.getId() + " already has " + StorageSite.PRIMARY_STORAGE + " set to true. There can be only one.");
                }
            }
        }
    }

}
