#!/bin/sh
BASEDIR=`dirname $0`
CLASSPATH=$BASEDIR/visualization/lib

java -classpath $CLASSPATH -Xmx1500m -cp $CLASSPATH/.:$CLASSPATH/snappy.jar:$CLASSPATH/cobra.jar:$CLASSPATH/js.jar:$CLASSPATH/core.jar snappy.ui.Snappy -N $1-terms.vec -Z $1-featurenames.csv

#java -Xmx1500m -cp .:snappy.jar:cobra.jar:js.jar:core.jar snappy.ui.Snappy -N $1-terms.vec -Z $1-featurenames.csv  -H "" $1-urls.csv 
