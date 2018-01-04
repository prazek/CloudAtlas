#!/bin/bash

source common.sh

java -cp "$CLASSPATH" \
  -Djava.rmi.server.codebase="$CODEBASE" \
  -Djava.rmi.server.hostname="$HOSTNAME" \
  -Djava.security.policy=Agent.policy \
    core.QuerySignerService signer-keys/public_key.der signer-keys/private_key.der


