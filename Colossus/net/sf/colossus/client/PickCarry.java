package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.util.KDialog;


/** Chooses between multiple carry options.
 *  @version $Id$
 *  @author David Ripton
 */
final class PickCarry extends KDialog implements ActionListener
{
    private Client client;
    private static final String cancel = "Decline carry";


    /** Each choice is a String of form "Warbear in Plains Hex G3" */
    PickCarry(JFrame parentFrame, Client client, int carryDamage, 
        Set choices)
    {
        super(parentFrame, "Apply " + carryDamage + 
            (carryDamage == 1 ? "carry to:" : " carries to:"), true);

        this.client = client;

        getContentPane().setLayout(new GridLayout(choices.size() + 1, 1));

        addButton(cancel);

        Iterator it = choices.iterator();
        while (it.hasNext())
        {
            String choice = (String)it.next();
            addButton(choice);
        }

        // Don't allow exiting without making a choice, or the game will hang.
        addWindowListener(new WindowAdapter() {} );

        pack();
        centerOnScreen();
        setVisible(true);
    }

    private void addButton(String text)
    {
        JButton button = new JButton(text);
        button.addActionListener(this);
        getContentPane().add(button);
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals(cancel))
        {
            client.leaveCarryMode();
        }
        else
        {
            String desc = e.getActionCommand();
            String targetHex = desc.substring(desc.length() - 2);
            client.applyCarries(targetHex);
        }
        dispose();
    }
}
