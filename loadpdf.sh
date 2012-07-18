#!/bin/sh

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

# do bundle install to download required gemfiles, if not already done
if [ ! -f $RUBYDIR/Gemfile.lock ]; then
	pushd $RUBYDIR
	bundle install
	popd
fi

# find pdf files in a directory, extract the text, convert to CSV
ruby -I $RUBYDIR $RUBYDIR/docloader.rb $1 -o $2.csv -r

# preprocess those suckers
#$BASEDIR/preprocess.sh $2
