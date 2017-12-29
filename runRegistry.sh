#!/bin/bash

source common.sh
rmiregistry -J-Djava.rmi.server.codebase="$CODEBASE" 4242