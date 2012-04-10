# Overview document set visualization system -- prototype 

Welcome! This is a prototype, pre-alpha distribution of the Overview open source document set visualization and exploration tool. For installation instructions, see http://overview.ap.org/blog/2012/02/getting-started-with-the-overview-prototype/

The quick version:

* You probabaly want to start with the sample files, available from https://github.com/overview/overview-sample-files
* The prototype runs on Windows and Mac, and Linux too if you replace visualization/lib/swt.jar with the appropriate version for your operating system.
* You will need Ruby and Java installed.
* Start with your documents in foo.csv, one document per row, with the document text in the column called "text" and a URL for that document in "url"
* Process that CSV using 'preprocess.sh foo'. This will chug away for a while and create many data files.
* Fire up the visualizer using 'overview.sh foo' 

# copyleft

GPLv3

# contact

need help? ask!

https://twitter.com/overviewproject

