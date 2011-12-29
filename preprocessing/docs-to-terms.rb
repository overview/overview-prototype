# docs-to-terms.rb
# Reads in a CSV and converts a specific field to sparse tf-idf form
# Looks for text in "text" column, and optional unique ID in "id" column. 
# Assigns sequential UIDs if missing

require 'csv'
require 'stemmer'
require 'tf-idf_csv.rb'
require 'lex.rb'


# algorithm constants
MIN_DOCS_FOR_VALID_TERM = 3  # number of documents a term must appear in to be counted in tfidf vector

# --------------------------------------- main ----------------------------------------

# Usage. Can specify files (and a limit on rows) but not 
if ARGV.length < 3
  puts("USAGE: docs-to-terms infile bigrams outfile [max rows]")
  Process.exit
end
infile_name = ARGV[0]
bigramsfile_name = ARGV[1]
outfile_name = ARGV[2]
puts "Converting documents to vector space representation..."

docs_read = 0
unique_docs_read = 0
docs_with_terms = 0
docs_with_non_culled_terms = 0

# load bigrams
lexer = Lexer.new
lexer.load_bigrams(bigramsfile_name)
puts "Loaded " + lexer.bigram_count.to_s + " bigrams"
lexer.load_stopwords(File.dirname(__FILE__) + "/stopwords.csv")

# Read each row of the input file, parse the text field into terms, add the doc to the TFIDF database
tfidf = Tf_Idf_CSV.new
csv_out = CSV.open(ARGV[1],"w")

CSV.foreach(ARGV[0], :headers=>true) do |row|
  
  text = row['text']
  if row.include?('uid')
    uid = row['uid']
  else
    uid = docs_read.to_s
  end
  docs_read+=1
  
  # process only if the uid hasn't been seen before
  if (uid != nil)
    uid.downcase!
    uid.strip!
  end  
  if tfidf.docs.include?(uid)
    puts("Warning: UID " + uid.to_s + " is repeated, skipping.")
  elsif (uid == "") || (uid == nil)
    puts("Warning: document number " + docs_read.to_s + " is missing a UID") 
  else
    unique_docs_read += 1
  
    # split and clean the text into a term list
    terms = lexer.make_terms(text)
  
    # Calculate term frequency / update doc frequency. Use doc unique ID as a key
    if terms.length > 0 
      tfidf.add_document(uid, terms)
      docs_with_terms+=1
    end
  end
  
  # break if we hit doc reading limit
  if ARGV.length > 3 && docs_read >= ARGV[3].to_i
    break
  end

end

# ----------------------- write out tfidf results --------------------

# CSV output
CSV.open(outfile_name,"w") do |f|
  
  # write header
  f << ["doc","term","tf_idf"]

  # loop over docs, then terms for each doc
  tfidf.docs.each do |doc|
    
    non_culled_terms_in_this_doc = 0
    
    tfidf.terms_in_doc(doc).each do |term|      
      
      # write only those terms which appear in at least e.g. 3 docs (on the advice of John Stasko)
      if tfidf.docs_with_term(term) >= MIN_DOCS_FOR_VALID_TERM    
        # write doc id, doc type, term name, then tfidf for that term in that doc
        f << [doc, term, tfidf.tf_idf(doc,term) ] if tfidf.tf(doc,term)
        non_culled_terms_in_this_doc +=1
      end
      
    end
    
    # count this document if we wrote at least one term for it
    if non_culled_terms_in_this_doc > 0
      docs_with_non_culled_terms += 1
    end
    
  end
  
end

puts(docs_read.to_s + " documents read")
puts(unique_docs_read.to_s + " uniquely keyed documents read")
#puts(docs_with_terms.to_s + " documents with non-empty terms")
puts(tfidf.terms.length.to_s + " input terms")
puts(tfidf.count_of_terms_which_occur_in_at_least_this_many_docs(MIN_DOCS_FOR_VALID_TERM).to_s + " terms not culled")
puts(docs_with_non_culled_terms.to_s + " documents with terms that were not culled")




