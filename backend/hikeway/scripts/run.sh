#!/bin/env bash
[[ $PWD =~ scripts$ ]] && cd ..
mvn package && \
cd target/ && \
jar -xf *.war && \
cd WEB-INF/ && \
java -classpath "lib/*:classes/." ua.nure.kryvko.hikeway.HikewayApplication
