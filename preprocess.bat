@echo off
set BASEDIR=%~dp0
set RUBYDIR=%BASEDIR%preprocessing

echo Running from %RUBYDIR%

REM Look for commonly occurring co-locations, and extract the top candidates. 
REM TODO: threshold for acceptance is hard-coded 
ruby -I %RUBYDIR% %RUBYDIR%\find-bigrams.rb %1.csv %1-bigrams.csv

REM Turn each document into a list of terms, weighted by TF-IDF. Bigrams are treated as a single term.
ruby -I %RUBYDIR% %RUBYDIR%\docs-to-terms.rb %1.csv %1-bigrams.csv %1-terms.csv

REM Convert to the vector representation used by Snappy. Normalizes the TF-IDF vector for each doc.
REM Also writes out a list of indices for each term. These are used later to create feature names for Snappy.
ruby -I %RUBYDIR% %RUBYDIR%\terms-to-vec.rb %1-terms.csv %1-terms.vec %1-termlist.csv

REM Process termlist to create feature names for Snappy
ruby -I %RUBYDIR% %RUBYDIR%\make-featurenames.rb %1-termlist.csv %1-featurenames.csv

REM Finally, extract URLs for Snappy
ruby -I %RUBYDIR% %RUBYDIR%\make-urls.rb %1.csv %1-urls.csv
 
