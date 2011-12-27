#!/bin/sh
BASEDIR=`dirname $0`
CLASSPATH=$BASEDIR/visualization/lib

java -classpath $CLASSPATH -Xmx1500m -cp $CLASSPATH/.:$CLASSPATH/snappy.jar:$CLASSPATH/cobra.jar:$CLASSPATH/js.jar:$CLASSPATH/core.jar snappy.ui.Snappy -V

