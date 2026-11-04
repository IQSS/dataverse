### Installer zip and war file now include the version number

The installer is now distributed as `dvinstall-VERSION.zip` (e.g. `dvinstall-6.12.1.zip`) instead of `dvinstall.zip`, and the war file inside it keeps its versioned name (e.g. `dataverse-6.12.1.war`) instead of being renamed to `dataverse.war`.

As a result, new installations deploy the application as `dataverse-VERSION` rather than `dataverse`, which matches how the application is named when upgrading an existing installation. Unzipping the installer still creates a `dvinstall` directory, and the installer is run the same way as before.
