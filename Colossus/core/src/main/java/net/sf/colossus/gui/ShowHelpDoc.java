package net.sf.colossus.gui;


import javax.swing.JTextArea;

import net.sf.colossus.guiutil.KFrame;


/**
 * Provides a simple JTextArea to display pointers to the Option
 * documentation HTML files in Internet.
 *
 * Eventually one day this should show the option documents
 * itself as an own JEditorPane, let user browse them etc...
 *
 * @author Clemens Katzer
 */
public final class ShowHelpDoc extends KFrame
{
    private static String title = "Pointer to Options documentation";

    ShowHelpDoc()
    {
        super(title);

        String text = "\n"
            + "  Help on the options is not built-in into Colossus yet, "
            + "but available from the Colossus home page."
            + "\n\n"
            + "  Documentation for server side options "
            + "(Player Selection dialog) can be found from page: \n"
            + "     http://colossus.sourceforge.net/docs/GetPlayersOptions.html"
            + "\n\n"
            + "  Documentation for client side features (MasterBoard menu bar) "
            + "can be found from page: \n"
            + "     http://colossus.sourceforge.net/docs/ClientMenuBar.html"
            + "\n\n"
            + "  Documentation for client side Preferences Window "
            + "(Window-Menu => Preferences) can be found from page: \n"
            + "     http://colossus.sourceforge.net/docs/ClientPreferences.html"
            + "\n\n"
            + "  Documentation for the Battle Map Window (features and usage) "
            + "can be found from page: \n"
            + "     http://colossus.sourceforge.net/docs/BattleFeatures.html"
            + "\n\n"
            + "  Documentation for the so-called \"WebClient\" can be found "
            + "from page: \n"
            + "     http://colossus.sourceforge.net/docs/WebClient.html"
            + "\n\n"
            + "  Some more general information about the Colossus Public Game Server "
            + "can be found from page: \n"
            + "     http://colossus.sourceforge.net/docs/"
            + "Colossus-Public-Game-Server.html" + "\n\n"
            + "  Eventually one day this may become a proper help "
            + "browsing here...\n\n\n";

        JTextArea contentPanel = new JTextArea(text, 16, 60);

        this.getContentPane().add(contentPanel);
        this.pack();
        this.setVisible(true);
    }
}
