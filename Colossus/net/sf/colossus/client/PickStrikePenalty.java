package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.util.KDialog;


/** Chooses between multiple strike penalty options.
 *  @version $Id$
 *  @author David Ripton
 */
final class PickStrikePenalty extends KDialog implements ActionListener
{
    private Client client;


    PickStrikePenalty(JFrame parentFrame, Client client, 
        java.util.List choices)
    {
        super(parentFrame, "Take strike penalty to carry?", true);

        this.client = client;

        getContentPane().setLayout(new GridLayout(choices.size(), 1));
        Iterator it = choices.iterator();
        while (it.hasNext())
        {
            String choice = (String)it.next();
            JButton button = new JButton(choice);
            button.addActionListener(this);
            getContentPane().add(button);
        }

        // Don't allow exiting without making a choice, or the game will hang.
        addWindowListener(new WindowAdapter() {} );

        pack();
        centerOnScreen();
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e)
    {
        client.assignStrikePenalty(e.getActionCommand());
        dispose();
    }
}
