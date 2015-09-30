package net.sf.colossus.webclient;


import java.awt.Container;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import net.sf.colossus.common.Options;
import net.sf.colossus.guiutil.KDialog;


/**
 * THIS IS NOT USED!!
 *
 * I started to work on this but later found it is really clumsy;
 * moved the two checkboxes back to serverTab, and the chat yellow
 * checkbox I instead move to the game client preferences; it's somehow
 * more logical that way... I think.
 *
 * TODO:  if it really stays unused, should one day cleanup and remove
 *        all this dead stuff.
 *
 * @author Clemens Katzer
 */

public class PreferencesDialog extends KDialog
{
    private static final Logger LOGGER = Logger
        .getLogger(PreferencesDialog.class.getName());


    public final static String ChatBgYellowCBText = "Chat-background yellow";

    private final Options options;


    public PreferencesDialog(Frame owner, Options options)
    {
        super(owner, "Preferences", false);
        // TODO Auto-generated constructor stub

        this.options = options;

        setupGUI();
        LOGGER.info("WebClient Preferences Dialog instantiated.");
    }

    private void setupGUI()
    {
        Container contentPane = getContentPane();

        /*
        ActionListener cbActionListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                handleAction(e);
            }
        };
        */

        JPanel preferencesPanel = new JPanel(new GridLayout(0, 2));
        preferencesPanel.setBorder(new TitledBorder("Preferences"));

        /*
        boolean cby = this.options.getOption(ChatBgYellowCBText);
        JCheckBox ChatYellowCB = new JCheckBox(ChatBgYellowCBText, cby);
        autoGamePaneCB.addActionListener(cbActionListener);
        preferencesPanel.add(ChatYellowCB);
        preferencesPanel.add(new JLabel(""));
        */
        contentPane.add(preferencesPanel);

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                setVisible(false);
            }
        });

        pack();
        useSaveWindow(options, "WebClientPreferences", null);
        setVisible(false);
        repaint();
    }

    /*
    private void handleAction(ActionEvent e)
    {
        String text = e.getActionCommand();
        Object component = e.getSource();
        if (component instanceof JCheckBox)
        {
            JCheckBox cb = (JCheckBox)component;
            options.setOption(text, cb.isSelected());
        }
        else
        {
            LOGGER.severe("Event source with text " + text
                + " is not a checkbox??");
        }
    }
    */

    public void toggleVisible()
    {
        setVisible(!isVisible());
    }
}
