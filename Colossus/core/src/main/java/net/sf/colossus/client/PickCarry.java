package net.sf.colossus.client;


import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;

import net.sf.colossus.util.KDialog;


/** Chooses between multiple carry options.
 *  @version $Id$
 *  @author David Ripton
 */
final class PickCarry extends KDialog implements ActionListener
{
    private Client client;
    private static final String cancel = "Decline carry";
    private SaveWindow saveWindow;

    /** Each choice is a String of form "Warbear in Plains Hex G3" */
    PickCarry(JFrame parentFrame, Client client, int carryDamage, Set choices)
    {
        super(parentFrame, "Apply " + carryDamage
            + (carryDamage == 1 ? "carry to:" : " carries to:"), false);

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
        addWindowListener(new WindowAdapter()
        {
            // @todo: this could probably be done by using 
            // setDefaultCloseOperation()
        });

        pack();
        saveWindow = new SaveWindow(client, "PickCarry");
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
        saveWindow.saveLocation(getLocation());
        dispose();
    }
}
