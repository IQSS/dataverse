## License sorting

Licenses as shown in the dropdown in UI can be now sorted by the superusers. See [Configuring Licenses](https://guides.dataverse.org/en/5.10/installation/config.html#configuring-licenses) section of the Installation Guide for reference.

## Backward Incompatibilities

License files are now required to contain the new "sortOrder" column. When attempting to create a new license without this field, an error would be returned. See [Configuring Licenses](https://guides.dataverse.org/en/5.10/installation/config.html#configuring-licenses) section of the Installation Guide for reference.