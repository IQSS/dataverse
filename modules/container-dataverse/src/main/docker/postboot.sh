#!/bin/bash

# check if we should disable DOI validation
if [[ ! -z "${DISABLE_DOI}" ]] && [[ "true" = "${DISABLE_DOI}" ]]; then
  echo "Disabling DOI validation"
  curl -X PUT -d FAKE http://localhost:8080/api/admin/settings/:DoiProvider
fi

# check if we should exclude emails from exports
if [[ ! -z "${EXCLUDE_EMAIL_EXPORTS}" ]] && [[ "true" = "${EXCLUDE_EMAIL_EXPORTS}" ]]; then
  echo "Excluding emails in exports"
  curl -X PUT -d true http://localhost:8080/api/admin/settings/:ExcludeEmailFromExport
fi


#wait-for-it localhost:8080 -- tail -f ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/logs/server.log