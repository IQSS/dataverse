package edu.harvard.iq.dataverse.jsf.support.login;

import edu.harvard.iq.dataverse.jsf.support.JsfTestConfig;

/**
 * Returns the LoginAdapter selected by the dataverse.test.jsf.loginadapter
 * setting (LOGIN_ADAPTER). Supported names, case-insensitive:
 *
 * <pre>
 *   builtin                  – Dataverse built-in username / password form
 *   shibboleth-direct        – IdP selector embedded in the Dataverse login
 *                              page (#idpSelectSelector)
 *   incommon-seamlessaccess  – InCommon / SeamlessAccess waypoint flow
 * </pre>
 */
public final class LoginAdapters {

    private LoginAdapters() {
    }

    /**
     * @throws IllegalStateException when the setting is missing or names an unknown adapter.
     */
    public static LoginAdapter get() {
        String adapterName = JsfTestConfig.loginAdapter();
        return switch (adapterName.trim().toLowerCase()) {
            case "builtin" -> new BuiltinAdapter();
            case "shibboleth-direct" -> new ShibbolethDirectAdapter();
            case "incommon-seamlessaccess" -> new InCommonSeamlessAccessAdapter();
            default -> throw new IllegalStateException("Unknown login adapter \"" + adapterName + "\". "
                    + "Valid options are: shibboleth-direct, incommon-seamlessaccess, builtin.");
        };
    }
}
