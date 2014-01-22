#!/usr/bin/env bash

# java 1.7 build
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.7.0_17.jdk/Contents/Home
if [ -z "$1" ]
then
    gradle clean test
    mvn clean install
else
    mvn clean $1
fi

# java 1.6 build
export JAVA_HOME=/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home
if [ -z "$1" ]
then
    mvn clean install
else
    mvn clean $1
fi