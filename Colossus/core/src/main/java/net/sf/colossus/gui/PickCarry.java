package net.sf.colossus.gui;


import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;

import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.guiutil.SaveWindow;
import net.sf.colossus.variant.BattleHex;


/**
 * Chooses between multiple carry options.
 *
 * @author David Ripton
 */
@SuppressWarnings("serial")
final class PickCarry extends KDialog
{
    private final ClientGUI gui;
    private final Set<String> choices;
    private static final String cancel = "Decline carry";
    private final SaveWindow saveWindow;

    /** Each choice is a String of form "Warbear in Plains Hex G3" */
    PickCarry(JFrame parentFrame, ClientGUI clientGui, int carryDamage,
        Set<String> choices)
    {
        super(parentFrame, "Apply " + carryDamage
            + (carryDamage == 1 ? " carry to:" : " carries to:"), false);

        this.gui = clientGui;
        this.choices = choices;

        getContentPane().setLayout(new GridLayout(choices.size() + 1, 1));

        addButton(cancel);

        for (String choice : choices)
        {
            addButton(choice);
        }

        // client disposes us during leaveCarryMode()
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // Don't allow exiting without making a choice, or the game will hang
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                PickCarry.this.gui.getCallbackHandler().leaveCarryMode();
            }
        });

        pack();
        saveWindow = new SaveWindow(gui.getOptions(), "PickCarry");
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
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String desc = e.getActionCommand();
                handleCarryToDescription(desc);
            }
        });
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
     *
     * @param desc String denoting a carry target choice
     */
    public void handleCarryToDescription(String desc)
    {
        if (desc.equals(cancel))
        {
            gui.getCallbackHandler().leaveCarryMode();
        }
        else
        {
            String targetHexLabel = desc.substring(desc.length() - 2);
            BattleHex targetHex = gui.getGame().getBattleSite().getTerrain()
                .getHexByLabel(targetHexLabel);
            gui.getCallbackHandler().applyCarries(targetHex);
        }
        saveWindow.saveLocation(getLocation());
        gui.disposePickCarryDialog();
    }

}
