#!/bin/bash

source common.sh

java -cp "$CLASSPATH" \
  -Djava.rmi.server.codebase="$CODEBASE" \
  -Djava.rmi.server.hostname="$HOSTNAME" \
  -Djava.security.policy=Fetcher.policy \
    fetcher.Fetcher "/uw/violet07" fetcher.ini


