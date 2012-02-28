# write out 'url' column of a CSV, no header, OR the 'text' column, if there is no URL col
# Also write out doc IDs, so that we can align with .vec files, where some documents may have been culled
# May need to generate same.
#
# Overview prototype
# Jonathan Stray, Feb 2012

require 'csv'

# cheap and effective check for HTML formatting
def IsHTML(text)
  tags = ["<p>", "<h1>", "<h2>", "<h3>", "<li>", "<a href"];
  return tags.inject(false) { |result, element| result || text.include?(element) }
end

# Usage. Can specify files (and a limit on rows) but not 
if ARGV.length < 2
  puts("USAGE make-urls.rb infile outfile")
  Process.exit
end
infile_name = ARGV[0]
outfile_name = ARGV[1]

wrote_header = false
docs_read = 0

# yes!
CSV.open(outfile_name,"w") do |f|
  CSV.foreach(infile_name, :headers=>true) do |row|  

    uid = (row['uid'] != nil) ? row['uid'] : docs_read.to_s

    if row['url'] != nil
      f << ['uid','url'] unless wrote_header
      f << [uid, row['url']]
    else
      f << ['uid','text'] unless wrote_header
      
      # if the text isn't already HTML, replace line breaks with <p>
      text = row['text']
      if !IsHTML(text)
        #grafs = text.split('\n')
        #text = grafs.inject('') { |string, graf| string += "<p>" + graf + "</p>" }
        htmltext = '';
        text.each_line { |line| htmltext += '<p>' + line + '</p>' }
        text = htmltext
      end   
      f << [uid, text]
    end

    wrote_header = true
    docs_read += 1
  end
end

