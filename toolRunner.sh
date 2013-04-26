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

# Verify that the user has Java 7 installed
# Otherwise there will be an obscure UnsupportedClassVersion error
version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
if [[ "$version" < "1.7" ]]; then
    echo "Need Java 7 or greater. You have Java $version installed."
    exit
fi

# Run a tool with the passed arguments
java -Xss128m -Xmx6000m -Dlog4j.configuration=log4j.properties -cp $DIR:$DIR/build:$DIR/dist/*:$DIR/lib/* edu.unc.genomics."$@"
