package net.sf.colossus.gui;


import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;

import net.sf.colossus.util.KDialog;
import net.sf.colossus.client.Client;
import net.sf.colossus.server.Constants;


/** Chooses between multiple strike penalty options.
 *  @version $Id$
 *  @author David Ripton
 */
final class PickStrikePenalty extends KDialog implements ActionListener
{
    private final Client client;
    private final SaveWindow saveWindow;

    PickStrikePenalty(JFrame parentFrame, Client client, List<String> choices)
    {
        super(parentFrame, "Take strike penalty to carry?", true);

        this.client = client;

        choices.add(Constants.cancelStrike);

        getContentPane().setLayout(new GridLayout(choices.size(), 1));
        Iterator<String> it = choices.iterator();
        while (it.hasNext())
        {
            String choice = it.next();
            JButton button = new JButton(choice);
            button.addActionListener(this);
            getContentPane().add(button);
        }

        // Don't allow exiting without making a choice, or the game will hang.
        addWindowListener(new WindowAdapter()
        {
            // @todo: this could probably be done by using 
            // setDefaultCloseOperation()
        });

        pack();
        saveWindow = new SaveWindow(client.getOptions(), "PickStrikePenalty");
        Point location = saveWindow.loadLocation();
        if (location == null)
        {
            centerOnScreen();
        }
        else
        {
            setLocation(location);
        }
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e)
    {
        client.assignStrikePenalty(e.getActionCommand());
        saveWindow.saveLocation(getLocation());
        dispose();
    }

    public void windowClosing()
    {
        client.assignStrikePenalty(Constants.cancelStrike);
        // saveWindow.saveLocation(getLocation());
        dispose();
    }
}
