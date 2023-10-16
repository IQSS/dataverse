Detection of mime-types based on a filename with extension and detection of the RO-Crate metadata files.

From now on, filenames with extensions can be added into `MimeTypeDetectionByFileName.properties` file. Filenames added there will take precedence over simply recognizing files by extensions. For example, two new filenames are added into that file:
```
ro-crate-metadata.json=application/ld+json; profile="http://www.w3.org/ns/json-ld#flattened http://www.w3.org/ns/json-ld#compacted https://w3id.org/ro/crate"
ro-crate-metadata.jsonld=application/ld+json; profile="http://www.w3.org/ns/json-ld#flattened http://www.w3.org/ns/json-ld#compacted https://w3id.org/ro/crate"
```

Therefore, files named `ro-crate-metadata.json` will be then detected as RO-Crated metadata files from now on, instead as generic `JSON` files.
For more information on the RO-Crate specifications, see https://www.researchobject.org/ro-crate
