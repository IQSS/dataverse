Idle session timeout for logged-in users has been made configurable in this release. 
The default is now set to 8 hours (this is a change from the previous default value of 24 hours). 
If you want to change it, set the setting :LoginSessionTimeout to the new value *in minutes*. 
For example, to reduce the timeout to 4 hours:                                         
                                                                                                                                          
 curl -X PUT -d 240 http://localhost:8080/api/admin/settings/:LoginSessionTimeout

Once again, this is the session timeout for *logged-in* users only. For the anonymous sessions the sessions are set to time out after the default ``session-timeout`` value (also in minutes) in the web.xml of the Dataverse application, which is set to 10 minutes. You will most likely not ever need to change this, but if you do, configure it by editing the web.xml file. 