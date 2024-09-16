The configuration setting `dataverse.pid.*.permalink.base-url`, which is used for PermaLinks, has been updated to
support greater flexibility. Previously, the string "/citation?persistentId=" was automatically appended to the
configured base URL. With this update, the base URL will now be used exactly as configured, without any automatic
additions.

**Upgrade instructions:**

- If you currently use a PermaLink provider with a configured `base-url`: You must manually append
   "/citation?persistentId=" to the existing base URL to maintain functionality.
- If you use a PermaLink provider without a configured `base-url`: No changes are required.