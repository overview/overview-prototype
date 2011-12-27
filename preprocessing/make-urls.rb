# write out 'url' column of a CSV, no header

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
    f << [row['url']]
  end
end

