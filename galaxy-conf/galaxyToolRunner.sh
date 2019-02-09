#!/usr/bin/env bash

if [ $# -eq 0 ]
then
  echo "USAGE: galaxyToolRunner.sh APPNAME [ARGS]";
  exit;
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -Dlog4j.configuration=log4j.properties -cp $DIR:$DIR/../build:$DIR/../dist/*:$DIR/../lib/* edu.unc.genomics."$@"
