#!/bin/bash

SOURCES=(out/production/CloudAtlas/ out/production/CloudAtlas/lib/*)

IFS=':'
CLASSPATH="${SOURCES[*]// /}"
unset IFS

CODEBASE="${SOURCES[@]/#/file://$PWD/}"

echo $CLASSPATH
echo $SOURCES
HOSTNAME=`hostname`

java -cp "$CLASSPATH" \
  -Djava.rmi.server.codebase="$CODEBASE" \
  -Djava.rmi.server.hostname="$HOSTNAME" \
  -Djava.security.policy=Fetcher.policy \
    fetcher.Fetcher "/uw/violet07" fetcher.ini


