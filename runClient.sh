#!/bin/bash

source common.sh

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


