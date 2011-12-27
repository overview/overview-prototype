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

//import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;

import snappy.data.SortedDistanceMatrix;
import snappy.graph.TopoTreeNode;

import org.lobobrowser.html.gui.*;
import org.lobobrowser.html.test.SimpleHtmlRendererContext;

/*
 * Receives double click events and responds to them by making a system call to open up the 
 * corresponding URL. 
 */
public class HtmlDispatch extends MouseAdapter implements KeyListener,
		ListSelectionListener {

	private HtmlPanel m_html_panel = null;
	private SimpleHtmlRendererContext m_context = null;
	// private JWebBrowser m_browser = null;
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
	public HtmlDispatch(JList list, String exec_prefix,
			ArrayList<String> item_urls, HtmlPanel panel,
			SimpleHtmlRendererContext context) {

		m_html_panel = panel;
		m_context = context;

		// m_browser = browser;
		m_list = list;
		m_list.addMouseListener(this);
		m_list.addKeyListener(this);
		m_list.addListSelectionListener(this);
		m_runtime = Runtime.getRuntime();
		if (m_exec_prefix != null)
			m_exec_prefix = exec_prefix;
		m_item_urls = item_urls;
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

	public static ArrayList<String> loadHTMLList(String listname) {

		ArrayList<String> returnList = new ArrayList<String>();

		try {

			BufferedReader breader = new BufferedReader(
					new FileReader(listname));

			String lineStr = breader.readLine();
			while (lineStr != null && lineStr.length() > 0) {

				returnList.add(lineStr);
				lineStr = breader.readLine();
			}

			breader.close();
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

			if (m_list.getSelectedIndex() != -1) {

				String item_url = m_item_urls.get(((Integer) m_list.getModel()
						.getElementAt(m_list.getSelectedIndex())).intValue());
				try {

					// open the url in the shell

					System.out.println("Executing : " + m_exec_prefix
							+ item_url + ".html ");
					m_runtime.exec("open " + m_exec_prefix + item_url
							+ ".html ");
				} catch (IOException e1) {

					e1.printStackTrace();
				}
			}
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		
		if( e.getValueIsAdjusting() )
			return;
		
		if( m_list.getSelectedIndex() > -1 ) {
			
			Integer itemVal = (Integer) m_list.getModel().getElementAt(m_list.getSelectedIndex());
    		String item_url = m_item_urls.get(itemVal.intValue());
    		try {
    		
    			String line = "";
    			BufferedReader r = new BufferedReader(new FileReader(m_exec_prefix + item_url + ".html"));
    			line = r.readLine();
    			String html_string = "";
    			while( line != null ) {
    				html_string += line;
    				line = r.readLine();
    			}
    			m_html_panel.setHtml(html_string, "", m_context);
//        			m_browser.setHTMLContent(html_string);

    			r.close();
//        			// open the url in the shell
//        			
//        			System.out.println("Executing : " + m_exec_prefix + item_url + ".html " );
//					m_runtime.exec(m_exec_prefix + item_url + ".html ");
			} catch (IOException e1) {

				e1.printStackTrace();
			}
		}
		
	}
}
