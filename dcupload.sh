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

# install a gem if not already there
function install_gem {
    count=`gem list | grep $1 | wc -l`
    if [ $count -ne 1 ]; then
        gem install $1
    fi
}

install_gem json_pure
install_gem rest-client

# find pdf and txt files in a directory, extract the text, convert to CSV
ruby -I $RUBYDIR $RUBYDIR/docloader.rb -l -r $1 -u $2 -p $3 -o $4.csv 
