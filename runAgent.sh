#!/bin/bash

source common.sh


echo core.Agent $name signer-keys/public_key.der

java -cp "$CLASSPATH" \
  -Djava.rmi.server.codebase="$CODEBASE" \
  -Djava.rmi.server.hostname="$HOSTNAME" \
  -Djava.security.policy=Agent.policy \
    core.Agent $name signer-keys/public_key.der


