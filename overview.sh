#!/bin/sh
BASEDIR=`dirname $0`
CLASSPATH=$BASEDIR/visualization/lib

java -classpath $CLASSPATH -Xmx6000m -cp $CLASSPATH/.:$CLASSPATH/snappy.jar:$CLASSPATH/DJNativeSwing-SWT.jar:$CLASSPATH/DJNativeSwing.jar:$CLASSPATH/js.jar:$CLASSPATH/core.jar:$CLASSPATH/swt.jar:$CLASSPATH/opencsv-2.3.jar snappy.ui.mainHolder -N $1-terms.vec -Z $1-featurenames.csv -U $1-urls.csv 
