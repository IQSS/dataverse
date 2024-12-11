Some repository extend the "oai_dc" metadata prefix with specific namespaces. In this case, harvesting of these datasets is not possible, as an XML parsing error is raised.

The PR [#10837](https://github.com/IQSS/dataverse/pull/10837) allows the harvesting of these datasets by excluding tags with namespaces that are not "dc:", and harvest only metadata with the "dc" namespace.
