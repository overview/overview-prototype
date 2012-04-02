package snappy.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;

import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;

import snappy.data.SortedDistanceMatrix;
import snappy.graph.TopoTreeNode;
import snappy.data.NZData;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVParser;

/*
 * Receives double click events and responds to them by making a system call to open up the 
 * corresponding URL. 
 */
public class HtmlDispatch implements KeyListener, ListSelectionListener {

    private JWebBrowser m_browser = null;
	private JList m_list = null;
	private NZData m_doclist = null;
	private Runtime m_runtime = null;
	private HashMap<String,String> m_item_texts = null;
	private boolean m_has_urls = false;


	public HtmlDispatch(JList list, 
						NZData doclist,
						String text_filename, 
						JWebBrowser browser) {


		m_list = list;
		m_list.addKeyListener(this);
		m_list.addListSelectionListener(this);
		m_browser = browser;
		m_doclist = doclist;
		m_runtime = Runtime.getRuntime();
		
		loadText(text_filename);
	}


	// Load the "text" for each document. This may be literally the HTML text, or a url. We determine which by looking for a 'text' column in the input CSV
	// We also need a 'uid' column, to match to the documents we read in the .vec file
	private void loadText(String text_filename) {

		try {

			// Set up CSV reader with no escape character (instead of \), as Ruby CSV files and RFC 4180 don't use it, and \"" was being incorrectly read.
			CSVReader reader = new CSVReader(new FileReader(text_filename),
											 CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, CSVParser.NULL_CHARACTER);

			// load headers, find 'uid' col, and 'url' colu, or 'text' col if no URL
			String [] headers = reader.readNext();
			m_has_urls = false;
			int contentColIdx = -1, uidColIdx = -1;
			for (int i=0; i<headers.length; i++) {
				String colName = headers[i].toLowerCase();
				
				if (colName.equals("url")) {
					contentColIdx = i;
					m_has_urls= true;
				} else if (colName.equals("text")) {
					contentColIdx = i;
				} else if (colName.equals("uid")) {
					uidColIdx = i;
				}
			}
			
			// If we have neither text nor URL, we have nothing to show
			if (contentColIdx == -1) {
				System.out.println("Warning: could not find text or url column in input file " + text_filename + ", cannot display documents.");
			}
			if (uidColIdx == -1) {
				System.out.println("Warning: could not find uid column in input file " + text_filename + ", cannot display documents.");
			}
		
			// If we have both content and uid columns, load all the docs
			if ((contentColIdx != -1)  && (uidColIdx != -1)){
					
				m_item_texts = new HashMap<String,String>();
				
				// iterate over reader.readNext until it returns null
				String[] line;
				Integer lineNo = 0;
				while ((line = reader.readNext()) != null) {
					//System.out.println("Reading line " + lineNo + ", line number in file is " + line[0]);
					m_item_texts.put(line[uidColIdx], line[contentColIdx]);
					
//					m_item_texts.put(lineNo.toString(), line[contentColIdx]);
//					int idx = lineNo.intValue();
//					String vecDocID = m_doclist.getDocIDString(lineNo);
//					String readDocID = line[uidColIdx];
					
					lineNo += 1;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String selectedDocumentID() {
		Integer selectedDocIndex = (Integer) m_list.getModel().getElementAt(m_list.getSelectedIndex());		
		return m_doclist.getDocIDString(selectedDocIndex.intValue());
	}
	
	// Returns the text/url of the currently selected doc in m_list. Maps through the uid
	private String selectedDocumentContent(String docID) {
		return m_item_texts.get(docID);
	}
	
	public void keyTyped(KeyEvent e) {

	}

	/** Handle the key-pressed event from the text field. */
	public void keyPressed(KeyEvent e) {

	}

	// If we have URLs, enter = open in browser
	public void keyReleased(KeyEvent e) {

		if (m_has_urls && (e.getKeyCode() == 10)) {

			String item_url = selectedDocumentContent(selectedDocumentID());
			try {

				// open the url in the shell
				m_runtime.exec("open " + item_url);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			InteractionLogger.log("OPEN DOC IN BROWSER",item_url);
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		
		if (m_item_texts == null)
			return;					// we never succeeded in loading the document texts/url
		
		if( e.getValueIsAdjusting() )
			return;
		
		if( m_list.getSelectedIndex() > -1 ) {	
			String docID = selectedDocumentID();
    		String item_string = selectedDocumentContent(docID);
    		
    		InteractionLogger.log("VIEW DOC",docID);
    		
    		// If the string is a URL, navigate there. Otherwise just consider it straight HTML content and load it
    		if (m_has_urls) {
    			
    			// If the URL is for document cloud, create a custom embed code (get rid of the sidebar etc.) 
    			// Yes, hacky. Might only work right for the URLs in the Iraq contractor dataset
    		    Pattern p = Pattern.compile("(https?://www.documentcloud.org/documents/.+)\\.html#document/p(\\d+)");
    		    Matcher m = p.matcher(item_string);
    		    
    		    if (m.find()) {
    		    	
    		    	String embed_cod = "<!DOCTYPE html><head></head><body>" +
    		    					   "<div id=\"foo\" class=\"DV-container\"></div>" + 
    		    					   "<script src=\"http://s3.documentcloud.org/viewer/loader.js\"></script>" +
    		    					   "<script>" +
    		    					   		"DV.load('" + m.group(1) + ".js', {" + 
    		    					   					"sidebar: false," +
    		    					   					"page: " + m.group(2) + "," + 
    		    					   					"container: \"#foo\"" + 
    		    	  						"});" +
    		    	  					"</script></body>";

    		    	m_browser.setHTMLContent(embed_cod);
    		    } else {
    		    	m_browser.navigate(item_string); // not a DocumentCloud URL, just navigate as usual
    		    }
    		} else {
    			m_browser.setHTMLContent(item_string);
    		}
		}		
	}
}
