## Globus framework improvements

The improvements and optimizations in this release build on top of the earlier work (such as PR #10781). They are based on the experience gained at IQSS as part of the production rollout of the Large Data Storage services that utilizes Globus.

The changes in this PR (#11125) focus on improving Globus *downloads* (i.e., transfers from Dataverse-linked Globus volumes to users' Globus collections). Most importantly, th mechanism of "Asynchronous Task Monitoring", first introduced in #10781 for *uploads*, has been extended to handle downloads as well. This generally makes downloads more reliable (specifically, in how Dataverse manages temporary access rules granted to users, minimizing the risk of consequent downloads failing because of stale access rules left in place).

See `globus-use-experimental-async-framework` under [Feature Flags](https://guides.dataverse.org/en/latest/installation/config.html#feature-flags) and [dataverse.files.globus-monitoring-server](https://guides.dataverse.org/en/latest/installation/config.html#dataverse-files-globus-monitoring-server) in the Installation Guide.

Multiple other improvements have been made making the underlying Globus framework more reliable and robust.


