#!/usr/bin/env bash

if [ $# -eq 0 ]
then
  echo "USAGE: galaxyToolRunner.sh APPNAME [ARGS]";
  exit;
fi

# Verify that the user has Java 7 installed
# Otherwise there will be an obscure UnsupportedClassVersion error
version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
if [[ "$version" < "1.7" ]]; then
    echo "Need Java 7 or greater. You have Java $version installed."
    exit
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -Dlog4j.configuration=log4j.properties -cp $DIR:$DIR/../build:$DIR/../dist/*:$DIR/../lib/* edu.unc.genomics."$@"