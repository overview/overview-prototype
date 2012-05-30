// Stub file to make sure NativeInterface.open() is called before AWT is initialized. This main was originally in snappy.ui.Snappy
// but the DJ Native components (used for integrated web browser) gave the following error:
//
// On Mac, "NativeInterface.initialize()"/"NativeInterface.open()" should not be called after AWT static initializers have run, 
// otherwise there can be all sorts of side effects (non-functional modal dialogs, etc.). Generally, the problem is when the 
// "main(String[])" method is located inside an AWT component subclass and the fix is to move that main method to a standalone 
// class. The problematic class here is "snappy.ui.Snappy"
//
// Jonathan Stray March 2012

package snappy.ui;

import snappy.ui.Snappy;

import chrriis.dj.nativeswing.swtimpl.NativeInterface;

public class mainHolder {
	
	public static void main( final String[] args ) {

		InteractionLogger.openLog("overview-log.csv");
		NativeInterface.open();  

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
        	Snappy snappy = null;
            public void run() {
            	
            	snappy = new Snappy(args);
//            	snappy.setSize(new Dimension(1024,700));
            }
        } );
		
        NativeInterface.runEventPump(); 
        
		InteractionLogger.log("SHUTDOWN");
        InteractionLogger.closeLog();
	}
}
