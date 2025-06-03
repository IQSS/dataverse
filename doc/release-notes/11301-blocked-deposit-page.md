### Reduced chance of losing metadata on Edit Dataset Metadata page

The remedy for the problem consists of two parts:
* Do not show the _host dataverse_ field when there is nothing to choose. This mimics the behaviour for templates.
* When you accidentally start typing in the _host dataverse_ field, undo the change with backspace, fill in the other metadata fields and save the draft, the page used to get blocked due to an exception. Reloading the page would erase all your input. The exception (caused by an invalid argument) is remedied by looking for the root dataverse.