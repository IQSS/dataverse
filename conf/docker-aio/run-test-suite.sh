#!/bin/bash
# This is the canonical list of which "IT" tests are expected to pass.

dvurl=$1
if [ -z "$dvurl" ]; then
	dvurl="http://localhost:8084"
fi

integrationtests=$(<tests/integration-tests.txt)

# Please note the "dataverse.test.baseurl" is set to run for "all-in-one" Docker environment.
# TODO: Rather than hard-coding the list of "IT" classes here, add a profile to pom.xml.
mvn test -Dtest=$integrationtests -Ddataverse.test.baseurl=$dvurl
