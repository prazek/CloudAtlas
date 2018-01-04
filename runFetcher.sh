#!/bin/bash

source common.sh

java -cp "$CLASSPATH" \
  -Djava.rmi.server.codebase="$CODEBASE" \
  -Djava.rmi.server.hostname="$HOSTNAME" \
  -Djava.library.path=libsigar/ \
  -Djava.security.policy=Fetcher.policy \
    fetcher.Fetcher "$name"  fetcher.ini


