# Exportación de metadatos (PLATICA)

## Cómo funciona

`GET /api/datasets/export?exporter=<formato>&persistentId=<pid>` → `ExportService.getExport()` → plugin `Exporter`
(`src/main/java/edu/harvard/iq/dataverse/export/*Exporter.java`). Cada exportador parte de una de tres fuentes que le
da `InternalExportDataProvider`:

| Fuente | Generada en | Formatos |
|---|---|---|
| JSON nativo (DTO) | `JsonPrinter.datasetAsJsonForDTO()` | `dataverse_json`, `ddi`, `oai_ddi`, `html`, `oai_datacite` (OpenAIRE), `oai_dc`, `dcterms` |
| JSON-LD Schema.org | `DatasetVersion.getJsonLd()` | `schema.org`, `croissant` (copia `publisher`) |
| XML DataCite | `XmlMetadataTemplate` | `Datacite` (el mismo XML que se envía a DataCite al registrar/actualizar DOIs) |

**Caché:** para versiones publicadas la exportación se genera al publicar y se guarda como fichero auxiliar
`export_<formato>.cached`; las peticiones posteriores se sirven desde ahí. Tras cambiar el código o la configuración
hay que regenerarlas:

```bash
curl -H "X-Dataverse-key: $API_TOKEN" "$SERVER/api/admin/metadata/reExportAll"
# o un único dataset:
curl -H "X-Dataverse-key: $API_TOKEN" "$SERVER/api/admin/metadata/:persistentId/reExportDataset?persistentId=doi:10.82663/XXXX"
```

## País del editor (`publisher.address.addressCountry`)

Setting `dataverse.platica.publisher-country` (env `DATAVERSE_PLATICA_PUBLISHER_COUNTRY`), por defecto `ES` en
`microprofile-config.properties`. Vacío = no se emite. Solo afecta a Schema.org (y Croissant, que lo copia):

```json
"publisher": { "@type": "Organization", "name": "PLATICA Repositorio",
               "address": { "@type": "PostalAddress", "addressCountry": "ES" } }
```

`provider` no cambia. El nombre sale del setting `:InstallationName` (o del nombre de la colección raíz).

### Por qué no en los demás formatos

Ninguno de los demás formatos tiene un campo de país o dirección para el editor. Añadir elementos fuera de su esquema
genera registros **inválidos**:

- **DataCite** (kernel 4.7): `<publisher>` es texto con los atributos `publisherIdentifier`,
  `publisherIdentifierScheme`, `schemeURI` y `xml:lang`. Un XML inválido haría fallar el registro y la actualización
  de DOIs en DataCite. La alternativa válida es un identificador ROR del editor (el registro ROR incluye el país).
- **OpenAIRE** (basado en DataCite 4.1): `<publisher>` es texto. Los validadores de OpenAIRE rechazarían el registro.
- **DDI Codebook 2.5**: `<distrbtr>`/`<producer>` solo admiten `abbr`, `affiliation` y `URI`. Romper el XSD afecta a
  los recolectores (otros Dataverse, CESSDA).
- **Dublin Core**: los 15 elementos son literales. Meter "(ES)" en `dc:publisher` contaminaría el nombre (y las citas).
- **OAI-ORE**: es JSON-LD, así que técnicamente se podría, pero se usa para bags de archivo e importación. No aporta.
