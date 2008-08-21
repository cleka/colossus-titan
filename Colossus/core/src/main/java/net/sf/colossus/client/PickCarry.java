package net.sf.colossus.client;


import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
    private final Client client;
    private final Set<String> choices;
    private static final String cancel = "Decline carry";
    private final SaveWindow saveWindow;

    /** Each choice is a String of form "Warbear in Plains Hex G3" */
    PickCarry(JFrame parentFrame, Client client, int carryDamage,
        Set<String> choices)
    {
        super(parentFrame, "Apply " + carryDamage
            + (carryDamage == 1 ? " carry to:" : " carries to:"), false);

        this.client = client;
        this.choices = choices;

        getContentPane().setLayout(new GridLayout(choices.size() + 1, 1));

        addButton(cancel);

        Iterator<String> it = choices.iterator();
        while (it.hasNext())
        {
            String choice = it.next();
            addButton(choice);
        }

        // Don't allow exiting without making a choice, or the game will hang.
        addWindowListener(new WindowAdapter()
        {
            // @todo: this could probably be done by using 
            // setDefaultCloseOperation()
        });

        pack();
        saveWindow = new SaveWindow(client.getOptions(), "PickCarry");
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

    /**
     * @param hex String The short string denoting a hex on the 
     *                   battle map, eg. A1 or D5
     *
     * Client calls this when user clicked on a hex (or chit) 
     * instead of the dialog.
     * Check whether the clicked hex is a potential carry 
     * and if yes, return choice the description string
     */
    public String findCarryChoiceForHex(String hex)
    {
        for (String desc : choices)
        {
            String choiceHex = desc.substring(desc.length() - 2);
            if (choiceHex.equals(hex))
            {
                return desc;
            }
        }
        return null;
    }

    /**
     * Called by click on one one of the buttons
     * (from actionPerformed) or, from battleMap via Client,
     * if a chit was clicked which is a potential carry target.
     * @param desc String denoting a carry target choice
     * 
     */
    public void handleCarryToDescription(String desc)
    {
        if (desc.equals(cancel))
        {
            client.leaveCarryMode();
        }
        else
        {
            String targetHex = desc.substring(desc.length() - 2);
            client.applyCarries(targetHex);
        }
        saveWindow.saveLocation(getLocation());
        dispose();
    }
    
    public void actionPerformed(ActionEvent e)
    {
        String desc = e.getActionCommand();
        handleCarryToDescription(desc);
    }

    /* TODO This does not get called, neither if defined with event
     *      or without??
     *      The strange thing is, in PickStrikePenalty it is done
     *      *exactly the same way* and there it works...
     */
    // public void windowClosing()
    @Override
    public void windowClosing(WindowEvent e)
    {
        System.out.println("PickCarry windowClosing 1");
        client.leaveCarryMode();
    }
    public void windowClosing()
    {
        System.out.println("PickCarry windowClosing 2");
        client.leaveCarryMode();
    }

}
