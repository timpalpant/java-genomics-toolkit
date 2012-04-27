#!/usr/bin/env bash

if [ $# -eq 0 ]
then
  echo "USAGE: galaxyToolRunner.sh APPNAME [ARGS]";
  exit;
fi

# Verify that the user has Java 7 installed
# Otherwise there will be an obscure UnsupportedClassVersion error
VER=`java -version 2>&1 | grep "java version" | awk '{print $3}' | tr -d \" | awk '{split($0, array, ".")} END{print array[2]}'`
if [[ $VER < 7 ]]; then
    echo "This tool requires Java >= 7. You have Java $VER installed."
    echo "Visit http://www.oracle.com/technetwork/java/javase/downloads/index.html"
    java -version
    exit
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -Dlog4j.configuration=log4j.properties -cp $DIR:$DIR/../build:$DIR/../dist/*:$DIR/../lib/* edu.unc.genomics."$@"