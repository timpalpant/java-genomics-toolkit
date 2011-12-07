#!/usr/bin/env bash

if [ $# -eq 0 ]
then
  echo "USAGE: toolRunner.sh APPNAME [ARGS]";
  exit;
fi

java -Dlog4j.configuration=log4j.properties -cp .:build:lib/* edu.unc.genomics."$@"