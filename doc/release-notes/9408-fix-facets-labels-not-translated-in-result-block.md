## Fix facets filter labels not translated in result block

On the main page, it's possible to filter results using search facets. If internationalization (i18n) has been activated in the Dataverse installation, allowing pages to be displayed in several languages, the facets are translated in the filter column. However, they aren't translated in the search results and remain in the default language, English.

This version of Dataverse fix this, and includes internationalization in the facets visible in the search results section.

For more information, see issue [#9408](https://github.com/IQSS/dataverse/issues/9408) and pull request [#10158](https://github.com/IQSS/dataverse/pull/10158)
