Dataverse + SolrCloud + Kerberos Authentication platform
================================================================

The contents of this directory contain documentation and platform configuration management files 
(e.g. Vagrantfile) that describe/implement a test environment for integrating Dataverse with a 
trio of remote servers providing a solrCloud service while requiring Kerberos authentication. 

This test environment establishes the following VMs on the **192.168.40.x** local subnet.<br>

+ *solrCloud1 (192.168.40.11\[:2181\]\[[:8983](http://192.168.40.11:8983/solr)\])*<BR>
  <sup>with ENABLE_TLS_ON_SOLR=1 (https://192.168.40.11\[[:8983](https://192.168.40.11:8983/solr)\])</sup>
+ *solrCloud2 (192.168.40.12\[:2181\]\[[:8983](http://192.168.40.12:8983/solr)\])*<BR>
  <sup>with ENABLE_TLS_ON_SOLR=1 (https://192.168.40.12\[[:8983](https://192.168.40.12:8983/solr)\])</sup>
+ *solrCloud3 (192.168.40.13\[:2181\]\[[:8983](http://192.168.40.13:8983/solr)\])*<BR>
  <sup>with ENABLE_TLS_ON_SOLR=1 (https://192.168.40.13\[[:8983](https://192.168.40.13:8983/solr)\])</sup>
+ *dataverse ([192.168.40.20:8080](http://192.168.40.20:8080))*<BR><BR>
+ *krbkdc (192.168.40.30)*

<BR>
Kerberos
--------
This test environment establishes the **DATAVERSE.TEST** kerberos realm.

In this environment service and application communications are authenticated using the following 
kerberos principals.

+ solr
    + solr/_host@DATAVERSE.TEST
    + HTTP/_host@DATAVERSE.TEST
+ Zookeeper
    + zookeeper/_host@DATAVERSE.TEST
    + HTTP/_host@DATAVERSE.TEST
+ Dataverse
    + dataverse/_host@DATAVERSE.TEST

<BR>
solrCloud
---------
Each solrCloud server runs solr as a service (see [../solrService_dataverse_platform/README.md](../solrService_dataverse_platform/README.md)) 
<BR>Each solrCloud server is also a member of a common zookeeper ensemble.

This environments configuration management uses an installation script packaged with Solr versions 
5.0.0 and newer to install solr as a system service.

This configuration is appropriate for use with all Solr version 5.x and newer packages.

NOTE: Solr 5+ no longer supports a default core/collection. This means that all api URI's must 
reference the collection/core name (e.g. http://myhost:8983/solr/collection1/...)!

<BR>
Browser Configuration <sub>[solr wiki](https://cwiki.apache.org/confluence/display/solr/Kerberos+Authentication+Plugin)</sub>
---------------------

"In order for your browser to access the Solr Admin UI after enabling Kerberos authentication, it 
must be able to negotiate with the Kerberos authenticator service to allow you access. Each browser 
supports this differently, and some (like Chrome) do not support it at all. If you see 401 errors 
when trying to access the Solr Admin UI after enabling Kerberos authentication, it's likely your 
browser has not been configured properly to know how or where to negotiate the authentication 
request."

For information on setting up your browser, including Google Chrome, to support Kerberos 
authentication please see [this website](http://www.cloudera.com/documentation/enterprise/latest/topics/cdh_sg_browser_access_kerberos_protected_url.html).

For Chrome on Mac you can use the following terminal commands
> defaults write com.google.Chrome AuthServerWhitelist "192.168.40.*"
<br>defaults write com.google.Chrome AuthNegotiateDelegateWhitelist "192.168.40.*"

To authenticate with the DATAVERSE.TEST realms KDC server start by adding the following realm 
information to your krb5.conf file. Then issue a standard kinit request.

> *<sub>ADD THE FOLLOWING TO /etc/krb5.conf \[realms\]</sub>*

> DATAVERSE.TEST = {<br>kdc = 192.168.40.30<br>admin_server = 192.168.40.30<br>default_domain = DATAVERSE.TEST<br>}

> *<sub>THEN {with password = password}</sub>*

> kinit dataverse_user@DATAVERSE.TEST 
