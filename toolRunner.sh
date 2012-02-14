#!/usr/bin/env bash

if [ $# -eq 0 ]
then
  echo "USAGE: toolRunner.sh APPNAME [ARGS]";
  exit;
fi

if [ "$1" = "list" ]
then
  find src/edu/unc/genomics/**/*.java -exec basename -s .java {} \;
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -Dlog4j.configuration=log4j.properties -cp $DIR:$DIR/build:$DIR/dist/*:$DIR/lib/* edu.unc.genomics."$@"