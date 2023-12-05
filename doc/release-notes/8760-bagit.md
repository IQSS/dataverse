For BagIT export, it is now possible to configure the following information in bag-info.txt...

Source-Organization: Harvard Dataverse
Organization-Address: 1737 Cambridge Street, Cambridge, MA, USA
Organization-Email: support@dataverse.harvard.edu

... using new JVM/MPCONFIG options:

- dataverse.bagit.sourceorg.name
- dataverse.bagit.sourceorg.address
- dataverse.bagit.sourceorg.email

Previously, customization was possible by editing `Bundle.properties` but this is no longer supported.

For details, see https://dataverse-guide--10122.org.readthedocs.build/en/10122/installation/config.html#bag-info-txt
