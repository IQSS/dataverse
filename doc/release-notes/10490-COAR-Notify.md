### Support for COAR Notify Relationship Announcement

Dataverse now supports sending and receiving [Linked Data Notification ](https://www.w3.org/TR/ldn/) messages involved in the 
[COAR Notify Relationship Announcement Workflow](https://coar-notify.net/catalogue/workflows/repository-relationship-repository/).

Dataverse can send messages to configured repositories announcing that a dataset has a related publication (as defined in the dataset metadata). This may be done automatically upon publication or triggered manually by a superuser. The receiving repository may do anything with the message, with the default expectation being that the repository will create a backlink from the publication to the dataset (assuming the publication exists in the repository, admins agree the link makes sense, etc.)

Conversely, Dataverse can receive notices from other configured repositories announcing relationships between their publications and datasets. If the referenced dataset exists in the Dataverse instance, a notification will be sent to users who can publish the dataset, or, optionally, only superusers who can publish the dataset. They can then decide whether to create a backlink to the publication in the dataset metadata.

(Earlier releases of Dataverse had experimental support in this area that was based on message formats defined prior to finalization of the COAR Notify specification for relationship announcements.)

#### New Settings/JVM Options

Configuration for sending messages involves specifying the
:COARNotifyRelationshipAnnouncementTargets and :COARNotifyRelationshipAnnouncementTriggerFields

Configuration to receive messages involves specifying
DATAVERSE_LDN_ALLOWED_HOSTS (dataverse.ldn.allowed-hosts)

Notifications are sent by default to users who can publish a dataset. The option below can be used to restrict notifications to superusers who can publish the dataset.
 
DATAVERSE_COAR_NOTIFY_RELATIONSHIP_ANNOUNCEMENT_NOTIFY_SUPERSUSERS_ONLY

