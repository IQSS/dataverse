#It was reported on 9949 that on the first launch of the containers Dataverse would not be deployed on payara
#this was caused by a race condition due postgress not being ready. A solion for docker compose was prepared
#but didn't work due a compatibility issue on the Maven pluggin [https://github.com/fabric8io/docker-maven-plugin/issues/888]
wait4x tcp "${DATAVERSE_DB_HOST:-postgres}:${DATAVERSE_DB_PORT:-5432}" -t 120s
