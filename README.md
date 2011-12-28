# Overview document set visualization system -- prototype 

Welcome! This is a prototype, pre-alpha distribution of the Overview open source document set visualization and exploration tool. For information on the project, including example uses, see overview.ap.org

The quick version:

* Start with your documents in foo.csv, one document per row, with the document text in the column called "text" and a URL for that document in "url"
* Process that CSV using 'preprocess.sh foo'. This will chug away for a while and create many data files.
* Fire up the visualizer using 'overview.sh foo' 

The preprocessing is in Ruby, the visualization is in Java, so you will need both of those environments installed. The scripts are Unix shell scripts, so they won't run on Windows at the moment. Sorry.

# copyleft

GPLv3

# contact

need help? ask!

https://twitter.com/overviewproject

