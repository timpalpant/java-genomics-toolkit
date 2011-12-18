#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -Dlog4j.configuration=log4j.properties -cp "$DIR/../Resources/Java/*" edu.unc.genomics.ToolRunner