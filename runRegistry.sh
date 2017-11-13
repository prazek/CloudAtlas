#!/bin/bash

SOURCES=(out/production/CloudAtlas/)

IFS=':'
CLASSPATH="${SOURCES[*]// /}"
unset IFS

CODEBASE="${SOURCES[@]/#/file://$PWD/}"

echo $CODEBASE
rmiregistry -J-Djava.rmi.server.codebase="$CODEBASE"
