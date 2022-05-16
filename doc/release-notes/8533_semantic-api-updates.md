
## Notes for Developers and Integrators

This release include an update to the experimental semantic API and the underlying assignment of URIs to metadatablock termshat are not explicitly mapped to terms in community vocabularies. The change affects the output of the OAI_ORE metadata export, the OAI_ORE file in archival bags, and the input/out allowed for those terms in the semantic api. For those updating integrating code or existing files intended for input into this release of Dataverse: URIs of the form:
  https://dataverse.org/schema/<block name>/<parentField name>#<childField title>, and
  https://dataverse.org/schema/<block name>/<Field title>
  are both replaced with URIs of the form:
  https://dataverse.org/schema/<block name>/<Field name>

## Additional Release Steps

Upgrade should include re-export of metadata files (only the OAI_ORE is affected).

For this PR and other changes coming from DataCommons, it will also be advisable for people archiving Bags to re-archive. More detail on the overall set if changes in those tbd PRs.
