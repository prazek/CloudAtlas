#!/bin/bash

source common.sh

echo java -cp "$CLASSPATH" \
       -Djava.rmi.server.codebase="$CODEBASE"      \
       -Djava.rmi.server.hostname="$HOSTNAME"      \
       -Djava.security.policy=Agent.policy         \
         core.Agent /uw/violet07

java -cp "$CLASSPATH" \
  -Djava.rmi.server.codebase="$CODEBASE" \
  -Djava.rmi.server.hostname="$HOSTNAME" \
  -Djava.security.policy=Agent.policy \
    core.Agent /uw/violet07 signer-keys/public_key.der


