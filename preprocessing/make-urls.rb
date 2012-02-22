# write out 'url' column of a CSV, no header, OR the 'text' column, if there is no URL col
# Overview prototype
# Jonathan Stray, Feb 2012

require 'csv'

# Usage. Can specify files (and a limit on rows) but not 
if ARGV.length < 2
  puts("USAGE make-urls.rb infile outfile")
  Process.exit
end
infile_name = ARGV[0]
outfile_name = ARGV[1]

# yes!
CSV.open(outfile_name,"w") do |f|
  CSV.foreach(infile_name, :headers=>true) do |row|  
    if row['url'] != nil
      f << [row['url']]
    else
      f << [row['text']]
    end
  end
end

