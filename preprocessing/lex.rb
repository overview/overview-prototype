# encoding: utf-8

# Overview prototype lexical analysis
# Terms text to a term list, including various cleanups
# This version implements bigram detection (from an externally provided vocabulary) 

if RUBY_VERSION < "1.9"
  require "rubygems"
  require "faster_csv"
  CSV = FCSV
else
  require "csv"
end

require 'set'
require "unicode_utils/downcase"
require "unicode_utils/char_type"

 # Language processing primitives 
def downcase_en(s)
  s.downcase
end

def strippunct_en(s)
  s.tr!('"()[]:,',' ')   # turn certain punctation into spaces
  s.gsub(/[^0-9a-z\'\-\s]/, '') # remove anything not alphanum, dash, apos, space (helps with OCR junk)
end

def downcase_es(s)
  norm = s.downcase
  norm.tr!("ÁÉÍÓÚÄËÏÖÜÑ", "áéíóúäëïöüñ")
  norm
end

def strippunct_es(s)
  s.tr!('"()[]:,',' ')   # turn certain punctation into spaces
  s.gsub(/[^0-9a-záéíóúäëïöüñ\'\-\s]/, '') # remove anything not alphanum, dash, apos, space (helps with OCR junk)
end

def downcase_uni(s)
  UnicodeUtils.downcase(s)
end

def notpunct_uni(c)
  (UnicodeUtils.char_type(c) == :Letter) ||
  (UnicodeUtils.char_type(c) == :Number) ||
  c == " " || c == "\t" || c == "\n"
end

def strippunct_uni(s)
  s2 = ""
  s.each_char { |c| if notpunct_uni(c) then s2+=c end }
  s2
end

class Lexer
  attr_accessor :bigrams
  attr_accessor :stopwords
  attr_accessor :stem_terms
  
  def initialize
   @bigrams = Set.new
   @stopwords = Set.new
   @stem_terms = false     # stem by default
  end


  @@stopword_files = { "en"=>"/stopwords-en.csv",
                      "es"=>"/stopwords-es.csv",
                      "ar"=>"/stopwords-ar.csv" }

  @@downcase_fns = {  "en" => Proc.new { |s| downcase_en(s) },
                      "es" => proc { |s| downcase_es(s) },
                      "ar" => proc { |s| downcase_uni(s) },
                      "un" => proc { |s| downcase_uni(s) } }

  @@strippunct_fns = {"en" => proc { |s| strippunct_en(s) },
                      "es" => proc { |s| strippunct_es(s) },
                      "ar" => proc { |s| strippunct_uni(s) },
                      "un" => proc { |s| strippunct_uni(s) } }

  def downcase_l(s, l)
    @@downcase_fns[l].call(s)
  end

  def strippunct_l(s, l)
    @@strippunct_fns[l].call(s)
  end

  def count_stopwords(text, l)
    stopwords = read_stopwords_file(File.dirname(__FILE__) + @@stopword_files[l])
    terms = downcase_l(text, l).split(' ')
    terms.count { |x| stopwords.include?(x) }
  end

  def detect_language(text)
    bestcnt = 5
    bestlang = "un"                         # assume "un" = generic unicode if lang not clear
    @@stopword_files.each do |lang, file|
      cnt = count_stopwords(text,lang)
      if cnt > bestcnt
        bestcnt = cnt
        bestlang = lang
      end
    end

    puts "Detected language #{bestlang}, found #{bestcnt} stopwords."

    bestlang
  end

  def detect_csv_language(filename)
    text = ""

    CSV.foreach(filename, :headers=>true) do |row|
      if row['text'] != nil
        text += " " + row['text']
      end
      if text.length > 10000 
        break
      end
    end

    detect_language(text)
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
  def read_stopwords_file(filename)
    stopwords = []
    CSV.foreach(filename) do |row|
      stopwords << row[0]
    end
    stopwords
    # puts "loaded " + stopwords.length.to_s + " stopwords."
  end

  def load_stopwords(lang)
    @stopwords = read_stopwords_file(File.dirname(__FILE__) + "/stopwords-" + lang + ".csv")
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
  
  def make_terms(text, lang)
    if !text
      return []
    end
  
    # Turn non-breaking spaces into spaces. This is more complex than it should be, 
    # due to Ruby version and platform character encoding differences
    # In particular Windows always seems to read as IBM437 encoding
    if RUBY_VERSION < "1.9"
      text.gsub!(/\302\240/,' ') 
    else
      # Character encoding plan: assume UTF-8, reinterpret
      # If that doens't give us a valid string, then re-encode as UTF-8, throwing out invalid chars
      if text.encoding.name != "UTF-8"  
        cleaned = text.dup.force_encoding('UTF-8')
        if !cleaned.valid_encoding?
          text.encode!( 'UTF-8', invalid: :replace, undef: :replace )
        else
          text = cleaned
        end     
      end
      text.gsub!("\u00A0", " ") # turn non-breaking spaces (UTF-8) into spaces 
    end

    text = downcase_l(text,lang)

    # cleanups on Cable and Warlogs data
    text.gsub!("&amp;","")  # data has some HTML apostrophe mess, clean it up
    text.gsub!("amp;","")
    text.gsub!("apos;","'")
    text.gsub!("''","'")    # double '' to single '
    text.gsub!(/<[^>]*>/, '') # strip things inside HTML tags

    # allow only a small set of characters
    text.tr!('"()[]:,',' ')   # turn certain punctation into spaces
    text = strippunct_l(text, lang)  # remove anything not in the language charset (helps with OCR junk)
    text.gsub!(/\s\s*/, ' ')  # collapse runs of spaces into single spaces

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
  
      # DISABLED stemming, for easier installation (stemmer gem not req'd) js 21/2/2012
      # no bigram here, stem the individual term, output if it's "acceptable"
      #if @stem_terms 
      #  t = t.stem
      #end
      
      if term_acceptable(t)
        termsout << t
      end
      
    end
    
    return termsout
  end
  
end