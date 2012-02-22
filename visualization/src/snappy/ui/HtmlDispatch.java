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

import au.com.bytecode.opencsv.CSVReader;

/*
 * Receives double click events and responds to them by making a system call to open up the 
 * corresponding URL. 
 */
public class HtmlDispatch extends MouseAdapter implements KeyListener,
		ListSelectionListener {

    private JWebBrowser m_browser = null;
	private JList m_list = null;
	private Runtime m_runtime = null;
	private String m_exec_prefix = "open";
	private ArrayList<String> m_item_urls = null;

	/**
	 * 
	 * @param tree
	 *            The tree control from which we are mouse events
	 * @param exec_prefix
	 *            The prefix to send (Default is "open")
	 * @param item_urls
	 *            The node number indexed list of URL strings
	 */
	public HtmlDispatch(JList list, 
						ArrayList<String> item_urls, 
						JWebBrowser browser) {


		m_list = list;
		m_list.addMouseListener(this);
		m_list.addKeyListener(this);
		m_list.addListSelectionListener(this);
		m_browser = browser;
		m_item_urls = item_urls;
		m_runtime = Runtime.getRuntime();
	}


	@Override
	public void mousePressed(MouseEvent e) {

		// map the mouse x,y to tree path

		// int selRow = m_tree.getRowForLocation(e.getX(), e.getY());
		// TreePath selPath = m_tree.getPathForLocation(e.getX(), e.getY());
		//
		// if(selRow != -1) {
		//
		// if(e.getClickCount() == 2) { // respond to double-click events only
		//
		// // get the clicked tree node from the tree path
		//
		// DefaultMutableTreeNode node = (DefaultMutableTreeNode)
		// selPath.getLastPathComponent();
		//
		// // respond to double clicks for the leaf nodes
		//
		// if( node.getUserObject() instanceof Integer ) {
		//
		// // grab the URL from the list
		//
		// String item_url =
		// m_item_urls.get(((Integer)node.getUserObject()).intValue());
		// try {
		//
		// String line = "";
		// BufferedReader r = new BufferedReader(new FileReader(item_url));
		// line = r.readLine();
		// String html_string = "";
		// while( line != null ) {
		// html_string += line;
		// line = r.readLine();
		// }
		// m_browser.setHTMLContent(html_string);
		//
		// r.close();
		// // // open the url in the shell
		// //
		// // System.out.println("Executing : " + m_exec_prefix + item_url +
		// ".html " );
		// // m_runtime.exec(m_exec_prefix + item_url + ".html ");
		// } catch (IOException e1) {
		//
		// e1.printStackTrace();
		// }
		// }
		// }
		// }
	}

	public static ArrayList<String> loadURLList(String listname) {

		ArrayList<String> returnList = new ArrayList<String>();

		try {

			
			CSVReader reader = new CSVReader(new FileReader(listname));

			// iterate over reader.readNext until it returns null
			String[] line;
			
			while ((line = reader.readNext()) != null) {
				returnList.add(line[0]);
			}
			
/*			BufferedReader breader = new BufferedReader(
					new FileReader(listname));

			String lineStr = breader.readLine();
			while (lineStr != null && lineStr.length() > 0) {

				returnList.add(lineStr);
				lineStr = breader.readLine();
			}

			breader.close();*/
		} catch (Exception e) {

			returnList = null;
			e.printStackTrace();
		}

		return returnList;
	}
	

	public void keyTyped(KeyEvent e) {

	}

	/** Handle the key-pressed event from the text field. */
	public void keyPressed(KeyEvent e) {

	}

	/** Handle the key-released event from the text field. */
	public void keyReleased(KeyEvent e) {

		if (e.getKeyCode() == 10) {

			String item_url = m_item_urls.get(((Integer) m_list.getModel().getElementAt(m_list.getSelectedIndex())).intValue());
			try {

				// open the url in the shell
				System.out.println("Executing : open " + item_url);
				m_runtime.exec("open " + item_url);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		
		if( e.getValueIsAdjusting() )
			return;
		
		if( m_list.getSelectedIndex() > -1 ) {
			
			Integer itemVal = (Integer) m_list.getModel().getElementAt(m_list.getSelectedIndex());
    		String item_string = m_item_urls.get(itemVal.intValue());
    		
    		// If the string is a URL, navigate there. Otherwise just consider it straight HTML content and load it
    		if ((item_string.indexOf("http://") != -1) || (item_string.indexOf("https://") != -1)) {
    			m_browser.navigate(item_string);
    		} else {
    			// If it doesn't look like HTML, wrap in pre-tags
    			if (item_string.indexOf("<p>") == -1) {
    				item_string = "<pre>" + item_string + "</pre>";
    			}
    			m_browser.setHTMLContent(item_string);
    		}
		}		
	}
}
