OAI-PMH error handling has been improved to display a machine-readable error in XML rather than a 500 error with no further information.

- /oai?foo=bar will show "No argument 'verb' found"
- /oai?verb=foo&verb=bar will show "Verb must be singular, given: '[foo, bar]'"
