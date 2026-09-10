This release resolves two older issues about provenance files and improves the related documentation.

When uploading a wrong provenance JSON, the user was still able to click on the preview button which caused an exception.
From now on, this button will not be available. Also, the error message about the wrong JSON now includes information about what the error actually is.

The user guide and the GUI now explicitly state that Dataverse only accepts the PROV-JSON format.
