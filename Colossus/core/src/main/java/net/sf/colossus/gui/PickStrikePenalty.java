package net.sf.colossus.gui;


import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;

import net.sf.colossus.common.Constants;
import net.sf.colossus.guiutil.KDialog;


/**
 * Chooses between multiple strike penalty options.
 *
 * @author David Ripton
 */
final class PickStrikePenalty extends KDialog
{
    private final ClientGUI gui;

    PickStrikePenalty(JFrame parentFrame, ClientGUI gui, List<String> choices)
    {
        super(parentFrame, "Take strike penalty to carry?", true);

        this.gui = gui;

        choices.add(Constants.cancelStrike);

        getContentPane().setLayout(new GridLayout(choices.size(), 1));
        Iterator<String> it = choices.iterator();
        while (it.hasNext())
        {
            String choice = it.next();
            JButton button = new JButton(choice);
            button.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    // TODO via some other way than getClient() ?
                    PickStrikePenalty.this.gui.getClient()
                        .assignStrikePenalty(e.getActionCommand());
                    dispose();
                }
            });

            getContentPane().add(button);
        }

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                // TODO via some other way than getClient() ?
                PickStrikePenalty.this.gui.getClient().assignStrikePenalty(
                    Constants.cancelStrike);
                dispose();
            }
        });

        pack();
        // useSaveWindow remembers and restores both size and location
        // Size should be based on needed size, so re-setSize() it afterwards
        // again:
        Dimension preferredSize = getPreferredSize();
        useSaveWindow(gui.getOptions(), "PickStrikePenalty", null);
        setSize(preferredSize);
        setVisible(true);
    }
}
