package net.sf.colossus.client;


import javax.swing.JTextArea;

import net.sf.colossus.util.KFrame;


/**
 * Provides a simple JTextArea to display pointers to the Option 
 * documentation HTML files in Internet.
 * Eventually one day this should show the option documents
 * itself as an own JEditorPane, let user browse them etc...
 * 
 * @version $Id$
 * @author Clemens Katzer
 */




public final class ShowHelpDoc extends KFrame
{
    private static String title = "Pointer to Options documentation";

    ShowHelpDoc()
    {
        super(title);

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

        this.getContentPane().add(contentPanel);
        this.pack();
        this.setVisible(true);
    }
}
