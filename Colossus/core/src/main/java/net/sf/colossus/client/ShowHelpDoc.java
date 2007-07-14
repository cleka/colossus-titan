package net.sf.colossus.client;



import javax.swing.JFrame;
import javax.swing.JTextArea;


/**
 * Provides a simple JTextArea to display pointers to the Option 
 * documentation HTML files in Internet.
 * Eventually one day this should show the option documents
 * itself as an own JEditorPane, let user browse them etc...
 * 
 * @version $Id: ShowHelpDoc.java 0000 2007-07-13 00:00:00Z cleka $
 * @author Clemens Katzer
 */




public final class ShowHelpDoc 
{
    private static JFrame viewFrame;
   
    
    ShowHelpDoc (JFrame parentFrame, IOptions options)
    {
        // primitive way to avoid having more than one. How to do better?
        if ( viewFrame != null )
        {
            viewFrame.dispose();
            viewFrame = null;
        }
        
        String title = "Pointer to Options documentation";
        
        viewFrame = new JFrame(title);
            
        String text = "\n" +
             "  Help on the options is not built-in into Colossus yet, " + 
             "but available from the Colossus home page.\n\n" + 
             "  Documentation for client side options (MasterBoard menu " +
             "bar) can be found from page: \n" + 
             "     http://colossus.sourceforge.net/docs/ClientOptions.html" +
             "\n\n" + 
             "  Documentation for server side options " + 
             "(Player Selection dialog) can be found from page: \n" + 
             "     http://colossus.sourceforge.net/docs/GetPlayersOptions.html" +
             "\n\n" +
             "  Eventually one day this may become a proper help " + 
             "browsing here...\n";
                
        JTextArea contentPanel = new JTextArea(text, 10, 60);
                
        viewFrame.getContentPane().add(contentPanel);
        viewFrame.pack();
        viewFrame.setVisible(true);
    }
}

