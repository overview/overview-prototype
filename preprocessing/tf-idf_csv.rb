require 'logger'

def count_if_gte(a,b)
  if a >= b
    return 1
  else
    return 0
  end
end

# This class expects a CSV input
# One row per document, 
# the first cell should be a document identifier
# each subsequent cell contains one term. 
# TF-IDF will be returned based on the number of times the term appears in each document, relative to the total number of documents it appears in
class Tf_Idf_CSV

  def initialize
    @logger = Logger.new(STDERR)

    reset_tf_idf
    @total_number_of_docs = 0 

    @term_count_per_doc = Hash.new # [document] => { "term1" => count1, "term2" => count2 }  
    @term_freq_per_doc = Hash.new # [document] => { "term1" => frequency1, "term2" => frequency2 }  
    @doc_count_per_term = Hash.new(0) # [term] => num_of_documents_which_contain_this_term
  end

  def docs
    @term_freq_per_doc.keys
  end

  def terms 
    @doc_count_per_term.keys
  end

  def count(doc, term)
    return nil unless @term_count_per_doc[doc]
    @term_count_per_doc[doc][term]
  end  
  
  def tf(doc, term)
    return nil unless @term_freq_per_doc[doc]
    @term_freq_per_doc[doc][term]
  end  
  
  def idf(term)
    @idf[term] ||= Math.log10(@total_number_of_docs / @doc_count_per_term[term]) 
  end
  
  def tf_idf(doc,term)
    return nil unless tf(doc, term)
    @tf_idf[doc][term] ||= tf(doc, term) * idf(term)
  end

  def terms_in_doc(doc)
    return @term_freq_per_doc[doc].keys
  end

  def stop_words
    @doc_count_per_term.select { |term, count| count == @total_number_of_docs }.keys
  end
  
  def docs_with_term(term)
    count = @doc_count_per_term[term]
    if count != nil
      return count
    else
      return 0
    end
  end
  
  def count_of_terms_which_occur_in_at_least_this_many_docs(thresh)
    @doc_count_per_term.inject(0) { |sum, (term,value)| sum + count_if_gte(value,thresh) }
  end
  
  def add_document(doc, terms)
    reset_tf_idf
    @total_number_of_docs += 1.0 # use float as we want divions later
    
    calculate(doc, terms)
    #@logger.debug("Added document '#{doc}'")
  end
  
  def add_csv(file_name)
    CSV.foreach(file_name) do |row|
      add_document(row[0],row[1..-1])
    end
  end

  def fast_write(csv_file_name, options = {})    
    CSV.open(csv_file_name,"w") do |f|
      f << ["doc","term","tf_idf"]
      docs.each do |doc|
        @term_freq_per_doc[doc].each do |term,freq|
          f << [doc,term,tf_idf(doc,term)] if tf(doc,term)
        end
      end
    end
  end
  
  # Save the results as CSV
  # Term, Doc1, Doc2, Doc3...
  # Eggs, 0.04535,,0.02
  def write_tf_idf(csv_file_name, options = {})
    decimal_places = options[:decimal_places] || 20
        
    CSV.open(csv_file_name,"w") do |f|
      f << ["terms", docs].flatten
      terms.each do |term|
        row = [term]
        docs.each do |doc|
          value = tf_idf(doc,term) ? ("%.#{decimal_places}f" % tf_idf(doc,term))  : nil
          value = nil if value.to_s =~ /^0.0+$/
          row << value
        end
        f << row
      end
    end
  end

  def write_tf(csv_file_name, options = {})
    decimal_places = options[:decimal_places] || 20
        
    CSV.open(csv_file_name,"w") do |f|
      f << ["terms", docs].flatten
      terms.each do |term|
        row = [term]
        docs.each do |doc|
          value = tf(doc,term) ? ("%.#{decimal_places}f" % tf(doc,term))  : nil
          value = nil if value.to_s =~ /^0.0+$/
          row << value
        end
        f << row
        # @logger.debug(row)
      end
    end
  end


  private
  

  def reset_tf_idf
    @idf = {}
    @tf_idf = Hash.new { |hash, key| hash[key] = {} }
    #@logger.debug("Reset tf-idf")
  end

  def calculate(doc, terms)
    term_size = terms.size.to_f
    term_count = Hash.new(0)
    term_freq = Hash.new
    
    # Count the number of times each term appears in this document
    terms.each do |term|
      term_count[term] += 1
    end

    # Normalize the count to find term frequency. Divide count by total number of terms in document
    term_count.each do |term, count|
      term_freq[term] = count / term_size
      @doc_count_per_term[term] += 1
    end

    @term_count_per_doc[doc] =  term_count
    @term_freq_per_doc[doc] =  term_freq
  end

end