#!/bin/sh

java -Djava.changelater.server.codebase=file:/changelater/ -Djava.changelater.server.hostname=localhost -Djava.security.policy=agent.policy changelater.Agent
