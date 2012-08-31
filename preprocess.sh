#!/bin/bash

BASEDIR=`dirname $0`
RUBYDIR=$BASEDIR/preprocessing

# install a gem if not already there
function install_gem {
	count=`gem list | grep $1 | wc -l`
	if [ $count -ne 1 ]; then
		gem install $1
	fi
}

# install fastercsv for Ruby 1.8 
count=`ruby -v | grep "ruby 1.9" | wc -l`
if [ $count -ne 1 ]; then
	install_gem fastercsv
fi

# Look for commonly occurring co-locations, and extract the top candidates. 
# TODO: threshold for acceptance is hard-coded 
ruby -I $RUBYDIR $RUBYDIR/find-bigrams.rb $1.csv $1-bigrams.csv

# Turn each document into a list of terms, weighted by TF-IDF. Bigrams are treated as a single term.
ruby -I $RUBYDIR $RUBYDIR/docs-to-terms.rb $1.csv $1-bigrams.csv $1-terms.csv

# Convert to the vector representation used by Snappy. Normalizes the TF-IDF vector for each doc.
# Also writes out a list of indices for each term. These are used later to create feature names for Snappy.
ruby -I $RUBYDIR $RUBYDIR/terms-to-vec.rb $1-terms.csv $1-terms.vec $1-termlist.csv

# Process termlist to create feature names for Overview
ruby -I $RUBYDIR $RUBYDIR/make-featurenames.rb $1-termlist.csv $1-featurenames.csv

# Finally, extract URLs/document text for the Overview doc viewer window 
ruby -I $RUBYDIR $RUBYDIR/make-urls.rb $1.csv $1-urls.csv

