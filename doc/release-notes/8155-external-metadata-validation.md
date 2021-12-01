### Support for optional external metadata validation scripts

This enables an installation administrator to provide custom scripts for additional metadata validation when datasets are being published and/or when Dataverse collections are being published or modified. Harvard Dataverse Repository has been using this mechanism to combat content that violates our Terms of Use. All the validation or verification logic is defined in these external scripts, thus making it possible for an installation to add checks custom-tailored to their needs.  

Please note that only the metadata are subject to these validation checks (not the content of any uploaded files!). 

For more information, see the [Database Settings](https://guides.dataverse.org/en/5.9/installation/config.html) section of the Guide.
