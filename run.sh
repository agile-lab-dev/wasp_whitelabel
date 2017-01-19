#!/usr/bin/env bash

# give up if anything goes wrong
set -e

# cd into the project root
SCRIPT_DIR=`dirname $0`
cd $SCRIPT_DIR

# run stage
sbt stage

# run wasp
./target/universal/stage/bin/wasp-template -Dconfig.file=conf/wasp.conf
