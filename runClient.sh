#!/bin/bash

SOURCES=(out/production/CloudAtlas/ out/production/CloudAtlas/lib/*.jar)

IFS=':'
CLASSPATH="${SOURCES[*]// /}"
unset IFS

CODEBASE="${SOURCES[@]/#/file://$PWD/}"

HOSTNAME=`hostname`

echo java -cp "$CLASSPATH" \
       -Djava.rmi.server.codebase="$CODEBASE"      \
       -Djava.rmi.server.hostname="$HOSTNAME"      \
       -Djava.security.policy=Client.policy         \
         client.Client /uw/violet07

java -cp "$CLASSPATH" \
  -Djava.rmi.server.codebase="$CODEBASE" \
  -Djava.rmi.server.hostname="$HOSTNAME" \
  -Djava.security.policy=Client.policy \
    client.Client /uw/violet07


