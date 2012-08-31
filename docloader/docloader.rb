# Scan a directory for PDF files (possibly recursively),
# optionally upload them to a DocumentCloud account,
# create a .CSV file for use with Overview
#
# Example usage:
#    ruby docloader.rb dir-full-of-PDFs -o output.csv
#    ruby docloader.rb dir-full-of-PDFs -l -u DOCCLOUD-USERNAME -p DOCCLOUD-PASSWORD -o output.csv
#
# Requires docsplit, http://documentcloud.github.com/docsplit/ 
 
require 'rubygems'
require 'rest_client'
require 'ostruct'
require 'optparse'
require 'uri'
require 'csv'
require 'json'

# ------------------------------------------- Modules, functions ----------------------------------------
# DocumentCloud uploader, text extraction, directory recursion, file matching
  
module DC

	# Things we need to upload:
	#   Username
	#   Password
	#   path to document
	def self.upload(user, pass, path, project=nil)
		#puts "path: #{path}"
		title = File.basename(path, File.extname(path))
		attributes = {
			:file	=> File.new(path, "rb"),
			:title	=> title,
			:access	=> 'private',
			:project=> project # currently needs: 4478, should be: 4478-2012advault
			#:data	=> {}
			#:secure=> false
		}
		url = "https://" + user.gsub('@','%40') + ":" + pass + "@www.documentcloud.org/api/upload.json"
		#puts url
		json_string = RestClient.post(url, attributes)
		JSON.parse(json_string)
	end
end

# extract text from specified PDF
# We use pdftotext. On Windows, we expect it to be located where we are
def extractTextFromPDF(filename)
	if ENV['OS'] == "Windows_NT"
		pdftotextexec = File.expand_path(File.dirname(__FILE__)) + "/pdftotext.exe"
	else
		pdftotextexec = "pdftotext"
	end
	text = `"#{pdftotextexec}" "#{filename}" -`
end

# extract text from specified file
# Format dependent
def extractTextFromFile(filename)
	if File.extname(filename) == ".pdf"
		extractTextFromPDF(filename)
	elsif File.extname(filename) == ".txt"
		File.open(filename).read
	end
end

# Recursively scan a directory structure for matching files, process each one
# Execute callfn for each file in direname where matchfn returns true, recurse into dirs if recurse is true
def scanDir(dirname, matchfn, callfn, recurse)
	Dir.foreach(dirname) do |filename|
		fullfilename = dirname + "/" + filename;
		if File.directory?(fullfilename)
			if recurse && filename != "." && filename != ".."		# don't infinite loop kthx
				scanDir(fullfilename, matchfn, callfn, recurse)
			end
		elsif matchfn.call(filename)
			callfn.call(fullfilename)
		end
	end
end


# Based on file extension, is this a document file? Potentially could include all file types that docsplit can read
def matchFn(filename)
	return [".txt", ".pdf"].include? File.extname(filename)
end


# upload/extract text from a single file
# precondition: File.exists?(filename)
def processFile(filename, options)
	puts "Processing #{filename}"

	# Either upload the file to DocumentCloud and get the resulting URL, 
	# or if not uploaded 8we just make a file:// URL
	# NB: we make the UID here, using the URL if uploaded but the *relative* filename if local
	# Otherwise saved tag files would never be portable between computers
	url = nil
	digest = nil	
	if options.upload
		result = DC.upload(options.username, options.password, filename, options.project)
		url = result["canonical_url"]
		#puts url
		digest = Digest::MD5.hexdigest(url)
	else
		if File.extname(filename) != ".txt"
			url = "file://" + File.expand_path(filename)
		end
		digest = Digest::MD5.hexdigest(filename)
	end
	
	if options.overviewCSVfilename
		# extract file text, append to Overview csv
		text = extractTextFromFile(filename)
		if url != nil
			options.csv << [text, digest, url]
		else
			options.csv << [text, digest]
		end
	end
end

# ------------------------------------------- Process command-line args ----------------------------------------

options = OpenStruct.new
options.upload = false
options.overview = false

OptionParser.new do |opts|
  	opts.banner = "Usage: docloader.rb [options] directory"

  	opts.on("-l", "--upload", "Upload each document to DocumentCloud") do |v|
    	options.upload = true
  	end

  	opts.on("-u", "--username USERNAME", "DocumentCloud username") do |v|
    	options.username = v
  	end

  	opts.on("-p", "--password PASSWORD", "DocumentCloud password") do |v|
    	options.password = v
  	end

  	opts.on("-j", "--project PROJECT-ID", "DocumentCloud project ID (digits preceding project name)") do |v|
    	options.project = v
  	end
  
  	opts.on("-o", "--overview CSV-FILENAME", "Write CSV for use with Overview") do |v|
    	options.overviewCSVfilename = v
  	end

	opts.on("-r", "--recurse", "Scan directory recursively") do |v|
		options.recurse = true
	end	  
end.parse!

#puts options
#puts ARGV

unless dirname = ARGV[0]
	puts "ERROR: no directory name specified"
	exit
end
	
unless options.upload || options.overviewCSVfilename
	puts "WARNING: neither -l nor -o specified, only listing files"
end

if options.upload && !(options.password && options.username)
	puts "ERROR: you must provide a username (-u) and password (-p) to upload to DocumentCloud"
	exit
end

# ------------------------------------------- Do it! ----------------------------------------

# Open output CSV filename, if we're processing for Overview
if options.overviewCSVfilename
	options.csv = CSV.open(options.overviewCSVfilename,"w")
	options.csv << ["text", "uid", "url"]
end

# And we're ready. Iterate, possibly recursively, through directory in question
scanDir(dirname, method(:matchFn), proc { |filename| processFile(filename, options) }, options.recurse )
