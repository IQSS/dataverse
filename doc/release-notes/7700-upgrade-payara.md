### Payara 5.2021.1 (or Higher) Required

Some changes in this release require an upgrade to Payara 5.2021.1 or higher.

Instructions on how to update can be found in the
[Payara documentation](https://docs.payara.fish/community/docs/5.2021.1/documentation/user-guides/upgrade-payara.html)

It would likely be safer to upgrade Payara first, while still running Dataverse 5.3, and then proceed with the steps below. Upgrading from an earlier version of Payara should be a straightforward process: Undeploy Dataverse; stop Payara; move the current Payara directory out of the way; unzip the new Payara version in its place; replace the brand new payara/glassfish/domains/domain1 with your old, preserved domain1; start Payara, deploy Dataverse 5.3. We still recommend that you read the detailed upgrade instructions above; and, if you run into any issues with this upgrade, it will help to be able to separate them from any problems with the upgrade of Dataverse proper.
