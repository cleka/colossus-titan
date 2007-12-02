package net.sf.colossus.client;


import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.Document;

import net.sf.colossus.server.Constants;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.ResourceLoader;


/**
 * Provides a JScrollPane to display the Variant README,
 * either within GetPlayer selection tab, 
 * or from main boards help (then in own JFrame). 
 * @version $Id: ShowReadme.java 0000 2007-02-16 00:00:00Z cleka $
 * @author Clemens Katzer
 */


public final class ShowReadme
{
    private static JFrame viewFrame;

    ShowReadme(IOptions options)
    {
        // primitive way to avoid having more than one. How to do better?
        if ( viewFrame != null )
        {
            viewFrame.dispose();
            viewFrame = null;
        }

        String variantName = options.getStringOption(Options.variant);

        // XXX Make sure chosen variant is in the list.
        // XXX2 Same code in GetPlayer.java
        if (variantName == null || variantName.length() == 0)
        {
            // Default variant
            variantName = Constants.variantArray[0];
        }

        String title = new String("README for variant " + variantName);

        viewFrame = new JFrame(title);

        JEditorPane readme = new JEditorPane();

        JScrollPane content = readmeContentScrollPane(readme, variantName);
        viewFrame.getContentPane().add(content);
        viewFrame.pack();
        viewFrame.setVisible(true);
    }

    /**
     * Return a scrollable pane that displays the Readme.
     * Also used by GetPlayer.java
     */
    public static final JScrollPane readmeContentScrollPane(
        JEditorPane readme, String variantName)
    {
        JPanel readmePane = new JPanel();
        JScrollPane readmeScrollPane = new JScrollPane(readmePane,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        readmePane.setLayout(new GridLayout(0, 1));
        readme.setEditable(false);
        // Must be tall enough for biggest variant readme file.
        // TODO: proper resizing according to need?
        Dimension readmeMaxSize = new Dimension(580, 2200);
        Dimension readmePrefSize = new Dimension(580, 2200);
        Dimension readmeScrollMaxSize = new Dimension(600, 2200);
        Dimension readmeScrollPrefSize = new Dimension(600, 500);
        readmePane.setMaximumSize(readmeMaxSize);
        readmePane.setPreferredSize(readmePrefSize);
        readmeScrollPane.setMaximumSize(readmeScrollMaxSize);
        readmeScrollPane.setPreferredSize(readmeScrollPrefSize);
        readmePane.add(readme);

        Document doc = VariantSupport.loadVariant(variantName, true);
        readme.setContentType((String)doc.getProperty(
            ResourceLoader.keyContentType));
        readme.setDocument(doc);

        return readmeScrollPane;
    }
}

