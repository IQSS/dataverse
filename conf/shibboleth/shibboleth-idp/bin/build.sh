#!/usr/bin/env bash

declare LOCATION

LOCATION=$(dirname $0)

$LOCATION/runclass.sh net.shibboleth.idp.installer.impl.IdPBuildWar --ansi --home "$LOCATION/.." "$@"

