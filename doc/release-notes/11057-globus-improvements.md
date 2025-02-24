## Globus framework improvements

The improvements in this release build on top of the earlier work (such as PR #10781) and are part of the continuous effort of improvements and optimization, as the result of the experience gained during the adoption of Globus at IQSS as part of our Large Data Storage services.

The changes in this PR (#11125) focus on improving Globus *downloads* (i.e., transfers from Dataverse-linked Globus volumes to users' Globus collections). The mechanism of "Asynchronous Task Monitoring", first introduced in #10781 for *uploads*, has been extended to handle downloads as well. This generally makes downloads more reliable (specifically, how Dataverse manages the temporary access rules granting access to data to users, minimizing the risk of consequent downloads failing because of stale access rules left in place).

See `globus-use-experimental-async-framework` under [Feature Flags](https://guides.dataverse.org/en/latest/installation/config.html#feature-flags) and [dataverse.files.globus-monitoring-server](https://guides.dataverse.org/en/latest/installation/config.html#dataverse-files-globus-monitoring-server) in the Installation Guide.

Multiple other improvements have been made making the code implementing the Globus framework more reliable and robust.


