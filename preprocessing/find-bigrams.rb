# find-bigrams.rb
# Reads in the text of a specific field of a CSV, generate unigram and bigram frequency tables,
# then identify collocations (common bigrams) using log-likelihood method
# Outputs a sorted list of bigrams with their liklihood ratios

# Lexical processing:
# - lowercase
# - split on spaces
# - strip punctuation

if RUBY_VERSION < "1.9"
  require "rubygems"
  require "faster_csv"
  CSV = FCSV
else
  require "csv"
end

require 'lex.rb'

# algorithm constants
MIN_EXAMPLES_FOR_VALID_BIGRAM = 5     # number of times we must see a bigram before ranking it as a collocation
MIN_LIKELIHOOD_FOR_VALID_BIGRAM = 20  # must be this many times more likely than chance for these words to be together

# ------------------Likelihood ratio collocation detection -------------- 
# see Foundations of Statistical Natural Language Processing, Manning and Schütze, Ch. 5
# This is how we decide whether two words occur as a bigram statistically more often than their individual 
# frequencies would suggest 

# computes log(x**k * (1-x)**(n-k)), but rewrite this for better numerical stablilty 
# (quite necessary, x**k is often numerically 0 which incorrectly gives -Infinity)
def LogL(k, n, x)
  if x==0 || x==1
    return -Math.log(0) # yeah, -Inf, so what?
  else 
	  return k*Math.log(x) + (n-k)*Math.log(1-x)
  end
end

# How much more likely is it that this is a bigram than a random occurence?
# Takes counts of first and second words, count of bigram, and total sample size

def bigram_likelihood(count1, count2, count12, term_count)
	p = count2/term_count.to_f
	p1 = count12/count1.to_f
	p2 = (count2-count12)/(term_count-count1).to_f
	return LogL(count12, count1, p) + LogL(count2-count12, term_count-count1, p) - LogL(count12, count1, p1) - LogL(count2-count12, term_count-count1, p2)
end


# --------------------------------------- main ----------------------------------------

# Usage. Can specify files (and a limit on rows) but not 
if ARGV.length < 2
  puts("USAGE find-bigrams.rb infile outfile [max rows]")
  Process.exit
end
infile_name = ARGV[0]
outfile_name = ARGV[1]
puts "Finding common bigrams..."

# ----------------------- build frequency tables --------------------

# fire up the lexer and fill it with stopwords, turn off stemming
lexer = Lexer.new
lang = lexer.detect_csv_language(infile_name)
lexer.load_stopwords(lang)
lexer.stem_terms = false

# Use hashes to store unigram and bigram counts. Default to 0 on creation
unigrams = Hash.new { |hash,key| hash[key] = 0 }
bigrams = Hash.new { |hash,key| hash[key] = 0 }
total_terms = 0

# Read each row of the input file, parse the text field into unigrams and bigrams
docs_read = 0
csv_out = CSV.open(outfile_name,"w")

CSV.foreach(infile_name, :headers=>true) do |row|
  
  text = row['text']
  docs_read+=1

  # split and clean the text into a term list
  terms = lexer.make_terms(text, lang)
  total_terms += terms.length
  
  # count unigrams and bigrams
  while terms.length > 0    
    unigrams[terms[0]] += 1
    
    if terms.length >= 2
      bigrams[ [terms[0],terms[1]] ] += 1
    end
    
    terms.shift
  end
  
  
  # break if we hit doc reading limit
  if ARGV.length > 2 && docs_read >= ARGV[2].to_i
    break
  end
  
end

puts(docs_read.to_s + " documents read")
puts(unigrams.length.to_s + " unigrams, " + bigrams.length.to_s + " bigrams found.")

# ----------------------- compute log likelihood for each bigram --------------------
# to prevent drowning the data in noise from singletons, require at least 3 instances of each bigram

sorted_bigrams = []

bigrams.each do |bigram, bigram_count| 
  if bigram_count >= MIN_EXAMPLES_FOR_VALID_BIGRAM
    a_count = unigrams[bigram[0]]             # number of occurences of the single words
    b_count = unigrams[bigram[1]]
  
    likelihood = -bigram_likelihood(a_count, b_count, bigram_count, total_terms)
    
    if likelihood >= MIN_LIKELIHOOD_FOR_VALID_BIGRAM
      sorted_bigrams << [bigram, likelihood]
    end
  end
end

# sort by decreasing likelihood, then alphabetic
def sort_bigram(x,y)
  if x[1] != y[1]
    return y[1] <=> x[1]
  else
    return (x[0][0] + x[0][1]) <=> (y[0][0] + y[0][1])
  end
end

sorted_bigrams.sort! {|x,y| sort_bigram(x,y) }  # sort in decreasing order by likelihood 

puts(sorted_bigrams.length.to_s + " bigrams were kept.")

# ----------------------- sort and write bigrams to CSV  --------------------
# write bigrams separated by spaces
CSV.open(outfile_name,"w") do |f|
  
  f << ["bigram","likelihood"]

  sorted_bigrams.each do |bigram|  
    f << [bigram[0][0] + " " + bigram[0][1], bigram[1]]   # word1 word2,likelihood
  end
  
end




