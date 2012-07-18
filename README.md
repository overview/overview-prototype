# Overview document set visualization system -- prototype 

Welcome! This is a prototype, pre-alpha distribution of the Overview open source document set visualization and exploration tool. *We in the process of rewriting this entire system as a web application, no installation needed.* Until then...

# Getting started
See http://overview.ap.org/blog/2012/02/getting-started-with-the-overview-prototype/

The quick version:

* You will need Ruby and Java installed.
* Overview requires the full text of all input documents in a single CSV file, ```myfile.csv``` (see format below)
* There are sample document set CSV files available from https://github.com/overview/overview-sample-files
* First, preprocess the CSV file: ```preprocess.sh myfile```
* Then fire up the visualization: ```visualization.sh myfile```

# For Mac, Windows, Linux
* Mac: should just work
* Windows: use the batch files ```something.bat``` instead of the shell scripts ```something.sh```
* Linux: replace visualization/lib/swt.jar with the appropriate version for your operating system.

# Loading up a document set
Again, see http://overview.ap.org/blog/2012/02/getting-started-with-the-overview-prototype/

* If you have a folder full of .pdf or .txt files, use the ```loadpdf.sh``` script
* If you want to upload that folder to DocumentCloud first, use the ```dcupload.sh`` script
* If you can convert your documents to the .csv format below, you can use Overview on just about anything

# CSV file format
Overview takes a csv of the document text as input, one document per row. The simplest possible format that Overview will read has exactly one column named "text":

    text
    this is the content of document the first
    and here is the text of document the second
    etc.
    .
    .
    . 
This will work, but if you later add documents to this file, your saved tags will break, because the tags are based on row numbers if you don't have a "uid" field like this:

    uid,text
    UNIQUEID_AAA, this is the content of document the first
    UNIQUEID_BBB,and here is the text of document the second
    etc.
    .
    .
    .

The uid field can be any unique identifier, such as a hash of the document text. Finally, if you want Overview to display the document in its embedded browser instead of just showing the text, you can add a URL field.

    uid,text,url
    UNIQUEID_AAA, this is the content of document the first,http://docs.com/AAA
    UNIQUEID_BBB,and here is the text of document the second,http://docs.com/BBB
    etc.
    .
    .
    .
Over view does not do any sort of web scraping with this URL, it just uses it to display the document.

The "text" field for each document has to be quoted and escaped according to the normal CSV rules if the document text runs more than one line or has commas in it. HTML text is fine, because Overview simply strips all tags before processing. There is no hard upper limit on the number of documents, but the current UI gets a bit bogged down at about the 10,000 to 20,000 range.

# copyleft

GPLv3

# contact

need help? ask!

http://overview.ap.org/get-involved/
https://twitter.com/overviewproject

