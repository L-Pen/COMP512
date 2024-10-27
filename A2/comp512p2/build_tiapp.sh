#!/bin/bash

#TODO Edit this dir name to where your comp512.jar and build_tiapp.sh script are.
BASEDIR=`pwd`

if [[ ! -d $BASEDIR ]]
then
	echo "Error $BASEDIR is not a valid dir."
	exit 1
fi

if [[ ! -f $BASEDIR/comp512.jar ]]
then
	echo "Error cannot locate $BASEDIR/comp512.jar . Make sure it is present."
	exit 1
fi

if [[ ! -d $BASEDIR/comp512st ]]
then
	echo "Error cannot locate $BASEDIR/comp512st directory . Make sure it is present."
	exit 1
fi

export CLASSPATH=$BASEDIR/comp512.jar:$BASEDIR
cd $BASEDIR

javac $(find -L comp512st -name '*.java')

