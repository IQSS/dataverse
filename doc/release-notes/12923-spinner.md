### Fixed "loading" spinner that stayed on screen after clicking "Contact Owner"

Since the move to Payara 7, clicking "Contact Owner" (or "Support" in the header) opened the contact dialog but left the loading spinner on screen, and the "Send Message" button was unstyled. The dialog is now loaded on demand, and the AJAX response that loads it asked the browser to load jQuery and PrimeFaces a second time. Reloading them removed the handlers that hide the spinner.

Dataverse now ignores scripts and stylesheets in AJAX responses that the page has already loaded (see `dv_ajax_resource_dedupe.js`). This works around a PrimeFaces 11 bug that was fixed in PrimeFaces 14.0.3 (https://github.com/primefaces/primefaces/issues/11714), and the workaround can be removed after PrimeFaces is upgraded.

No action is needed when upgrading. Users who still see the old spinner after upgrading may need to hard-reload their browser to pick up the new JavaScript file.

See also #12923.
