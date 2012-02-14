#!/usr/bin/env bash

if [ $# -eq 0 ]
then
  echo "USAGE: galaxyToolRunner.sh APPNAME [ARGS]";
  exit;
fi

if [ "$1" = "list" ]
then
  find src/edu/unc/genomics/**/*.java -exec basename -s .java {} \;
fi

java -Dlog4j.configuration=log4j.properties -cp .:../build:../lib/* edu.unc.genomics."$@"