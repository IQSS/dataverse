## Banner Messages Can Be Hidden Behind an Icon

In previous versions of Dataverse, the full text of banner messages are shown. Now it's possible to hide banner messages behind a "News" link with a bullhorn icon in the navbar, to the left of "Add Data". Clicking it will reveal the messages.

To hide a banner message behind an icon, set the new optional `collapsed` field to `true` when adding the banner message via API. The default is `false`, which shows the full text as before. The list of banner messages returned by the API now includes `dismissibleByUser` and `collapsed`. See [the guides](https://guides.dataverse.org/en/latest/api/native-api.html#manage-banner-messages) for details.
