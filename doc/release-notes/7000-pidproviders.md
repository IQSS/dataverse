# Changes to PID provider JVM settings

In prepration for a future feature to use multiple PID providers at the same time, all JVM settings for PID providers
have been enabled to be configured using MicroProfile Config. In the same go, they were renamed to match the name
of the provider to be configured.

Please watch your log files for deprecation warnings. Your old settings will be picked up, but you should migrate
to the new names to avoid unnecessary log clutter and get prepared for more future changes. An example message
looks like this:

```
[#|2023-03-31T16:55:27.992+0000|WARNING|Payara 5.2022.5|edu.harvard.iq.dataverse.settings.source.AliasConfigSource|_ThreadID=30;_ThreadName=RunLevelControllerThread-1680281704925;_TimeMillis=1680281727992;_LevelValue=900;|
   Detected deprecated config option doi.username in use. Please update your config to use dataverse.pid.datacite.username.|#]
```
