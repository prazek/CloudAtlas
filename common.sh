#!/bin/bash

SOURCES=(target/classes/ $HOME/.m2/repository)

if [ ! -e classpath.txt ]; then
    mvn dependency:build-classpath -Dmdep.outputFile=classpath.txt
fi

CLASSPATH="$PWD/target/classes/:`cat classpath.txt`"
CODEBASE="${SOURCES[@]/#/file://$PWD/}"

echo "$CLASSPATH"
echo "$CODEBASE"

HOSTNAME=`hostname`

