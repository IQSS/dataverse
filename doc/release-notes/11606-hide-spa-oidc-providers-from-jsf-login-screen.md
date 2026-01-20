This release fixes a bug where the value of the dataverse.auth.oidc.enabled setting, available when Provisioning an authentication provider via JVM options (see ref: https://guides.dataverse.org/en/latest/installation/oidc.html#provision-via-jvm-options) was not being not being propagated to the current Dataverse user interface (where enabled=false providers are not displayed for login/registration) or represented in the GET api/admin/authenticationProviders API call.

A new JVM setting ('dataverse.auth.oidc.hidden-jsf') was added to hide an enabled OIDC Provider from the JSF UI. 

For Dataverse instances deploying both the current JSF UI and the new SPA UI, this fix allows the OIDC Keycloak provider configured for the SPA to be hidden in the JSF UI (useful in cases where it would duplicate other configured providers).
