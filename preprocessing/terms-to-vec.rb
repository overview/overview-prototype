# terms-to-vec.rb -- War Logs visualization project
# Reads a term file (in the format produced by docs-to-terms) and produces a sparse VEC file for use with Glimmer
# Each document vector is reconstructred from the input, sorted, normalized, and written out

# Jonathan Stray, December 2010 - December 2011

if RUBY_VERSION < "1.9"
  require "rubygems"
  require "faster_csv"
  CSV = FCSV
else
  require "csv"
end

# Write document vector
# vector is a hash of term ID -> tfidf pairs, these need to be sorted, normalized, and formatted to file
@docs_written = 0

def write_doc(id, vector, file)
  if vector.length > 0        # this check is important for handling several boundary cases (like first call in loop below)
    file << id.to_s + " "     # start by writing ID
    
    # compute vector length
    length = Math.sqrt(vector.inject(0) { | sumsq, (term_id,tf_idf) | sumsq += tf_idf*tf_idf })

    # write out each element, normalized
    vector.sort.map do |term_id, tfidf|
      file << "(" << term_id.to_s << "," << (tfidf/length).to_s << ") "
    end

    file << "\n"  
    @docs_written += 1
  end
end

def write_termlist_file(term_to_id, file)
  terms = term_to_id.keys.sort
  terms.each do |term|
    file.puts(term_to_id[term].to_s + "," + term)
  end
end

# -------------------- Open files, etc.----------------- 

minargs = 3
if ARGV.length < minargs
  puts("USAGE: terms-to-vec infile vec_outfile termlist_outfile [max rows]")
  Process.exit
end
infile_name = ARGV[0]
vec_outfile = File.open(ARGV[1], "w") # open output file
termlist_outfile = File.open(ARGV[2], "w")
puts "Creating vector and feature name files..."

# -------------------- main loop -----------------------------
num_terms = 0
term_to_id = Hash.new
id_to_term = Array.new
cur_doc_vector = Hash.new
last_doc_id = nil
docs_encountered = 0

# Each row is a term in a particular document. All terms for each document appear contiguously.
CSV.foreach(infile_name, :headers=>true) do |row|
  doc_id = row['doc'];
  term = row['term']
  tfidf = row['tf_idf'].to_f

  # Found new doc?
  if doc_id != last_doc_id
    
    # write previous document out
    if last_doc_id != nil
      write_doc(last_doc_id, cur_doc_vector,vec_outfile)
    end
    cur_doc_vector.clear
    last_doc_id = doc_id

    # break out of the loop if we've now seen as many docs as we were asked to read
    if ARGV.length > minargs && docs_encountered == ARGV[minargs].to_i
      break
    end
    docs_encountered+=1
  end
    
  # if we haven't encountered this term before, assign it an ID
  # also add it to the term array
  if !term_to_id.key?(term)
    num_terms+=1
    term_to_id[term] = num_terms
    id_to_term << term
    term_id = num_terms
  else
    term_id = term_to_id[term]
  end
    
  # add this term to the (sparse) document vector
  cur_doc_vector[term_id] = tfidf
  
end

# Write final files
write_doc(last_doc_id, cur_doc_vector, vec_outfile)
write_termlist_file(term_to_id, termlist_outfile)

puts(docs_encountered.to_s + " documents encountered");
puts(@docs_written.to_s + " documents written")
puts(num_terms.to_s + " terms")
