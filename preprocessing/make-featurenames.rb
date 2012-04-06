# simple little file that sorts on first col, then outputs only second

if RUBY_VERSION < "1.9"
  require "rubygems"
  require "faster_csv"
  CSV = FCSV
else
  require "csv"
end

# Usage. Can specify files (and a limit on rows) but not 
if ARGV.length < 2
  puts("USAGE make-featurenames.rb infile outfile")
  Process.exit
end
infile_name = ARGV[0]
outfile_name = ARGV[1]

# read
features = []
CSV.foreach(infile_name) do |row|  
  features.push(row)
end

# sort
features.sort! { |x,y| x[0].to_i <=> y[0].to_i }

# write
CSV.open(outfile_name,"w") do |f|
  features.each do |row|  
    f << [row[1]]
  end
end




