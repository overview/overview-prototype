# detect-language.rb
# Reads in first ten rows of CSV, detects langauge based on "text" column, outputs to console
#
# Overview prototype
# Jonathan Stray, August 2012

if RUBY_VERSION < "1.9"
  require "rubygems"
  require "faster_csv"
  CSV = FCSV
else
  require "csv"
end

require 'lex.rb'


# Usage. Can specify files (and a limit on rows) but not 
if ARGV.length < 1
  puts("USAGE: detect-language infile")
  Process.exit
end

# Read some docs
text = ""
docs_to_read = 10

CSV.foreach(ARGV[0], :headers=>true) do |row|
  text += " " + row['text']
  docs_to_read-=1
  if docs_to_read == 0 
    break
  end
end

lexer = Lexer.new
puts lexer.detect_language(text)






