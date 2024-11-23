#!/bin/bash

export ZOOBINDIR=~/apache-zookeeper-3.8.4-bin/bin

if [[ -z "$ZOOBINDIR" ]]
then
	echo "Error!! ZOOBINDIR is not set" 1>&2
	exit 1
fi

. $ZOOBINDIR/zkEnv.sh

javac -cp $CLASSPATH:../task:.: DistProcess.java
