# Update to Payara 5.2022.3 highly recommended

*NOTE: this might be rephrased to "required" depending on https://github.com/IQSS/dataverse/pull/8915 being merged.*

With lots of bug and security fixes included, we encourage everyone to update to Payara 5.2022.3 as soon as possible.

**Note:** with the approaching EOL for the Payara 5 Community release train it's likely we will switch to a
yet-to-be-released Payara 6 in the not-so-far-away future.

We recommend you ensure you followed all update instructions from the past releases regarding Payara.
(latest Payara update was for [v5.6](https://github.com/IQSS/dataverse/release/v5.6))

Upgrading requires a maintenance window and downtime. Please plan ahead, create backups of your database, etc.

The steps below are a simple matter of reusing your existing domain directory with the new distribution.
But we also recommend that you review the Payara upgrade instructions as it could be helpful during any troubleshooting:
[Payara Release Notes](https://docs.payara.fish/community/docs/Release%20Notes/Release%20Notes%205.2022.3.html)

If you are running Payara as a non-root user (and you should be!), remember not to execute the commands below as root.
Use `sudo` to change to that user first. For example, `sudo -i -u dataverse` if `dataverse` is your dedicated
application user.

In the following commands we assume that Payara 5 is installed in `/usr/local/payara5`. If not, adjust as needed.

```shell
export PAYARA=/usr/local/payara5
```

(or `setenv PAYARA /usr/local/payara5` if you are using a `csh`-like shell)

1. Undeploy the previous version

```shell
    $PAYARA/bin/asadmin list-applications
    $PAYARA/bin/asadmin undeploy dataverse<-version>
```

2. Stop Payara

```shell
    service payara stop
    rm -rf $PAYARA/glassfish/domains/domain1/generated
```

3. Move the current Payara directory out of the way

```shell
    mv $PAYARA $PAYARA.MOVED
```

4. Download the new Payara version (5.2022.3), and unzip it in its place

5. Replace the brand new payara/glassfish/domains/domain1 with your old, preserved domain1

6. Start Payara

```shell
    service payara start
```

7. Deploy this version.

```shell
    $PAYARA/bin/asadmin deploy dataverse-5.12.war
```

8. Restart payara

```shell
    service payara stop
    service payara start
```