#!/bin/bash

source common.sh

echo java -cp "$CLASSPATH" \
       -Djava.rmi.server.codebase="$CODEBASE"      \
       -Djava.rmi.server.hostname="$HOSTNAME"      \
         core.NetworkTest

java -cp "$CLASSPATH" \
  -Djava.rmi.server.codebase="$CODEBASE" \
  -Djava.rmi.server.hostname="$HOSTNAME" \
    core.NetworkTest


