require 'set'
require 'csv'
require 'stemmer'

class String
  include Stemmable
end

# Overview prototype lexical analysis
# Terms text to a term list, including various cleanups
# This version implements bigram detection (from an externally provided vocabulary) and Porter stemming
class Lexer
  attr_accessor :bigrams
  attr_accessor :stopwords
  attr_accessor :stem_terms
  
  def initialize
   @bigrams = Set.new
   @stopwords = Set.new
   @stem_terms = false     # stem by default
  end
  
  # Load a list of birgrams from a CSV. Ignore the likelihood values -- use everything in the file
  # The bigrams are assumed lowercase and cleaned of undesirable punctuation
  # Turns spaces into underscores, to make bigrams a little clearer when many terms are printed 
  # Obviously, if you're trying to FIND bigrams, don't call this
  def load_bigrams(filename)
    #puts filename
    CSV.foreach(filename, :headers=>true) do |row|
      bigram = row['bigram']
      bigram.gsub!(' ','_')
      @bigrams << bigram
    end
  end
  
  def bigram_count
    return @bigrams.length.to_i
  end
  
  # Load a list of stopwords, which are removed from the token stream by make_terms
  def load_stopwords(filename)
    CSV.foreach(filename) do |row|
      @stopwords << row[0]
    end
    puts "loaded " + stopwords.length.to_s + " stopwords."
  end
  
  # Remove certain types of terms. Applied only to unigrams. At the moment:
  #  - we insist that terms are at least three chars
  #  - discard if starts with a digit and 40% or more of the characters are digits
  #  - discard stopwords
  def term_acceptable(t)
    if t.length < 3
      return false                                    # too short, unacceptable
    elsif @stopwords.include?(t)
      return false                                    # stopword, unacceptable
    else
      if (t =~ /\d/) != 0
        return true                                   # doesn't start with digit, acceptable
      else
        return  10*t.scan(/\d/).length < 4*t.length   # < 40% digits, acceptable
      end
    end
  end
    
  # Given a string, returns a list of terms
  # Find bigrams greedily and do not output compontent terms, but allow overlaps. 
  # E.g. if [1 2] and [2 3] are bigrams, then [1 2 3] => [1 2],[2 3] 
  # Stem unigrams
  
  def make_terms(text)
    if !text
      return []
    end
    
    text.downcase!
    text.gsub!(/<[^>]*>/, '') # strip HTML, I'm not sure how/why HTML ended up in the text anyway
    text.tr!('"()[]:,',' ')   # turn certain punctation into spaces

    text.gsub!("&amp;","")  # data has some HTML apostrophe mess, clean it up
    text.gsub!("amp;","")
    text.gsub!("apos;","'")
    text.gsub!("''","'")    # double '' to single '

    terms = text.split(' ')
    terms.map!{ |t| t.sub(/^[^a-z0-9]+/,'').sub(/[^a-z0-9]+$/,'') } # remove leading/trailing punctuation
    
    # Now scan through the term list and spit out ungrams, bigrams
    termsout = []
    
    while t = terms.shift
    
      # look for a bigram starting with t
      if terms.length && terms[0] != nil
        t2 = terms[0]
        bigram = t + "_" + t2
        if @bigrams.include?(bigram)
          termsout << bigram
          #puts bigram
          next
        end
      end
      
      # no bigram here, stem the individual term, output if it's "acceptable"
      if @stem_terms 
        t = t.stem
      end
      if term_acceptable(t)
        termsout << t
      end
      
    end
    
    return termsout
  end
  
end