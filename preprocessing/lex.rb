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

class Lexer
  attr_accessor :bigrams
  attr_accessor :stopwords
  attr_accessor :stem_terms
  
  def initialize
   @bigrams = Set.new
   @stopwords = Set.new
   @stem_terms = false     # stem by default
  end
  
  def downcase_en(s)
    s.downcase
  end

  def strippunct_en(s)
    s.tr!('"()[]:,',' ')   # turn certain punctation into spaces
    s.gsub(/[^0-9a-z\'\-\s]/, '') # remove anything not alphanum, dash, apos, space (helps with OCR junk)
  end

  def downcase_es(s)
    norm = s.downcase
#    norm.tr!("ÁÉÍÓÚ", "áéíóú")
    norm.tr!("\u00c1\u00c9\u00cd\u00d3\u00da", "\u00e1\u00e9\u00ed\u00f3\u00fa")
#    norm.tr!('ÄËÏÖÜ', 'äëïöü')
    norm.tr!("\u00c4\u00cb\u00cf\u00d6\u00dc", "\u00e4\u00eb\u00ef\u00f6\u00fc")
#    norm.tr!('Ñ','ñ')
    norm.tr!("\u00d1","\u00f1")
    norm
  end

  def strippunct_es(s)
    s.tr!('"()[]:,',' ')   # turn certain punctation into spaces
    s.gsub(/[^0-9a-z\u00e1\u00e9\u00ed\u00f3\u00fa\u00e4\u00eb\u00ef\u00f6\u00fc\u00f1\'\-\s]/, '') # remove anything not alphanum, dash, apos, space (helps with OCR junk)
  end

  def downcase_l(s, l)
    if l == "en"
      downcase_en(s)
    else
      downcase_es(s)
    end
  end

  def strippunct_l(s, l)
    if l == "en"
      strippunct_en(s)
    else
      strippunct_es(s)
    end
  end

  def detect_language(text)
    stopwords_en = read_stopwords_file(File.dirname(__FILE__) + "/stopwords-en.csv")
    terms_en = downcase_en(text).split(' ')
    count_en = terms_en.count { |x| stopwords_en.include?(x) }

    stopwords_es = read_stopwords_file(File.dirname(__FILE__) + "/stopwords-es.csv")
    terms_es = downcase_es(text).split(' ')
    count_es = terms_es.count { |x| stopwords_es.include?(x) }

#    terms_es.each { |x| puts x + ", included=" + stopwords_es.include?(x).to_s }
    puts "English stopwords found: #{count_en}, Spanish stopwords found: #{count_es}"

    if count_en > count_es
      "en"
    else
      "es"
    end
  end

  def detect_csv_language(filename)
    text = ""

    CSV.foreach(filename, :headers=>true) do |row|
      if row['text'] != nil
        rowtext = clean_text(row['text'])
        text += " " + rowtext
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
  
  # Force the text to valid UTF-8 encoding. 
  # Assumes source is UTF-8, ignores stated encoding (which tends to be wrong on Windows)
  def clean_text(text)
    if RUBY_VERSION >= "1.9"

      # First, force to UTF-8 encoding
      if text.encoding.name != "UTF-8"  
        text = text.force_encoding('UTF-8')
      end

      # If we still don't have a valid string, re-encode
      if !text.valid_encoding?
        text = text.encode('UTF-16', invalid: :replace, undef: :replace).encode('UTF-8')
      end

    end
    text
  end

  # Given a string, returns a list of terms
  # Find bigrams greedily and do not output compontent terms, but allow overlaps. 
  # E.g. if [1 2] and [2 3] are bigrams, then [1 2 3] => [1 2],[2 3] 
  # Stem unigrams
  
  def make_terms(text, lang)
    if !text
      return []
    end
  
    text = clean_text(text)

    # Turn non-breaking spaces into spaces. This is more complex than it should be, 
    # due to Ruby version and platform character encoding differences
    # In particular Windows always seems to read as IBM437 encoding
    if RUBY_VERSION < "1.9"
      text.gsub!(/\302\240/,' ') 
    else
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