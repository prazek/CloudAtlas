#!/bin/sh

SOURCES=(out/production/CloudAtlas/ out/production/CloudAtlas/lib/sigar.jar)

IFS=':'
CLASSPATH="${SOURCES[*]// /}"
unset IFS

CODEBASE="${SOURCES[@]/#/file://$PWD/}"

HOSTNAME=`hostname`

echo java -cp "$CLASSPATH" \
       -Djava.rmi.server.codebase="$CODEBASE"      \
       -Djava.rmi.server.hostname="$HOSTNAME"      \
       -Djava.security.policy=Agent.policy         \
         changelater.Agent /home

java -cp "$CLASSPATH" \
  -Djava.rmi.server.codebase="$CODEBASE" \
  -Djava.rmi.server.hostname="$HOSTNAME" \
  -Djava.security.policy=Agent.policy \
    changelater.Agent /home


