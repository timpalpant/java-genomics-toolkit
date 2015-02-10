#!/usr/bin/env bash

# Help
if [ $# -eq 0 ]
then
  echo "USAGE: toolRunner.sh APPNAME [ARGS]";
  echo "To list available tools: toolRunner.sh list";
  exit
fi

# Get the root directory in case this script is being called from elsewhere
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# List the available tools (this is a little hacky)
if [ "$1" = "list" ]
then
  for d in `ls $DIR/src/edu/unc/genomics`
  do
    if [ -d $DIR/src/edu/unc/genomics/$d ]
    then
      for f in `ls $DIR/src/edu/unc/genomics/$d/*`
      do
        scriptname=`basename $f .java`
        echo $d.$scriptname
      done
    fi
  done
  exit
fi

# Run a tool with the passed arguments
java -Xmx2000m -Dlog4j.configuration=log4j.properties -cp $DIR:$DIR/build:$DIR/dist/*:$DIR/lib/* edu.unc.genomics."$@"
