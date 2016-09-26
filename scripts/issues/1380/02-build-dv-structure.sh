#!/bin/bash

echo Run this after running setup-users.sh, and making Pete an
echo admin on the root dataverse.


PETE=$(grep :result: users.out | grep Pete | cut -f4 -d: | tr -d \ )
UMA=$(grep :result: users.out | grep Uma | cut -f4 -d: | tr -d \ )

pushd ../../api
./setup-dvs.sh $PETE $UMA
popd
