Two new feature flags mount React components from `dataverse-frontend` inside the JSF UI, both off by default:

- `dataverse.feature.react-uploader` replaces the file uploader on the dataset edit page.
- `dataverse.feature.react-tree-view` renders the Files tab tree view.

The components are not shipped in the WAR. Build them from `dataverse-frontend`, deploy them behind your web server or as a WAR alongside Dataverse, and point `dataverse.reusable-components.base-url` at that path. The flags do nothing until it is set. They also require `dataverse.feature.api-session-auth`, and `dataverse.feature.api-session-auth-hardening` is recommended alongside it. See [Reusable Frontend Components](https://guides.dataverse.org/en/latest/installation/reusable-components.html) for setup.

With `react-uploader` enabled, creating a dataset becomes a two-step flow: save the metadata first, then add files on the edit page. The React uploader needs a persisted dataset before it can request upload URLs.

Direct-upload responses now carry a `tagging` field telling the client what to send as `x-amz-tagging`, and multipart uploads are tagged server-side. An S3 store that rejects object tagging now needs `dataverse.files.<driverId>.disable-tagging` for multipart uploads, where previously only single-part uploads required it.
