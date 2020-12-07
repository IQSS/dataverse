# Java Message System Configuration

The ingest part of Dataverse uses the Java Message System to create ingest tasks in a queue.
That queue had been configured from command line or domain.xml before. This has now changed to being done
in code.

In the unlikely case you might want to change any of these settings, feel free to change and recompile or raise an issue.
See `IngestQueueProducer` for more details.

If you want to clean up your existing installation, you can delete the old, unused queue like this:
```shell
asadmin delete-connector-connection-pool --cascade=true jms/IngestQueueConnectionFactoryPool
```