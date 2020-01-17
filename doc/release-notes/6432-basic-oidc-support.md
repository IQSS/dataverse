# Basic OpenID Connect support
Working on epic #5974 brought us refactored code for our OAuth 2.0 based login options.
This has now been extended to provide basic support for any OpenID Connect compliant
authentication provider.

While with our OAuth 2.0 login options you had to implement support for every provider
by pull request, OpenID Connect provides a standardized way for authentication, user 
details and more. You are able to use any provider just by loading a configuration file,
without touching the codebase.

While the usual prominent providers like Google et al feature OIDC support, there are
plenty of options to easily attach your current user storage to a custom made provider,
using enterprise grade software. See documentation for more details.

This is to be extended with support for attribute mapping, group syncing and more in
future versions of the code.  