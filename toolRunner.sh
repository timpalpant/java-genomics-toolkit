#!/usr/bin/env bash

java -Dlog4j.configuration=log4j.properties -cp .:build:lib/* "$@"