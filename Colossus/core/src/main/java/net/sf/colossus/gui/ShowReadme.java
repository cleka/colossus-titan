package net.sf.colossus.gui;


import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.Document;

import net.sf.colossus.guiutil.KFrame;
import net.sf.colossus.util.StaticResourceLoader;
import net.sf.colossus.variant.Variant;


/**
 * Provides a JScrollPane to display the Variant README,
 * either within GetPlayer selection tab,
 * or from main boards help (then in own KFrame).
 *
 * @author Clemens Katzer
 */
public final class ShowReadme extends KFrame
{
    private JEditorPane myReadme;

    ShowReadme(Variant variant)
    {
        super("ShowReadme");

        String variantName = variant.getName();
        Document doc = variant.getReadme();

        String title = "README for variant " + variantName;
        setTitle(title);

        // KFrame does the registration:
        net.sf.colossus.util.InstanceTracker.setId(this, title);

        myReadme = new JEditorPane();
        JScrollPane content = readmeContentScrollPane(myReadme, doc);
        getContentPane().add(content);
        pack();
        setVisible(true);
    }

    @Override
    public void dispose()
    {
        if (myReadme != null)
        {
            myReadme.getParent().remove(myReadme);
            myReadme = null;
        }
        super.dispose();
    }

    /**
     * Return a scrollable pane that displays the Readme.
     * Also used by GetPlayer.java
     */
    public static final JScrollPane readmeContentScrollPane(
        JEditorPane readme, Document doc)
    {
        JPanel readmePane = new JPanel();
        JScrollPane readmeScrollPane = new JScrollPane(readmePane,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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

        readme.setContentType((String)doc
            .getProperty(StaticResourceLoader.KEY_CONTENT_TYPE));
        readme.setDocument(doc);

        return readmeScrollPane;
    }
}
