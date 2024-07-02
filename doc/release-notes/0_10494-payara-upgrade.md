# Upgrade Payara to v6.2024.6

With this version of Dataverse, we encourage you to upgrade to version 6.2024.6.
This will address security issues accumulated since the release of 6.2023.8, which was required since Dataverse release 6.0.

## Instructions for Upgrading

If you are using GDCC containers, this upgrade is included when pulling new release images.
No manual intervention is necessary.

We recommend you ensure you followed all update instructions from the past releases regarding Payara.
(Latest Payara update was for [v6.0](https://github.com/IQSS/dataverse/releases/tag/v6.0))

Upgrading requires a maintenance window and downtime. Please plan ahead, create backups of your database, etc.

The steps below are a simple matter of reusing your existing domain directory with the new distribution.
But we also recommend that you review the Payara upgrade instructions as it could be helpful during any troubleshooting:
[Payara Release Notes](https://docs.payara.fish/community/docs/Release%20Notes/Release%20Notes%206.2024.6.html)
We assume you are already on a Dataverse 6.x installation, using a Payara 6.x release.

```shell
export PAYARA=/usr/local/payara6
```

(or `setenv PAYARA /usr/local/payara6` if you are using a `csh`-like shell)

1\. Undeploy the previous version

```shell
    $PAYARA/bin/asadmin list-applications
    $PAYARA/bin/asadmin undeploy dataverse<-version>
```

2\. Stop Payara

```shell
    service payara stop
    rm -rf $PAYARA/glassfish/domains/domain1/generated
    rm -rf $PAYARA/glassfish/domains/domain1/osgi-cache
    rm -rf $PAYARA/glassfish/domains/domain1/lib/databases
```

3\. Move the current Payara directory out of the way

```shell
    mv $PAYARA $PAYARA.MOVED
```

4\. Download the new Payara version (6.2024.6), and unzip it in its place

5\. Replace the brand new payara/glassfish/domains/domain1 with your old, preserved domain1

6\. Make sure that you have the following `--add-opens` options in your domain.xml. If not present, add them:

```diff
--- payara-6.2023.8/glassfish/domains/domain1/config/domain.xml
+++ payara-6.2024.6/glassfish/domains/domain1/config/domain.xml
@@ -212,12 +212,16 @@
                 <jvm-options>--add-opens=java.naming/javax.naming.spi=ALL-UNNAMED</jvm-options>
                 <jvm-options>--add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED</jvm-options>
                 <jvm-options>--add-opens=java.logging/java.util.logging=ALL-UNNAMED</jvm-options>
+                <jvm-options>--add-opens=java.management/javax.management=ALL-UNNAMED</jvm-options>
+                <jvm-options>--add-opens=java.management/javax.management.openmbean=ALL-UNNAMED</jvm-options>
                 <jvm-options>[17|]--add-exports=java.base/sun.net.www=ALL-UNNAMED</jvm-options>
                 <jvm-options>[17|]--add-exports=java.base/sun.security.util=ALL-UNNAMED</jvm-options>
                 <jvm-options>[17|]--add-opens=java.base/java.lang.invoke=ALL-UNNAMED</jvm-options>
                 <jvm-options>[17|]--add-opens=java.desktop/java.beans=ALL-UNNAMED</jvm-options>
                 <jvm-options>[17|]--add-exports=jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED</jvm-options>
                 <jvm-options>[17|]--add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED</jvm-options>
+                <jvm-options>[17|]--add-opens=java.base/java.io=ALL-UNNAMED</jvm-options>
+                <jvm-options>[21|]--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED</jvm-options>
                 <jvm-options>-Xmx512m</jvm-options>
                 <jvm-options>-XX:NewRatio=2</jvm-options>
                 <jvm-options>-XX:+UnlockDiagnosticVMOptions</jvm-options>
@@ -447,12 +451,16 @@
                 <jvm-options>--add-opens=java.naming/javax.naming.spi=ALL-UNNAMED</jvm-options>
                 <jvm-options>--add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED</jvm-options>
                 <jvm-options>--add-opens=java.logging/java.util.logging=ALL-UNNAMED</jvm-options>
+                <jvm-options>--add-opens=java.management/javax.management=ALL-UNNAMED</jvm-options>
+                <jvm-options>--add-opens=java.management/javax.management.openmbean=ALL-UNNAMED</jvm-options>
                 <jvm-options>[17|]--add-exports=java.base/sun.net.www=ALL-UNNAMED</jvm-options>
                 <jvm-options>[17|]--add-exports=java.base/sun.security.util=ALL-UNNAMED</jvm-options>
                 <jvm-options>[17|]--add-opens=java.base/java.lang.invoke=ALL-UNNAMED</jvm-options>
                 <jvm-options>[17|]--add-opens=java.desktop/java.beans=ALL-UNNAMED</jvm-options>
                 <jvm-options>[17|]--add-exports=jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED</jvm-options>
                 <jvm-options>[17|]--add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED</jvm-options>
+                <jvm-options>[17|]--add-opens=java.base/java.io=ALL-UNNAMED</jvm-options>
+                <jvm-options>[21|]--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED</jvm-options>
                 <jvm-options>-Xmx512m</jvm-options>
                 <jvm-options>-XX:NewRatio=2</jvm-options>
                 <jvm-options>-XX:+UnlockDiagnosticVMOptions</jvm-options>
```
(You can also save this as a patch file and try to apply it.)

TODO: For the combined 6.3 release note, I would consider replacing the patch format above with just the 4 specific options, for clarity etc. (L.A.) As in: 
```
         <jvm-options>--add-opens=java.management/javax.management=ALL-UNNAMED</jvm-options>
         <jvm-options>--add-opens=java.management/javax.management.openmbean=ALL-UNNAMED</jvm-options>
         <jvm-options>[17|]--add-opens=java.base/java.io=ALL-UNNAMED</jvm-options>
         <jvm-options>[21|]--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED</jvm-options>
```

7\. Start Payara

```shell
    service payara start
```

8\. Deploy this version.

```shell
    $PAYARA/bin/asadmin deploy dataverse-6.3.war
```

9\. Restart payara

```shell
    service payara stop
    service payara start
