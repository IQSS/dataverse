A significant bug in the superuser-only "Update-Current-Version" publication was found and fixed in this release. If the Update-Current-Version option was used when changes were made to the dataset Terms (rather than to dataset metadata), or if the PID provider service was down/returned an error, the update would fail and render the dataset unusable and require restoration from a backup. The fix in this release allows the update to succeed in both of these cases and redesigns the functionality such that any unknown issues should not make the dataset unusable (i.e. the error would be reported and the dataset would remain in its current state with the last-published version as it was and changes still in the draft version.)

Users of earlier Dataverse releases are encouraged to alert their superusers to this issue. Those who wish to disable this functionality have two options:
* Change the dataset.updateRelease entry in the Bundle.properties file (or local language version) to "Do Not Use" or similar (doesn't disable but alerts superusers to the issue), or
* Edit the dataset.xhtml file to remove the lines

<c:if test="#{dataverseSession.user.isSuperuser()}">
  <f:selectItem rendered="#" itemLabel="#{bundle['dataset.updateRelease']}" itemValue="3" />
</c:if>

, delete the contents of the generated and osgi-cache directories in the Dataverse Payara domain, and restart the Payara server.
