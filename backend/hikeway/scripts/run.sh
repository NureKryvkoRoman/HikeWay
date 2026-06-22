#!/bin/env bash
cd ..
mvn package
cd target/
jar -xf *.war
cd WEB_INF
java -classpath "lib/*:classes/." ua.nure.kryvko.hikeway.HikewayApplication
