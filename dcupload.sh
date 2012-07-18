#!/bin/sh

BASEDIR=`dirname $0`
RUBYDIR=$BASEDIR/docloader

if [ $# -ne 4 ]
then
    echo "USAGE:"
    echo "  dcupload.sh <directory with documents> <documentcloud user name> <documentcloud password> <name>"
    echo " "
    echo "After upload completes, do:"
    echo "  preprocess.sh <name>"
    echo "  overview.sh <name>"
    exit 
fi

# do bundle install to download required gemfiles, if not already done
if [ ! -f $RUBYDIR/Gemfile.lock ]; then
	pushd $RUBYDIR
	bundle install
	popd
fi

# find pdf and txt files in a directory, extract the text, convert to CSV
ruby -I $RUBYDIR $RUBYDIR/docloader.rb -l -r $1 -u $2 -p $3 -o $4.csv 
