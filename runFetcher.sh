#!/bin/sh

SOURCES=(out/production/CloudAtlas/)

IFS=':'
CLASSPATH="${SOURCES[*]// /}"
unset IFS

CODEBASE="${SOURCES[@]/#/file://$PWD/}"


HOSTNAME=`hostname`

java -cp "$CLASSPATH" \
  -Djava.rmi.server.codebase="$CODEBASE" \
  -Djava.rmi.server.hostname="$HOSTNAME" \
  -Djava.security.policy=Fetcher.policy \
    changelater.Fetcher home


