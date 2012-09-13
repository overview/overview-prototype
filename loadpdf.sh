#!/bin/bash

BASEDIR=`dirname $0`
RUBYDIR=$BASEDIR/docloader

if [ $# -ne 2 ]
then
    echo "USAGE:"
    echo "  loadpdf.sh <directory with documents> <name>"
    echo " "
    echo "After processing completes, do:"
    echo "  overview.sh <name>"
    exit 
fi

# install a gem if not already there
function install_gem {
	count=`gem list | grep $1 | wc -l`
	if [ $count -ne 1 ]; then
		gem install $1
	fi
}

install_gem json_pure
install_gem rest-client
install_gem unicode_utils

# find pdf files in a directory, extract the text, convert to CSV
ruby -I $RUBYDIR $RUBYDIR/docloader.rb $1 -o $2.csv -r

# preprocess those suckers
$BASEDIR/preprocess.sh $2
