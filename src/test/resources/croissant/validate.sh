#!/bin/sh
# Check datasets with this tool as well: https://huggingface.co/spaces/JoaquinVanschoren/croissant-checker
#
# Following warnings are expected for drafts.
# -  [Metadata(Draft Dataset)] Property "https://schema.org/datePublished" is recommended, but does not exist.
# -  [Metadata(Draft Dataset)] WarningException("Version doesn't follow MAJOR.MINOR.PATCH: DRAFT. For more information refer to: https://semver.org/spec/v2.0.0.html")
mlcroissant validate --jsonld $1
exit
for i in */expected/*.json; do
  echo testing $i
  mlcroissant validate --jsonld $i
done
